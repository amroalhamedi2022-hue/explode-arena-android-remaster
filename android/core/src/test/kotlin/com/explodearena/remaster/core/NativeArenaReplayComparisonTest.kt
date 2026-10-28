package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeArenaReplayComparisonTest {
    private fun stage() = StageData(
        id = 7,
        image = "assets/maps/map_007.png",
        width = 128,
        height = 64,
        obstacleX = 0,
        obstacleY = 0,
        grid = List(8) { IntArray(16) },
        spawns = listOf(
            Spawn(24.0, 24.0), Spawn(88.0, 24.0),
            Spawn(24.0, 40.0), Spawn(88.0, 40.0),
        ),
    )

    private fun traceLine(): String =
        NativeArenaReplaySession(stage()).snapshot().traceLine()

    @Test fun detectsFirstFullRosterBotFieldDivergence() {
        val expected = traceLine()
        val fields = expected.split('|').toMutableList()
        val bots = fields[5].split(';').toMutableList()
        val firstBot = bots[1].split(',').toMutableList()
        firstBot[1] = if (firstBot[1] == "0") "1" else "0"
        bots[1] = firstBot.joinToString(",")
        fields[5] = bots.joinToString(";")
        val actual = fields.joinToString("|")

        val difference = NativeArenaReplayComparison.firstDivergence(
            listOf(expected), listOf(actual),
        )

        assertEquals("bots", difference?.field)
        assertEquals(0, difference?.frameIndex)
    }

    @Test fun rejectsIdenticalMalformedFullRosterTraces() {
        val malformed = traceLine().replace("nextBombId=1", "nextBombId=0")
        val difference = NativeArenaReplayComparison.firstDivergence(
            listOf(malformed), listOf(malformed),
        )

        assertEquals("expected-next-bomb-id", difference?.field)
    }

    @Test fun rejectsRepeatedActiveTickEvenWhenBothTracesMatch() {
        val session = NativeArenaReplaySession(stage())
        val line = session.step(NativeArenaReplayInput()).traceLine()
        val difference = NativeArenaReplayComparison.firstDivergence(
            listOf(line, line), listOf(line, line),
        )

        assertEquals(1, difference?.frameIndex)
        assertEquals("expected-tick-sequence", difference?.field)
    }

    @Test fun rejectsOversizedTraceBeforeParsing() {
        val oversized = "x".repeat(NativeArenaReplayComparison.MAX_TRACE_LINE_LENGTH + 1)
        val valid = traceLine()
        val difference = NativeArenaReplayComparison.firstDivergence(
            listOf(oversized), listOf(valid),
        )

        assertEquals("expected-line-length", difference?.field)
        assertEquals("valid arena trace", difference?.expected)
    }

    @Test fun rejectsEmptyIdenticalTraces() {
        val difference = NativeArenaReplayComparison.firstDivergence(emptyList(), emptyList())
        assertEquals("empty-trace", difference?.field)
    }
}
