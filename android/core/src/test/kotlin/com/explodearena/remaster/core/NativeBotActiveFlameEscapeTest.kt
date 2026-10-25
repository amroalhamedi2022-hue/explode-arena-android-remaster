package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/**
 * N15: A bot standing in active fire must be able to cross the still-burning
 * intermediate positions to reach a non-burning refuge. Native timing is
 * provisional and does NOT establish original Symbian/EKA2L1 parity.
 */
class NativeBotActiveFlameEscapeTest {
    private fun openStage() = StageData(
        id = 0, image = "test.png", width = 160, height = 160,
        obstacleX = 0, obstacleY = 0,
        grid = List(20) { IntArray(20) },
        spawns = listOf(Spawn(52.0, 52.0), Spawn(112.0, 112.0)),
    )

    private fun burningStage(): Pair<StageData, NativeBombSimulation> {
        val stage = openStage()
        val simulation = NativeBombSimulation(stage)
        assertTrue(simulation.place(Actor(40.0, 40.0), "player"))
        repeat(96) { simulation.tick() }
        assertTrue(simulation.bombs.isEmpty())
        assertTrue(simulation.isBurning(52.0, 52.0, StageCollision.ACTOR_RADIUS))
        return stage to simulation
    }

    @Test fun flameOnlyEscapeCrossesBurningIntermediateCellsWithoutInventingBomb() {
        val (stage, sim) = burningStage()
        val bot = Actor(52.0, 52.0)
        val direction = NativeBotEscapeRoute.firstDirection(
            stage, bot, sim, bot.x, bot.y,
            allowBurningTransit = true, ignoreProposedBomb = true,
        )
        assertNotNull("A nearby non-burning refuge exists", direction)
        assertTrue("Fire escape must actually move", direction!! in 1..4)
        assertTrue(sim.bombs.isEmpty())
    }

    @Test fun controllerMovesOutwardWhileStillStandingInFlames() {
        val (stage, sim) = burningStage()
        val bot = Actor(52.0, 52.0)
        val target = Actor(112.0, 112.0)
        val controller = NativeBotController()
        val choice = controller.decide(stage, bot, target, sim, 0)
        assertFalse("Do not plant while standing in flames", choice.placeBomb)
        assertTrue("Escape direction must not be zero", choice.dx != 0.0 || choice.dy != 0.0)
        val result = NativeBotTurn.advance(stage, bot, target, sim, controller, 1)
        assertTrue("Movement must not freeze just because the next 0.9px is still burning", result.moved)
        assertFalse(result.bombPlaced)
        assertTrue(sim.isBurning(bot.x, bot.y, StageCollision.ACTOR_RADIUS))
    }

    @Test fun safeBotWithoutBombDoesNotEnterEscapeMode() {
        val (stage, sim) = burningStage()
        val bot = Actor(112.0, 112.0)
        assertFalse(sim.isBurning(bot.x, bot.y, StageCollision.ACTOR_RADIUS))
        assertEquals(0, NativeBotEscapeRoute.firstDirection(
            stage, bot, sim, bot.x, bot.y,
            allowBurningTransit = true, ignoreProposedBomb = true,
        ))
        val decision = NativeBotController().decide(stage, bot, Actor(128.0, 112.0), sim, 0)
        assertFalse("Ordinary safe chase must not pretend to be a flame escape",
            decision.dx == 0.0 && decision.dy == 0.0)
    }

    @Test fun repeatedFixedStepsReachUnlitRefugeBeforeFlameIsRemoved() {
        val (stage, sim) = burningStage()
        val bot = Actor(52.0, 52.0)
        val controller = NativeBotController()
        val target = Actor(112.0, 112.0)
        var moved = 0
        for (tick in 0 until 40) {
            if (!sim.isBurning(bot.x, bot.y, StageCollision.ACTOR_RADIUS)) break
            val choice = controller.decide(stage, bot, target, sim, tick.toLong())
            assertFalse(choice.placeBomb)
            val oldX = bot.x
            val oldY = bot.y
            StageCollision.move(
                stage, bot, choice.dx, choice.dy,
                NativeBotController.SPEED, FixedStepClock.STEP_SECONDS, sim.bombs,
            )
            if (bot.x != oldX || bot.y != oldY) moved++
        }
        assertTrue("Escape must involve actual movement", moved > 0)
        assertFalse("Without ticking the flame away, the bot must reach an unlit refuge",
            sim.isBurning(bot.x, bot.y, StageCollision.ACTOR_RADIUS))
        assertTrue(sim.flames.isNotEmpty())
    }

    @Test fun flameEscapeCannotClaimARefugeStillInsideActiveFire() {
        val (stage, sim) = burningStage()
        val bot = Actor(52.0, 52.0)
        val direction = NativeBotEscapeRoute.firstDirection(
            stage, bot, sim, bot.x, bot.y,
            allowBurningTransit = true, ignoreProposedBomb = true,
        )
        assertNotNull(direction)
        // The next 60Hz step is permitted to remain in fire, but it cannot
        // be the final destination: the BFS checks that its refuge is unlit.
        val nx = bot.x + (if (direction == 1) 1 else if (direction == 3) -1 else 0) *
            NativeBotController.SPEED * FixedStepClock.STEP_SECONDS
        val ny = bot.y + (if (direction == 2) 1 else if (direction == 4) -1 else 0) *
            NativeBotController.SPEED * FixedStepClock.STEP_SECONDS
        assertTrue(sim.isBurning(nx, ny, StageCollision.ACTOR_RADIUS))
    }
}
