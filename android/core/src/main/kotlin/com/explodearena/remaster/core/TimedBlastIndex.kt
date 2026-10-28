package com.explodearena.remaster.core

import kotlin.math.abs
import kotlin.math.floor

/**
 * Bounded primitive spatial lookup for 16px-grid blast cells.
 *
 * A query with the native actor radius (5px) can overlap at most two cell
 * centers on each axis. Four open-addressed table lookups preserve the strict
 * hitbox boundary used by NativeBombSimulation.blastHits without scanning all
 * timed cells or allocating per-query lists/boxed grid keys.
 */
internal class TimedBlastIndex(cells: Iterable<NativeBombSimulation.TimedBlastCell>) {
    private val keys: LongArray
    private val deadlines: DoubleArray
    private val occupied: BooleanArray
    private val mask: Int

    init {
        val source = ArrayList<NativeBombSimulation.TimedBlastCell>()
        for (threat in cells) {
            require(source.size < MAX_INDEXED_CELLS) { "Too many blast cells for a bounded index" }
            val cell = threat.cell
            require(cell.x.isFinite() && cell.y.isFinite()) { "Blast cell coordinates must be finite" }
            require(threat.secondsUntilBlast.isFinite() && threat.secondsUntilBlast >= 0.0) {
                "Blast deadlines must be finite and non-negative"
            }
            val column = cellIndex(cell.x)
            val row = cellIndex(cell.y)
            require(abs(center(column) - cell.x) < ALIGNMENT_EPSILON &&
                abs(center(row) - cell.y) < ALIGNMENT_EPSILON) {
                "Blast cells must align to the 16px native bomb grid"
            }
            source.add(threat)
        }

        var capacity = 1
        val requiredCapacity = maxOf(2, source.size * 2)
        while (capacity < requiredCapacity) {
            require(capacity < (1 shl 30)) { "Blast index capacity overflow" }
            capacity = capacity shl 1
        }
        keys = LongArray(capacity)
        deadlines = DoubleArray(capacity)
        occupied = BooleanArray(capacity)
        mask = capacity - 1

        for (threat in source) {
            val column = cellIndex(threat.cell.x)
            val row = cellIndex(threat.cell.y)
            val key = key(column, row)
            val slot = slotFor(key)
            if (!occupied[slot]) {
                occupied[slot] = true
                keys[slot] = key
                deadlines[slot] = threat.secondsUntilBlast
            } else if (threat.secondsUntilBlast < deadlines[slot]) {
                // Several bomb rays may overlap one cell; keep the earliest danger.
                deadlines[slot] = threat.secondsUntilBlast
            }
        }
    }

    fun contains(x: Double, y: Double, radius: Double = StageCollision.ACTOR_RADIUS): Boolean =
        earliestDeadlineAt(x, y, radius) != null

    /** Earliest deadline among indexed cells overlapping this actor footprint. */
    fun earliestDeadlineAt(
        x: Double,
        y: Double,
        radius: Double = StageCollision.ACTOR_RADIUS,
    ): Double? {
        require(x.isFinite() && y.isFinite()) { "Query coordinates must be finite" }
        require(radius.isFinite() && radius >= 0.0 && radius <= StageCollision.ACTOR_RADIUS) {
            "Indexed query radius must be within the native actor radius"
        }
        val column = queryIndex(x)
        val row = queryIndex(y)
        if (column == INVALID_INDEX || row == INVALID_INDEX) return null
        val reach = HALF_CELL + radius
        var earliest = Double.POSITIVE_INFINITY
        val right = column + 1
        val below = row + 1
        val d00 = deadlineAtCell(column, row, x, y, reach)
        val d01 = deadlineAtCell(column, below, x, y, reach)
        val d10 = deadlineAtCell(right, row, x, y, reach)
        val d11 = deadlineAtCell(right, below, x, y, reach)
        if (d00 < earliest) earliest = d00
        if (d01 < earliest) earliest = d01
        if (d10 < earliest) earliest = d10
        if (d11 < earliest) earliest = d11
        return if (earliest == Double.POSITIVE_INFINITY) null else earliest
    }

    private fun deadlineAtCell(
        column: Int,
        row: Int,
        x: Double,
        y: Double,
        reach: Double,
    ): Double {
        if (abs(center(column) - x) >= reach || abs(center(row) - y) >= reach) {
            return Double.POSITIVE_INFINITY
        }
        val candidateKey = key(column, row)
        val slot = slotFor(candidateKey)
        return if (occupied[slot] && keys[slot] == candidateKey) {
            deadlines[slot]
        } else {
            Double.POSITIVE_INFINITY
        }
    }

    private fun slotFor(candidateKey: Long): Int {
        var slot = ((candidateKey xor (candidateKey ushr 32)) * HASH_MULTIPLIER).toInt() and mask
        while (occupied[slot] && keys[slot] != candidateKey) {
            slot = (slot + 1) and mask
        }
        return slot
    }

    companion object {
        private const val STEP = 16.0
        private const val HALF_CELL = 8.0
        private const val ALIGNMENT_EPSILON = 1e-6
        private const val MAX_INDEXED_CELLS = 100_000
        private const val HASH_MULTIPLIER = 0x9E3779B9L
        private const val INVALID_INDEX = Int.MAX_VALUE

        fun fromCells(cells: Iterable<Spawn>): TimedBlastIndex =
            TimedBlastIndex(cells.asSequence().map {
                NativeBombSimulation.TimedBlastCell(it, 0.0)
            }.asIterable())

        private fun cellIndex(value: Double): Int {
            val raw = floor((value - HALF_CELL) / STEP)
            require(raw.isFinite() && raw >= Int.MIN_VALUE.toDouble() && raw < Int.MAX_VALUE.toDouble()) {
                "Blast cell coordinate is outside the supported grid"
            }
            return raw.toInt()
        }

        private fun queryIndex(value: Double): Int {
            val raw = floor((value - HALF_CELL) / STEP)
            if (!raw.isFinite() || raw < Int.MIN_VALUE.toDouble() ||
                raw >= Int.MAX_VALUE.toDouble()) return INVALID_INDEX
            return raw.toInt()
        }

        private fun center(index: Int): Double = index * STEP + HALF_CELL

        private fun key(column: Int, row: Int): Long =
            (column.toLong() shl 32) or (row.toLong() and 0xffffffffL)
    }
}
