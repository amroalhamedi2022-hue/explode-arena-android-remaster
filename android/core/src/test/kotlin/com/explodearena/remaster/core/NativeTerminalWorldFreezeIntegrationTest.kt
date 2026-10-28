package com.explodearena.remaster.core

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** N41: terminal outcome/fuse isolation through the production arena tick. */
class NativeTerminalWorldFreezeIntegrationTest {
    private fun originalStages(): List<StageData> {
        val file = File("../../web/assets/stages.js")
        assertTrue("Missing original stage source: ${file.absolutePath}", file.isFile)
        return StageCatalog.parse(file.readText(Charsets.UTF_8))
    }

    @Test fun lostRoundFreezesActorsAndLiveFuseAcrossAll41OriginalArenas() {
        val stages = originalStages()
        assertEquals((0..40).toList(), stages.map { it.id })
        assertEquals(mapOf(1 to 18, 2 to 2, 4 to 21),
            stages.groupingBy { it.spawns.size }.eachCount())

        for (stage in stages) {
            val spawn = NativeSpawnLayout.resolve(stage).first()
            val player = Actor(spawn.x, spawn.y)
            val roster = NativeOpponentRoster.create(stage, player)
            val simulation = NativeBombSimulation(stage)
            assertTrue("arena ${stage.id}: player bomb placement", simulation.place(player))

            val playerVitality = NativeVitality(
                initialHealth = 1,
                initialInvulnerabilitySeconds = 0.0,
                hitInvulnerabilitySeconds = 0.0,
            ).apply { assertTrue(tick(true)) }
            val round = NativeRoundFlow()
            round.tick(
                playerDefeated = playerVitality.defeated,
                opponentDefeated = false,
                opponentPresent = roster.present,
                hasNextStage = true,
            )
            assertEquals("arena ${stage.id}", NativeRoundFlow.State.LOST, round.state)
            assertTrue("arena ${stage.id}", round.isLatched)

            roster.bots.forEach { it.walking = true }
            val playerPosition = player.x to player.y
            val playerPassBomb = player.passBomb
            val botPositions = roster.bots.associate { it.owner to (it.actor.x to it.actor.y) }
            val initialBomb = simulation.bombs.single()

            repeat(120) { tick ->
                val result = NativeArenaTick.advance(
                    stage = stage,
                    player = player,
                    playerVitality = playerVitality,
                    opponents = roster,
                    simulation = simulation,
                    roundFlow = round,
                    tick = tick.toLong(),
                    dx = 1.0,
                    dy = -1.0,
                    hasNextStage = true,
                )
                assertFalse("arena ${stage.id}: no player movement after LOST", result.playerWalking)
                assertEquals("arena ${stage.id}: no bomb placement", 0, result.bombsPlaced)
                assertEquals("arena ${stage.id}: no detonation", 0, result.bombsDetonated)
                assertFalse("arena ${stage.id}: no stage transition", result.advanceStage)
            }

            assertEquals("arena ${stage.id}: player position", playerPosition, player.x to player.y)
            assertEquals("arena ${stage.id}: owner exit-grace state", playerPassBomb, player.passBomb)
            assertEquals("arena ${stage.id}: fuse and bomb snapshot", initialBomb, simulation.bombs.single())
            for (bot in roster.bots) {
                assertEquals("arena ${stage.id}: ${bot.owner} position",
                    botPositions.getValue(bot.owner), bot.actor.x to bot.actor.y)
                assertFalse("arena ${stage.id}: stale walking animation", bot.walking)
            }
            assertEquals("arena ${stage.id}: terminal result remains latched",
                NativeRoundFlow.State.LOST, round.state)
        }
    }


