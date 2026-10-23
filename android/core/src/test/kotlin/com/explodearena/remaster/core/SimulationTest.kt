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
        val actor = Actor(12.0, 28.0)
        StageCollision.move(stage, actor, 1.0, 0.0, 77.0, 1.0)
        assertTrue(actor.x < 19.0)
        assertEquals(28.0, actor.y, 1e-9)
        assertTrue(StageCollision.blocked(stage, 28.0, 28.0))
        assertFalse(StageCollision.blocked(stage, 12.0, 12.0))
    }

    @Test fun nearestFreeNeverReturnsBlockedTile() {
        val stage = stage()
        val safe = StageCollision.nearestFree(stage, 28.0, 28.0)!!
        assertFalse(StageCollision.blocked(stage, safe.x, safe.y, 4.0))
    }
}
