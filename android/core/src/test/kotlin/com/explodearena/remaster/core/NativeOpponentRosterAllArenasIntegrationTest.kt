package com.explodearena.remaster.core

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/**
 * N33: exercise the real 41-stage catalog through the opponent roster and
 * fixed-step bomb simulation. This is native-source integration coverage only;
 * it does not prove original Symbian/EKA2L1 behavior.
 */
class NativeOpponentRosterAllArenasIntegrationTest {
    private fun originalStages(): List<StageData> {
        val file = File("../../web/assets/stages.js")
        assertTrue("Missing original stage source: ${file.absolutePath}", file.isFile)
        return StageCatalog.parse(file.readText(Charsets.UTF_8))
    }

    @Test fun everyOriginalArenaRunsIndependentBotsWithoutTerrainOrActorOverlap() {
        val stages = originalStages()
        assertEquals((0..40).toList(), stages.map { it.id })
        assertEquals(mapOf(1 to 18, 2 to 2, 4 to 21),
            stages.groupingBy { it.spawns.size }.eachCount())

        for (stage in stages) {
            val resolved = NativeSpawnLayout.resolve(stage)
            val player = Actor(resolved.first().x, resolved.first().y)
            val roster = NativeOpponentRoster.create(stage, player)
            val simulation = NativeBombSimulation(stage)
            val label = "arena ${stage.id}"

            assertEquals(label, stage.spawns.size - 1, roster.bots.size)
            assertEquals(label, roster.bots.size, roster.bots.map { it.owner }.toSet().size)
            assertEquals(label, roster.bots.size, roster.bots.map { it.controller }.toSet().size)
            assertEquals(label, roster.bots.size, roster.bots.map { it.actor }.toSet().size)

            // Cross the initial approach, repeated replanning, and enough time
            // for any early-placed default-fuse bomb to detonate. All simulation
            // steps are fixed 60Hz source steps; no rendering/device is involved.
            repeat(120) { tick ->
                val tickResult = roster.advanceSimulationTick(stage, player, simulation, tick.toLong())
                assertTrue("$label negative bomb placement count", tickResult.bombsPlaced >= 0)
                assertTrue("$label negative detonation count", tickResult.bombsDetonated >= 0)
                for (slot in roster.bots.filterNot { it.vitality.defeated }) {
                    assertFalse(
                        "$label ${slot.owner} entered terrain or a blocking bomb at tick $tick",
                        StageCollision.blocked(
                            stage, slot.actor.x, slot.actor.y, StageCollision.ACTOR_RADIUS,
                            simulation.bombs, slot.actor.passBomb,
                        ),
                    )
                }

                val living = roster.bots.filterNot { it.vitality.defeated }
                val allActors = listOf(player) + living.map { it.actor }
                for (i in allActors.indices) {
                    for (j in i + 1 until allActors.size) {
                        assertFalse(
                            "$label actors $i/$j overlap at tick $tick",
                            StageCollision.blockedByActors(
                                allActors[i].x, allActors[i].y, listOf(allActors[j]),
                            ),
                        )
                    }
                }

            }
        }
    }
}
