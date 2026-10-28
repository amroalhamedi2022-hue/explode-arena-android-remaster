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
    private fun terminalLine(state: String = "LOST", tick: Long = 0L): String {
        val fields = traceLine().split('|').toMutableList()
        fields[2] = tick.toString()
        fields[3] = "round=$state"
        return fields.joinToString("|")
    }

    private fun withPlayerX(line: String, x: Double): String {
        val fields = line.split('|').toMutableList()
        val player = fields[4].removePrefix("player=").split(',').toMutableList()
        player[1] = java.lang.Long.toHexString(java.lang.Double.doubleToLongBits(x))
        fields[4] = "player=" + player.joinToString(",")
        return fields.joinToString("|")
    }

    @Test fun frozenTerminalHoldRejectsActorMutationAtTheSameTick() {
        val terminal = terminalLine()
        val changed = withPlayerX(terminal, 25.0)

        val difference = NativeArenaReplayComparison.firstDivergence(
            listOf(terminal, terminal),
            listOf(terminal, changed),
        )

        assertEquals(1, difference?.frameIndex)
        assertEquals("actual-terminal-hold-mutation", difference?.field)
    }

    @Test fun frozenTerminalStateCannotAdvanceItsTick() {
        val terminal = terminalLine("COMPLETE")
        val fields = terminal.split('|').toMutableList()
        fields[2] = "1"
        val advanced = fields.joinToString("|")

        val difference = NativeArenaReplayComparison.firstDivergence(
            listOf(terminal, terminal),
            listOf(terminal, advanced),
        )

        assertEquals(1, difference?.frameIndex)
        assertEquals("actual-terminal-state-advanced", difference?.field)
    }

    @Test fun identicalFrozenTerminalFramesRemainValid() {
        val terminal = terminalLine("ADVANCING")
        assertNull(
            NativeArenaReplayComparison.firstDivergence(
                listOf(terminal, terminal),
                listOf(terminal, terminal),
            ),
        )
    }

    @Test fun lostAndDrawRetryMustRestoreTheExactInitialFrame() {
        val initial = traceLine()
        val mutatedRetry = withPlayerX(initial, 25.0)

        for (state in listOf("LOST", "DRAW")) {
            val terminal = terminalLine(state, tick = 1L)

            assertNull(
                "$state may retry only by restoring the initial PLAYING frame at tick zero",
                NativeArenaReplayComparison.firstDivergence(
                    listOf(initial, terminal, initial),
                    listOf(initial, terminal, initial),
                ),
            )
            val difference = NativeArenaReplayComparison.firstDivergence(
                listOf(initial, terminal, initial),
                listOf(initial, terminal, mutatedRetry),
            )
            assertEquals(2, difference?.frameIndex)
            assertEquals("actual-retry-reset-mutation", difference?.field)
        }
    }

    @Test fun retryWithoutAnInitialPlayingTickZeroBaselineIsRejected() {
        val terminal = terminalLine("LOST")
        val retry = traceLine()
        val difference = NativeArenaReplayComparison.firstDivergence(
            listOf(terminal, retry),
            listOf(terminal, retry),
        )
        assertEquals("expected-retry-reset-baseline-missing", difference?.field)
    }

}
