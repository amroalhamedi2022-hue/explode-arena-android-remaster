package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeBotTurnTest {
    private fun openStage() = StageData(
        id = 0, image = "open.png", width = 160, height = 160,
        obstacleX = 0, obstacleY = 0,
        grid = List(20) { IntArray(20) },
        spawns = listOf(Spawn(47.5, 40.0), Spawn(63.5, 40.0)),
    )

    @Test fun plantsAtPlannedCellBeforeCrossingBombSnapBoundary() {
        val stage = openStage()
        val simulation = NativeBombSimulation(stage)
        val bot = Actor(47.5, 40.0)
        val turn = NativeBotTurn.advance(
            stage, bot, Actor(63.5, 40.0), simulation, NativeBotController(), 0,
        )
        assertTrue("The bot should have a diagonal refuge in the open arena", turn.bombPlaced)
        assertTrue(turn.moved)
        assertEquals(1, simulation.bombs.size)
        // At x=47.5 the 16px bomb cell is centered at x=40.
        // If movement happened first, x>48 would snap the bomb to x=56.
        assertEquals(40.0, simulation.bombs.single().x, 0.0)
        assertTrue("Bot must move after placing its bomb", bot.x > 48.0)
        assertEquals(simulation.bombs.single().id, bot.passBomb)
    }

    @Test fun doesNotPlantAgainWhileOwnBombIsActive() {
        val stage = openStage()
        val simulation = NativeBombSimulation(stage)
        val bot = Actor(47.5, 40.0)
        val controller = NativeBotController()
        val first = NativeBotTurn.advance(stage, bot, Actor(63.5, 40.0), simulation, controller, 0)
        assertTrue(first.bombPlaced)
        val second = NativeBotTurn.advance(stage, bot, Actor(63.5, 40.0), simulation, controller, 1)
        assertFalse(second.bombPlaced)
        assertEquals(1, simulation.bombs.size)
    }
}
