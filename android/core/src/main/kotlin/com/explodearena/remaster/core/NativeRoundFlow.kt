package com.explodearena.remaster.core

/**
 * Native round-result/transition coordinator. The 90-tick victory hold and
 * campaign-end UI are PROVISIONAL until matched against Symbian EKA2L1.
 * No stage transition is inferred on a solo arena with no opponent.
 * Called exactly once per fixed 60 Hz simulation tick.
 */
class NativeRoundFlow(private val victoryHoldTicks: Int = 90) {
    enum class State { PLAYING, WON, LOST, DRAW, ADVANCING, COMPLETE }

    init { require(victoryHoldTicks > 0) }

    var state: State = State.PLAYING
        private set
    private var winTicks = 0
    // Lock campaign progression when victory begins; later caller changes must not
    // turn a final arena into a next-stage transition (or vice versa).
    private var hasNextStageAtVictory: Boolean? = null
    // Every canonical arena tick, including the frozen victory hold, is consumed once.
    private var lastCanonicalTick: Long? = null

    val canRetry: Boolean get() = state == State.LOST || state == State.DRAW
    val isPlaying: Boolean get() = state == State.PLAYING

    /** Presentation-time animation runs during play and the victory hold only. */
    val advancesPresentationClock: Boolean
        get() = state == State.PLAYING || state == State.WON

    /**
     * A latched outcome/transition cannot be changed by another gameplay tick.
     * WON is deliberately excluded: its victory-hold counter must keep ticking.
     */
    val isLatched: Boolean
        get() = when (state) {
            State.LOST, State.DRAW, State.ADVANCING, State.COMPLETE -> true
            State.PLAYING, State.WON -> false
        }

    /** Validate without consuming a tick so malformed requests fail before gameplay mutation. */
    internal fun validateCanonicalTick(tick: Long) {
        require(tick >= 0L) { "Invalid canonical arena tick" }
        val previous = lastCanonicalTick
        require(previous == null || tick > previous) {
            "Canonical arena tick must increase strictly"
        }
    }

    /** Reserve only after the whole request has passed its non-mutating preflight. */
    internal fun reserveCanonicalTick(tick: Long) {
        validateCanonicalTick(tick)
        lastCanonicalTick = tick
    }

    /** Returns true only once when the caller must load the next arena. */
    fun tick(
        playerDefeated: Boolean,
        opponentDefeated: Boolean,
        opponentPresent: Boolean,
        hasNextStage: Boolean,
    ): Boolean {
        when (state) {
            State.PLAYING -> {
                state = when {
                    playerDefeated && opponentPresent && opponentDefeated -> State.DRAW
                    playerDefeated -> State.LOST
                    opponentPresent && opponentDefeated -> State.WON
                    else -> State.PLAYING
                }
                if (state == State.WON) hasNextStageAtVictory = hasNextStage
            }
            State.WON -> {
                winTicks++
                if (winTicks >= victoryHoldTicks) {
                    val shouldAdvance = checkNotNull(hasNextStageAtVictory) {
                        "Victory must capture stage availability before the hold begins"
                    }
                    state = if (shouldAdvance) State.ADVANCING else State.COMPLETE
                    return shouldAdvance
                }
            }
            else -> Unit
        }
        return false
    }
}
