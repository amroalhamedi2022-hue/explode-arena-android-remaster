package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeBombSimulationTest {
    private fun stage(wallColumn: Int = -1) = StageData(
        id = 0, image = "assets/maps/map_000.png", width = 128, height = 64,
        obstacleX = 0, obstacleY = 0,
        grid = List(8) { IntArray(16) { col -> if (col == wallColumn) 751 else 0 } },
        spawns = listOf(Spawn(24.0, 24.0)),
    )

    @Test fun placementUses16PixelCellsOwnerLimitsAndRejectsOverlap() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.place(Actor(23.0, 23.0)))
        assertEquals(24.0, sim.bombs.single().x, 0.0)
        assertFalse(sim.place(Actor(40.0, 24.0))) // same owner, one active bomb
        assertFalse(sim.place(Actor(23.0, 23.0), "player2")) // occupied tile
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        assertEquals(2, sim.bombs.size)
        assertThrows(IllegalArgumentException::class.java) { sim.place(Actor(24.0, 24.0), "unknown") }
    }

    @Test fun fuseDetonatesAt96FixedTicksAndFlamesExpire() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.place(Actor(24.0, 24.0)))
        repeat(95) { assertEquals(0, sim.tick()) }
        assertEquals(1, sim.tick())
        assertTrue(sim.bombs.isEmpty())
        assertTrue(sim.isBurning(24.0, 24.0))
        assertTrue(sim.isBurning(56.0, 24.0))
        assertFalse(sim.isBurning(120.0, 56.0))
        repeat(27) { sim.tick() }
        assertFalse(sim.isBurning(24.0, 24.0))
    }

    @Test fun chainReactionDetonatesLaterPlacedBombImmediately() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.place(Actor(24.0, 24.0)))
        repeat(48) { sim.tick() }
        assertTrue(sim.place(Actor(56.0, 24.0), "player2"))
        repeat(47) { assertEquals(0, sim.tick()) }
        assertEquals(2, sim.tick())
        assertTrue(sim.bombs.isEmpty())
        assertEquals(2, sim.flames.size)
    }

    @Test fun obstacleStopsBlastBeforeNextBomb() {
        val sim = NativeBombSimulation(stage(wallColumn = 8)) // x = 64..71; blocks center 72
        assertTrue(sim.place(Actor(24.0, 24.0)))
        repeat(48) { sim.tick() }
        assertTrue(sim.place(Actor(88.0, 24.0), "player2"))
        repeat(47) { sim.tick() }
        assertEquals(1, sim.tick())
        assertEquals(1, sim.bombs.size)
        assertFalse(sim.isBurning(88.0, 24.0))
    }

    @Test fun invalidRulesAreRejectedBeforeSimulation() {
        assertThrows(IllegalArgumentException::class.java) { NativeBombSimulation(stage(), bombLimit = 0) }
        assertThrows(IllegalArgumentException::class.java) { NativeBombSimulation(stage(), bombFuseSeconds = Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { NativeBombSimulation(stage(), bombRange = 99) }
        assertThrows(IllegalArgumentException::class.java) { NativeBombSimulation(stage(), blastLifetimeSeconds = 0.0) }
    }
}
