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

    @Test fun excessiveCollisionSubstepsAreRejectedBeforeTickReservationAndCanRetry() {
        // Deliberately malformed dimensions prove the substep cap is independent
        // of the arena's claimed pixel size; the real catalog is much smaller.
        val stage = stage().copy(
            width = Int.MAX_VALUE,
            height = Int.MAX_VALUE,
            spawns = listOf(Spawn(24.0, 24.0)),
        )
        val player = Actor(24.0, 24.0)
        val roster = NativeOpponentRoster(emptyList())
        val simulation = NativeBombSimulation(stage)
        val vitality = NativeVitality()
        val round = NativeRoundFlow()

        assertThrows(IllegalArgumentException::class.java) {
            NativeArenaTick.advance(
                stage, player, vitality, roster, simulation, round,
                tick = 0L, dx = 1.0, dy = 0.0, hasNextStage = false,
                playerSpeed = 100_000_000.0,
            )
        }
        assertEquals(24.0, player.x, 0.0)
        assertEquals(24.0, player.y, 0.0)
        assertEquals(3, vitality.health)
        assertTrue(simulation.bombs.isEmpty())
        assertTrue(simulation.flames.isEmpty())
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)

        // The rejected request did not consume the canonical tick.
        val retry = NativeArenaTick.advance(
            stage, player, vitality, roster, simulation, round,
            tick = 0L, dx = 0.0, dy = 0.0, hasNextStage = false,
        )
        assertFalse(retry.playerWalking)
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)
    }

    @Test fun playerInsideSolidTerrainIsRejectedBeforeTickReservationAndCanRetry() {
        val original = stage()
        val wallGrid = original.grid.mapIndexed { row, cells ->
            cells.copyOf().also { if (row == 3) it[3] = 1 }
        }
        val stage = original.copy(grid = wallGrid)
        val player = Actor(32.0, 28.0) // overlaps the solid cell at column 3, row 3
        val bot = NativeOpponentRoster.BotSlot("bot", Actor(72.0, 24.0))
        val roster = NativeOpponentRoster(listOf(bot))
        val simulation = NativeBombSimulation(stage)
        val vitality = NativeVitality()
        val round = NativeRoundFlow()

        assertThrows(IllegalArgumentException::class.java) {
            NativeArenaTick.advance(
                stage, player, vitality, roster, simulation, round,
                tick = 11L, dx = 1.0, dy = 0.0, hasNextStage = true,
            )
        }
        assertEquals(32.0, player.x, 0.0)
        assertEquals(28.0, player.y, 0.0)
        assertEquals(72.0, bot.actor.x, 0.0)
        assertEquals(24.0, bot.actor.y, 0.0)
        assertTrue(simulation.bombs.isEmpty())
        assertTrue(simulation.flames.isEmpty())
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)

        // Repair the invalid start position and reuse the same tick. Rejection
        // must not reserve the canonical clock or partially advance the world.
        player.x = 16.0
        player.y = 16.0
        NativeArenaTick.advance(
            stage, player, vitality, roster, simulation, round,
            tick = 11L, dx = 0.0, dy = 0.0, hasNextStage = true,
        )
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)
    }

    @Test fun opponentInsideSolidTerrainIsRejectedBeforePlayerMoves() {
        val original = stage()
        val wallGrid = original.grid.mapIndexed { row, cells ->
            cells.copyOf().also { if (row == 3) it[3] = 1 }
        }
        val stage = original.copy(grid = wallGrid)
        val player = Actor(16.0, 16.0)
        val bot = NativeOpponentRoster.BotSlot("bot", Actor(32.0, 28.0))
        val roster = NativeOpponentRoster(listOf(bot))
        val simulation = NativeBombSimulation(stage)
        val vitality = NativeVitality()
        val round = NativeRoundFlow()

        assertThrows(IllegalArgumentException::class.java) {
            NativeArenaTick.advance(
                stage, player, vitality, roster, simulation, round,
                tick = 4L, dx = 1.0, dy = 0.0, hasNextStage = true,
            )
        }
        assertEquals(16.0, player.x, 0.0)
        assertEquals(16.0, player.y, 0.0)
        assertEquals(32.0, bot.actor.x, 0.0)
        assertEquals(28.0, bot.actor.y, 0.0)
        assertTrue(simulation.bombs.isEmpty())
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)

        // The same tick remains available after the offending bot is repaired.
        bot.actor.x = 72.0
        bot.actor.y = 24.0
        NativeArenaTick.advance(
            stage, player, vitality, roster, simulation, round,
            tick = 4L, dx = 0.0, dy = 0.0, hasNextStage = true,
        )
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)
    }
    @Test fun playerOverlapWithLivingOpponentIsRejectedBeforeBombFuseAgesAndCanRetrySameTick() {
        val stage = stage()
        val player = Actor(24.0, 24.0)
        val bot = NativeOpponentRoster.BotSlot("bot", Actor(72.0, 24.0))
        val roster = NativeOpponentRoster(listOf(bot))
        val simulation = NativeBombSimulation(stage)
        assertTrue(simulation.place(player))
        val bombId = simulation.bombs.single().id
        val remainingBefore = requireNotNull(simulation.bombs.single().remaining)
        val vitality = NativeVitality()
        val round = NativeRoundFlow()

        // Simulate a corrupt/replayed world after a valid roster was created.
        bot.actor.x = player.x
        bot.actor.y = player.y
        assertThrows(IllegalArgumentException::class.java) {
            NativeArenaTick.advance(
                stage, player, vitality, roster, simulation, round,
                tick = 31L, dx = 0.0, dy = 0.0, hasNextStage = true,
            )
        }

        assertEquals(24.0, player.x, 0.0)
        assertEquals(24.0, player.y, 0.0)
        assertEquals(24.0, bot.actor.x, 0.0)
        assertEquals(24.0, bot.actor.y, 0.0)
        assertEquals(3, vitality.health)
        assertEquals(remainingBefore, requireNotNull(simulation.bombs.single().remaining), 0.0)
        assertTrue(simulation.flames.isEmpty())
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)

        // Repair the overlap. Reusing tick 31 must succeed and age the live fuse
        // exactly once; the rejected tick must not have been reserved.
        bot.actor.x = 72.0
        bot.actor.y = 24.0
        NativeArenaTick.advance(
            stage, player, vitality, roster, simulation, round,
            tick = 31L, dx = 0.0, dy = 0.0, hasNextStage = true,
        )
        val remainingAfter = requireNotNull(simulation.bombs.first { it.id == bombId }.remaining)
        assertEquals(remainingBefore - FixedStepClock.STEP_SECONDS, remainingAfter, 1e-9)
    }

    @Test fun livingOpponentsCannotStartOverlappingAndRejectedTickRemainsAvailable() {
        val stage = stage()
        val player = Actor(24.0, 24.0)
        val first = NativeOpponentRoster.BotSlot("bot", Actor(72.0, 24.0))
        val second = NativeOpponentRoster.BotSlot("bot2", Actor(72.0, 48.0))
        val roster = NativeOpponentRoster(listOf(first, second))
        val simulation = NativeBombSimulation(stage)
        val vitality = NativeVitality()
        val round = NativeRoundFlow()

        second.actor.x = first.actor.x
        second.actor.y = first.actor.y
        assertThrows(IllegalArgumentException::class.java) {
            NativeArenaTick.advance(
                stage, player, vitality, roster, simulation, round,
                tick = 18L, dx = 0.0, dy = 0.0, hasNextStage = true,
            )
        }
        assertEquals(24.0, player.x, 0.0)
        assertEquals(24.0, player.y, 0.0)
        assertEquals(72.0, first.actor.x, 0.0)
        assertEquals(24.0, first.actor.y, 0.0)
        assertEquals(72.0, second.actor.x, 0.0)
        assertEquals(24.0, second.actor.y, 0.0)
        assertTrue(simulation.bombs.isEmpty())
        assertTrue(simulation.flames.isEmpty())
        assertEquals(3, vitality.health)
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)

        second.actor.x = 72.0
        second.actor.y = 48.0
        NativeArenaTick.advance(
            stage, player, vitality, roster, simulation, round,
            tick = 18L, dx = 0.0, dy = 0.0, hasNextStage = true,
        )
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)
    }

}
