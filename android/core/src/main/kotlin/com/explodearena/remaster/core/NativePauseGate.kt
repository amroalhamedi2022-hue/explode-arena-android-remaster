package com.explodearena.remaster.core

/**
 * Single gate for manual pause AND Android/libGDX background lifecycle.
 * Backgrounding always requires an explicit resume tap on return, preventing
 * an unseen bomb fuse or bot action immediately after switching applications.
 * This is native lifecycle safety, not evidence of original Symbian menus.
 */
class NativePauseGate {
    private var manuallyPaused = false
    private var backgrounded = false

    val isPaused: Boolean get() = manuallyPaused || backgrounded

    /** Manual pause button. Background callbacks cannot accidentally unpause. */
    fun toggle(clock: FixedStepClock): Boolean {
        if (!backgrounded) manuallyPaused = !manuallyPaused
        clock.clearPending()
        return isPaused
    }

    /** Idempotent: discard fractional time and require a deliberate user resume. */
    fun onBackground(clock: FixedStepClock) {
        backgrounded = true
        manuallyPaused = true
        clock.clearPending()
    }

    /** Return to foreground without restarting the simulation or its timers. */
    fun onForeground(clock: FixedStepClock) {
        backgrounded = false
        clock.clearPending()
    }

    fun advance(clock: FixedStepClock, deltaSeconds: Double, tick: () -> Unit): Int {
        // Validate before the paused fast path; otherwise pause masks malformed timing input.
        require(deltaSeconds.isFinite() && deltaSeconds >= 0.0) { "Invalid frame time" }
        return if (isPaused) 0 else clock.advance(deltaSeconds, tick)
    }
}
