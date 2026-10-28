package com.explodearena.remaster.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** N78: hot collision probes must validate only the grid rows they touch. */
class StageCollisionBoundedGridValidationTest {
    private class CountingRows(private val backing: MutableList<IntArray>) : AbstractMutableList<IntArray>() {
        var reads = 0
        var iterations = 0

        override val size: Int get() = backing.size
        override fun get(index: Int): IntArray {
            reads++
            return backing[index]
        }
        override fun add(index: Int, element: IntArray) { backing.add(index, element) }
        override fun set(index: Int, element: IntArray): IntArray = backing.set(index, element)
        override fun removeAt(index: Int): IntArray = backing.removeAt(index)
        override fun iterator(): MutableIterator<IntArray> {
            iterations++
            return super.iterator()
        }
    }

    private fun stage(rows: List<IntArray>): StageData = StageData(
        id = 0,
        image = "assets/maps/map_000.png",
        width = 64,
        height = 64,
        obstacleX = 0,
        obstacleY = 0,
        grid = rows,
        spawns = listOf(Spawn(4.0, 4.0)),
    )

    @Test fun blockedChecksOnlyHeaderAndOverlappedRowsNotEveryGridRow() {
        val backing = MutableList(8) { IntArray(8) }
        val rows = CountingRows(backing)
        val arena = stage(rows)
        rows.reads = 0
        rows.iterations = 0

        // Corruption in a distant row is never indexed by this top-left probe.
        rows[7] = intArrayOf(0)
        rows.reads = 0
        rows.iterations = 0

        assertFalse(StageCollision.blocked(arena, 4.0, 4.0, radius = 0.0))
        assertTrue("collision preflight must not traverse all rows", rows.iterations == 0)
        assertTrue("only header + touched row should be read", rows.reads <= 3)
    }

    @Test fun malformedTouchedRowFailsClosedAndNearestFreeDoesNotIndexIt() {
        val backing = MutableList(8) { IntArray(8) }
        val arena = stage(backing)
        backing[1] = intArrayOf(0)

        assertEquals(-1, StageCollision.tile(arena, 4.0, 12.0))
        assertTrue(StageCollision.blocked(arena, 4.0, 12.0, radius = 0.0))

        // Force nearestFree to search beyond the first blocked row; it must
        // return null on the malformed row rather than indexing past its end.
        backing[0] = IntArray(8) { 1 }
        assertNull(StageCollision.nearestFree(arena, 4.0, 4.0, radius = 4.0))
    }

    @Test fun changedListSizeAndFirstRowShapeFailClosed() {
        val mutableRows = MutableList(8) { IntArray(8) }
        val arena = stage(mutableRows)
        mutableRows[0] = IntArray(7)
        assertEquals(-1, StageCollision.tile(arena, 4.0, 4.0))
        assertTrue(StageCollision.blocked(arena, 4.0, 4.0, radius = 0.0))

        mutableRows[0] = IntArray(8)
        mutableRows.clear()
        assertEquals(-1, StageCollision.tile(arena, 4.0, 4.0))
        assertTrue(StageCollision.blocked(arena, 4.0, 4.0, radius = 0.0))
    }
}
