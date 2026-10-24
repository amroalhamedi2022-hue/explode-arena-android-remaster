package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativePauseGateTest {
    @Test fun pauseFreezesBombFuseAndResumesWithoutCatchUp() {
        val stage = StageData(0, "test.png", 64, 64, 0, 0,
            List(8) { IntArray(8) }, listOf(Spawn(24.0, 24.0)))
        val bombs = NativeBombSimulation(stage, bombFuseSeconds = 0.1)
        assertTrue(bombs.place(Actor(24.0, 24.0)))
        val clock = FixedStepClock()
        val gate = NativePauseGate()
        var detonations = 0
        val tick = { detonations += bombs.tick() }
        repeat(3) { assertEquals(1, gate.advance(clock, FixedStepClock.STEP_SECONDS, tick)) }
        assertEquals(0, detonations)
        assertTrue(gate.toggle(clock))
        repeat(200) { assertEquals(0, gate.advance(clock, 0.25, tick)) }
        assertEquals(3L, clock.ticks)
        assertEquals(1, bombs.bombs.size)
        assertEquals(0, detonations)
        assertFalse(gate.toggle(clock))
        repeat(3) { gate.advance(clock, FixedStepClock.STEP_SECONDS, tick) }
        assertEquals(6L, clock.ticks)
        assertEquals(1, detonations)
        assertTrue(bombs.bombs.isEmpty())
    }

    @Test fun pauseFreezesVictoryHoldAndDiscardsPartialFrameOnBothEdges() {
        val gate = NativePauseGate()
        val clock = FixedStepClock()
        val round = NativeRoundFlow(victoryHoldTicks = 2)
        assertEquals(0, gate.advance(clock, FixedStepClock.STEP_SECONDS / 2) {
            round.tick(false, true, true, true)
        })
        assertTrue(gate.toggle(clock))
        repeat(20) {
            gate.advance(clock, 0.25) { round.tick(false, true, true, true) }
        }
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)
        assertFalse(gate.toggle(clock))
        assertEquals(0, gate.advance(clock, FixedStepClock.STEP_SECONDS / 2) {
            round.tick(false, true, true, true)
        })
        assertEquals(1, gate.advance(clock, FixedStepClock.STEP_SECONDS / 2) {
            round.tick(false, true, true, true)
        })
        assertEquals(NativeRoundFlow.State.WON, round.state)
        assertEquals(2, gate.advance(clock, FixedStepClock.STEP_SECONDS * 2) {
            round.tick(false, true, true, true)
        })
        assertEquals(NativeRoundFlow.State.ADVANCING, round.state)
    }

    @Test fun touchPauseIsDebouncedAndWorksBesideStageNavigation() {
        val controls = NativeTouchControls()
        val layout = controls.layout(1200, 800)
        val pause = NativeTouchControls.Contact(0, layout.pauseX, layout.stageY)
        val previous = NativeTouchControls.Contact(1, layout.previousX, layout.stageY)
        val first = controls.update(listOf(pause, previous), 1200, 800)
        assertTrue(first.pauseToggle)
        assertTrue(first.previousStage)
        assertFalse(controls.update(listOf(pause, previous), 1200, 800).pauseToggle)
        controls.update(emptyList(), 1200, 800)
        assertTrue(controls.update(listOf(pause), 1200, 800).pauseToggle)
    }
}
