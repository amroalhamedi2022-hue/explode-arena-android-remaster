package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/** N25: shared per-tick context must preserve each bot's existing decisions. */
class NativeBotThreatContextTest {
    private fun arena() = StageData(
        id = 14, image = "assets/maps/map_014.png", width = 160, height = 160,
        obstacleX = 0, obstacleY = 0, grid = List(20) { IntArray(20) },
        spawns = listOf(
            Spawn(24.0, 24.0), Spawn(120.0, 24.0),
            Spawn(24.0, 120.0), Spawn(120.0, 120.0),
        ),
    )

    @Test fun sharedSnapshotAndIndexMatchLinearOracleWithoutMutatingSimulation() {
        val stage = arena()
        val simulation = NativeBombSimulation(stage)
        assertTrue(simulation.setRemoteDetonator("player", true))
        assertTrue(simulation.place(Actor(56.0, 24.0), "player"))
        assertTrue(simulation.place(Actor(24.0, 24.0), "player2"))
        repeat(60) { simulation.tick() }

        val bombsBefore = simulation.bombs.map { it.copy() }
        val flamesBefore = simulation.flames.map { it.copy(cells = it.cells.toList()) }
        val nextIdBefore = simulation.nextBombId
        val remoteOwnersBefore = simulation.remoteDetonatorOwners
        val expected = simulation.predictedTimedThreatSnapshot(0.75)
        val context = NativeBotThreatContext.create(simulation)

        assertEquals(expected, context.snapshot)
        assertEquals(context.snapshot.cells.map { it.cell }, context.cells)
        for (x in -2..162 step 2) for (y in -2..162 step 2) {
            val expectedHit = context.cells.any { cell ->
                NativeBombSimulation.blastHits(
                    cell, x.toDouble(), y.toDouble(), StageCollision.ACTOR_RADIUS,
                )
            }
            assertEquals("indexed/linear mismatch at ($x,$y)", expectedHit,
                context.imminentIndex.contains(x.toDouble(), y.toDouble(), StageCollision.ACTOR_RADIUS))
        }

        assertEquals(bombsBefore, simulation.bombs)
        assertEquals(flamesBefore, simulation.flames)
        assertEquals(nextIdBefore, simulation.nextBombId)
        assertEquals(remoteOwnersBefore, simulation.remoteDetonatorOwners)
    }

    @Test fun sharedThreatFootprintsMatchStandaloneOracleAcrossWallAndHitboxBoundaries() {
        val wallStage = arena().copy(
            grid = List(20) { row ->
                IntArray(20) { col -> if (col == 8 && row in 2..17) 1 else 0 }
            },
        )
        val simulation = NativeBombSimulation(wallStage)
        assertTrue(simulation.place(Actor(56.0, 24.0), "player"))
        assertTrue(simulation.place(Actor(56.0, 120.0), "player2"))
        repeat(60) { simulation.tick() }

        val context = NativeBotThreatContext.create(simulation)
        assertEquals(context.snapshot.threats, context.threatFootprints.map { it.threat })
        assertEquals(2, context.threatFootprints.size)

        // The cached footprint must be observationally identical to the
        // original wouldBlastHitFrom calculation, including strict hitbox
        // edges and rays stopped by an offset 8px obstacle grid.
        for (footprint in context.threatFootprints) {
            for (x in -4..164 step 4) for (y in -4..164 step 4) {
                val expected = simulation.wouldBlastHitFrom(
                    footprint.threat.bomb.x,
                    footprint.threat.bomb.y,
                    x.toDouble(),
                    y.toDouble(),
                    StageCollision.ACTOR_RADIUS,
                )
                val actual = footprint.cells.any { cell ->
                    NativeBombSimulation.blastHits(
                        cell, x.toDouble(), y.toDouble(), StageCollision.ACTOR_RADIUS,
                    )
                }
                assertEquals(
                    "cached footprint mismatch for ${footprint.threat.bomb.owner} at ($x,$y)",
                    expected, actual,
                )
            }
        }
    }

