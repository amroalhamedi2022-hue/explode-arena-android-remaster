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
                NativeFrameCommands.resolve(none, NativeFrameCommands.Keys(), false, false, playing, advancesClock = true))
        }
        assertEquals(NativeFrameCommands.Decision(advanceWorld = true),
            NativeFrameCommands.resolve(NativeTouchControls.Frame(placeBomb = true, detonateRemote = true),
                NativeFrameCommands.Keys(), false, false, false, advancesClock = true))
    }

    @Test fun lostAndDrawnRoundsFreezeClockUntilExplicitRetry() {
        val terminalStates = listOf(
            NativeRoundFlow(victoryHoldTicks = 1).also { it.tick(true, false, true, true) },
            NativeRoundFlow(victoryHoldTicks = 1).also { it.tick(true, true, true, true) },
        )
        for (round in terminalStates) {
            val initialState = round.state
            assertTrue(initialState == NativeRoundFlow.State.LOST || initialState == NativeRoundFlow.State.DRAW)
            assertTrue(round.canRetry)
            assertFalse(round.isPlaying)
            val idle = NativeFrameCommands.resolve(
                none, NativeFrameCommands.Keys(), false, round.canRetry, round.isPlaying,
            )
            assertEquals(NativeFrameCommands.Decision(), idle)
            val remoteOnly = NativeFrameCommands.resolve(
                NativeTouchControls.Frame(detonateRemote = true),
                NativeFrameCommands.Keys(), false, round.canRetry, round.isPlaying,
            )
            assertEquals(NativeFrameCommands.Decision(), remoteOnly)
            assertEquals(NativeFrameCommands.Decision(retry = true),
                NativeFrameCommands.resolve(
                    NativeTouchControls.Frame(placeBomb = true),
                    NativeFrameCommands.Keys(), false, round.canRetry, round.isPlaying,
                ))

            val clock = FixedStepClock()
            val gate = NativePauseGate()
            repeat(40) {
                assertEquals(0, gate.advance(clock, if (idle.advanceWorld) 0.25 else 0.0) {})
            }
            assertEquals(0L, clock.ticks)
            assertEquals(initialState, round.state)
        }
    }

    @Test fun lostAndDrawnRoundsIgnoreNavigationAndRetryWinsCombinedFrame() {
        val terminalStates = listOf(
            NativeRoundFlow(victoryHoldTicks = 1).also { it.tick(true, false, true, true) },
            NativeRoundFlow(victoryHoldTicks = 1).also { it.tick(true, true, true, true) },
        )
        for (round in terminalStates) {
            assertTrue(round.canRetry)
            val navigationOnly = NativeFrameCommands.resolve(
                NativeTouchControls.Frame(nextStage = true, previousStage = true, detonateRemote = true),
                NativeFrameCommands.Keys(nextStage = true, previousStage = true),
                paused = false, canRetry = round.canRetry, isPlaying = round.isPlaying,
            )
            assertEquals(NativeFrameCommands.Decision(), navigationOnly)

            val retryPlusNavigation = NativeFrameCommands.resolve(
                NativeTouchControls.Frame(nextStage = true, placeBomb = true, detonateRemote = true),
                NativeFrameCommands.Keys(previousStage = true),
                paused = false, canRetry = round.canRetry, isPlaying = round.isPlaying,
            )
            assertEquals(NativeFrameCommands.Decision(retry = true), retryPlusNavigation)

            val keyboardRetryPlusNavigation = NativeFrameCommands.resolve(
                none,
                NativeFrameCommands.Keys(placeBomb = true, nextStage = true),
                paused = false, canRetry = round.canRetry, isPlaying = round.isPlaying,
            )
            assertEquals(NativeFrameCommands.Decision(retry = true), keyboardRetryPlusNavigation)
        }
    }

    @Test fun terminalNavigationCannotAdvanceClockOrAgeBombFuse() {
        val stage = StageData(0, "terminal.png", 128, 128, 0, 0,
            List(16) { IntArray(16) }, listOf(Spawn(40.0, 40.0)))
        val terminalStates = listOf(
            NativeRoundFlow(victoryHoldTicks = 1).also { it.tick(true, false, true, true) },
            NativeRoundFlow(victoryHoldTicks = 1).also { it.tick(true, true, true, true) },
        )
        for (round in terminalStates) {
            val initialState = round.state
            val simulation = NativeBombSimulation(stage)
            assertTrue(simulation.place(Actor(40.0, 40.0)))
            val initialFuse = simulation.bombs.single().remaining!!
            val clock = FixedStepClock()
            val gate = NativePauseGate()
            repeat(40) {
                val command = NativeFrameCommands.resolve(
                    NativeTouchControls.Frame(nextStage = true, detonateRemote = true),
                    NativeFrameCommands.Keys(previousStage = true),
                    paused = false, canRetry = round.canRetry, isPlaying = round.isPlaying,
                )
                assertEquals(NativeFrameCommands.Decision(), command)
                assertEquals(0, gate.advance(clock, if (command.advanceWorld) 0.25 else 0.0) {
                    simulation.tick()
                })
            }
            assertEquals(0L, clock.ticks)
            assertEquals(initialFuse, simulation.bombs.single().remaining!!, 0.0)
            assertEquals(initialState, round.state)
        }
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

    @Test fun completedFinalArenaFreezesClockWhileVictoryHoldStillAdvances() {
        val flow = NativeRoundFlow(victoryHoldTicks = 1)
        assertFalse(flow.tick(false, true, true, hasNextStage = false))
        assertEquals(NativeRoundFlow.State.WON, flow.state)

        val clock = FixedStepClock()
        val gate = NativePauseGate()
        val victory = NativeFrameCommands.resolve(
            none, NativeFrameCommands.Keys(), paused = false,
            canRetry = flow.canRetry, isPlaying = flow.isPlaying,
            advancesClock = flow.advancesPresentationClock,
        )
        assertTrue("the victory hold must keep advancing", victory.advanceWorld)
        assertEquals(1, gate.advance(clock, FixedStepClock.STEP_SECONDS) {})

        assertFalse(flow.tick(false, true, true, hasNextStage = false))
        assertEquals(NativeRoundFlow.State.COMPLETE, flow.state)
        assertFalse(flow.advancesPresentationClock)
        val terminal = NativeFrameCommands.resolve(
            none, NativeFrameCommands.Keys(), paused = false,
            canRetry = flow.canRetry, isPlaying = flow.isPlaying,
            advancesClock = flow.advancesPresentationClock,
        )
        assertEquals(NativeFrameCommands.Decision(), terminal)
        repeat(40) {
            assertEquals(0, gate.advance(
                clock, if (terminal.advanceWorld) 0.25 else 0.0,
            ) {})
        }
        assertEquals("COMPLETE must not keep the fixed-step clock alive", 1L, clock.ticks)
    }

}
