package com.explodearena.remaster.core

/**
 * Strict comparator for the full-roster replay emitted by NativeArenaReplaySession.
 * A match is a native regression result only; it is not evidence of Symbian parity.
 */
object NativeArenaReplayComparison {
    internal const val MAX_TRACE_FRAMES = 100_000
    internal const val MAX_TRACE_LINE_LENGTH = 512 * 1024
    private const val SCHEMA = "EA-ARENA-PROVISIONAL-v1"
    private val validStates = setOf("PLAYING", "WON", "LOST", "DRAW", "ADVANCING", "COMPLETE")
    private val ownerPattern = Regex("[A-Za-z0-9_-]{1,32}")
    private val hexBits = Regex("[0-9a-f]{1,16}")
    private val fieldNames = listOf(
        "schema", "stage", "tick", "round-state", "player", "bots",
        "remote-owners", "next-bomb-id", "bombs", "flames",
    )

    data class Divergence(
        val frameIndex: Int,
        val field: String,
        val expected: String?,
        val actual: String?,
    )

    private data class ActorRecord(val owner: String, val passBomb: Long?)
    private data class FrameRecord(
        val fields: List<String>,
        val stage: Int,
        val tick: Long,
        val state: String,
    )

    private fun bitsValue(text: String): Double? {
        if (!hexBits.matches(text)) return null
        return runCatching {
            java.lang.Double.longBitsToDouble(java.lang.Long.parseUnsignedLong(text, 16))
        }.getOrNull()?.takeIf { it.isFinite() }
    }

    private fun positiveId(text: String): Long? = text.toLongOrNull()?.takeIf { it > 0L }

    private fun actor(raw: String, isPlayer: Boolean): ActorRecord? {
        val values = raw.split(',')
        if (values.size != 9) return null
        val owner = values[0]
        if (!ownerPattern.matches(owner) ||
            (isPlayer && owner != "player") ||
            (!isPlayer && owner == "player")) return null
        if (bitsValue(values[1]) == null || bitsValue(values[2]) == null) return null
        val health = values[3].toIntOrNull() ?: return null
        if (health !in 0..1000) return null
        val invulnerability = bitsValue(values[4]) ?: return null
        if (invulnerability < 0.0 || values[5] !in setOf("true", "false")) return null
        val passBomb = if (values[6] == "none") null else positiveId(values[6]) ?: return null
        if (isPlayer) {
            if (values[7] != "na" || values[8] != "na") return null
        } else {
            if (values[7] !in setOf("true", "false")) return null
            if ((values[8].toIntOrNull() ?: -1) !in 0..3) return null
        }
        return ActorRecord(owner, passBomb)
    }

    private fun parseLine(line: String): Pair<FrameRecord?, String?> {
        if (line.length > MAX_TRACE_LINE_LENGTH) return null to "line-length"
        val fields = line.split('|')
        if (fields.firstOrNull() != SCHEMA) return null to "schema"
        if (fields.size != 10) return null to "fields"
        val stage = fields[1].toIntOrNull()?.takeIf { it in 0..40 } ?: return null to "stage"
        val tick = fields[2].toLongOrNull()?.takeIf { it >= 0L } ?: return null to "tick"
        if (!fields[3].startsWith("round=")) return null to "round-state"
        val state = fields[3].removePrefix("round=")
        if (state !in validStates) return null to "round-state"

        if (!fields[4].startsWith("player=")) return null to "player"
        val player = actor(fields[4].removePrefix("player="), isPlayer = true) ?: return null to "player"
        if (!fields[5].startsWith("bots=")) return null to "bots"
        val botParts = fields[5].split(';')
        val botCount = botParts[0].removePrefix("bots=").toIntOrNull()
        if (!botParts[0].startsWith("bots=") || botCount == null ||
            botCount !in 0..3 || botParts.size != botCount + 1) return null to "bots"
        val actors = mutableListOf(player)
        val owners = linkedSetOf(player.owner)
        for (index in 1 until botParts.size) {
            val bot = actor(botParts[index], isPlayer = false) ?: return null to "bots"
            if (!owners.add(bot.owner)) return null to "bots"
            actors += bot
        }

        if (!fields[6].startsWith("remoteOwners=")) return null to "remote-owners"
        val remoteText = fields[6].removePrefix("remoteOwners=")
        val remoteOwners = if (remoteText.isEmpty()) emptyList() else remoteText.split(',')
        if (remoteOwners.any { it !in owners } || remoteOwners.toSet().size != remoteOwners.size) {
            return null to "remote-owners"
        }

        if (!fields[7].startsWith("nextBombId=")) return null to "next-bomb-id"
        val nextBombId = positiveId(fields[7].removePrefix("nextBombId=")) ?: return null to "next-bomb-id"
        if (actors.any { it.passBomb != null && it.passBomb >= nextBombId }) return null to "pass-bomb"

        if (!fields[8].startsWith("bombs=")) return null to "bombs"
        val bombParts = fields[8].split(';')
        val bombCount = bombParts[0].removePrefix("bombs=").toIntOrNull()
        if (!bombParts[0].startsWith("bombs=") || bombCount == null ||
            bombCount !in 0..100 || bombParts.size != bombCount + 1) return null to "bombs"
        val bombIds = HashSet<Long>()
        for (index in 1 until bombParts.size) {
            val values = bombParts[index].split(',')
            if (values.size != 6) return null to "bombs"
            val id = positiveId(values[0]) ?: return null to "bombs"
            if (id >= nextBombId || !bombIds.add(id) || values[1] !in owners ||
                bitsValue(values[2]) == null || bitsValue(values[3]) == null ||
                values[5] !in setOf("true", "false")) return null to "bombs"
            if (values[5] == "true") {
                if (values[4] != "remote") return null to "bombs"
            } else {
                val remaining = bitsValue(values[4]) ?: return null to "bombs"
                if (remaining <= 0.0) return null to "bombs"
            }
        }

        if (!fields[9].startsWith("flames=")) return null to "flames"
        val flameParts = fields[9].split(';')
        val flameCount = flameParts[0].removePrefix("flames=").toIntOrNull()
        if (!flameParts[0].startsWith("flames=") || flameCount == null ||
            flameCount !in 0..100 || flameParts.size != flameCount + 1) return null to "flames"
        for (index in 1 until flameParts.size) {
            val entry = flameParts[index]
            val separator = entry.indexOf(':')
            if (separator <= 0) return null to "flames"
            val remaining = bitsValue(entry.substring(0, separator)) ?: return null to "flames"
            if (remaining <= 0.0) return null to "flames"
            val cellText = entry.substring(separator + 1)
            if (!cellText.endsWith('/')) return null to "flames"
            val cells = cellText.dropLast(1).split('/')
            if (cells.size !in 1..49 || cells.any { cell ->
                    val xy = cell.split(',')
                    xy.size != 2 || xy.any { bitsValue(it) == null }
                }) return null to "flames"
        }
        return FrameRecord(fields, stage, tick, state) to null
    }

