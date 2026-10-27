package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeBombSimulationTest {
    private fun stage(wallColumn: Int = -1) = StageData(
        id = 0, image = "assets/maps/map_000.png", width = 128, height = 64,
        obstacleX = 0, obstacleY = 0,
        grid = List(8) { IntArray(16) { col -> if (col == wallColumn) 751 else 0 } },
        spawns = listOf(Spawn(24.0, 24.0)),
    )

    @Test fun placementUses16PixelCellsOwnerLimitsAndRejectsOverlap() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.place(Actor(23.0, 23.0)))
        assertEquals(24.0, sim.bombs.single().x, 0.0)
        assertFalse(sim.place(Actor(40.0, 24.0))) // same owner, one active bomb
        assertFalse(sim.place(Actor(23.0, 23.0), "player2")) // occupied tile
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        assertEquals(2, sim.bombs.size)
        assertThrows(IllegalArgumentException::class.java) { sim.place(Actor(24.0, 24.0), "unknown") }
    }

    @Test fun fuseDetonatesAt96FixedTicksAndFlamesExpire() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.place(Actor(24.0, 24.0)))
        repeat(95) { assertEquals(0, sim.tick()) }
        assertEquals(1, sim.tick())
        assertTrue(sim.bombs.isEmpty())
        assertTrue(sim.isBurning(24.0, 24.0))
        assertTrue(sim.isBurning(56.0, 24.0))
        assertFalse(sim.isBurning(120.0, 56.0))
        repeat(27) { sim.tick() }
        assertFalse(sim.isBurning(24.0, 24.0))
    }

    @Test fun chainReactionDetonatesLaterPlacedBombImmediately() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.place(Actor(24.0, 24.0)))
        repeat(48) { sim.tick() }
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        repeat(47) { assertEquals(0, sim.tick()) }
        assertEquals(2, sim.tick())
        assertTrue(sim.bombs.isEmpty())
        assertEquals(2, sim.flames.size)
    }

    @Test fun obstacleStopsBlastBeforeNextBomb() {
        val sim = NativeBombSimulation(stage(wallColumn = 8)) // x = 64..71; blocks center 72
        assertTrue(sim.place(Actor(24.0, 24.0)))
        repeat(48) { sim.tick() }
        assertTrue(sim.place(Actor(88.0, 24.0), "player2"))
        repeat(47) { sim.tick() }
        assertEquals(1, sim.tick())
        assertEquals(1, sim.bombs.size)
        assertFalse(sim.isBurning(88.0, 24.0))
    }

    @Test fun blastRayCannotSkipThinWallOnOffsetObstacleGrid() {
        // Wall occupies x=28..35, between bomb-cell centers x=24 and x=40.
        // Checking only the next 16px endpoint incorrectly lets flame pass.
        val offsetStage = StageData(
            id = 0, image = "assets/maps/map_000.png", width = 128, height = 64,
            obstacleX = 4, obstacleY = 0,
            grid = List(8) { IntArray(16) { col -> if (col == 3) 751 else 0 } },
            spawns = listOf(Spawn(24.0, 24.0)),
        )
        val sim = NativeBombSimulation(offsetStage)
        assertTrue(sim.place(Actor(24.0, 24.0)))
        repeat(96) { sim.tick() }
        assertTrue(sim.isBurning(24.0, 24.0))
        assertFalse(sim.isBurning(40.0, 24.0))
    }

    @Test fun blastRayStillTraversesClearOffsetObstacleGrid() {
        val offsetStage = StageData(
            id = 0, image = "assets/maps/map_000.png", width = 128, height = 64,
            obstacleX = 4, obstacleY = 0,
            grid = List(8) { IntArray(16) },
            spawns = listOf(Spawn(24.0, 24.0)),
        )
        val sim = NativeBombSimulation(offsetStage)
        assertTrue(sim.place(Actor(24.0, 24.0)))
        repeat(96) { sim.tick() }
        assertTrue(sim.isBurning(40.0, 24.0))
    }

    @Test fun invalidRulesAreRejectedBeforeSimulation() {
        assertThrows(IllegalArgumentException::class.java) { NativeBombSimulation(stage(), bombLimit = 0) }
        assertThrows(IllegalArgumentException::class.java) { NativeBombSimulation(stage(), bombFuseSeconds = Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { NativeBombSimulation(stage(), bombRange = 99) }
        assertThrows(IllegalArgumentException::class.java) { NativeBombSimulation(stage(), blastLifetimeSeconds = 0.0) }
    }
    @Test fun edgeOfActorHitboxTouchesLastFlameCell() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.place(Actor(24.0, 24.0)))
        repeat(96) { sim.tick() }
        // The last eastward flame is centered at x=72. Actor at x=84
        // overlaps its 8px flame extent with a 5px actor radius, but its
        // center is outside the flame. This is provisional JS parity only.
        assertFalse(sim.isBurning(84.0, 24.0, radius = 0.0))
        assertTrue(sim.isBurning(84.0, 24.0, radius = StageCollision.ACTOR_RADIUS))
        assertFalse(sim.isBurning(85.0, 24.0, radius = StageCollision.ACTOR_RADIUS))
    }

    @Test fun bombPlacedIntoExistingFlameChainsOnNextTick() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.place(Actor(24.0, 24.0)))
        repeat(96) { sim.tick() }
        assertTrue(sim.isBurning(56.0, 24.0))
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        assertEquals(1, sim.tick())
        assertTrue(sim.bombs.isEmpty())
        assertEquals(2, sim.flames.size)
    }

    @Test fun remoteBombCannotSurviveActiveFlameEvenWithoutFuse() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.place(Actor(24.0, 24.0)))
        repeat(96) { sim.tick() }
        assertTrue(sim.setRemoteDetonator("player2", true))
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        assertTrue(sim.bombs.single().remote)
        assertNull(sim.bombs.single().remaining)
        assertEquals(1, sim.tick())
        assertTrue(sim.bombs.isEmpty())
    }

    @Test fun flameInItsFinalTickStillIgnitesNewlyPlacedBomb() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.place(Actor(24.0, 24.0)))
        repeat(96) { sim.tick() }
        repeat(26) { sim.tick() }
        assertTrue(sim.isBurning(56.0, 24.0))
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        assertEquals(1, sim.tick())
        assertTrue(sim.bombs.isEmpty())
    }


    @Test fun imminentTimedFootprintExcludesRemoteAndValidatesWindow() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(24.0, 24.0), "player"))
        repeat(120) { sim.tick() }
        assertNull(sim.bombs.single().remaining)
        assertTrue(sim.predictedImminentTimedBlastCells(20.0).isEmpty())
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        assertTrue(sim.predictedImminentTimedBlastCells(0.75).isEmpty())
        repeat(60) { sim.tick() }
        val threat = sim.predictedImminentTimedBlastCells(0.75)
        assertTrue(threat.any { NativeBombSimulation.blastHits(it, 56.0, 24.0) })
        assertThrows(IllegalArgumentException::class.java) {
            sim.predictedImminentTimedBlastCells(Double.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            sim.predictedImminentTimedBlastCells(-1.0)
        }
    }

    @Test fun timedThreatSnapshotMatchesLegacyViewsWithoutMutatingSimulation() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(24.0, 24.0), "player"))
        // This timed bomb's ray reaches and schedules the remote bomb.
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        val bombsBefore = sim.bombs
        val flamesBefore = sim.flames
        val nextIdBefore = sim.nextBombId

        val snapshot = sim.predictedTimedThreatSnapshot(2.0)

        assertEquals(sim.predictedTimedChainThreats(2.0), snapshot.threats)
        val legacyCells = sim.predictedTimedBlastCells(2.0)
        assertEquals(legacyCells.map { it.cell }.toSet(), snapshot.cells.map { it.cell }.toSet())
        assertEquals(snapshot.cells.size, snapshot.cells.map { it.cell }.toSet().size)
        assertTrue("overlapping bomb rays should be coalesced", legacyCells.size > snapshot.cells.size)
        snapshot.cells.forEach { cell ->
            val earliest = legacyCells.filter { it.cell == cell.cell }.minOf { it.secondsUntilBlast }
            assertEquals(earliest, cell.secondsUntilBlast, 0.0)
        }
        assertTrue(snapshot.threats.any { it.bomb.owner == "player" && it.bomb.remote })
        assertTrue(snapshot.threats.any { it.bomb.owner == "player2" && !it.bomb.remote })
        assertEquals(bombsBefore, sim.bombs)
        assertEquals(flamesBefore, sim.flames)
        assertEquals(nextIdBefore, sim.nextBombId)
    }


    @Test fun bombIdentityExhaustionFailsBeforeMutatingActorOrBombList() {
        val sim = NativeBombSimulation(stage(), initialBombId = Long.MAX_VALUE)
        val actor = Actor(24.0, 24.0)
        val before = sim.bombs

        val failure = assertThrows(IllegalStateException::class.java) {
            sim.place(actor)
        }

        assertEquals("Bomb ID counter exhausted", failure.message)
        assertEquals(before, sim.bombs)
        assertNull(actor.passBomb)
        assertEquals(Long.MAX_VALUE, sim.nextBombId)
    }

    @Test fun bombSimulationRejectsNonPositiveInitialIdentity() {
        for (initialId in listOf(0L, -1L)) {
            assertThrows(IllegalArgumentException::class.java) {
                NativeBombSimulation(stage(), initialBombId = initialId)
            }
        }
    }
}
