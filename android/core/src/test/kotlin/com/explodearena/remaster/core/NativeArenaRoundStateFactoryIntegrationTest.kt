package com.explodearena.remaster.core

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** N44: stage/retry construction uses the same fresh-state factory as production. */
class NativeArenaRoundStateFactoryIntegrationTest {
    private fun originalStages(): List<StageData> {
        val file = File("../../web/assets/stages.js")
        assertTrue("Missing original stage source: ${file.absolutePath}", file.isFile)
        return StageCatalog.parse(file.readText(Charsets.UTF_8))
    }

    @Test fun createsIndependentPlayerAndBotStateForAllFortyOneArenas() {
        val stages = originalStages()
        assertEquals((0..40).toList(), stages.map { it.id })
        assertEquals(mapOf(1 to 18, 2 to 2, 4 to 21),
            stages.groupingBy { it.spawns.size }.eachCount())

        for (stage in stages) {
            val state = NativeArenaRoundStateFactory.create(stage)
            val label = "arena ${stage.id}"
            val expectedPlayer = NativeSpawnLayout.resolve(stage).first()
            assertEquals(label, expectedPlayer.x, state.player.x, 0.0)
            assertEquals(label, expectedPlayer.y, state.player.y, 0.0)
            assertEquals(label, stage.spawns.size - 1, state.opponents.bots.size)
            assertEquals(label, state.opponents.bots.size,
                state.opponents.bots.map { it.owner }.toSet().size)
            assertEquals(label, state.opponents.bots.size,
                state.opponents.bots.map { it.actor }.toSet().size)
            assertTrue(label, state.simulation.bombs.isEmpty())
            assertTrue(label, state.simulation.flames.isEmpty())
            assertEquals(label, NativeRoundFlow.State.PLAYING, state.roundFlow.state)
            assertFalse(label, state.playerVitality.defeated)
        }
    }

    @Test fun retryFactoryDropsBombsAndLatchedOutcomeInsteadOfReusingMutableState() {
        val stage = StageData(
            id = 0, image = "test.png", width = 64, height = 64,
            obstacleX = 0, obstacleY = 0, grid = List(8) { IntArray(8) },
            spawns = listOf(Spawn(24.0, 24.0), Spawn(40.0, 40.0)),
        )
        val previous = NativeArenaRoundStateFactory.create(stage)
        assertTrue("precondition: old round can place a bomb", previous.simulation.place(previous.player))
        assertEquals(1, previous.simulation.bombs.size)
        assertFalse(previous.simulation.flames.isNotEmpty())
        assertFalse(previous.roundFlow.tick(
            playerDefeated = true,
            opponentDefeated = false,
            opponentPresent = previous.opponents.present,
            hasNextStage = true,
        ))
        assertEquals(NativeRoundFlow.State.LOST, previous.roundFlow.state)

        val retry = NativeArenaRoundStateFactory.create(stage)
        assertNotSame(previous.player, retry.player)
        assertNotSame(previous.simulation, retry.simulation)
        assertNotSame(previous.playerVitality, retry.playerVitality)
        assertNotSame(previous.opponents, retry.opponents)
        assertNotSame(previous.roundFlow, retry.roundFlow)
        assertTrue(retry.simulation.bombs.isEmpty())
        assertTrue(retry.simulation.flames.isEmpty())
        assertEquals(1L, retry.simulation.nextBombId)
        assertEquals(NativeRoundFlow.State.PLAYING, retry.roundFlow.state)
        assertFalse(retry.playerVitality.defeated)
        assertEquals(1, retry.opponents.bots.size)
    }
}
