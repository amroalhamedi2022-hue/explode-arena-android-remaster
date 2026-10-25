package com.explodearena.remaster.core

/**
 * Atomic per-render input arbitration. A pause, arena switch or retry must
 * never also plant/detonate a bomb or advance the previous/new arena in the
 * same render frame. This is native UI safety, NOT Symbian keypad parity.
 */
internal object NativeFrameCommands {
    data class Keys(
        val pause: Boolean = false,
        val nextStage: Boolean = false,
        val previousStage: Boolean = false,
        val placeBomb: Boolean = false,
        val detonateRemote: Boolean = false,
    )

    data class Decision(
        val togglePause: Boolean = false,
        val stageDelta: Int = 0,
        val retry: Boolean = false,
        val placeBomb: Boolean = false,
        val detonateRemote: Boolean = false,
        val advanceWorld: Boolean = false,
    )

    fun resolve(
        touch: NativeTouchControls.Frame,
        keys: Keys,
        paused: Boolean,
        canRetry: Boolean,
        isPlaying: Boolean,
    ): Decision {
        // Highest priority: toggling pause consumes the entire frame.
        if (touch.pauseToggle || keys.pause) return Decision(togglePause = true)
        if (paused) return Decision()

        // Stage navigation must not also mutate either arena's bomb/AI state.
        if (touch.nextStage || keys.nextStage) return Decision(stageDelta = 1)
        if (touch.previousStage || keys.previousStage) return Decision(stageDelta = -1)

        val bombRequested = touch.placeBomb || keys.placeBomb
        if (bombRequested && canRetry) return Decision(retry = true)

        // Deliberate simultaneous bomb + remote taps remain supported while
        // actively playing; the simulation itself enforces remote ownership.
        return Decision(
            placeBomb = bombRequested && isPlaying,
            detonateRemote = (touch.detonateRemote || keys.detonateRemote) && isPlaying,
            advanceWorld = true, // includes the provisional victory hold
        )
    }
}
