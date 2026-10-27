package com.explodearena.remaster.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Integration of libGDX lifecycle callbacks with the production pause gate. */
class NativeLifecycleCallbacksTest {
    @Test fun backgroundResumeKeepsFuseFrozenUntilExplicitResumeTap() {
        val stage = StageData(
            0, "test.png", 64, 64, 0, 0,
            List(8) { IntArray(8) },
            listOf(Spawn(24.0, 24.0)),
        )
        val actor = Actor(24.0, 24.0)
        val bombs = NativeBombSimulation(stage, bombFuseSeconds = 0.1)
        assertTrue(bombs.place(actor))

        val clock = FixedStepClock()
        val gate = NativePauseGate()
        val callbacks = NativeLifecycleCallbacks(gate, clock)
        var detonations = 0
        val tick = { detonations += bombs.tick() }

        repeat(3) {
            assertEquals(1, gate.advance(clock, FixedStepClock.STEP_SECONDS, tick))
        }
        assertEquals(3L, clock.ticks)

        // These are the callbacks invoked by ExplodeArenaNativeGame.pause/resume.
        callbacks.onPause()
        callbacks.onPause() // repeated OS callbacks must be harmless
        callbacks.onResume()
        repeat(100) {
            assertEquals(0, gate.advance(clock, 0.25, tick))
        }

        assertEquals(3L, clock.ticks)
        assertEquals(0, detonations)
        assertEquals(1, bombs.bombs.size)
        assertTrue(gate.isPaused)

        assertFalse(gate.toggle(clock)) // explicit user resume
        repeat(3) {
            gate.advance(clock, FixedStepClock.STEP_SECONDS, tick)
        }
        assertEquals(6L, clock.ticks)
        assertEquals(1, detonations)
        assertTrue(bombs.bombs.isEmpty())
    }
}
