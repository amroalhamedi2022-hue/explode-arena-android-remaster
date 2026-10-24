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

    @Test fun oneKeyDetonatesEveryOwnedRemoteBombWithoutAffectingDistantOtherOwner() {
        val sim = NativeBombSimulation(stage(), bombLimit = 3)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.setRemoteDetonator("player2", true))
        assertTrue(sim.place(Actor(24.0, 24.0), "player"))
        assertTrue(sim.place(Actor(120.0, 24.0), "player"))
        assertTrue(sim.place(Actor(168.0, 72.0), "player2"))
        repeat(600) { assertEquals(0, sim.tick()) }
        assertEquals(3, sim.bombs.size)
        assertTrue(sim.detonateRemote("player"))
        assertEquals(1, sim.bombs.size)
        assertEquals("player2", sim.bombs.single().owner)
        assertTrue(sim.bombs.single().remote)
        assertTrue(sim.isBurning(24.0, 24.0))
        assertTrue(sim.isBurning(120.0, 24.0))
        assertFalse(sim.detonateRemote("player"))
        assertTrue(sim.detonateRemote("player2"))
        assertTrue(sim.bombs.isEmpty())
    }

    @Test fun timedBombPlacedBeforePickupKeepsFuseAndIsNotDirectlyRemoteDetonated() {
        val sim = NativeBombSimulation(stage(), bombLimit = 3)
        assertTrue(sim.place(Actor(24.0, 24.0), "player"))
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(120.0, 24.0), "player"))
        assertTrue(sim.place(Actor(168.0, 72.0), "player"))
        assertEquals(1, sim.bombs.count { !it.remote })
        assertEquals(2, sim.bombs.count { it.remote })
        assertTrue(sim.setRemoteDetonator("player", false))
        assertFalse(sim.hasRemoteDetonator("player"))
        // Removing the power does not retroactively change placed remote bombs.
        assertTrue(sim.detonateRemote("player"))
        assertEquals(1, sim.bombs.size)
        assertFalse(sim.bombs.single().remote)
        assertEquals(24.0, sim.bombs.single().x, 0.0)
        repeat(95) { assertEquals(0, sim.tick()) }
        assertEquals(1, sim.tick())
    }

    @Test fun remoteBlastCanChainReactIntoTimedBomb() {
        val sim = NativeBombSimulation(stage(), bombLimit = 2)
        assertTrue(sim.place(Actor(24.0, 24.0), "player"))
        assertTrue(sim.setRemoteDetonator("player2", true))
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        assertTrue(sim.detonateRemote("player2"))
        assertTrue(sim.bombs.isEmpty())
        assertEquals(2, sim.flames.size)
    }
}