    @Test fun botsStopWalkingOnTheExactTickThatLatchesLoss() {
        val stage = StageData(
            id = 0, image = "test.png", width = 128, height = 128,
            obstacleX = 0, obstacleY = 0, grid = List(16) { IntArray(16) },
            spawns = listOf(Spawn(96.0, 96.0), Spawn(40.0, 40.0)),
        )

        // Prove the fixture produces movement on the same AI tick in an otherwise
        // equivalent live round; otherwise a false result would not test the fix.
        val baselinePlayer = Actor(96.0, 96.0)
        val baselineRoster = NativeOpponentRoster.create(stage, baselinePlayer)
        baselineRoster.advanceSimulationTick(stage, baselinePlayer, NativeBombSimulation(stage), 6L)
        assertTrue("fixture must produce a moving bot", baselineRoster.bots.any { it.walking })

        val player = Actor(96.0, 96.0)
        val roster = NativeOpponentRoster.create(stage, player)
        val simulation = NativeBombSimulation(
            stage, bombFuseSeconds = 0.1, blastLifetimeSeconds = 0.5, bombRange = 1,
        )
        assertTrue(simulation.place(player))
        repeat(6) { simulation.tick() }
        assertTrue("fixture must have an active flame", simulation.flames.isNotEmpty())
        assertTrue(simulation.isBurning(player.x, player.y, StageCollision.ACTOR_RADIUS))

        val vitality = NativeVitality(
            initialHealth = 1, initialInvulnerabilitySeconds = 0.0,
            hitInvulnerabilitySeconds = 0.0,
        )
        val round = NativeRoundFlow(victoryHoldTicks = 1)
        NativeArenaTick.advance(
            stage, player, vitality, roster, simulation, round,
            tick = 6L, dx = 0.0, dy = 0.0, hasNextStage = true,
        )

        assertTrue("final flame must latch player defeat", vitality.defeated)
        assertEquals(NativeRoundFlow.State.LOST, round.state)
        assertTrue("terminal transition tick must stop bot walking", roster.bots.none { it.walking })
    }

    @Test fun drawFreezesFuseAndCannotBecomeAnotherOutcome() {
        val stage = StageData(
            id = 0, image = "test.png", width = 128, height = 128,
            obstacleX = 0, obstacleY = 0, grid = List(16) { IntArray(16) },
            spawns = listOf(Spawn(96.0, 96.0), Spawn(40.0, 40.0)),
        )
        val player = Actor(96.0, 96.0)
        val bot = NativeOpponentRoster.BotSlot(
            owner = "bot",
            actor = Actor(40.0, 40.0),
            vitality = NativeVitality(1, 0.0, 0.0).apply { assertTrue(tick(true)) },
        )
        val roster = NativeOpponentRoster(listOf(bot))
        val simulation = NativeBombSimulation(stage, bombFuseSeconds = 0.1)
        assertTrue(simulation.place(player))
        val playerVitality = NativeVitality(1, 0.0, 0.0).apply { assertTrue(tick(true)) }
        val round = NativeRoundFlow()
        round.tick(true, true, opponentPresent = true, hasNextStage = true)
        assertEquals(NativeRoundFlow.State.DRAW, round.state)
        val initialBomb = simulation.bombs.single()
        val botPosition = bot.actor.x to bot.actor.y

        repeat(120) { tick ->
            val result = NativeArenaTick.advance(
                stage, player, playerVitality, roster, simulation, round,
                tick.toLong(), dx = -1.0, dy = 1.0, hasNextStage = true,
            )
            assertFalse(result.playerWalking)
            assertEquals(0, result.bombsPlaced)
            assertEquals(0, result.bombsDetonated)
            assertFalse(result.advanceStage)
        }

        assertEquals(initialBomb, simulation.bombs.single())
        assertEquals(botPosition, bot.actor.x to bot.actor.y)
        assertEquals(NativeRoundFlow.State.DRAW, round.state)
        assertTrue(round.canRetry)
    }

    @Test fun victoryHoldKeepsTickingButAdvanceAndCompleteAreLatched() {
        val advancing = NativeRoundFlow(victoryHoldTicks = 1)
        advancing.tick(false, true, opponentPresent = true, hasNextStage = true)
        assertEquals(NativeRoundFlow.State.WON, advancing.state)
        assertFalse(advancing.isLatched)
        assertTrue(advancing.tick(false, true, opponentPresent = true, hasNextStage = true))
        assertEquals(NativeRoundFlow.State.ADVANCING, advancing.state)
        assertTrue(advancing.isLatched)
        assertFalse(advancing.tick(false, true, opponentPresent = true, hasNextStage = true))
        assertEquals(NativeRoundFlow.State.ADVANCING, advancing.state)

        val complete = NativeRoundFlow(victoryHoldTicks = 1)
        complete.tick(false, true, opponentPresent = true, hasNextStage = false)
        assertEquals(NativeRoundFlow.State.WON, complete.state)
        assertFalse(complete.tick(false, true, opponentPresent = true, hasNextStage = false))
        assertEquals(NativeRoundFlow.State.COMPLETE, complete.state)
        assertTrue(complete.isLatched)
    }
}
