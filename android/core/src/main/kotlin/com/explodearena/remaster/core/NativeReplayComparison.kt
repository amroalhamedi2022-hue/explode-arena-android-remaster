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
    private val fieldNames = listOf(
        "stage/tick", "actor-position", "health/invulnerability",
        "pass-bomb", "remote-owners", "next-bomb-id", "bombs", "flames",
    )

    /** Reject malformed/empty or discontinuous recordings even when both match. */
    private fun invalidFrame(parts: List<String>, previousTick: Long?): String? {
        if (parts.size != 10) return "trace-fields"
        val stage = parts[1].toIntOrNull()
        if (stage == null || stage < 0) return "stage"
        val tick = parts[2].toLongOrNull()
        if (tick == null || tick < 0) return "tick"
        if (previousTick != null &&
            (previousTick == Long.MAX_VALUE || tick != previousTick + 1L)) return "tick-sequence"
        return null
    }

    data class Divergence(
        val frameIndex: Int,
        val field: String,
        val expected: String?,
        val actual: String?,
    )

    /** Returns the first differing frame/field, or null for identical traces. */
    fun firstDivergence(expected: Iterable<String>, actual: Iterable<String>): Divergence? {
        val left = expected.iterator()
        val right = actual.iterator()
        var frame = 0
        var expectedPreviousTick: Long? = null
        var actualPreviousTick: Long? = null
        while (left.hasNext() && right.hasNext()) {
            val a = left.next()
            val b = right.next()
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
            if (a != b) {
                val index = lhs.indices.first { lhs[it] != rhs[it] }
                val name = when (index) {
                    1 -> "stage"
                    2 -> "tick"
                    else -> fieldNames.getOrElse(index - 2) { "field-$index" }
                }
                return Divergence(frame, name, lhs[index], rhs[index])
            }
            expectedPreviousTick = lhs[2].toLong()
            actualPreviousTick = rhs[2].toLong()
            frame++
        }
        return when {
            frame == 0 && !left.hasNext() && !right.hasNext() ->
                Divergence(0, "empty-trace", null, null)
            left.hasNext() -> Divergence(frame, "frame-count", left.next(), null)
            right.hasNext() -> Divergence(frame, "frame-count", null, right.next())
            else -> null
        }
    }
}
