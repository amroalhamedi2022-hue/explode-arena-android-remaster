package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeReplayTraceValidationTest {
    private fun session(): NativeReplaySession {
        val stage = StageData(
            5, "assets/maps/map_005.png", 128, 64, 0, 0,
            List(8) { IntArray(16) }, listOf(Spawn(24.0, 24.0)),
        )
        return NativeReplaySession(stage, Spawn(24.0, 24.0))
    }

    private fun sample(): String = session().step(NativeReplayInput(placeBomb = true)).traceLine()

    private fun issue(line: String): String =
        NativeReplayComparison.firstDivergence(listOf(line), listOf(line))!!.field

    @Test fun rejectsIdenticalCorruptPayloadsInsteadOfClaimingParity() {
        val good = sample()
        val fields = good.split('|')
        val invalid = mapOf(
            3 to "nan,0",
            4 to "invalid,0",
            5 to "pass=-1",
            6 to "remoteOwners=player,player",
            7 to "nextBombId=-1",
            8 to "bombs=1",
            9 to "flames=1",
        )
        val expected = mapOf(
            3 to "expected-actor-position",
            4 to "expected-health/invulnerability",
            5 to "expected-pass-bomb",
            6 to "expected-remote-owners",
            7 to "expected-next-bomb-id",
            8 to "expected-bombs",
            9 to "expected-flames",
        )
        for ((index, replacement) in invalid) {
            val altered = fields.toMutableList().also { it[index] = replacement }.joinToString("|")
            assertEquals("field $index", expected[index], issue(altered))
        }
    }

    @Test fun rejectsOutOfCatalogStagesAndMidTraceStageChanges() {
        val first = sample()
        val outside = first.replaceFirst("|5|", "|41|")
        assertEquals("expected-stage", issue(outside))
        val changed = first.replaceFirst("|5|", "|6|").replaceFirst("|1|", "|2|")
        val diff = NativeReplayComparison.firstDivergence(
            listOf(first, changed), listOf(first, changed),
        )!!
        assertEquals(1, diff.frameIndex)
        assertEquals("expected-stage-sequence", diff.field)
    }

    @Test fun acceptsNativeBombAndFlamePayloadsAcrossDetonation() {
        val s = session()
        val trace = (0 until 100).map { index ->
            s.step(NativeReplayInput(placeBomb = index == 0)).traceLine()
        }
        assertNull(NativeReplayComparison.firstDivergence(trace, trace))
        assertTrue(trace.any { it.contains("|flames=1;") })
    }

    @Test fun rejectsDuplicateBombIdsAndInvalidFlameCoordinates() {
        val valid = sample()
        val fields = valid.split('|').toMutableList()
        val bomb = fields[8].substringAfter(';')
        fields[8] = "bombs=2;$bomb;$bomb"
        assertEquals("expected-bombs", issue(fields.joinToString("|")))
        fields[8] = "bombs=0"
        fields[9] = "flames=1;3ff0000000000000:bad,3ff0000000000000/"
        assertEquals("expected-flames", issue(fields.joinToString("|")))
    }
}
