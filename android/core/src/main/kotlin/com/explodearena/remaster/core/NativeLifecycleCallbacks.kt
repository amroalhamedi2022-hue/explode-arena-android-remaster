package com.explodearena.remaster.core

/**
 * Connects libGDX's Android lifecycle callbacks to the production pause gate
 * and quarantines held touch pointers until a complete release frame arrives.
 * Foregrounding clears stale frame time but never resumes gameplay by itself.
 */
internal class NativeLifecycleCallbacks(
    private val gate: NativePauseGate,
    private val clock: FixedStepClock,
    private val touchControls: NativeTouchControls,
) {
    fun onPause() {
        gate.onBackground(clock)
        touchControls.suppressUntilRelease()
    }

    fun onResume() {
        gate.onForeground(clock)
    }
}
