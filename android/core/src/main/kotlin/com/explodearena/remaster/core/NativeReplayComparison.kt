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
        while (left.hasNext() && right.hasNext()) {
            val a = left.next()
            val b = right.next()
            val lhs = a.split('|')
            val rhs = b.split('|')
            if (lhs.firstOrNull()?.substringBefore('|')?.substringBeforeLast('|') != SCHEMA &&
                lhs.firstOrNull()?.substringBefore('|') != SCHEMA
            ) {
                return Divergence(frame, "expected-schema", SCHEMA, lhs.firstOrNull())
            }
            if (rhs.firstOrNull() != SCHEMA) {
                return Divergence(frame, "actual-schema", SCHEMA, rhs.firstOrNull())
            }
            // v2 has a schema, stage, tick, then seven state fields.
            if (lhs.size != 10 || rhs.size != 10) {
                return Divergence(frame, "trace-fields", lhs.size.toString(), rhs.size.toString())
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
            frame++
        }
        return when {
            left.hasNext() -> Divergence(frame, "frame-count", left.next(), null)
            right.hasNext() -> Divergence(frame, "frame-count", null, right.next())
            else -> null
        }
    }
}
