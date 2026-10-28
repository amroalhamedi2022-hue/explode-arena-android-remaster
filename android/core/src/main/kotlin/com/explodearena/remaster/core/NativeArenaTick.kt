package com.explodearena.remaster.core

/**
 * One production fixed-step for the native arena. Keeping player movement,
 * opponent/bomb advancement, both damage samples, and round resolution here
 * lets integration tests exercise the same ordering used by libGDX.
 *
 * All movement, AI, health and timing semantics remain provisional until
 * independently matched against the original Symbian game in EKA2L1.
 */
internal object NativeArenaTick {
    data class Result(
        val playerWalking: Boolean,
        val bombsPlaced: Int,
        val bombsDetonated: Int,
        val advanceStage: Boolean,
    )

    fun advance(
        stage: StageData,
        player: Actor,
        playerVitality: NativeVitality,
        opponents: NativeOpponentRoster,
        simulation: NativeBombSimulation,
        roundFlow: NativeRoundFlow,
        tick: Long,
        dx: Double,
        dy: Double,
        hasNextStage: Boolean,
        playerSpeed: Double = 77.0,
    ): Result {
        // Validate the complete tick contract before moving the player or
        // touching any mutable subsystem. In a multi-spawn arena an invalid
        // tick previously moved the player before a bot controller rejected it;
        // in a solo arena no controller existed to reject it at all.
        require(tick >= 0L) { "Invalid arena tick" }
        require(
            dx.isFinite() && dy.isFinite() &&
                dx in -1.0..1.0 && dy in -1.0..1.0
        ) { "Arena input must be finite and normalized" }
        // Validate before the latched-round fast path too: terminal ticks must
        // reject malformed input without mutating opponent animation state.
        require(playerSpeed.isFinite() && playerSpeed >= 0.0) { "Invalid player speed" }
        require(player.x.isFinite() && player.y.isFinite()) { "Invalid player position" }
        // Reject replayed ticks before moving the player: the roster's old guard
        // ran only after player movement, leaving a partially advanced world.
        roundFlow.validateCanonicalTick(tick)
        if (roundFlow.isPlaying) {
            opponents.validateNextSimulationTick(tick)
            opponents.validateStartState(stage, player, simulation)
            val requestedDistance = playerSpeed * FixedStepClock.STEP_SECONDS
            // Match StageCollision's bounded substep preflight before reserving the tick.
            StageCollision.movementStepCount(stage, requestedDistance)
        }
        // Reserve the round/presentation clock only after the non-mutating
        // preflight succeeds, but before movement, fuse aging or victory-hold work.
        roundFlow.reserveCanonicalTick(tick)

        var playerWalking = false
        var bombsPlaced = 0
        var bombsDetonated = 0

        // LOST/DRAW must remain retry-only; ADVANCING/COMPLETE are consumed
        // by the outer render loop. Do not run AI, move actors, age fuses or
        // re-resolve a latched outcome during catch-up ticks.
        if (roundFlow.isLatched) {
            opponents.stopWalking()
            return Result(false, 0, 0, false)
        }

        if (roundFlow.isPlaying) {
            val oldX = player.x
            val oldY = player.y
            StageCollision.move(
                stage, player, dx, dy, playerSpeed,
                FixedStepClock.STEP_SECONDS, simulation.bombs,
                opponents.blockingActors(),
            )
            playerWalking = player.x != oldX || player.y != oldY

            // This canonical roster entry point plans/moves bots, advances the
            // bomb simulation exactly once, then samples the bots' full damage
            // interval. The player samples that same interval immediately after.
            val botTick = opponents.advanceSimulationTick(stage, player, simulation, tick)
            bombsPlaced = botTick.bombsPlaced
            bombsDetonated = botTick.bombsDetonated
            playerVitality.tick(
                simulation.isBurningDuringLastTick(
                    player.x, player.y, StageCollision.ACTOR_RADIUS,
                ),
            )
        } else {
            opponents.stopWalking()
        }

        // Round resolution is part of the same fixed-step contract. Victory
        // hold ticks continue after gameplay freezes; defeat never advances.
        val advanceStage = roundFlow.tick(
            playerVitality.defeated,
            opponents.allDefeated,
            opponents.present,
            hasNextStage,
        )
        // LOST/DRAW stop scheduling subsequent gameplay ticks in the render loop,
        // so clear bot animation state on the exact tick that latches the outcome.
        if (roundFlow.isLatched) opponents.stopWalking()
        return Result(playerWalking, bombsPlaced, bombsDetonated, advanceStage)
    }
}
