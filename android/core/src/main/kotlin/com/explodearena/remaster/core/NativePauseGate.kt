package com.explodearena.remaster.core

/**
 * Native pause gate for the 60 Hz simulation. Pausing must freeze movement,
 * bomb fuses, flames, bot decisions and round-result hold timers together.
 * This is a control/lifecycle feature, NOT proof of original Symbian menu art.
 */
class NativePauseGate {
    var isPaused: Boolean = false
        private set

    /** Clear fractional time on BOTH edges so a paused interval never catches up. */
    fun toggle(clock: FixedStepClock): Boolean {
        isPaused = !isPaused
        clock.clearPending()
        return isPaused
    }

    fun advance(clock: FixedStepClock, deltaSeconds: Double, tick: () -> Unit): Int {
        return if (isPaused) 0 else clock.advance(deltaSeconds, tick)
    }
}
