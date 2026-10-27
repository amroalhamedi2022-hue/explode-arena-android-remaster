package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/** Regression against the native simulation's own chain-reaction rules. */
class NativeBotChainThreatTest {
    private fun stage(wallColumn: Int = -1) = StageData(
        id = 0, image = "test.png", width = 128, height = 64,
        obstacleX = 0, obstacleY = 0,
        grid = List(8) { IntArray(16) { col -> if (col == wallColumn) 751 else 0 } },
        spawns = listOf(Spawn(24.0, 24.0)),
    )

    @Test fun imminentTimedBombIgnitesRemoteAndExtendsThreatFootprint() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(56.0, 24.0), "player"))
        assertTrue(sim.place(Actor(24.0, 24.0), "player2"))
        assertTrue(sim.predictedTimedChainThreats(0.75).isEmpty())
        repeat(60) { sim.tick() }
        val threats = sim.predictedTimedChainThreats(0.75)
        assertEquals(2, threats.size)
        assertTrue(threats.any { it.bomb.remote && it.secondsUntilBlast in 0.59..0.61 })
        assertFalse(sim.wouldBlastHitFrom(24.0, 24.0, 104.0, 24.0))
        assertTrue(sim.predictedImminentTimedBlastCells(0.75).any {
            NativeBombSimulation.blastHits(it, 104.0, 24.0)
        })
        repeat(35) { assertEquals(0, sim.tick()) }
        assertEquals(2, sim.tick())
        assertTrue(sim.bombs.isEmpty())
    }

    @Test fun timedTriggerPropagatesAcrossTwoRemoteBombs() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.setRemoteDetonator("player2", true))
        assertTrue(sim.place(Actor(24.0, 24.0), "bot"))
        assertTrue(sim.place(Actor(56.0, 24.0), "player"))
        assertTrue(sim.place(Actor(104.0, 24.0), "player2"))
        repeat(60) { sim.tick() }
        val threats = sim.predictedTimedChainThreats(0.75)
        assertEquals(3, threats.size)
        assertEquals(2, threats.count { it.bomb.remote })
        assertTrue(threats.all { it.secondsUntilBlast in 0.59..0.61 })
    }

    @Test fun solidWallStopsTimedToRemoteChainPrediction() {
        val sim = NativeBombSimulation(stage(wallColumn = 8))
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(88.0, 24.0), "player"))
        assertTrue(sim.place(Actor(24.0, 24.0), "player2"))
        repeat(60) { sim.tick() }
        val threats = sim.predictedTimedChainThreats(0.75)
        assertEquals(1, threats.size)
        assertFalse(threats.single().bomb.remote)
        assertFalse(sim.predictedImminentTimedBlastCells(0.75).any {
            NativeBombSimulation.blastHits(it, 104.0, 24.0)
        })
    }

    @Test fun remoteBombsAloneNeverAcquireFictionalCountdown() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.setRemoteDetonator("player2", true))
        assertTrue(sim.place(Actor(24.0, 24.0), "player"))
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        repeat(120) { sim.tick() }
        assertTrue(sim.predictedTimedChainThreats(20.0).isEmpty())
        assertTrue(sim.predictedImminentTimedBlastCells(20.0).isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            sim.predictedTimedChainThreats(Double.NaN)
        }
    }

    @Test fun botEscapesRemoteRayIgnitedByOpponentTimedBomb() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(56.0, 24.0), "player"))
        assertTrue(sim.place(Actor(24.0, 24.0), "player2"))
        repeat(60) { sim.tick() }
        val bot = Actor(88.0, 24.0)
        assertFalse(sim.wouldBlastHitFrom(24.0, 24.0, bot.x, bot.y))
        assertTrue(sim.wouldBlastHitFrom(56.0, 24.0, bot.x, bot.y))
        val refuge = NativeBotEscapeRoute.firstDirection(arena, bot, sim, 56.0, 24.0)
        assertEquals(2, refuge)
        val decision = NativeBotController().decide(arena, bot, Actor(120.0, 24.0), sim, 60)
        assertEquals(0.0, decision.dx, 0.0)
        assertEquals(1.0, decision.dy, 0.0)
        assertFalse(decision.placeBomb)
    }

    @Test fun escapePlanIsThrottledButUnsafeCachedStepReplansImmediately() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(56.0, 24.0), "player"))
        assertTrue(sim.place(Actor(24.0, 24.0), "player2"))
        repeat(60) { sim.tick() }

        val bot = Actor(88.0, 24.0)
        val target = Actor(120.0, 24.0)
        val controller = NativeBotController()
        val first = controller.decide(arena, bot, target, sim, 60)
        assertEquals(60L, controller.lastPlannedTick)
        assertFalse(first.placeBomb)

        // A stable, still-safe escape direction is reused between 10Hz plans.
        assertEquals(first, controller.decide(arena, bot, target, sim, 61))
        controller.decide(arena, bot, target, sim, 65)
        assertEquals(60L, controller.lastPlannedTick)

        // The six-tick budget expires at 60Hz tick 66.
        controller.decide(arena, bot, target, sim, 66)
        assertEquals(66L, controller.lastPlannedTick)

        // This actor does not overlap the bot now, but blocks the cached next step.
        controller.decide(
            arena, bot, target, sim, 67,
            otherActors = listOf(Actor(88.0, 34.5)),
        )
        assertEquals(67L, controller.lastPlannedTick)
    }

    @Test fun chainedRemoteThreatUsesTimedFuseTravelDeadline() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(56.0, 24.0), "player"))
        assertTrue(sim.place(Actor(24.0, 24.0), "player2"))
        repeat(90) { sim.tick() }
        val bot = Actor(88.0, 24.0)
        assertNull(NativeBotEscapeRoute.firstDirection(arena, bot, sim, 56.0, 24.0))
    }
}
