package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class TimedBlastIndexTest {
    private fun cell(column: Int, row: Int) =
        Spawn(column * 16.0 + 8.0, row * 16.0 + 8.0)

    @Test fun membershipMatchesLinearHitboxAtGridAndStrictBoundaries() {
        val cells = listOf(
            cell(-2, -1), cell(-1, 0), cell(0, 0), cell(1, 1),
            cell(3, -2), cell(3, -2),
        )
        val index = TimedBlastIndex.fromCells(cells)
        val coordinates = (-3..4).flatMap { grid ->
            val center = grid * 16.0 + 8.0
            listOf(
                center - 13.001, center - 13.0, center - 12.999,
                center - 8.0, center, center + 8.0,
                center + 12.999, center + 13.0, center + 13.001,
            )
        }.distinct()

        for (x in coordinates) for (y in coordinates) {
            val expected = cells.any {
                NativeBombSimulation.blastHits(it, x, y, StageCollision.ACTOR_RADIUS)
            }
            assertEquals("x=$x y=$y", expected, index.contains(x, y))
        }
    }

    @Test fun earliestOverlappingDeadlineWinsAndDuplicateCellsAreCoalesced() {
        val index = TimedBlastIndex(
            listOf(
                NativeBombSimulation.TimedBlastCell(cell(0, 0), 2.0),
                NativeBombSimulation.TimedBlastCell(cell(0, 0), 1.5),
                NativeBombSimulation.TimedBlastCell(cell(1, 0), 0.75),
                NativeBombSimulation.TimedBlastCell(cell(4, 4), 0.1),
            ),
        )

        // At the midpoint, both adjacent flame cells overlap a 5px actor.
        assertEquals(0.75, requireNotNull(index.earliestDeadlineAt(16.0, 8.0)), 0.0)
        assertEquals(0.1, requireNotNull(index.earliestDeadlineAt(72.0, 72.0)), 0.0)
        assertNull(index.earliestDeadlineAt(200.0, 200.0))
    }

    @Test fun emptyIndexAndNonFiniteCoordinatesFailClosed() {
        val empty = TimedBlastIndex.fromCells(emptyList())
        assertFalse(empty.contains(8.0, 8.0))
        assertNull(empty.earliestDeadlineAt(8.0, 8.0))

        assertThrows(IllegalArgumentException::class.java) {
            empty.contains(Double.NaN, 8.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            empty.contains(8.0, 8.0, StageCollision.ACTOR_RADIUS + 0.01)
        }
    }

    @Test fun rejectsUnalignedCellsAndInvalidDeadlines() {
        assertThrows(IllegalArgumentException::class.java) {
            TimedBlastIndex(listOf(
                NativeBombSimulation.TimedBlastCell(Spawn(8.5, 8.0), 1.0),
            ))
        }
        assertThrows(IllegalArgumentException::class.java) {
            TimedBlastIndex(listOf(
                NativeBombSimulation.TimedBlastCell(cell(0, 0), Double.NaN),
            ))
        }
    }
}
