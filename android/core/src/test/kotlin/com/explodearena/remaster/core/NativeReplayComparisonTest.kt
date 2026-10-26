package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeReplayComparisonTest {
    private fun stage() = StageData(
        5, "assets/maps/map_005.png", 128, 64, 0, 0,
        List(8) { IntArray(16) }, listOf(Spawn(24.0, 24.0)),
    )

    private fun minimalTraceLine(tick: Long): String =
        "EA-NATIVE-PROVISIONAL-v2|5|$tick|4038000000000000,4038000000000000|3,0|pass=none|remoteOwners=|nextBombId=1|bombs=0|flames=0"

    private data class CountedTrace(val lines: Iterable<String>, val consumed: () -> Int)

    private fun countedTrace(count: Int): CountedTrace {
        var consumed = 0
        val lines = object : Iterable<String> {
            override fun iterator(): Iterator<String> = object : Iterator<String> {
                private var tick = 0L
                override fun hasNext(): Boolean = tick < count.toLong()
                override fun next(): String {
                    if (!hasNext()) throw NoSuchElementException()
                    consumed++
                    return minimalTraceLine(tick++)
                }
            }
        }
        return CountedTrace(lines) { consumed }
    }

    private fun run(dx: Double = 0.0, remote: Boolean = false): List<String> {
        val session = NativeReplaySession(stage(), Spawn(24.0, 24.0))
        if (remote) session.setRemoteDetonator(enabled = true)
        return (0 until 8).map { i ->
            session.step(NativeReplayInput(dx = dx, placeBomb = i == 0)).traceLine()
        }
    }

    @Test fun identicalRunsHaveNoDivergence() {
        assertNull(NativeReplayComparison.firstDivergence(run(), run()))
    }

    @Test fun detectsFirstActorMovementDivergenceAtExactTick() {
        val diff = NativeReplayComparison.firstDivergence(run(), run(dx = 1.0))!!
        assertEquals(0, diff.frameIndex)
        assertEquals("actor-position", diff.field)
        assertNotEquals(diff.expected, diff.actual)
    }

    @Test fun detectsRemotePowerEvenBeforeBombPlacement() {
        val sessionA = NativeReplaySession(stage(), Spawn(24.0, 24.0))
        val sessionB = NativeReplaySession(stage(), Spawn(24.0, 24.0))
        sessionB.setRemoteDetonator(enabled = true)
        val diff = NativeReplayComparison.firstDivergence(
            listOf(sessionA.snapshot().traceLine()), listOf(sessionB.snapshot().traceLine()),
        )!!
        assertEquals("remote-owners", diff.field)
    }

    @Test fun reportsTruncatedReferenceWithoutSilentlyAcceptingIt() {
        val full = run()
        val diff = NativeReplayComparison.firstDivergence(full, full.take(3))!!
        assertEquals(3, diff.frameIndex)
        assertEquals("frame-count", diff.field)
        assertEquals(full[3], diff.expected)
        assertNull(diff.actual)
    }

    @Test fun frameCountMismatchBoundsOversizedTailDiagnosticsOnBothSides() {
        val valid = minimalTraceLine(0)
        val oversized = "x".repeat(NativeReplayComparison.MAX_TRACE_LINE_LENGTH + 1)
        val marker =
            "oversized-line(length=${oversized.length},limit=${NativeReplayComparison.MAX_TRACE_LINE_LENGTH})"

        val expectedTail = NativeReplayComparison.firstDivergence(
            listOf(valid, oversized), listOf(valid),
        )!!
        assertEquals(1, expectedTail.frameIndex)
        assertEquals("frame-count", expectedTail.field)
        assertEquals(marker, expectedTail.expected)
        assertNull(expectedTail.actual)
        assertNotEquals(oversized, expectedTail.expected)

        val actualTail = NativeReplayComparison.firstDivergence(
            listOf(valid), listOf(valid, oversized),
        )!!
        assertEquals(1, actualTail.frameIndex)
        assertEquals("frame-count", actualTail.field)
        assertNull(actualTail.expected)
        assertEquals(marker, actualTail.actual)
        assertNotEquals(oversized, actualTail.actual)
    }

    @Test fun rejectsTwoEmptyTracesInsteadOfClaimingParity() {
        val diff = NativeReplayComparison.firstDivergence(emptyList(), emptyList())!!
        assertEquals("empty-trace", diff.field)
    }

    @Test fun rejectsIdenticalMalformedStageAndTickFields() {
        val trace = run().first()
        val invalidStage = trace.replaceFirst("|5|1|", "|not-a-stage|1|")
        val stageDiff = NativeReplayComparison.firstDivergence(
            listOf(invalidStage), listOf(invalidStage),
        )!!
        assertEquals("expected-stage", stageDiff.field)
        val invalidTick = trace.replaceFirst("|5|1|", "|5|-1|")
        val tickDiff = NativeReplayComparison.firstDivergence(
            listOf(invalidTick), listOf(invalidTick),
        )!!
        assertEquals("expected-tick", tickDiff.field)
    }

    @Test fun rejectsRepeatedTicksEvenWhenBothTracesAreIdentical() {
        val trace = run()
        val repeated = listOf(trace[0], trace[0])
        val diff = NativeReplayComparison.firstDivergence(repeated, repeated)!!
        assertEquals(1, diff.frameIndex)
        assertEquals("expected-tick-sequence", diff.field)
    }

    @Test fun rejectsUnversionedOrUnrelatedTraceSchemas() {
        val trace = run().first()
        val diff = NativeReplayComparison.firstDivergence(
            listOf(trace), listOf(trace.replace("EA-NATIVE-PROVISIONAL-v2", "EKA2L1-RAW")),
        )!!
        assertEquals("actual-schema", diff.field)
    }
    @Test fun rejectsOversizedExpectedAndActualLinesBeforeParsing() {
        val oversized = "x".repeat(NativeReplayComparison.MAX_TRACE_LINE_LENGTH + 1)
        val valid = minimalTraceLine(0)

        val expected = NativeReplayComparison.firstDivergence(
            listOf(oversized), listOf(valid),
        )!!
        assertEquals("expected-line-length", expected.field)
        assertEquals(NativeReplayComparison.MAX_TRACE_LINE_LENGTH.toString(), expected.expected)
        assertEquals((NativeReplayComparison.MAX_TRACE_LINE_LENGTH + 1).toString(), expected.actual)
        assertNotEquals(oversized, expected.actual)

        val actual = NativeReplayComparison.firstDivergence(
            listOf(valid), listOf(oversized),
        )!!
        assertEquals("actual-line-length", actual.field)
        assertEquals(NativeReplayComparison.MAX_TRACE_LINE_LENGTH.toString(), actual.expected)
        assertEquals((NativeReplayComparison.MAX_TRACE_LINE_LENGTH + 1).toString(), actual.actual)
        assertNotEquals(oversized, actual.actual)
    }

    @Test fun stopsAtFrameLimitWithoutConsumingTheUnboundedTail() {
        val count = NativeReplayComparison.MAX_TRACE_FRAMES + 10
        val expected = countedTrace(count)
        val actual = countedTrace(count)

        val diff = NativeReplayComparison.firstDivergence(expected.lines, actual.lines)!!

        assertEquals(NativeReplayComparison.MAX_TRACE_FRAMES, diff.frameIndex)
        assertEquals("frame-limit", diff.field)
        assertEquals("at-most ${NativeReplayComparison.MAX_TRACE_FRAMES} frames", diff.expected)
        assertEquals("additional frame(s) in both trace", diff.actual)
        assertEquals(NativeReplayComparison.MAX_TRACE_FRAMES, expected.consumed())
        assertEquals(NativeReplayComparison.MAX_TRACE_FRAMES, actual.consumed())
    }

}