    @Test fun freshContextReflectsFuseChangesInsteadOfCachingAcrossTicks() {
        val stage = arena()
        val simulation = NativeBombSimulation(stage)
        assertTrue(simulation.place(Actor(56.0, 24.0), "player"))
        val beforeFuseWindow = NativeBotThreatContext.create(simulation)
        assertTrue(beforeFuseWindow.cells.isEmpty())

        repeat(60) { simulation.tick() }
        val insideFuseWindow = NativeBotThreatContext.create(simulation)
        assertTrue(insideFuseWindow.cells.isNotEmpty())
        assertTrue("Old context must remain an immutable earlier snapshot", beforeFuseWindow.cells.isEmpty())
    }

    @Test fun sharedContextPreservesStandaloneDecisionForEveryBot() {
        val stage = arena()
        val player = Actor(24.0, 24.0)
        val roster = NativeOpponentRoster.create(stage, player)
        val simulation = NativeBombSimulation(stage)
        assertTrue(simulation.setRemoteDetonator("player", true))
        assertTrue(simulation.place(Actor(56.0, 24.0), "player"))
        assertTrue(simulation.place(Actor(56.0, 120.0), "player2"))
        repeat(60) { simulation.tick() }

        val context = NativeBotThreatContext.create(simulation)
        val bombsBefore = simulation.bombs.map { it.copy() }
        val flamesBefore = simulation.flames.map { it.copy(cells = it.cells.toList()) }
        val nextIdBefore = simulation.nextBombId
        val remoteOwnersBefore = simulation.remoteDetonatorOwners
        val playerSnapshot = Actor(player.x, player.y, player.passBomb)

        for (slot in roster.bots) {
            val sharedBot = Actor(slot.actor.x, slot.actor.y, slot.actor.passBomb)
            val standaloneBot = Actor(slot.actor.x, slot.actor.y, slot.actor.passBomb)
            val blockers = listOf(playerSnapshot) + roster.bots
                .filter { it !== slot }
                .map { Actor(it.actor.x, it.actor.y, it.actor.passBomb) }
            val sharedDecision = NativeBotController(slot.owner).decideWithContext(
                stage, sharedBot, playerSnapshot, simulation, 61, blockers, context,
            )
            val standaloneDecision = NativeBotController(slot.owner).decide(
                stage, standaloneBot, playerSnapshot, simulation, 61, blockers,
            )
            assertEquals("shared context changed ${slot.owner} decision", standaloneDecision, sharedDecision)
            assertEquals(standaloneBot.x, sharedBot.x, 0.0)
            assertEquals(standaloneBot.y, sharedBot.y, 0.0)
        }

        assertEquals(bombsBefore, simulation.bombs)
        assertEquals(flamesBefore, simulation.flames)
        assertEquals(nextIdBefore, simulation.nextBombId)
        assertEquals(remoteOwnersBefore, simulation.remoteDetonatorOwners)
    }
    @Test fun rosterCreatesExactlyOneContextPerLivingTickAndNoneWhenAllBotsAreDefeated() {
        val stage = arena()
        val player = Actor(24.0, 24.0)
        val seed = NativeOpponentRoster.create(stage, player)
        var contextCreations = 0
        val roster = NativeOpponentRoster(seed.bots) { simulation ->
            contextCreations++
            NativeBotThreatContext.create(simulation)
        }
        val simulation = NativeBombSimulation(stage)

        roster.advance(stage, player, simulation, 0)
        assertEquals("one shared context serves every bot in the tick", 1, contextCreations)
        roster.advance(stage, player, simulation, 1)
        assertEquals("context is refreshed once on the next tick", 2, contextCreations)

        roster.bots.forEach { slot -> repeat(350) { slot.vitality.tick(burning = true) } }
        assertEquals(0, roster.advance(stage, player, simulation, 2))
        assertEquals("defeated bots need no planning context", 2, contextCreations)
    }

    @Test fun invalidContextHorizonFailsClosedWithoutMutatingSimulation() {
        val stage = arena()
        val simulation = NativeBombSimulation(stage)
        assertTrue(simulation.place(Actor(56.0, 24.0), "player"))
        val bombsBefore = simulation.bombs.map { it.copy() }
        val nextIdBefore = simulation.nextBombId
        for (invalid in listOf(Double.NaN, Double.POSITIVE_INFINITY, -0.01)) {
            assertThrows(IllegalArgumentException::class.java) {
                NativeBotThreatContext.create(simulation, invalid)
            }
        }
        assertEquals(bombsBefore, simulation.bombs)
        assertEquals(nextIdBefore, simulation.nextBombId)
    }

}
