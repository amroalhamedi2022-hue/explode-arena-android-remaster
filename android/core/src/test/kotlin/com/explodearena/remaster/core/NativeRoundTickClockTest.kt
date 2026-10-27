package com.explodearena.remaster.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** N53: AI planning cadence starts from a deterministic tick for each attempt. */
class NativeRoundTickClockTest {
    private fun originalStages(): List<StageData> {
        val file = File("../../web/assets/stages.js")
        assertTrue("Missing original stage source: ${file.absolutePath}", file.isFile)
        return StageCatalog.parse(file.readText(Charsets.UTF_8))
    }

    @Test fun everyFreshAttemptStartsAtZeroAcrossAllFortyOneOriginalArenas() {
        val stages = originalStages()
        assertEquals((0..40).toList(), stages.map { it.id })
        assertEquals(mapOf(1 to 18, 2 to 2, 4 to 21),
            stages.groupingBy { it.spawns.size }.eachCount())

        for (stage in stages) {
            val firstAttempt = NativeArenaRoundStateFactory.create(stage)
            assertEquals("arena ${stage.id} initial AI tick", 0L, firstAttempt.tickClock.nextTick())
            repeat(17) { firstAttempt.tickClock.nextTick() }

            // Retry/stage replacement uses the production factory and must not
            // inherit the old attempt's periodic route-planning phase.
            val nextAttempt = NativeArenaRoundStateFactory.create(stage)
            assertEquals("arena ${stage.id} fresh attempt AI tick", 0L, nextAttempt.tickClock.nextTick())
        }
    }

    @Test fun roundTickClocksAreIndependentAndMonotonicWithinEachAttempt() {
        val stage = originalStages().first()
        val first = NativeArenaRoundStateFactory.create(stage).tickClock
        val second = NativeArenaRoundStateFactory.create(stage).tickClock

        assertEquals(0L, first.nextTick())
        assertEquals(0L, second.nextTick())
        assertEquals(1L, first.nextTick())
        repeat(20) { first.nextTick() }
        assertEquals("other attempt must not inherit elapsed ticks", 1L, second.nextTick())
        assertEquals(22L, first.nextTick())
    }
}