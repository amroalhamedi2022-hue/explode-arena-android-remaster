package com.explodearena.remaster.core

/**
 * Compares recorded native fixed-tick traces without a GPU, Android device,
 * WebView, or loading every frame into memory. A reference trace can be kept
 * outside the repository when original EKA2L1 footage is transcribed.
 *
 * Both inputs MUST use the same EA-NATIVE-PROVISIONAL-v2 schema. A matching
 * native trace is only a regression check: it does NOT prove Symbian parity.
 * Never silently compare an unknown original-game format as if it matched.
 */
object NativeReplayComparison {
    private const val SCHEMA = "EA-NATIVE-PROVISIONAL-v2"
    internal const val MAX_TRACE_FRAMES = 100_000
    internal const val MAX_TRACE_LINE_LENGTH = 512 * 1024
    private val fieldNames = listOf(
        "stage/tick", "actor-position", "health/invulnerability",
        "pass-bomb", "remote-owners", "next-bomb-id", "bombs", "flames",
    )

    /** Reject malformed/empty or discontinuous recordings even when both match. */
    private fun invalidFrame(parts: List<String>, previousTick: Long?): String? {
        if (parts.size != 10) return "trace-fields"
        val stage = parts[1].toIntOrNull()
        if (stage == null || stage !in 0..40) return "stage"
        val tick = parts[2].toLongOrNull()
        if (tick == null || tick < 0) return "tick"
        if (previousTick != null &&
            (previousTick == Long.MAX_VALUE || tick != previousTick + 1L)) return "tick-sequence"
        return NativeTraceFieldValidation.issue(parts)
    }

    data class Divergence(
        val frameIndex: Int,
        val field: String,
        val expected: String?,
        val actual: String?,
    )

    /**
     * Keeps frame-count diagnostics bounded. The ordinary comparison path has
     * already checked line length before parsing, but the extra tail frame is
     * read after one iterator ends and must not bypass that protection.
     */
    private fun frameCountDiagnostic(line: String): String =
        if (line.length <= MAX_TRACE_LINE_LENGTH) {
            line
        } else {
            "oversized-line(length=${line.length},limit=$MAX_TRACE_LINE_LENGTH)"
        }

    /** Returns the first differing frame/field, or null for identical traces. */
    fun firstDivergence(expected: Iterable<String>, actual: Iterable<String>): Divergence? {
        val left = expected.iterator()
        val right = actual.iterator()
        var frame = 0
        var expectedPreviousTick: Long? = null
        var actualPreviousTick: Long? = null
        var expectedStage: Int? = null
        var actualStage: Int? = null
        while (frame < MAX_TRACE_FRAMES && left.hasNext() && right.hasNext()) {
            val a = left.next()
            val b = right.next()
            // Bound raw input before split() allocates a field list. Do not echo
            // untrusted oversized content into the divergence result.
            if (a.length > MAX_TRACE_LINE_LENGTH) {
                return Divergence(
                    frame, "expected-line-length",
                    MAX_TRACE_LINE_LENGTH.toString(), a.length.toString(),
                )
            }
            if (b.length > MAX_TRACE_LINE_LENGTH) {
                return Divergence(
                    frame, "actual-line-length",
                    MAX_TRACE_LINE_LENGTH.toString(), b.length.toString(),
                )
            }
            val lhs = a.split('|')
            val rhs = b.split('|')
            if (lhs.firstOrNull() != SCHEMA) {
                return Divergence(frame, "expected-schema", SCHEMA, lhs.firstOrNull())
            }
            if (rhs.firstOrNull() != SCHEMA) {
                return Divergence(frame, "actual-schema", SCHEMA, rhs.firstOrNull())
            }
            // A matching malformed trace is not a valid regression result.
            // Validate each side before comparing its fields.
            val expectedIssue = invalidFrame(lhs, expectedPreviousTick)
            if (expectedIssue != null) return Divergence(frame, "expected-$expectedIssue", null, a)
            val actualIssue = invalidFrame(rhs, actualPreviousTick)
            if (actualIssue != null) return Divergence(frame, "actual-$actualIssue", null, b)
            if (expectedStage != null && lhs[1].toInt() != expectedStage) {
                return Divergence(frame, "expected-stage-sequence", expectedStage.toString(), lhs[1])
            }
            if (actualStage != null && rhs[1].toInt() != actualStage) {
                return Divergence(frame, "actual-stage-sequence", actualStage.toString(), rhs[1])
            }
            if (a != b) {
                val index = lhs.indices.first { lhs[it] != rhs[it] }
                val name = when (index) {
                    1 -> "stage"
                    2 -> "tick"
                    else -> fieldNames.getOrElse(index - 2) { "field-$index" }
                }
                return Divergence(frame, name, lhs[index], rhs[index])
            }
            expectedStage = lhs[1].toInt()
            actualStage = rhs[1].toInt()
            expectedPreviousTick = lhs[2].toLong()
            actualPreviousTick = rhs[2].toLong()
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
            return Divergence(
                frame,
                "frame-limit",
                "at-most $MAX_TRACE_FRAMES frames",
                "additional frame(s) in $side trace",
            )
        }
        return when {
            frame == 0 && !expectedHasMore && !actualHasMore ->
                Divergence(0, "empty-trace", null, null)
            expectedHasMore -> Divergence(frame, "frame-count", frameCountDiagnostic(left.next()), null)
            actualHasMore -> Divergence(frame, "frame-count", null, frameCountDiagnostic(right.next()))
            else -> null
        }
    }
}
