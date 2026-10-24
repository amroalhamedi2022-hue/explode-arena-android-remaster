package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeReplayComparisonTest {
    private fun stage() = StageData(
        5, "assets/maps/map_005.png", 128, 64, 0, 0,
        List(8) { IntArray(16) }, listOf(Spawn(24.0, 24.0)),
    )

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
        assertNull(diff.actual)
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
}
