package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/**
 * N14: an escape route must not pass through a bomb ray after its predicted
 * ignition deadline merely because the endpoint is outside all blast rays.
 * All native AI timings remain provisional; EKA2L1 parity is NOT established.
 */
class NativeBotTimedRouteTest {
    // Only the right-hand corridor reaches a refuge from the proposed bomb.
    // A second bomb's vertical ray cuts across that corridor, while the bot
    // starts diagonally outside that second ray.
    private fun corridor() = StageData(
        id = 0, image = "test.png", width = 128, height = 96,
        obstacleX = 0, obstacleY = 0,
        grid = List(12) { row ->
            IntArray(16) { col -> if (row in 2..7 && col in 4..15) 0 else 751 }
        },
        spawns = listOf(Spawn(40.0, 40.0), Spawn(56.0, 24.0)),
    )

    private val bot = Actor(40.0, 40.0)

    @Test fun imminentTimedRayCannotBeCrossedToReachSafeEndpoint() {
        val arena = corridor()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.place(Actor(56.0, 24.0), "player"))
        repeat(90) { assertEquals(0, sim.tick()) }
        assertFalse(sim.wouldBlastHitFrom(56.0, 24.0, bot.x, bot.y))
        assertTrue(sim.wouldBlastHitFrom(40.0, 40.0, bot.x, bot.y))
        assertNull(
            "The only exit crosses a timed vertical ray after its deadline",
            NativeBotEscapeRoute.firstDirection(arena, bot, sim, 40.0, 40.0),
        )
    }

    @Test fun distantFuseAllowsEarlyCrossingBeforeBlast() {
        val arena = corridor()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.place(Actor(56.0, 24.0), "player"))
        val route = NativeBotEscapeRoute.firstDirection(arena, bot, sim, 40.0, 40.0)
        assertEquals("The ray is not yet imminent and the exit is outside both blasts", 1, route)
    }

    @Test fun standaloneRemoteRayHasNoFabricatedAutomaticDeadline() {
        val arena = corridor()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(56.0, 24.0), "player"))
        assertTrue(sim.predictedTimedBlastCells(20.0).isEmpty())
        assertEquals(1, NativeBotEscapeRoute.firstDirection(arena, bot, sim, 40.0, 40.0))
    }

    @Test fun remoteChainInheritsTimedRayDeadlineOnEscapeRoute() {
        val arena = corridor()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(56.0, 24.0), "player"))
        assertTrue(sim.place(Actor(88.0, 24.0), "player2"))
        repeat(90) { assertEquals(0, sim.tick()) }
        val chain = sim.predictedTimedChainThreats(0.2)
        assertEquals(2, chain.size)
        assertTrue(chain.any { it.bomb.remote })
        assertNull(NativeBotEscapeRoute.firstDirection(arena, bot, sim, 40.0, 40.0))
    }

    @Test fun timestampedFootprintsPreserveDeadlinesAndValidateHorizon() {
        val arena = corridor()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.place(Actor(56.0, 24.0), "player"))
        assertTrue(sim.predictedTimedBlastCells(0.75).isEmpty())
        repeat(60) { sim.tick() }
        val cells = sim.predictedTimedBlastCells(0.75)
        assertTrue(cells.isNotEmpty())
        assertTrue(cells.all { it.secondsUntilBlast in 0.59..0.61 })
        assertTrue(cells.any {
            NativeBombSimulation.blastHits(it.cell, 56.0, 40.0, StageCollision.ACTOR_RADIUS)
        })
        assertThrows(IllegalArgumentException::class.java) {
            sim.predictedTimedBlastCells(Double.NaN)
        }
    }

    private fun openArena() = StageData(
        id = 0, image = "assets/maps/map_000.png", width = 160, height = 96,
        obstacleX = 0, obstacleY = 0,
        grid = List(12) { IntArray(20) },
        spawns = listOf(Spawn(24.0, 24.0), Spawn(56.0, 24.0)),
    )

    @Test fun hypotheticalShortFuseRejectsRouteThatCrossesItsOwnBlastAfterDeadline() {
        val arena = openArena()
        val bot = Actor(24.0, 24.0)
        val sim = NativeBombSimulation(arena, bombFuseSeconds = 0.2)

        assertNull(
            "No safe route fits before the hypothetical bomb's 0.2s deadline",
            NativeBotEscapeRoute.firstDirection(arena, bot, sim, bot.x, bot.y),
        )
        assertTrue("Planning must not place a real bomb", sim.bombs.isEmpty())
        assertEquals("Planning must not consume a bomb ID", 1L, sim.nextBombId)
    }

    @Test fun hypotheticalNormalFuseKeepsAReachableRefuge() {
        val arena = openArena()
        val bot = Actor(24.0, 24.0)
        val sim = NativeBombSimulation(arena, bombFuseSeconds = 1.6)

        assertEquals(1, NativeBotEscapeRoute.firstDirection(arena, bot, sim, bot.x, bot.y))
        assertTrue(sim.bombs.isEmpty())
        assertEquals(1L, sim.nextBombId)
    }

    @Test fun hypotheticalTimedBombPropagatesDeadlineToRemoteBombWithoutMutatingState() {
        val arena = openArena()
        val sim = NativeBombSimulation(arena, bombFuseSeconds = 0.2)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(56.0, 24.0), "player"))
        val before = sim.bombs
        val nextIdBefore = sim.nextBombId

        val predicted = sim.predictedPlacementTimedBlastCells(24.0, 24.0, 0.5)

        assertTrue(
            "The proposed blast should chain the remote bomb at the same deadline",
            predicted.any {
                it.cell == Spawn(56.0, 24.0) &&
                    kotlin.math.abs(it.secondsUntilBlast - 0.2) < 1e-9
            },
        )
        assertEquals(before, sim.bombs)
        assertEquals(nextIdBefore, sim.nextBombId)
        assertTrue(sim.hasRemoteDetonator("player"))
    }

}
