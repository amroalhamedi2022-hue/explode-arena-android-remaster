package com.explodearena.remaster.core

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** N35: all-arena integration through the same canonical tick used by libGDX. */
class NativeArenaTickAllArenasIntegrationTest {
    private fun originalStages(): List<StageData> {
        val file = File("../../web/assets/stages.js")
        assertTrue("Missing original stage source: ${file.absolutePath}", file.isFile)
        return StageCatalog.parse(file.readText(Charsets.UTF_8))
    }

    @Test fun canonicalProductionTickRunsPlayerBotsBombsAndRoundFlowAcrossAllArenas() {
        val stages = originalStages()
        assertEquals((0..40).toList(), stages.map { it.id })
        assertEquals(
            mapOf(1 to 18, 2 to 2, 4 to 21),
            stages.groupingBy { it.spawns.size }.eachCount(),
        )
        val directions = listOf(1.0 to 0.0, 0.0 to 1.0, -1.0 to 0.0, 0.0 to -1.0)

        for (stage in stages) {
            val player = NativeSpawnLayout.resolve(stage).first().let { Actor(it.x, it.y) }
            val roster = NativeOpponentRoster.create(stage, player)
            val simulation = NativeBombSimulation(stage)
            val playerVitality = NativeVitality()
            val round = NativeRoundFlow(victoryHoldTicks = 90)
            val label = "arena ${stage.id}"

            assertEquals(label, stage.spawns.size - 1, roster.bots.size)
            assertEquals(label, roster.bots.size, roster.bots.map { it.owner }.toSet().size)
            assertEquals(label, roster.bots.size, roster.bots.map { it.controller }.toSet().size)
            assertEquals(label, roster.bots.size, roster.bots.map { it.actor }.toSet().size)

            repeat(120) { tick ->
                val (dx, dy) = directions[tick % directions.size]
                val result = NativeArenaTick.advance(
                    stage = stage,
                    player = player,
                    playerVitality = playerVitality,
                    opponents = roster,
                    simulation = simulation,
                    roundFlow = round,
                    tick = tick.toLong(),
                    dx = dx,
                    dy = dy,
                    hasNextStage = false,
                )
                assertTrue("$label negative bomb placement count", result.bombsPlaced >= 0)
                assertTrue("$label negative detonation count", result.bombsDetonated >= 0)

                assertFalse(
                    "$label player entered terrain or a blocking bomb at tick $tick",
                    StageCollision.blocked(
                        stage, player.x, player.y, StageCollision.ACTOR_RADIUS,
                        simulation.bombs, player.passBomb,
                    ),
                )
                val living = roster.bots.filterNot { it.vitality.defeated }
                for (slot in living) {
                    assertFalse(
                        "$label ${slot.owner} entered terrain or a blocking bomb at tick $tick",
                        StageCollision.blocked(
                            stage, slot.actor.x, slot.actor.y, StageCollision.ACTOR_RADIUS,
                            simulation.bombs, slot.actor.passBomb,
                        ),
                    )
                }
                val actors = listOf(player) + living.map { it.actor }
                for (i in actors.indices) for (j in i + 1 until actors.size) {
                    assertFalse(
                        "$label actors $i/$j overlap at tick $tick",
                        StageCollision.blockedByActors(actors[i].x, actors[i].y, listOf(actors[j])),
                    )
                }
            }
        }
    }

    @Test fun playerDamageAtFinalFlameBoundaryResolvesRoundInTheSameTick() {
        val stage = StageData(
            id = 0, image = "test.png", width = 64, height = 64,
            obstacleX = 0, obstacleY = 0, grid = List(8) { IntArray(8) },
            spawns = listOf(Spawn(24.0, 24.0)),
        )
        val player = Actor(24.0, 24.0)
        val simulation = NativeBombSimulation(
            stage, bombFuseSeconds = 0.1, blastLifetimeSeconds = 0.05, bombRange = 1,
        )
        assertTrue(simulation.place(player))
        val roster = NativeOpponentRoster.create(stage, player)
        val vitality = NativeVitality(
            initialHealth = 1, initialInvulnerabilitySeconds = 0.0,
            hitInvulnerabilitySeconds = 0.0,
        )
        val round = NativeRoundFlow(victoryHoldTicks = 1)

        repeat(12) { tick ->
            NativeArenaTick.advance(
                stage, player, vitality, roster, simulation, round,
                tick.toLong(), dx = 0.0, dy = 0.0, hasNextStage = false,
            )
        }

        assertTrue("Player must receive the final active-flame damage sample", vitality.defeated)
        assertEquals(NativeRoundFlow.State.LOST, round.state)
    }

