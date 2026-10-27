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
}
