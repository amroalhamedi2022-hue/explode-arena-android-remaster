package com.explodearena.remaster.core

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class NativeArenaReplaySessionTest {
    private fun syntheticStage(spawns: List<Spawn>, id: Int = 7) = StageData(
        id = id,
        image = "assets/maps/map_${id.toString().padStart(3, '0')}.png",
        width = 128,
        height = 64,
        obstacleX = 0,
        obstacleY = 0,
        grid = List(8) { IntArray(16) },
        spawns = spawns,
    )

    private fun fourSpawnStage() = syntheticStage(
        listOf(Spawn(24.0, 24.0), Spawn(88.0, 24.0), Spawn(24.0, 40.0), Spawn(88.0, 40.0)),
    )

    private fun script(tick: Int) = NativeArenaReplayInput(
        dx = when (tick % 12) { 0, 1, 2 -> 1.0; 6, 7 -> -1.0; else -> 0.0 },
        dy = when (tick % 14) { 3, 4 -> 1.0; 9, 10 -> -1.0; else -> 0.0 },
        placeBomb = tick == 0 || tick == 100,
        remoteDetonate = tick == 24,
    )

    @Test fun fourSpawnTraceIsByteIdenticalAndIncludesEveryBotAndHazard() {
        val first = NativeArenaReplaySession(fourSpawnStage())
        val second = NativeArenaReplaySession(fourSpawnStage())
        assertTrue(first.setRemoteDetonator(enabled = true))
        assertTrue(second.setRemoteDetonator(enabled = true))
        val trace = (0 until 180).map { tick ->
            val input = script(tick)
            val a = first.step(input).traceLine()
            val b = second.step(input).traceLine()
            assertEquals("diverged at tick $tick", a, b)
            a
        }
        assertTrue(trace.any { it.contains("|bots=3;bot,") })
        assertTrue(trace.any { it.contains("|remoteOwners=player|") })
        assertTrue(trace.any { line -> !line.endsWith("|flames=0") })
        assertTrue(trace.last().startsWith("EA-ARENA-PROVISIONAL-v1|7|"))
        assertNull("full-roster trace must pass its own strict comparator",
            NativeArenaReplayComparison.firstDivergence(trace, trace))
    }

    @Test fun invalidInputDoesNotMutateAnyPlayerBotBombOrFlameState() {
        val replay = NativeArenaReplaySession(fourSpawnStage())
        val before = replay.snapshot()
        for (bad in listOf(Double.NaN, Double.POSITIVE_INFINITY, 1.01, -1.01)) {
            assertThrows(IllegalArgumentException::class.java) {
                replay.step(NativeArenaReplayInput(dx = bad, placeBomb = true, remoteDetonate = true))
            }
            assertEquals(before, replay.snapshot())
        }
    }

    @Test fun actualFortyOneStageCatalogHasRepeatableFullRosterTicks() {
        val catalogFile = File("../../web/assets/stages.js")
        assertTrue("Original stage catalog missing: ${catalogFile.absolutePath}", catalogFile.isFile)
        val stages = StageCatalog.parse(catalogFile.readText(Charsets.UTF_8))
        assertEquals(41, stages.size)
        assertEquals(mapOf(1 to 18, 2 to 2, 4 to 21), stages.groupingBy { it.spawns.size }.eachCount())

        for (stage in stages) {
            val first = NativeArenaReplaySession(stage)
            val second = NativeArenaReplaySession(stage)
            val expectedTrace = mutableListOf<String>()
            val actualTrace = mutableListOf<String>()
            repeat(30) { tick ->
                val input = NativeArenaReplayInput(
                    dx = if (tick % 9 in 0..2) 1.0 else 0.0,
                    dy = if (tick % 11 in 5..6) -1.0 else 0.0,
                    placeBomb = tick == 0,
                )
                val expected = first.step(input).traceLine()
                val actual = second.step(input).traceLine()
                assertEquals("stage ${stage.id}, tick $tick", expected, actual)
                expectedTrace += expected
                actualTrace += actual
            }
            assertNull(
                "stage ${stage.id} full-roster comparison",
                NativeArenaReplayComparison.firstDivergence(expectedTrace, actualTrace),
            )
        }
    }
}
