package com.explodearena.remaster.core

/**
 * Connects libGDX's Android lifecycle callbacks to the same pause gate used by
 * manual controls. Resume clears stale frame time but never resumes gameplay.
 */
internal class NativeLifecycleCallbacks(
    private val gate: NativePauseGate,
    private val clock: FixedStepClock,
) {
    fun onPause() = gate.onBackground(clock)
    fun onResume() = gate.onForeground(clock)
}
