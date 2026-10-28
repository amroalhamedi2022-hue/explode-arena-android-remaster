package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/**
 * N13: predicted next-tick flame ignition must agree with the native tick()
 * implementation. All timings are provisional, NOT original Symbian evidence.
 */
class NativeActiveFlameChainTest {
    private fun stage(wallColumn: Int = -1) = StageData(
        id = 0, image = "test.png", width = 160, height = 96,
        obstacleX = 0, obstacleY = 0,
        grid = List(12) { IntArray(20) { col -> if (col == wallColumn) 751 else 0 } },
        spawns = listOf(Spawn(24.0, 24.0), Spawn(88.0, 24.0)),
    )

    private fun burningStart(sim: NativeBombSimulation) {
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(24.0, 24.0), "player"))
        assertTrue(sim.detonateRemote("player"))
        assertTrue(sim.isBurning(56.0, 24.0))
    }

    @Test fun liveFlameSchedulesRemoteForTheVeryNextTick() {
        val sim = NativeBombSimulation(stage())
        burningStart(sim)
        assertTrue(sim.setRemoteDetonator("player2", true))
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        val threats = sim.predictedTimedChainThreats(0.75)
        assertEquals(1, threats.size)
        assertTrue(threats.single().bomb.remote)
        assertEquals(FixedStepClock.STEP_SECONDS, threats.single().secondsUntilBlast, 0.0)
        assertEquals(1, sim.tick())
        assertTrue(sim.bombs.isEmpty())
    }

    @Test fun activeFlameSeedsTransitiveRemoteChainAndThreatFootprint() {
        val sim = NativeBombSimulation(stage())
        burningStart(sim)
        assertTrue(sim.setRemoteDetonator("player2", true))
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        assertTrue(sim.place(Actor(104.0, 24.0), "player"))
        val threats = sim.predictedTimedChainThreats(0.75)
        assertEquals(2, threats.size)
        assertTrue(threats.all {
            it.bomb.remote && it.secondsUntilBlast == FixedStepClock.STEP_SECONDS
        })
        assertTrue(sim.predictedImminentTimedBlastCells(0.75).any {
            NativeBombSimulation.blastHits(it, 136.0, 24.0)
        })
        assertEquals(2, sim.tick())
        assertTrue(sim.bombs.isEmpty())
    }

    @Test fun finalFlameTickStillIgnitesBombBeforeFlameExpires() {
        val sim = NativeBombSimulation(stage())
        burningStart(sim)
        repeat(26) { assertEquals(0, sim.tick()) }
        assertTrue(sim.flames.isNotEmpty())
        assertTrue(sim.setRemoteDetonator("player2", true))
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        assertEquals(1, sim.predictedTimedChainThreats(0.75).size)
        assertEquals(1, sim.tick())
    }

    @Test fun expiredFlameAndWallDoNotInventRemoteCountdowns() {
        val expired = NativeBombSimulation(stage())
        burningStart(expired)
        repeat(28) { expired.tick() }
        assertTrue(expired.flames.isEmpty())
        assertTrue(expired.setRemoteDetonator("player2", true))
        assertTrue(expired.place(Actor(56.0, 24.0), "player2"))
        assertTrue(expired.predictedTimedChainThreats(20.0).isEmpty())
        assertEquals(0, expired.tick())

        val blocked = NativeBombSimulation(stage(wallColumn = 8))
        assertTrue(blocked.setRemoteDetonator("player", true))
        assertTrue(blocked.place(Actor(24.0, 24.0), "player"))
        assertTrue(blocked.detonateRemote("player"))
        assertFalse(blocked.isBurning(88.0, 24.0))
        assertTrue(blocked.setRemoteDetonator("player2", true))
        assertTrue(blocked.place(Actor(88.0, 24.0), "player2"))
        assertTrue(blocked.predictedTimedChainThreats(20.0).isEmpty())
        assertEquals(0, blocked.tick())
    }

    @Test fun flameSeedDoesNotWaitForFreshTimedFuseAndBotRefusesLateChase() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        burningStart(sim)
        assertTrue(sim.place(Actor(56.0, 24.0), "player2")) // ordinary timed bomb
        val threat = sim.predictedTimedChainThreats(0.75).single()
        assertFalse(threat.bomb.remote)
        assertEquals(FixedStepClock.STEP_SECONDS, threat.secondsUntilBlast, 0.0)
        val bot = Actor(88.0, 24.0)
        assertFalse(sim.isBurning(bot.x, bot.y, StageCollision.ACTOR_RADIUS))
        val decision = NativeBotController().decide(
            arena, bot, Actor(136.0, 24.0), sim, 0,
        )
        assertFalse(decision.placeBomb)
        assertEquals(0.0, decision.dx, 0.0)
        assertEquals(0.0, decision.dy, 0.0)
        assertEquals(1, sim.tick())
    }
}
