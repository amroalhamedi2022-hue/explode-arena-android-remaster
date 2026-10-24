package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/** Provisional bot safety regressions; not original EKA2L1 timing evidence. */
class NativeBotThreatDeadlineTest {
    private fun openStage() = StageData(
        id = 0, image = "test.png", width = 128, height = 128,
        obstacleX = 0, obstacleY = 0,
        grid = List(16) { IntArray(16) },
        spawns = listOf(Spawn(40.0, 40.0), Spawn(104.0, 104.0)),
    )

    @Test fun opponentBombAboutToExplodeCannotGrantImpossibleEscape() {
        val stage = openStage()
        val sim = NativeBombSimulation(stage)
        val bot = Actor(40.0, 40.0)
        assertTrue(sim.place(Actor(40.0, 40.0), "player"))
        repeat(92) { sim.tick() }
        assertEquals(1, sim.bombs.size)
        assertTrue(sim.bombs.single().remaining!! > 0.0)
        // Proposed bot bomb is far away; the immediate threat is the
        // opponent's already-placed timed bomb, not the proposed bomb.
        assertNull(NativeBotEscapeRoute.firstDirection(stage, bot, sim, 104.0, 104.0))
    }

    @Test fun distantTimedBombDoesNotImposeAnUnrelatedEscapeDeadline() {
        val stage = openStage()
        val sim = NativeBombSimulation(stage)
        val bot = Actor(40.0, 40.0)
        assertTrue(sim.place(Actor(104.0, 104.0), "player"))
        repeat(92) { sim.tick() }
        assertFalse(sim.wouldBlastHitFrom(104.0, 104.0, bot.x, bot.y))
        // The proposed bomb at bot's position still has a reachable refuge.
        assertNotNull(NativeBotEscapeRoute.firstDirection(stage, bot, sim, 40.0, 40.0))
    }

    @Test fun remoteBombWithoutFuseDoesNotCreateAFalseCountdown() {
        val stage = openStage()
        val sim = NativeBombSimulation(stage)
        val bot = Actor(40.0, 40.0)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(bot, "player"))
        repeat(200) { sim.tick() }
        assertNull(sim.bombs.single().remaining)
        assertNotNull(NativeBotEscapeRoute.firstDirection(stage, bot, sim, 104.0, 104.0))
    }
}
