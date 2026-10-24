package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeSpriteFramesTest {
    @Test fun originalDirectionalFrameGroups() {
        assertEquals(24, NativeSpriteFrames.player(0, false, 0.0))
        assertEquals(16, NativeSpriteFrames.player(1, false, 0.0))
        assertEquals(8, NativeSpriteFrames.player(2, false, 0.0))
        assertEquals(0, NativeSpriteFrames.player(3, false, 0.0))
        assertEquals(8, NativeSpriteFrames.player(99, false, 0.0))
        assertEquals(3, NativeSpriteFrames.player(3, true, 0.25))
        assertEquals(31, NativeSpriteFrames.player(0, true, 7.0 / 12.0))
    }

    @Test fun flameFrameClampsAndNeverIndexesBeyondAtlas() {
        assertEquals(0, NativeSpriteFrames.flame(0.45, 0.45))
        assertEquals(7, NativeSpriteFrames.flame(0.225, 0.45))
        assertEquals(13, NativeSpriteFrames.flame(0.0, 0.45))
        assertEquals(0, NativeSpriteFrames.flame(1.0, 0.45))
        assertThrows(IllegalArgumentException::class.java) {
            NativeSpriteFrames.flame(Double.NaN, 0.45)
        }
    }

    @Test fun partialFrameTimeIsNotCarriedIntoNewStage() {
        val clock = FixedStepClock()
        assertEquals(0, clock.advance(FixedStepClock.STEP_SECONDS * 0.9) {})
        clock.clearPending()
        assertEquals(0, clock.advance(FixedStepClock.STEP_SECONDS * 0.2) {})
        assertEquals(1, clock.advance(FixedStepClock.STEP_SECONDS * 0.8) {})
    }
}
