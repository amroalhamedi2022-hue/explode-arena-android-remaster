package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/** N75: the arena tick and bomb simulation must share the same full stage definition. */
class NativeArenaTickStageBindingIntegrationTest {
    private fun stage(wallColumn: Int = -1) = StageData(
        id = 0,
        image = "assets/maps/map_000.png",
        width = 96,
        height = 64,
        obstacleX = 0,
        obstacleY = 0,
        grid = List(8) { IntArray(12) { col -> if (col == wallColumn) 751 else 0 } },
        spawns = listOf(Spawn(16.0, 16.0), Spawn(72.0, 24.0)),
    )

    @Test fun canonicalArenaTickRejectsMismatchedSimulationBeforeAnyMutationAndCanRetrySameTick() {
        val arenaStage = stage()
        // Same ID/dimensions/spawns, but a different collision grid.
        val simulationStage = stage(wallColumn = 4)
        val player = Actor(16.0, 16.0)
        val bot = NativeOpponentRoster.BotSlot("bot", Actor(72.0, 24.0))
        val roster = NativeOpponentRoster(listOf(bot))
        val wrongSimulation = NativeBombSimulation(simulationStage)
        assertTrue(wrongSimulation.place(player, owner = "player"))
        val fuseBefore = wrongSimulation.bombs.single().remaining
        val playerBefore = Actor(player.x, player.y, player.passBomb)
        val botBefore = Actor(bot.actor.x, bot.actor.y, bot.actor.passBomb)
        val playerVitality = NativeVitality()
        val playerHealthBefore = playerVitality.health
        val botHealthBefore = bot.vitality.health
        val round = NativeRoundFlow()

        assertThrows(IllegalArgumentException::class.java) {
            NativeArenaTick.advance(
                arenaStage, player, playerVitality, roster, wrongSimulation, round,
                tick = 4L, dx = 1.0, dy = 0.0, hasNextStage = true,
            )
        }

        assertEquals(playerBefore.x, player.x, 0.0)
        assertEquals(playerBefore.y, player.y, 0.0)
        assertEquals(playerBefore.passBomb, player.passBomb)
        assertEquals(botBefore.x, bot.actor.x, 0.0)
        assertEquals(botBefore.y, bot.actor.y, 0.0)
        assertEquals(botBefore.passBomb, bot.actor.passBomb)
        assertEquals(fuseBefore, wrongSimulation.bombs.single().remaining)
        assertEquals(playerHealthBefore, playerVitality.health)
        assertEquals(botHealthBefore, bot.vitality.health)
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)

        // The rejected tick was not reserved by either canonical clock.
        player.passBomb = null
        val matchingSimulation = NativeBombSimulation(arenaStage)
        val retry = NativeArenaTick.advance(
            arenaStage, player, playerVitality, roster, matchingSimulation, round,
            tick = 4L, dx = 0.0, dy = 0.0, hasNextStage = true,
        )
        assertFalse(retry.playerWalking)
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)
    }

    @Test fun directRosterTickAlsoRejectsMismatchedSimulationBeforeFuseOrTickReservation() {
        val arenaStage = stage()
        val player = Actor(16.0, 16.0)
        val bot = NativeOpponentRoster.BotSlot("bot", Actor(72.0, 24.0))
        val roster = NativeOpponentRoster(listOf(bot))
        val wrongSimulation = NativeBombSimulation(stage(wallColumn = 4))
        assertTrue(wrongSimulation.place(player, owner = "player"))
        val fuseBefore = wrongSimulation.bombs.single().remaining
        val botBefore = Actor(bot.actor.x, bot.actor.y, bot.actor.passBomb)

        assertThrows(IllegalArgumentException::class.java) {
            roster.advanceSimulationTick(arenaStage, player, wrongSimulation, tick = 9L)
        }

        assertEquals(fuseBefore, wrongSimulation.bombs.single().remaining)
        assertEquals(botBefore.x, bot.actor.x, 0.0)
        assertEquals(botBefore.y, bot.actor.y, 0.0)
        assertEquals(botBefore.passBomb, bot.actor.passBomb)

        player.passBomb = null
        val matchingSimulation = NativeBombSimulation(arenaStage)
        val retry = roster.advanceSimulationTick(arenaStage, player, matchingSimulation, tick = 9L)
        assertEquals(0, retry.bombsDetonated)
    }

    @Test fun structurallyEquivalentStageCopiesRemainCompatible() {
        val simulation = NativeBombSimulation(stage())
        simulation.requireStageDefinition(stage())
    }
}