    @Test fun invalidPlayerSpeedIsRejectedBeforeTerminalTickStopsOpponents() {
        val stage = StageData(
            id = 0, image = "test.png", width = 64, height = 64,
            obstacleX = 0, obstacleY = 0,
            grid = List(8) { IntArray(8) },
            spawns = listOf(Spawn(24.0, 24.0), Spawn(40.0, 40.0)),
        )
        val player = Actor(24.0, 24.0)
        val roster = NativeOpponentRoster.create(stage, player)
        val bot = roster.bots.single()
        bot.walking = true
        val simulation = NativeBombSimulation(stage)
        assertTrue(simulation.place(player))
        val playerVitality = NativeVitality()
        val round = NativeRoundFlow(victoryHoldTicks = 1)
        round.tick(
            playerDefeated = true,
            opponentDefeated = false,
            opponentPresent = true,
            hasNextStage = false,
        )
        assertEquals(NativeRoundFlow.State.LOST, round.state)

        val playerPosition = player.x to player.y
        val botPosition = bot.actor.x to bot.actor.y
        val bombCount = simulation.bombs.size
        for (invalidSpeed in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0)) {
            assertThrows(IllegalArgumentException::class.java) {
                NativeArenaTick.advance(
                    stage = stage,
                    player = player,
                    playerVitality = playerVitality,
                    opponents = roster,
                    simulation = simulation,
                    roundFlow = round,
                    tick = 0L,
                    dx = 0.0,
                    dy = 0.0,
                    hasNextStage = false,
                    playerSpeed = invalidSpeed,
                )
            }
            assertEquals(playerPosition.first, player.x, 0.0)
            assertEquals(playerPosition.second, player.y, 0.0)
            assertEquals(botPosition.first, bot.actor.x, 0.0)
            assertEquals(botPosition.second, bot.actor.y, 0.0)
            assertTrue("terminal tick must not clear bot walking before validation", bot.walking)
            assertEquals(bombCount, simulation.bombs.size)
            assertEquals(NativeRoundFlow.State.LOST, round.state)
        }
    }

    private data class FinalFlameOutcome(
        val botDefeated: Boolean,
        val playerDefeated: Boolean,
        val botDefeatedTick: Int,
        val advanceSignals: Int,
        val finalState: NativeRoundFlow.State,
    )

    private fun runBotFinalFlameScenario(hasNextStage: Boolean): FinalFlameOutcome {
        val stage = StageData(
            id = 0, image = "test.png", width = 128, height = 128,
            obstacleX = 0, obstacleY = 0,
            grid = List(16) { IntArray(16) },
            spawns = listOf(Spawn(96.0, 96.0), Spawn(40.0, 40.0)),
        )
        val player = Actor(96.0, 96.0)
        val playerVitality = NativeVitality(
            initialHealth = 3,
            initialInvulnerabilitySeconds = 0.0,
            hitInvulnerabilitySeconds = 0.0,
        )
        val botVitality = NativeVitality(
            initialHealth = 1,
            initialInvulnerabilitySeconds = 0.0,
            hitInvulnerabilitySeconds = 0.0,
        )
        val bot = NativeOpponentRoster.BotSlot(
            owner = "bot",
            actor = Actor(40.0, 40.0),
            vitality = botVitality,
        )
        val roster = NativeOpponentRoster(listOf(bot))
        val simulation = NativeBombSimulation(
            stage,
            bombLimit = 1,
            bombFuseSeconds = 0.1,
            blastLifetimeSeconds = 0.05,
            bombRange = 1,
        )
        assertTrue("Bot-owned timed bomb must be accepted", simulation.place(bot.actor, "bot"))
        val round = NativeRoundFlow(victoryHoldTicks = 1)
        var botDefeatedTick = -1
        var advanceSignals = 0

        repeat(12) { tick ->
            val result = NativeArenaTick.advance(
                stage = stage,
                player = player,
                playerVitality = playerVitality,
                opponents = roster,
                simulation = simulation,
                roundFlow = round,
                tick = tick.toLong(),
                dx = 0.0,
                dy = 0.0,
                hasNextStage = hasNextStage,
            )
            if (botVitality.defeated && botDefeatedTick < 0) botDefeatedTick = tick
            if (result.advanceStage) advanceSignals++
        }
        return FinalFlameOutcome(
            botDefeated = botVitality.defeated,
            playerDefeated = playerVitality.defeated,
            botDefeatedTick = botDefeatedTick,
            advanceSignals = advanceSignals,
            finalState = round.state,
        )
    }

    @Test fun botDeathOnFinalFlameTickWinsRoundAndSignalsNextStageExactlyOnce() {
        val outcome = runBotFinalFlameScenario(hasNextStage = true)

        assertTrue("Bot must be defeated by the timed bomb's final flame interval", outcome.botDefeated)
        assertTrue("The bot must be defeated during the 12 fixed-step integration window", outcome.botDefeatedTick in 0..11)
        assertFalse("The distant player must survive", outcome.playerDefeated)
        assertEquals("Victory hold should emit one transition, not repeat", 1, outcome.advanceSignals)
        assertEquals(NativeRoundFlow.State.ADVANCING, outcome.finalState)
    }

    @Test fun botDeathOnFinalFlameTickCompletesFinalArenaWithoutWrapping() {
        val outcome = runBotFinalFlameScenario(hasNextStage = false)

        assertTrue(outcome.botDefeated)
        assertFalse(outcome.playerDefeated)
        assertEquals("Final arena must not emit a next-stage signal", 0, outcome.advanceSignals)
        assertEquals(NativeRoundFlow.State.COMPLETE, outcome.finalState)
    }

    @Test fun simultaneousFinalFlameDefeatResolvesDrawWithoutStageAdvance() {
        val stage = StageData(
            id = 0, image = "test.png", width = 128, height = 128,
            obstacleX = 0, obstacleY = 0, grid = List(16) { IntArray(16) },
            spawns = listOf(Spawn(40.0, 40.0), Spawn(72.0, 40.0)),
        )
        val player = Actor(40.0, 40.0)
        val playerVitality = NativeVitality(
            initialHealth = 1, initialInvulnerabilitySeconds = 0.0,
            hitInvulnerabilitySeconds = 0.0,
        )
        val botVitality = NativeVitality(
            initialHealth = 1, initialInvulnerabilitySeconds = 0.0,
            hitInvulnerabilitySeconds = 0.0,
        )
        val roster = NativeOpponentRoster(
            listOf(
                NativeOpponentRoster.BotSlot(
                    owner = "bot",
                    actor = Actor(72.0, 40.0),
                    vitality = botVitality,
                ),
            ),
        )
        val simulation = NativeBombSimulation(
            stage, bombLimit = 1, bombFuseSeconds = 0.1,
            blastLifetimeSeconds = 0.05, bombRange = 2,
        )
        assertTrue("Player's timed bomb must be accepted", simulation.place(player, "player"))
        val round = NativeRoundFlow(victoryHoldTicks = 1)
        var playerDefeatedTick = -1
        var botDefeatedTick = -1
        var advanceSignals = 0

        repeat(12) { tick ->
            val result = NativeArenaTick.advance(
                stage = stage,
                player = player,
                playerVitality = playerVitality,
                opponents = roster,
                simulation = simulation,
                roundFlow = round,
                tick = tick.toLong(),
                dx = 0.0,
                dy = 0.0,
                hasNextStage = true,
            )
            if (playerVitality.defeated && playerDefeatedTick < 0) playerDefeatedTick = tick
            if (botVitality.defeated && botDefeatedTick < 0) botDefeatedTick = tick
            if (result.advanceStage) advanceSignals++
        }

        assertTrue("Player must be defeated by the timed blast", playerVitality.defeated)
        assertTrue("The final living bot must be defeated by the same blast", botVitality.defeated)
        assertTrue("Both deaths must occur during the integration window", playerDefeatedTick in 0..11)
        assertEquals("Mutual final-flame deaths must share one canonical tick", playerDefeatedTick, botDefeatedTick)
        assertEquals(NativeRoundFlow.State.DRAW, round.state)
        assertTrue("A draw must allow retry", round.canRetry)
        assertEquals("A draw must never advance the stage", 0, advanceSignals)
    }

    @Test fun fourSpawnRoundWaitsForLastLivingBotBeforeAdvancingExactlyOnce() {
        val stage = StageData(
            id = 0, image = "test.png", width = 128, height = 128,
            obstacleX = 0, obstacleY = 0, grid = List(16) { IntArray(16) },
            spawns = listOf(
                Spawn(40.0, 88.0),
                Spawn(40.0, 40.0),
                Spawn(56.0, 40.0),
                Spawn(72.0, 40.0),
            ),
        )
        val player = Actor(40.0, 88.0)
        val alreadyDefeated = listOf(
            NativeOpponentRoster.BotSlot(
                owner = "bot", actor = Actor(40.0, 40.0),
                vitality = NativeVitality(1, 0.0, 0.0),
            ),
            NativeOpponentRoster.BotSlot(
                owner = "bot2", actor = Actor(56.0, 40.0),
                vitality = NativeVitality(1, 0.0, 0.0),
            ),
        )
        val finalBot = NativeOpponentRoster.BotSlot(
            owner = "bot3", actor = Actor(72.0, 40.0),
            vitality = NativeVitality(1, 0.0, 0.0),
        )
        val roster = NativeOpponentRoster(alreadyDefeated + finalBot)
        alreadyDefeated.forEach { assertTrue("Fixture bot must be defeated", it.vitality.tick(true)) }
        assertEquals(1, roster.aliveCount)
        assertFalse(roster.allDefeated)
        assertEquals(1, roster.blockingActors().size)

        val simulation = NativeBombSimulation(
            stage, bombLimit = 1, bombFuseSeconds = 0.1,
            blastLifetimeSeconds = 0.05, bombRange = 1,
        )
        assertTrue("The last living bot's timed bomb must be accepted", simulation.place(finalBot.actor, finalBot.owner))
        val playerVitality = NativeVitality(
            initialHealth = 3, initialInvulnerabilitySeconds = 0.0,
            hitInvulnerabilitySeconds = 0.0,
        )
        val round = NativeRoundFlow(victoryHoldTicks = 1)
        var prematureOutcome = false
        var finalBotDefeatedTick = -1
        var advanceSignals = 0

        repeat(16) { tick ->
            val result = NativeArenaTick.advance(
                stage = stage,
                player = player,
                playerVitality = playerVitality,
                opponents = roster,
                simulation = simulation,
                roundFlow = round,
                tick = tick.toLong(),
                dx = 0.0,
                dy = 0.0,
                hasNextStage = true,
            )
            if (!finalBot.vitality.defeated && round.state != NativeRoundFlow.State.PLAYING) {
                prematureOutcome = true
            }
            if (finalBot.vitality.defeated && finalBotDefeatedTick < 0) finalBotDefeatedTick = tick
            if (result.advanceStage) advanceSignals++
        }

        assertFalse("One or two defeated bots must not win a four-spawn arena", prematureOutcome)
        assertTrue("The final living bot must be defeated by its timed bomb", finalBot.vitality.defeated)
        assertTrue("The final bot should be defeated inside the integration window", finalBotDefeatedTick in 0..15)
        assertFalse("The distant player must survive", playerVitality.defeated)
        assertEquals("A four-spawn victory emits one transition only", 1, advanceSignals)
        assertEquals(NativeRoundFlow.State.ADVANCING, round.state)
    }


    @Test fun canonicalTickKeepsFinalArenaFinalWhenNextStageFlagChangesDuringVictoryHold() {
        val stage = StageData(
            id = 40,
            image = "test.png",
            width = 64,
            height = 64,
            obstacleX = 0,
            obstacleY = 0,
            grid = List(8) { IntArray(8) },
            spawns = listOf(Spawn(24.0, 24.0), Spawn(40.0, 40.0)),
        )
        val state = NativeArenaRoundStateFactory.create(stage)
        val bot = state.opponents.bots.single()
        repeat(350) { bot.vitality.tick(burning = true) }
        assertTrue("precondition: last opponent is defeated", bot.vitality.defeated)

        val victory = NativeArenaTick.advance(
            stage = stage,
            player = state.player,
            playerVitality = state.playerVitality,
            opponents = state.opponents,
            simulation = state.simulation,
            roundFlow = state.roundFlow,
            tick = 0L,
            dx = 0.0,
            dy = 0.0,
            hasNextStage = false,
        )
        assertFalse(victory.advanceStage)
        assertEquals(NativeRoundFlow.State.WON, state.roundFlow.state)

        repeat(90) { index ->
            val result = NativeArenaTick.advance(
                stage = stage,
                player = state.player,
                playerVitality = state.playerVitality,
                opponents = state.opponents,
                simulation = state.simulation,
                roundFlow = state.roundFlow,
                tick = index.toLong() + 1L,
                dx = 0.0,
                dy = 0.0,
                hasNextStage = true,
            )
            assertFalse("final arena must not signal a transition at hold tick $index", result.advanceStage)
        }
        assertEquals(NativeRoundFlow.State.COMPLETE, state.roundFlow.state)
    }

}