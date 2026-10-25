package com.explodearena.remaster.core

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/**
 * Contract against all 41 actual Mainline stage records, not synthetic grids.
 * The 5px collision radius is provisional and requires EKA2L1 parity testing.
 */
class NativeSpawnLayoutTest {
    private fun originalStages(): List<StageData> {
        val file = File("../../web/assets/stages.js")
        assertTrue("Missing original stage source: ${file.absolutePath}", file.isFile)
        return StageCatalog.parse(file.readText(Charsets.UTF_8))
    }

    @Test fun all41OriginalArenasHaveSafeNonOverlappingNativeActorSpawns() {
        val stages = originalStages()
        assertEquals((0..40).toList(), stages.map { it.id })
        assertEquals(mapOf(1 to 18, 2 to 2, 4 to 21),
            stages.groupingBy { it.spawns.size }.eachCount())
        for (stage in stages) {
            val resolved = NativeSpawnLayout.resolve(stage)
            assertEquals("arena ${stage.id}", stage.spawns.size, resolved.size)
            for ((index, position) in resolved.withIndex()) {
                assertFalse("arena ${stage.id} spawn ${index + 1} collides at 5px",
                    StageCollision.blocked(
                        stage, position.x, position.y, StageCollision.ACTOR_RADIUS,
                    ))
            }
            for (i in resolved.indices) for (j in i + 1 until resolved.size) {
                val a = resolved[i]
                val b = resolved[j]
                assertFalse("arena ${stage.id} spawns ${i + 1}/${j + 1} overlap",
                    kotlin.math.abs(a.x - b.x) < 2 * StageCollision.ACTOR_RADIUS &&
                        kotlin.math.abs(a.y - b.y) < 2 * StageCollision.ACTOR_RADIUS)
            }
            val player = Actor(resolved.first().x, resolved.first().y)
            val roster = NativeOpponentRoster.create(stage, player)
            assertEquals("arena ${stage.id}", resolved.drop(1),
                roster.bots.map { Spawn(it.actor.x, it.actor.y) })
        }
    }

    @Test fun realSourceSpawnsBlockedAtActorRadiusAreRepositionedDeterministically() {
        val stages = originalStages().associateBy { it.id }
        assertEquals(Spawn(20.0, 244.0), NativeSpawnLayout.resolve(stages.getValue(26))[2])
        assertEquals(Spawn(20.0, 20.0), NativeSpawnLayout.resolve(stages.getValue(35))[0])
        assertEquals(Spawn(164.0, 252.0), NativeSpawnLayout.resolve(stages.getValue(40))[3])
        for (id in listOf(26, 35, 40)) {
            assertEquals(NativeSpawnLayout.resolve(stages.getValue(id)),
                NativeSpawnLayout.resolve(stages.getValue(id)))
        }
    }

    @Test fun rejectsUnsupportedCountsAndOverlappingResolvedSpawns() {
        val open = StageData(
            id = 7, image = "assets/maps/map_007.png", width = 64, height = 64,
            obstacleX = 0, obstacleY = 0, grid = List(8) { IntArray(8) },
            spawns = listOf(Spawn(20.0, 20.0)),
        )
        assertThrows(IllegalArgumentException::class.java) {
            NativeSpawnLayout.resolve(open.copy(spawns = listOf(
                Spawn(20.0, 20.0), Spawn(30.0, 30.0), Spawn(40.0, 40.0),
            )))
        }
        assertThrows(IllegalStateException::class.java) {
            NativeSpawnLayout.resolve(open.copy(spawns = listOf(
                Spawn(20.0, 20.0), Spawn(20.0, 20.0),
            )))
        }
    }
}
