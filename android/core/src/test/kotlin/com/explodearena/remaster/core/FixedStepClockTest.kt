package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class FixedStepClockTest {
    @Test fun lastRepresentableTickRunsAndNextTickFailsBeforeCallback() {
        val clock = FixedStepClock(Long.MAX_VALUE - 1L)
        var callbacks = 0

        assertEquals(1, clock.advance(FixedStepClock.STEP_SECONDS) { callbacks++ })
        assertEquals(1, callbacks)
        assertEquals(Long.MAX_VALUE, clock.ticks)

        assertThrows(IllegalStateException::class.java) {
            clock.advance(FixedStepClock.STEP_SECONDS) { callbacks++ }
        }
        assertEquals("overflow must fail before mutating gameplay", 1, callbacks)
        assertEquals(Long.MAX_VALUE, clock.ticks)
    }

    @Test fun exhaustedClockStillAcceptsZeroDeltaWithoutCallingBack() {
        val clock = FixedStepClock(Long.MAX_VALUE)
        assertEquals(0, clock.advance(0.0) { fail("zero delta must not tick") })
        assertEquals(Long.MAX_VALUE, clock.ticks)
    }

    @Test fun negativeInitialCounterIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { FixedStepClock(-1L) }
    }
}
