package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/** Golden contract for current JS-mainline PROVISIONAL bomb solidity, not Symbian parity. */
class NativeBombCollisionTest {
    private fun stage() = StageData(
        id = 0, image = "assets/maps/map_000.png", width = 128, height = 64,
        obstacleX = 0, obstacleY = 0,
        grid = List(8) { IntArray(16) },
        spawns = listOf(Spawn(24.0, 24.0)),
    )

    @Test fun placingActorCanExitOnceButCannotWalkBackThroughOwnBomb() {
        val stage = stage()
        val sim = NativeBombSimulation(stage)
        val actor = Actor(24.0, 24.0)
        assertTrue(sim.place(actor))
        assertEquals(sim.bombs.single().id, actor.passBomb)
        repeat(20) {
            StageCollision.move(stage, actor, 1.0, 0.0, 77.0, FixedStepClock.STEP_SECONDS, sim.bombs)
        }
        assertNull(actor.passBomb)
        assertTrue(actor.x > 36.0)
        repeat(30) {
            StageCollision.move(stage, actor, -1.0, 0.0, 77.0, FixedStepClock.STEP_SECONDS, sim.bombs)
        }
        assertTrue("Cannot re-enter 7px bomb + 5px actor hitbox", actor.x >= 36.0)
    }

    @Test fun otherActorCannotCrossBombAndPlacementCannotOverlap() {
        val stage = stage()
        val sim = NativeBombSimulation(stage)
        val placer = Actor(24.0, 24.0)
        assertTrue(sim.place(placer))
        val other = Actor(48.0, 24.0)
        repeat(40) {
            StageCollision.move(stage, other, -1.0, 0.0, 77.0, FixedStepClock.STEP_SECONDS, sim.bombs)
        }
        assertTrue(other.x >= 36.0)
        assertNull(other.passBomb)
        assertFalse(sim.place(Actor(24.0, 24.0), "player2"))
    }

    @Test fun bombExpiryClearsStalePassEvenWhenNoInput() {
        val stage = stage()
        val sim = NativeBombSimulation(stage)
        val actor = Actor(24.0, 24.0)
        assertTrue(sim.place(actor))
        repeat(96) { sim.tick() }
        assertTrue(sim.bombs.isEmpty())
        StageCollision.move(stage, actor, 0.0, 0.0, 77.0, FixedStepClock.STEP_SECONDS, sim.bombs)
        assertNull(actor.passBomb)
    }

    @Test fun wallAndMapBoundsStillBlockWithoutBombs() {
        val stage = stage()
        val actor = Actor(8.0, 8.0)
        StageCollision.move(stage, actor, -1.0, 0.0, 77.0, 1.0 / 60.0)
        assertTrue(actor.x >= 5.0)
        assertTrue(StageCollision.blocked(stage, 0.0, 8.0))
    }
}