    private fun sequenceIssue(previous: FrameRecord?, current: FrameRecord): String? {
        if (previous == null) return null
        if (previous.stage != current.stage) return "stage-sequence"
        if (previous.tick < Long.MAX_VALUE && current.tick == previous.tick + 1L) return null
        if (current.tick == previous.tick && current.state == previous.state &&
            current.state in setOf("LOST", "DRAW", "ADVANCING", "COMPLETE")) return null
        if (previous.state in setOf("LOST", "DRAW") &&
            current.state == "PLAYING" && current.tick == 0L) return null
        return "tick-sequence"
    }

    private fun preview(value: String): String =
        if (value.length <= 160) value else value.take(160) + "…"

    /** Returns the first validated frame/field divergence, or null for identical valid traces. */
    fun firstDivergence(expected: Iterable<String>, actual: Iterable<String>): Divergence? {
        val left = expected.iterator()
        val right = actual.iterator()
        var frame = 0
        var previousExpected: FrameRecord? = null
        var previousActual: FrameRecord? = null
        while (frame < MAX_TRACE_FRAMES && left.hasNext() && right.hasNext()) {
            val leftLine = left.next()
            val rightLine = right.next()
            val (leftRecord, leftIssue) = parseLine(leftLine)
            if (leftIssue != null || leftRecord == null) {
                return Divergence(frame, "expected-${leftIssue ?: "invalid"}", "valid arena trace", leftIssue)
            }
            val leftSequenceIssue = sequenceIssue(previousExpected, leftRecord)
            if (leftSequenceIssue != null) {
                return Divergence(frame, "expected-$leftSequenceIssue", "valid frame sequence", leftSequenceIssue)
            }
            val (rightRecord, rightIssue) = parseLine(rightLine)
            if (rightIssue != null || rightRecord == null) {
                return Divergence(frame, "actual-${rightIssue ?: "invalid"}", "valid arena trace", rightIssue)
            }
            val rightSequenceIssue = sequenceIssue(previousActual, rightRecord)
            if (rightSequenceIssue != null) {
                return Divergence(frame, "actual-$rightSequenceIssue", "valid frame sequence", rightSequenceIssue)
            }
            if (leftLine != rightLine) {
                val index = leftRecord.fields.indices.firstOrNull {
                    leftRecord.fields[it] != rightRecord.fields[it]
                } ?: 0
                return Divergence(
                    frame, fieldNames[index],
                    preview(leftRecord.fields[index]), preview(rightRecord.fields[index]),
                )
            }
            previousExpected = leftRecord
            previousActual = rightRecord
            frame++
        }
        val expectedHasMore = left.hasNext()
        val actualHasMore = right.hasNext()
        if (frame == MAX_TRACE_FRAMES && (expectedHasMore || actualHasMore)) {
            val side = when {
                expectedHasMore && actualHasMore -> "both"
                expectedHasMore -> "expected"
                else -> "actual"
            }
            return Divergence(frame, "frame-limit", "at-most $MAX_TRACE_FRAMES frames", "additional frame(s) in $side trace")
        }
        return when {
            frame == 0 && !expectedHasMore && !actualHasMore ->
                Divergence(0, "empty-trace", null, null)
            expectedHasMore -> Divergence(frame, "frame-count", preview(left.next()), null)
            actualHasMore -> Divergence(frame, "frame-count", null, preview(right.next()))
            else -> null
        }
    }
}
