package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeBotControllerTest {
    private fun stage(withWall: Boolean = false) = StageData(
        id = 0, image = "test.png", width = 128, height = 128,
        obstacleX = 0, obstacleY = 0,
        grid = List(16) { row ->
            IntArray(16) { col -> if (withWall && col == 6 && row < 12) 751 else 0 }
        },
        spawns = listOf(Spawn(20.0, 20.0), Spawn(76.0, 20.0)),
    )

    @Test fun approachesPlayerOnOpenArena() {
        val arena = stage()
        val choice = NativeBotController().decide(
            arena, Actor(20.0, 20.0), Actor(76.0, 20.0),
            NativeBombSimulation(arena), 0,
        )
        assertEquals(1.0, choice.dx, 0.0)
        assertEquals(0.0, choice.dy, 0.0)
        assertFalse(choice.placeBomb)
    }

    @Test fun routesAroundWallRatherThanWalkingIntoIt() {
        val arena = stage(withWall = true)
        val choice = NativeBotController().decide(
            arena, Actor(36.0, 20.0), Actor(76.0, 20.0),
            NativeBombSimulation(arena), 0,
        )
        assertEquals(0.0, choice.dx, 0.0)
        assertEquals(1.0, choice.dy, 0.0)
    }

    @Test fun closeOpponentTriggersProvisionalBombOnlyWithEscapeAndCapacity() {
        val arena = stage()
        val bot = Actor(40.0, 40.0)
        val player = Actor(56.0, 40.0)
        val sim = NativeBombSimulation(arena)
        val controller = NativeBotController()
        assertTrue(controller.decide(arena, bot, player, sim, 0).placeBomb)
        assertTrue(sim.place(bot, "bot"))
        assertFalse(controller.decide(arena, bot, player, sim, 1).placeBomb)
    }

    @Test fun neverSelectsMovementIntoActiveFlame() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.place(Actor(40.0, 40.0)))
        repeat(96) { sim.tick() }
        val bot = Actor(24.0, 24.0)
        val choice = NativeBotController().decide(arena, bot, Actor(76.0, 24.0), sim, 0)
        val nextX = bot.x + choice.dx * NativeBotController.SPEED * FixedStepClock.STEP_SECONDS
        val nextY = bot.y + choice.dy * NativeBotController.SPEED * FixedStepClock.STEP_SECONDS
        assertFalse(sim.isBurning(nextX, nextY, StageCollision.ACTOR_RADIUS))
    }
}
