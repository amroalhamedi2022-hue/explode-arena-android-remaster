package com.explodearena.remaster.core

/**
 * Executes a provisional opponent turn in one testable native simulation step.
 *
 * Bomb placement MUST precede movement: the escape planner predicts a blast
 * at the actor position it receives, and placing after movement can snap the
 * bomb into a different 16px bomb cell near tile boundaries. No Symbian AI parity
 * is claimed by this ordering fix.
 */
internal object NativeBotTurn {
    data class Result(
        val decision: NativeBotController.Decision,
        val bombPlaced: Boolean,
        val moved: Boolean,
    )

    fun advance(
        stage: StageData,
        bot: Actor,
        target: Actor,
        simulation: NativeBombSimulation,
        controller: NativeBotController,
        tick: Long,
    ): Result {
        val decision = controller.decide(stage, bot, target, simulation, tick)
        val placed = decision.placeBomb && simulation.place(bot, "bot")
        val oldX = bot.x
        val oldY = bot.y
        StageCollision.move(
            stage, bot, decision.dx, decision.dy,
            NativeBotController.SPEED, FixedStepClock.STEP_SECONDS, simulation.bombs,
        )
        return Result(decision, placed, bot.x != oldX || bot.y != oldY)
    }
}
