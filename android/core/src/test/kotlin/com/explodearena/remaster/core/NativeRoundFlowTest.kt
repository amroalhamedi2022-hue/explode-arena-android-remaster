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

    @Test fun invalidHoldIsRejectedAndNewRoundIsIndependent() {
        assertThrows(IllegalArgumentException::class.java) { NativeRoundFlow(0) }
        val previous = NativeRoundFlow(1)
        previous.tick(true, false, true, true)
        val next = NativeRoundFlow(1)
        assertEquals(NativeRoundFlow.State.LOST, previous.state)
        assertEquals(NativeRoundFlow.State.PLAYING, next.state)
    }
}
