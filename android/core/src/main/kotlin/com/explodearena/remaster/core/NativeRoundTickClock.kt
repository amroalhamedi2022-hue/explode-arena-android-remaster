package com.explodearena.remaster.core

/**
 * Deterministic AI tick sequence scoped to one arena attempt.
 *
 * The renderer's fixed-step clock is process-wide and remains monotonic across
 * stage swaps/retries. AI cadence must instead restart at tick zero for every
 * fresh round so its first planning deadline does not depend on prior play time.
 */
internal class NativeRoundTickClock {
    private var nextTick = 0L

    /** Returns the current zero-based round tick, then advances exactly once. */
    fun nextTick(): Long {
        check(nextTick < Long.MAX_VALUE) { "Arena round tick counter exhausted" }
        return nextTick++
    }
}