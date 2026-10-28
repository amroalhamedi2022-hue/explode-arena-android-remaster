package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class SimulationTest {
    private fun stage() = StageData(
        0, "assets/maps/map_000.png", 64, 64, 0, 0,
        List(8) { row -> IntArray(8) { col -> if (row == 3 && col == 3) 751 else 0 } },
        listOf(Spawn(12.0, 12.0)),
    )

    @Test fun fixedStepIsIndependentOfRenderFrameRate() {
        val small = FixedStepClock()
        val large = FixedStepClock()
        repeat(60) { small.advance(1.0 / 60.0) {} }
        repeat(4) { large.advance(0.25) {} }
        assertEquals(60L, small.ticks)
        assertEquals(small.ticks, large.ticks)
    }

    @Test fun frameClampRejectsUnboundedCatchup() {
        val clock = FixedStepClock()
        assertEquals(15, clock.advance(10.0) {})
        assertEquals(15L, clock.ticks)
        assertThrows(IllegalArgumentException::class.java) { clock.advance(Double.NaN) {} }
        assertThrows(IllegalArgumentException::class.java) { clock.advance(-1.0) {} }
    }

    @Test fun lcgMatchesProvisionalJavaScriptUnsignedSequence() {
        val rng = SeededRng(1)
        assertEquals(1015568748L, rng.nextUnsigned())
        assertEquals(1586005467L, rng.nextUnsigned())
    }

    @Test fun movementUsesAxisSeparatedCollisionAndNoWallPenetration() {
        val stage = stage()
        val actor = Actor(12.0, 26.0)
        StageCollision.move(stage, actor, 1.0, 0.0, 77.0, 1.0)
        assertTrue(actor.x < 19.0)
        assertEquals(26.0, actor.y, 1e-9)
        assertTrue(StageCollision.blocked(stage, 26.0, 26.0))
        assertFalse(StageCollision.blocked(stage, 12.0, 12.0))
    }

    @Test fun hitboxCannotEncloseSolidCellWhileAllCornersLookFree() {
        val arena = stage() // Solid tile occupies [24,32) on both axes.
        // Actor spans [23,33] on both axes: every corner is in a
        // neighboring empty tile, but the entire solid cell is inside.
        assertEquals(0, StageCollision.tile(arena, 23.0, 23.0))
        assertEquals(0, StageCollision.tile(arena, 33.0, 33.0))
        assertTrue(StageCollision.blocked(arena, 28.0, 28.0, 5.0))
        // Near, but not yet touching the solid cell.
        assertFalse(StageCollision.blocked(arena, 18.0, 28.0, 5.0))
        // Once the actor overlaps the cell, the collision is solid.
        assertTrue(StageCollision.blocked(arena, 20.0, 28.0, 5.0))
    }

    @Test fun nearestFreeNeverReturnsBlockedTile() {
        val stage = stage()
        val safe = StageCollision.nearestFree(stage, 26.0, 26.0)!!
        assertFalse(StageCollision.blocked(stage, safe.x, safe.y, 4.0))
    }
    @Test fun invalidCoordinatesNeverBecomeFreeTiles() {
        val stage = stage()
        assertEquals(-1, StageCollision.tile(stage, Double.NaN, 12.0))
        assertEquals(-1, StageCollision.tile(stage, 12.0, Double.POSITIVE_INFINITY))
        assertTrue(StageCollision.blocked(stage, Double.NaN, 12.0))
        assertTrue(StageCollision.blocked(stage, 12.0, Double.NEGATIVE_INFINITY))
        assertTrue(StageCollision.blocked(stage, 12.0, 12.0, Double.NaN))
        assertTrue(StageCollision.blocked(stage, 12.0, 12.0, -1.0))
    }

    @Test fun movementRejectsNonfiniteSpeedAndTimeWithoutMutatingActor() {
        val stage = stage()
        val actor = Actor(12.0, 12.0)
        assertThrows(IllegalArgumentException::class.java) {
            StageCollision.move(stage, actor, 1.0, 0.0, Double.POSITIVE_INFINITY, 1.0 / 60.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            StageCollision.move(stage, actor, 1.0, 0.0, 77.0, Double.NaN)
        }
        assertEquals(12.0, actor.x, 0.0)
        assertEquals(12.0, actor.y, 0.0)
    }

    @Test fun hugeFiniteMovementCannotOverflowOrHangSimulation() {
        val arena = stage()
        val actor = Actor(12.0, 12.0)
        assertThrows(IllegalArgumentException::class.java) {
            StageCollision.move(arena, actor, 1.0, 0.0, Double.MAX_VALUE, Double.MAX_VALUE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            StageCollision.move(arena, actor, 1.0, 0.0, 100_000.0, 1.0)
        }
        assertEquals(12.0, actor.x, 0.0)
        assertEquals(12.0, actor.y, 0.0)
    }

    @Test fun rejectedReplayMovementPreservesBombGraceAndPosition() {
        // A previously exited bomb is no longer in the bomb list. Invalid
        // movement must not clear the actor's passBomb before throwing.
        val arena = stage()
        val actor = Actor(12.0, 12.0, passBomb = 42L)
        assertThrows(IllegalArgumentException::class.java) {
            StageCollision.move(arena, actor, 1.0, 0.0, 100_000.0, 1.0)
        }
        assertEquals(12.0, actor.x, 0.0)
        assertEquals(12.0, actor.y, 0.0)
        assertEquals(42L, actor.passBomb)
        // A valid stationary tick still clears stale grace as intended.
        StageCollision.move(arena, actor, 0.0, 0.0, 77.0, 1.0 / 60.0)
        assertNull(actor.passBomb)
    }

    @Test fun nearestFreeRejectsNonFiniteOrOversizedQueries() {
        val arena = stage()
        assertNull(StageCollision.nearestFree(arena, Double.NaN, 12.0))
        assertNull(StageCollision.nearestFree(arena, 12.0, Double.POSITIVE_INFINITY))
        assertNull(StageCollision.nearestFree(arena, 12.0, 12.0, Double.NaN))
        assertNull(StageCollision.nearestFree(arena, 12.0, 12.0, 100.0))
    }


    @Test fun nearestFreeRespectsOffsetGridCellCenters() {
        // Only the center 8px cell is walkable. Its center is (24,24),
        // not the world-aligned (20,20) chosen by the old search.
        val offset = StageData(
            42, "offset.png", 48, 48, 4, 4,
            List(5) { row -> IntArray(5) { col ->
                if (row == 2 && col == 2) 0 else 751
            } },
            listOf(Spawn(8.0, 8.0)),
        )
        val found = StageCollision.nearestFree(offset, 8.0, 8.0, radius = 3.0)
        assertNotNull(found)
        assertEquals(24.0, found!!.x, 0.0)
        assertEquals(24.0, found.y, 0.0)
        assertFalse(StageCollision.blocked(offset, found.x, found.y, 3.0))
    }

    @Test fun movementRejectsInvalidActorAndOverflowedDirection() {
        val arena = stage()
        val actor = Actor(Double.NaN, 12.0)
        assertThrows(IllegalArgumentException::class.java) {
            StageCollision.move(arena, actor, 1.0, 0.0, 77.0, 1.0 / 60.0)
        }
        val safe = Actor(12.0, 12.0)
        assertThrows(IllegalArgumentException::class.java) {
            StageCollision.move(arena, safe, Double.MAX_VALUE, Double.MAX_VALUE, 77.0, 1.0 / 60.0)
        }
        assertEquals(12.0, safe.x, 0.0)
        assertEquals(12.0, safe.y, 0.0)
    }
}
