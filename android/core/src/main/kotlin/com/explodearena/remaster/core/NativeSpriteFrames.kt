package com.explodearena.remaster.core

import kotlin.math.floor

/** Original Mainline frame atlas selection; EKA2L1 animation parity unverified. */
object NativeSpriteFrames {
    const val PLAYER_FRAME_COUNT = 32
    const val BLAST_FRAME_COUNT = 14

    // Original Mainline direction indices: up, left, down, right.
    fun player(direction: Int, walking: Boolean, elapsedSeconds: Double): Int {
        require(elapsedSeconds.isFinite() && elapsedSeconds >= 0.0)
        val base = when (direction) {
            0 -> 24
            1 -> 16
            2 -> 8
            3 -> 0
            else -> 8
        }
        return base + if (walking) (floor(elapsedSeconds * 12.0) % 8.0).toInt() else 0
    }

    fun flame(remainingSeconds: Double, durationSeconds: Double): Int {
        require(remainingSeconds.isFinite() && durationSeconds.isFinite() && durationSeconds > 0.0)
        val progress = (1.0 - remainingSeconds / durationSeconds).coerceIn(0.0, 1.0)
        return floor(progress * BLAST_FRAME_COUNT).toInt().coerceIn(0, BLAST_FRAME_COUNT - 1)
    }
}
