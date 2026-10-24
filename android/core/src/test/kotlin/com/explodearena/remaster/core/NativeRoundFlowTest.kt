package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeRoundFlowTest {
    @Test fun victoryAdvancesExactlyOnceAfterHold() {
        val flow = NativeRoundFlow(victoryHoldTicks = 90)
        assertFalse(flow.tick(false, true, true, true))
        assertEquals(NativeRoundFlow.State.WON, flow.state)
        repeat(89) { assertFalse(flow.tick(false, true, true, true)) }
        assertTrue(flow.tick(false, true, true, true))
        assertEquals(NativeRoundFlow.State.ADVANCING, flow.state)
        repeat(180) { assertFalse(flow.tick(false, true, true, true)) }
    }

    @Test fun soloArenaCannotWinJustBecauseOpponentIsAbsent() {
        val flow = NativeRoundFlow(victoryHoldTicks = 1)
        repeat(180) { assertFalse(flow.tick(false, true, false, true)) }
        assertTrue(flow.isPlaying)
    }

    @Test fun playerDefeatStopsProgressionAndAllowsRetry() {
        val flow = NativeRoundFlow(victoryHoldTicks = 1)
        assertFalse(flow.tick(true, false, true, true))
        assertEquals(NativeRoundFlow.State.LOST, flow.state)
        assertTrue(flow.canRetry)
        repeat(180) { assertFalse(flow.tick(false, true, true, true)) }
    }

    @Test fun simultaneousDefeatIsDrawNotVictory() {
        val flow = NativeRoundFlow(victoryHoldTicks = 1)
        assertFalse(flow.tick(true, true, true, true))
        assertEquals(NativeRoundFlow.State.DRAW, flow.state)
        assertTrue(flow.canRetry)
        repeat(180) { assertFalse(flow.tick(false, true, true, true)) }
    }

    @Test fun finalArenaStopsWithoutWrappingToFirst() {
        val flow = NativeRoundFlow(victoryHoldTicks = 1)
        assertFalse(flow.tick(false, true, true, false))
        assertFalse(flow.tick(false, true, true, false))
        assertEquals(NativeRoundFlow.State.COMPLETE, flow.state)
        repeat(180) { assertFalse(flow.tick(false, true, true, true)) }
    }

    @Test fun fixedStepClockAndVitalityDriveActualRoundTransition() {
        val clock = FixedStepClock()
        val player = NativeVitality(initialHealth = 1, initialInvulnerabilitySeconds = 0.0)
        val bot = NativeVitality(
            initialHealth = 1, initialInvulnerabilitySeconds = 0.0,
            hitInvulnerabilitySeconds = 0.0,
        )
        val flow = NativeRoundFlow(victoryHoldTicks = 3)
        var transitions = 0
        repeat(5) { frame ->
            clock.advance(FixedStepClock.STEP_SECONDS) {
                player.tick(burning = false)
                bot.tick(burning = frame == 0)
                if (flow.tick(player.defeated, bot.defeated, true, true)) transitions++
            }
        }
        assertEquals(5L, clock.ticks)
        assertFalse(player.defeated)
        assertTrue(bot.defeated)
        assertEquals(1, transitions)
        assertEquals(NativeRoundFlow.State.ADVANCING, flow.state)
    }

    @Test fun invalidHoldIsRejectedAndNewRoundIsIndependent() {
        assertThrows(IllegalArgumentException::class.java) { NativeRoundFlow(0) }
        val previous = NativeRoundFlow(1)
        previous.tick(true, false, true, true)
        val next = NativeRoundFlow(1)
        assertEquals(NativeRoundFlow.State.LOST, previous.state)
        assertEquals(NativeRoundFlow.State.PLAYING, next.state)
    }
}
