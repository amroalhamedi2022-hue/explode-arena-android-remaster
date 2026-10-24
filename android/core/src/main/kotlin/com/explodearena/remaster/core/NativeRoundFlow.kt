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

    val canRetry: Boolean get() = state == State.LOST || state == State.DRAW
    val isPlaying: Boolean get() = state == State.PLAYING

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
            }
            State.WON -> {
                winTicks++
                if (winTicks >= victoryHoldTicks) {
                    state = if (hasNextStage) State.ADVANCING else State.COMPLETE
                    return hasNextStage
                }
            }
            else -> Unit
        }
        return false
    }
}
