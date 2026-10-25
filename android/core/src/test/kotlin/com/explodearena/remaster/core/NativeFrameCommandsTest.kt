package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/** Source-level atomic input tests; real Android delivery is NOT TESTED. */
class NativeFrameCommandsTest {
    private val none = NativeTouchControls.Frame()

    @Test fun pauseConsumesSimultaneousNavigationBombRemoteAndSimulation() {
        val command = NativeFrameCommands.resolve(
            NativeTouchControls.Frame(pauseToggle = true, nextStage = true, placeBomb = true, detonateRemote = true),
            NativeFrameCommands.Keys(nextStage = true, placeBomb = true),
            paused = false, canRetry = false, isPlaying = true,
        )
        assertEquals(NativeFrameCommands.Decision(togglePause = true), command)
    }

    @Test fun pausedFrameIgnoresAllGameplayKeysButAcceptsExplicitUnpause() {
        val touch = NativeTouchControls.Frame(nextStage = true, placeBomb = true, detonateRemote = true)
        assertEquals(NativeFrameCommands.Decision(), NativeFrameCommands.resolve(
            touch, NativeFrameCommands.Keys(placeBomb = true), true, false, true,
        ))
        assertEquals(NativeFrameCommands.Decision(togglePause = true), NativeFrameCommands.resolve(
            touch, NativeFrameCommands.Keys(pause = true), true, false, true,
        ))
    }

    @Test fun nextStageWinsOverPreviousAndBombsAndDoesNotTickEitherArena() {
        val command = NativeFrameCommands.resolve(
            NativeTouchControls.Frame(nextStage = true, previousStage = true, placeBomb = true, detonateRemote = true),
            NativeFrameCommands.Keys(), false, false, true,
        )
        assertEquals(NativeFrameCommands.Decision(stageDelta = 1), command)
    }

    @Test fun previousStageViaKeyboardConsumesTheWholeFrame() {
        val command = NativeFrameCommands.resolve(
            none, NativeFrameCommands.Keys(previousStage = true, detonateRemote = true),
            false, false, true,
        )
        assertEquals(NativeFrameCommands.Decision(stageDelta = -1), command)
    }

    @Test fun retryDoesNotAlsoPlantBombOrAdvanceDefeatedRound() {
        val command = NativeFrameCommands.resolve(
            NativeTouchControls.Frame(placeBomb = true, detonateRemote = true),
            NativeFrameCommands.Keys(), false, true, false,
        )
        assertEquals(NativeFrameCommands.Decision(retry = true), command)
    }

    @Test fun normalGameplayAllowsIndependentSimultaneousBombAndRemoteInputs() {
        val command = NativeFrameCommands.resolve(
            NativeTouchControls.Frame(placeBomb = true, detonateRemote = true),
            NativeFrameCommands.Keys(), false, false, true,
        )
        assertEquals(NativeFrameCommands.Decision(
            placeBomb = true, detonateRemote = true, advanceWorld = true,
        ), command)
    }

    @Test fun idleAndVictoryHoldFramesAdvanceWithoutInventingBombActions() {
        for (playing in listOf(true, false)) {
            assertEquals(NativeFrameCommands.Decision(advanceWorld = true),
                NativeFrameCommands.resolve(none, NativeFrameCommands.Keys(), false, false, playing))
        }
        assertEquals(NativeFrameCommands.Decision(advanceWorld = true),
            NativeFrameCommands.resolve(NativeTouchControls.Frame(placeBomb = true, detonateRemote = true),
                NativeFrameCommands.Keys(), false, false, false))
    }

    @Test fun heldTouchAfterArenaSwapRequiresFullReleaseBeforeNewBombOrNavigation() {
        val controls = NativeTouchControls()
        val l = controls.layout(1200, 800)
        val next = NativeTouchControls.Contact(0, l.nextX, l.stageY)
        val bomb = NativeTouchControls.Contact(1, l.bombX, l.bombY)
        val first = controls.update(listOf(next, bomb), 1200, 800)
        assertEquals(1, NativeFrameCommands.resolve(
            first, NativeFrameCommands.Keys(), false, false, true).stageDelta)
        controls.suppressUntilRelease() // selectStage/retryStage calls this.
        repeat(2) {
            val held = controls.update(listOf(next, bomb), 1200, 800)
            assertEquals(NativeTouchControls.Frame(), held)
        }
        assertEquals(NativeTouchControls.Frame(), controls.update(emptyList(), 1200, 800))
        val fresh = controls.update(listOf(bomb), 1200, 800)
        assertTrue(NativeFrameCommands.resolve(
            fresh, NativeFrameCommands.Keys(), false, false, true).placeBomb)
    }
    @Test fun transitionFramesCannotAgeBombFuseEvenAfterLongRenderDelay() {
        val stage = StageData(0, "test.png", 128, 128, 0, 0,
            List(16) { IntArray(16) }, listOf(Spawn(40.0, 40.0)))
        val bombs = NativeBombSimulation(stage)
        assertTrue(bombs.place(Actor(40.0, 40.0)))
        val initialFuse = bombs.bombs.single().remaining!!
        val clock = FixedStepClock()
        val gate = NativePauseGate()
        val stageChange = NativeFrameCommands.resolve(
            NativeTouchControls.Frame(nextStage = true, placeBomb = true),
            NativeFrameCommands.Keys(), false, false, true,
        )
        val retry = NativeFrameCommands.resolve(
            NativeTouchControls.Frame(placeBomb = true),
            NativeFrameCommands.Keys(), false, true, false,
        )
        repeat(20) { index ->
            val decision = if (index % 2 == 0) stageChange else retry
            assertFalse(decision.advanceWorld)
            assertEquals(0, gate.advance(clock,
                if (decision.advanceWorld) 0.25 else 0.0) { bombs.tick() })
        }
        assertEquals(0L, clock.ticks)
        assertEquals(initialFuse, bombs.bombs.single().remaining!!, 0.0)
        val play = NativeFrameCommands.resolve(
            none, NativeFrameCommands.Keys(), false, false, true,
        )
        assertTrue(play.advanceWorld)
        assertEquals(1, gate.advance(clock, FixedStepClock.STEP_SECONDS) { bombs.tick() })
        assertTrue(bombs.bombs.single().remaining!! < initialFuse)
    }

}
