package com.explodearena.remaster.core

import kotlin.math.abs
import kotlin.math.floor

/**
 * Bounded spatial lookup for 16px-grid blast cells.
 *
 * A query with the native actor radius (5px) can overlap at most two cell
 * centers on each axis. The four candidate hash lookups preserve the strict
 * hitbox boundary used by NativeBombSimulation.blastHits without scanning
 * every timed cell during BFS.
 */
internal class TimedBlastIndex(cells: Iterable<NativeBombSimulation.TimedBlastCell>) {
    private val deadlinesByCell = HashMap<Long, Double>()

    init {
        for (threat in cells) {
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
            val key = key(column, row)
            val previous = deadlinesByCell[key]
            if (previous == null || threat.secondsUntilBlast < previous) {
                deadlinesByCell[key] = threat.secondsUntilBlast
            }
        }
    }

    fun contains(x: Double, y: Double, radius: Double = StageCollision.ACTOR_RADIUS): Boolean =
        earliestDeadlineAt(x, y, radius) != null

    /** Earliest deadline among all indexed blast cells overlapping this actor footprint. */
    fun earliestDeadlineAt(
        x: Double,
        y: Double,
        radius: Double = StageCollision.ACTOR_RADIUS,
    ): Double? {
        require(x.isFinite() && y.isFinite()) { "Query coordinates must be finite" }
        require(radius.isFinite() && radius in 0.0..StageCollision.ACTOR_RADIUS) {
            "Indexed query radius must be within the native actor radius"
        }
        val column = queryIndex(x) ?: return null
        val row = queryIndex(y) ?: return null
        val reach = HALF_CELL + radius
        var earliest: Double? = null
        for (candidateColumn in column..(column + 1)) {
            val centerX = center(candidateColumn)
            if (abs(centerX - x) >= reach) continue
            for (candidateRow in row..(row + 1)) {
                val deadline = deadlinesByCell[key(candidateColumn, candidateRow)] ?: continue
                if (abs(center(candidateRow) - y) >= reach) continue
                if (earliest == null || deadline < earliest) earliest = deadline
            }
        }
        return earliest
    }

    companion object {
        private const val STEP = 16.0
        private const val HALF_CELL = 8.0
        private const val ALIGNMENT_EPSILON = 1e-6

        fun fromCells(cells: Iterable<Spawn>): TimedBlastIndex =
            TimedBlastIndex(cells.map { NativeBombSimulation.TimedBlastCell(it, 0.0) })

        private fun cellIndex(value: Double): Int {
            val raw = floor((value - HALF_CELL) / STEP)
            require(raw.isFinite() && raw >= Int.MIN_VALUE.toDouble() && raw < Int.MAX_VALUE.toDouble()) {
                "Blast cell coordinate is outside the supported grid"
            }
            return raw.toInt()
        }

        private fun queryIndex(value: Double): Int? {
            val raw = floor((value - HALF_CELL) / STEP)
            if (!raw.isFinite() || raw < Int.MIN_VALUE.toDouble() ||
                raw >= Int.MAX_VALUE.toDouble()) return null
            return raw.toInt()
        }

        private fun center(index: Int): Double = index * STEP + HALF_CELL

        private fun key(column: Int, row: Int): Long =
            (column.toLong() shl 32) or (row.toLong() and 0xffffffffL)
    }
}
