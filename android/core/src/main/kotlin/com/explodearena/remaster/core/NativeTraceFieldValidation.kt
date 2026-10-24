package com.explodearena.remaster.core

/**
 * Strict, bounded validation for EA-NATIVE-PROVISIONAL-v2 trace payloads.
 * Identical malformed recordings must never be reported as successful parity.
 * This validates the *native* trace format only, not original Symbian behavior.
 */
internal object NativeTraceFieldValidation {
    private const val MAX_FIELD_LENGTH = 65_536
    private val hexBits = Regex("[0-9a-f]{1,16}")
    private val validOwners = setOf("player", "player2", "bot")

    private fun finiteBits(text: String): Boolean {
        if (!hexBits.matches(text)) return false
        return runCatching {
            java.lang.Double.longBitsToDouble(java.lang.Long.parseUnsignedLong(text, 16)).isFinite()
        }.getOrDefault(false)
    }

    private fun positiveId(text: String): Boolean = (text.toLongOrNull() ?: 0L) > 0L

    fun issue(fields: List<String>): String? {
        if (fields.size != 10) return "trace-fields"
        if (fields.any { it.length > MAX_FIELD_LENGTH }) return "oversized-field"
        val actor = fields[3].split(',')
        if (actor.size != 2 || actor.any { !finiteBits(it) }) return "actor-position"
        val vitality = fields[4].split(',')
        if (vitality.size != 2 ||
            (vitality[0].toIntOrNull() ?: -1) !in 0..1000 ||
            !finiteBits(vitality[1])) return "health/invulnerability"
        val invulnerability = java.lang.Double.longBitsToDouble(
            java.lang.Long.parseUnsignedLong(vitality[1], 16)
        )
        if (invulnerability < 0.0) return "health/invulnerability"

        val pass = fields[5].removePrefix("pass=")
        if (!fields[5].startsWith("pass=") ||
            (pass != "none" && !positiveId(pass))) return "pass-bomb"
        if (!fields[6].startsWith("remoteOwners=")) return "remote-owners"
        val remote = fields[6].removePrefix("remoteOwners=")
        if (remote !in setOf("", "player", "player2", "player,player2")) return "remote-owners"
        if (!fields[7].startsWith("nextBombId=") ||
            !positiveId(fields[7].removePrefix("nextBombId="))) return "next-bomb-id"
        val nextBombId = fields[7].removePrefix("nextBombId=").toLong()

        val bombParts = fields[8].split(';')
        val bombCount = bombParts[0].removePrefix("bombs=").toIntOrNull()
        if (!bombParts[0].startsWith("bombs=") ||
            bombCount == null || bombCount !in 0..100 || bombParts.size != bombCount + 1) return "bombs"
        val ids = HashSet<Long>()
        for (i in 1 until bombParts.size) {
            val b = bombParts[i].split(',')
            if (b.size != 6 || !positiveId(b[0]) || b[0].toLong() >= nextBombId ||
                !ids.add(b[0].toLong()) || b[1] !in validOwners ||
                !finiteBits(b[2]) || !finiteBits(b[3]) ||
                b[5] !in setOf("true", "false") ||
                (b[5] == "true" && b[4] != "remote") ||
                (b[5] == "false" && (!finiteBits(b[4]) ||
                    java.lang.Double.longBitsToDouble(
                        java.lang.Long.parseUnsignedLong(b[4], 16)
                    ) <= 0.0))) return "bombs"
        }

        // Exit grace may only refer to a bomb ID already allocated by this replay.
        // Otherwise a forged future ID could bypass bomb collision validation.
        if (pass != "none" && pass.toLong() >= nextBombId) return "pass-bomb"

        val flameParts = fields[9].split(';')
        val flameCount = flameParts[0].removePrefix("flames=").toIntOrNull()
        if (!flameParts[0].startsWith("flames=") ||
            flameCount == null || flameCount !in 0..100 ||
            flameParts.size != flameCount + 1) return "flames"
        for (i in 1 until flameParts.size) {
            val separator = flameParts[i].indexOf(':')
            if (separator <= 0 || !finiteBits(flameParts[i].substring(0, separator))) return "flames"
            val remaining = java.lang.Double.longBitsToDouble(
                java.lang.Long.parseUnsignedLong(flameParts[i].substring(0, separator), 16)
            )
            if (remaining <= 0.0) return "flames"
            val cellText = flameParts[i].substring(separator + 1)
            if (!cellText.endsWith('/')) return "flames"
            val cells = cellText.dropLast(1).split('/')
            if (cells.isEmpty() || cells.size > 49 || cells.any { cell ->
                    val xy = cell.split(',')
                    xy.size != 2 || xy.any { !finiteBits(it) }
                }) return "flames"
        }
        return null
    }
}
