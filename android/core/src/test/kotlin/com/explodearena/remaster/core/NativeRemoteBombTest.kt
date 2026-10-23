package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeRemoteBombTest {
    private fun stage() = StageData(
        id = 0, image = "assets/maps/map_000.png", width = 192, height = 96,
        obstacleX = 0, obstacleY = 0,
        grid = List(12) { IntArray(24) },
        spawns = listOf(Spawn(24.0, 24.0)),
    )

    @Test fun remoteBombHasNoFuseAndDetonatesOnlyOnOwnerRequest() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(24.0, 24.0)))
        assertTrue(sim.bombs.single().remote)
        assertNull(sim.bombs.single().remaining)
        repeat(600) { assertEquals(0, sim.tick()) }
        assertEquals(1, sim.bombs.size)
        assertFalse(sim.detonateRemote("player2"))
        assertTrue(sim.detonateRemote("player"))
        assertTrue(sim.bombs.isEmpty())
        assertTrue(sim.isBurning(24.0, 24.0))
        assertFalse(sim.detonateRemote("player"))
    }

    @Test fun ordinaryTimedBombsAndOwnerPowerAreUnchanged() {
        val sim = NativeBombSimulation(stage())
        assertFalse(sim.setRemoteDetonator("bot", true))
        assertTrue(sim.place(Actor(24.0, 24.0)))
        assertFalse(sim.bombs.single().remote)
        assertFalse(sim.detonateRemote())
        repeat(95) { assertEquals(0, sim.tick()) }
        assertEquals(1, sim.tick())
    }

    @Test fun ambiguousMultipleRemoteBombsAreNeverDetonatedByGuesswork() {
        val sim = NativeBombSimulation(stage(), bombLimit = 2)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(24.0, 24.0)))
        assertTrue(sim.place(Actor(120.0, 24.0)))
        assertEquals(2, sim.bombs.size)
        assertFalse(sim.detonateRemote("player"))
        assertEquals(2, sim.bombs.size)
        assertTrue(sim.setRemoteDetonator("player", false))
        assertFalse(sim.hasRemoteDetonator("player"))
        // Existing remote bombs keep their mode when the power is disabled.
        assertTrue(sim.bombs.all { it.remote && it.remaining == null })
    }
}
