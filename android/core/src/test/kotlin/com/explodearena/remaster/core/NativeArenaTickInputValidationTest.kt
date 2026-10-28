package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/** N56: malformed canonical-tick inputs must fail before any gameplay mutation. */
class NativeArenaTickInputValidationTest {
    private fun stage() = StageData(
        id = 0,
        image = "test.png",
        width = 96,
        height = 64,
        obstacleX = 0,
        obstacleY = 0,
        grid = List(8) { IntArray(12) },
        spawns = listOf(Spawn(24.0, 24.0), Spawn(72.0, 24.0)),
    )

    @Test fun negativeTickDoesNotMovePlayerOrAdvanceAnySubsystemBeforeFailure() {
        val stage = stage()
        val player = Actor(24.0, 24.0)
        val bot = NativeOpponentRoster.BotSlot("bot", Actor(72.0, 24.0))
        val roster = NativeOpponentRoster(listOf(bot))
        val simulation = NativeBombSimulation(stage)
        val vitality = NativeVitality()
        val round = NativeRoundFlow()
        val playerBefore = Actor(player.x, player.y, player.passBomb)
        val botBefore = Actor(bot.actor.x, bot.actor.y, bot.actor.passBomb)
        val healthBefore = vitality.health
        val botHealthBefore = bot.vitality.health

        assertThrows(IllegalArgumentException::class.java) {
            NativeArenaTick.advance(
                stage, player, vitality, roster, simulation, round,
                tick = -1L, dx = 1.0, dy = 0.0, hasNextStage = true,
            )
        }

        assertEquals(playerBefore.x, player.x, 0.0)
        assertEquals(playerBefore.y, player.y, 0.0)
        assertEquals(playerBefore.passBomb, player.passBomb)
        assertEquals(botBefore.x, bot.actor.x, 0.0)
        assertEquals(botBefore.y, bot.actor.y, 0.0)
        assertEquals(botBefore.passBomb, bot.actor.passBomb)
        assertEquals(healthBefore, vitality.health)
        assertEquals(botHealthBefore, bot.vitality.health)
        assertTrue(simulation.bombs.isEmpty())
        assertTrue(simulation.flames.isEmpty())
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)
    }

    @Test fun nonFiniteAndOutOfRangeInputIsRejectedAtomicallyEvenWithoutBots() {
        val stage = stage().copy(spawns = listOf(Spawn(24.0, 24.0)))
        val player = Actor(24.0, 24.0)
        val roster = NativeOpponentRoster(emptyList())
        val simulation = NativeBombSimulation(stage)
        val vitality = NativeVitality()
        val round = NativeRoundFlow()

        for ((dx, dy) in listOf(
            Double.NaN to 0.0,
            0.0 to Double.POSITIVE_INFINITY,
            1.01 to 0.0,
            0.0 to -1.01,
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                NativeArenaTick.advance(
                    stage, player, vitality, roster, simulation, round,
                    tick = 0L, dx = dx, dy = dy, hasNextStage = false,
                )
            }
            assertEquals(24.0, player.x, 0.0)
            assertEquals(24.0, player.y, 0.0)
            assertEquals(3, vitality.health)
            assertTrue(simulation.bombs.isEmpty())
            assertEquals(NativeRoundFlow.State.PLAYING, round.state)
        }
    }

    @Test fun duplicateCanonicalTickIsRejectedBeforePlayerOrWorldMutation() {
        val stage = stage()
        val player = Actor(24.0, 24.0)
        val bot = NativeOpponentRoster.BotSlot("bot", Actor(72.0, 24.0))
        val roster = NativeOpponentRoster(listOf(bot))
        val simulation = NativeBombSimulation(stage)
        val vitality = NativeVitality()
        val round = NativeRoundFlow()

        NativeArenaTick.advance(
            stage, player, vitality, roster, simulation, round,
            tick = 7L, dx = 0.0, dy = 0.0, hasNextStage = true,
        )
        val playerBefore = Actor(player.x, player.y, player.passBomb)
        val botBefore = Actor(bot.actor.x, bot.actor.y, bot.actor.passBomb)
        val bombsBefore = simulation.bombs
        val flamesBefore = simulation.flames
        val playerHealthBefore = vitality.health
        val botHealthBefore = bot.vitality.health
        val roundBefore = round.state

        assertThrows(IllegalArgumentException::class.java) {
            NativeArenaTick.advance(
                stage, player, vitality, roster, simulation, round,
                tick = 7L, dx = 1.0, dy = 0.0, hasNextStage = true,
            )
        }

        assertEquals(playerBefore.x, player.x, 0.0)
        assertEquals(playerBefore.y, player.y, 0.0)
        assertEquals(playerBefore.passBomb, player.passBomb)
        assertEquals(botBefore.x, bot.actor.x, 0.0)
        assertEquals(botBefore.y, bot.actor.y, 0.0)
        assertEquals(botBefore.passBomb, bot.actor.passBomb)
        assertEquals(bombsBefore, simulation.bombs)
        assertEquals(flamesBefore, simulation.flames)
        assertEquals(playerHealthBefore, vitality.health)
        assertEquals(botHealthBefore, bot.vitality.health)
        assertEquals(roundBefore, round.state)

        // The rejected duplicate did not corrupt the clock; the next unique tick works.
        NativeArenaTick.advance(
            stage, player, vitality, roster, simulation, round,
            tick = 8L, dx = 0.0, dy = 0.0, hasNextStage = true,
        )
    }

    @Test fun duplicateTickCannotFastForwardTheFrozenVictoryHold() {
        val stage = stage().copy(spawns = listOf(Spawn(24.0, 24.0)))
        val player = Actor(24.0, 24.0)
        val roster = NativeOpponentRoster(emptyList())
        val simulation = NativeBombSimulation(stage)
        val vitality = NativeVitality()
        val round = NativeRoundFlow(victoryHoldTicks = 2)
        assertFalse(round.tick(
            playerDefeated = false, opponentDefeated = true,
            opponentPresent = true, hasNextStage = true,
        ))
        assertEquals(NativeRoundFlow.State.WON, round.state)

        val firstHoldTick = NativeArenaTick.advance(
            stage, player, vitality, roster, simulation, round,
            tick = 20L, dx = 0.0, dy = 0.0, hasNextStage = true,
        )
        assertFalse(firstHoldTick.advanceStage)
        assertEquals(NativeRoundFlow.State.WON, round.state)

        assertThrows(IllegalArgumentException::class.java) {
            NativeArenaTick.advance(
                stage, player, vitality, roster, simulation, round,
                tick = 20L, dx = 0.0, dy = 0.0, hasNextStage = true,
            )
        }
        assertEquals(NativeRoundFlow.State.WON, round.state)

        val secondHoldTick = NativeArenaTick.advance(
            stage, player, vitality, roster, simulation, round,
            tick = 21L, dx = 0.0, dy = 0.0, hasNextStage = true,
        )
        assertTrue(secondHoldTick.advanceStage)
        assertEquals(NativeRoundFlow.State.ADVANCING, round.state)
    }

}
