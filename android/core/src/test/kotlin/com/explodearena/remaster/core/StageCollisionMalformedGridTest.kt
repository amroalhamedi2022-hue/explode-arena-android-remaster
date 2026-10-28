package com.explodearena.remaster.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** N77: post-construction mutation of caller-owned grid aliases must fail closed. */
class StageCollisionMalformedGridTest {
    private fun stage(rows: MutableList<IntArray>): StageData = StageData(
        id = 0,
        image = "assets/maps/map_000.png",
        width = 16,
        height = 16,
        obstacleX = 0,
        obstacleY = 0,
        grid = rows,
        spawns = listOf(Spawn(4.0, 4.0)),
    )

    @Test fun shortenedOverlappedRowIsBlockedWithoutIndexingPastItsEnd() {
        val rows = mutableListOf(intArrayOf(0, 0), intArrayOf(0, 0))
        val arena = stage(rows)
        rows[1] = intArrayOf(0)

        assertEquals(-1, StageCollision.tile(arena, 12.0, 12.0))
        assertTrue(StageCollision.blocked(arena, 12.0, 12.0, radius = 0.0))
    }

    @Test fun shortenedFirstRowInvalidatesTheWholeGridGeometry() {
        val rows = mutableListOf(intArrayOf(0, 0), intArrayOf(0, 0))
        val arena = stage(rows)
        rows[0] = intArrayOf(0)

        assertEquals(-1, StageCollision.tile(arena, 4.0, 4.0))
        assertTrue(StageCollision.blocked(arena, 4.0, 4.0, radius = 0.0))
    }

    @Test fun clearedCallerOwnedGridFailsClosed() {
        val rows = mutableListOf(intArrayOf(0, 0), intArrayOf(0, 0))
        val arena = stage(rows)
        rows.clear()

        assertEquals(-1, StageCollision.tile(arena, 4.0, 4.0))
        assertTrue(StageCollision.blocked(arena, 4.0, 4.0, radius = 0.0))
    }
}
