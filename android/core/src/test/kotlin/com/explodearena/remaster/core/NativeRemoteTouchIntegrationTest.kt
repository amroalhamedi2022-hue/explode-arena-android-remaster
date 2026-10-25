package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/**
 * Pure-Kotlin interaction contract for the on-screen keypad-8 command.
 * This tests control -> bomb engine behavior, NOT actual Android touch delivery
 * or the as-yet-unidentified original Symbian remote pickup.
 */
class NativeRemoteTouchIntegrationTest {
    private fun stage() = StageData(
        id = 0, image = "assets/maps/map_000.png", width = 192, height = 96,
        obstacleX = 0, obstacleY = 0,
        grid = List(12) { IntArray(24) },
        spawns = listOf(Spawn(24.0, 24.0)),
    )

    @Test fun touchKeyEightOnlyDetonatesExplicitlyEnabledRemoteBombs() {
        val sim = NativeBombSimulation(stage(), bombLimit = 3)
        val controls = NativeTouchControls()
        val l = controls.layout(1200, 800)
        val remote = NativeTouchControls.Contact(0, l.remoteX, l.remoteY)
        assertTrue(sim.place(Actor(24.0, 24.0)))
        assertFalse(sim.detonateRemote()) // No pickup; timed bomb remains timed.
        assertEquals(1, sim.bombs.size)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(88.0, 24.0)))
        assertEquals(1, sim.bombs.count { it.remote })
        val first = controls.update(listOf(remote), 1200, 800)
        assertTrue(first.detonateRemote)
        assertTrue(sim.detonateRemote())
        assertEquals(1, sim.bombs.size)
        assertFalse(sim.bombs.single().remote)
        assertFalse(controls.update(listOf(remote), 1200, 800).detonateRemote)
        assertFalse(sim.detonateRemote())
    }

    @Test fun backgroundQuarantineDoesNotDetonateBombsOnResume() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(24.0, 24.0)))
        val controls = NativeTouchControls()
        val l = controls.layout(1200, 800)
        val remote = NativeTouchControls.Contact(0, l.remoteX, l.remoteY)
        val clock = FixedStepClock()
        val gate = NativePauseGate()
        gate.onBackground(clock)
        controls.suppressUntilRelease()
        gate.onForeground(clock)
        assertTrue(gate.isPaused)
        val stale = controls.update(listOf(remote), 1200, 800)
        assertFalse(stale.detonateRemote)
        assertEquals(1, sim.bombs.size)
        assertEquals(NativeTouchControls.Frame(), controls.update(emptyList(), 1200, 800))
        // Only an explicit resume action followed by a fresh tap may detonate.
        assertFalse(gate.toggle(clock))
        assertTrue(controls.update(listOf(remote), 1200, 800).detonateRemote)
        assertTrue(sim.detonateRemote())
        assertTrue(sim.bombs.isEmpty())
    }
}
