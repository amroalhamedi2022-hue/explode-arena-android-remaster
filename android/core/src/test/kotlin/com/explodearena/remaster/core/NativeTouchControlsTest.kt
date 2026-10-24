package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeTouchControlsTest {
    @Test fun padIsScreenRelativeWithDeadZoneAndNormalizedDiagonal() {
        val controls = NativeTouchControls()
        val l = controls.layout(1200, 800)
        val neutral = controls.update(listOf(NativeTouchControls.Contact(0, l.padX, l.padY)), 1200, 800)
        assertEquals(0f, neutral.dx, 0f)
        assertEquals(0f, neutral.dy, 0f)
        val diagonal = controls.update(
            listOf(NativeTouchControls.Contact(0, l.padX + l.radius, l.padY + l.radius)), 1200, 800,
        )
        assertEquals(1.0, kotlin.math.hypot(diagonal.dx.toDouble(), diagonal.dy.toDouble()), 1e-5)
        assertTrue(diagonal.dx > 0f && diagonal.dy > 0f)
    }

    @Test fun bombTriggersOnlyOnNewPointerAndSupportsSimultaneousMovement() {
        val controls = NativeTouchControls()
        val l = controls.layout(1200, 800)
        val moving = NativeTouchControls.Contact(0, l.padX + l.radius, l.padY)
        val bomb = NativeTouchControls.Contact(1, l.bombX, l.bombY)
        assertFalse(controls.update(listOf(moving), 1200, 800).placeBomb)
        val first = controls.update(listOf(moving, bomb), 1200, 800)
        assertTrue(first.placeBomb)
        assertTrue(first.dx > 0f)
        assertFalse(controls.update(listOf(moving, bomb), 1200, 800).placeBomb)
        controls.update(listOf(moving), 1200, 800)
        assertTrue(controls.update(listOf(moving, bomb), 1200, 800).placeBomb)
    }

    @Test fun stageButtonsAreDebouncedAndResetWorks() {
        val controls = NativeTouchControls()
        val l = controls.layout(900, 600)
        val previous = NativeTouchControls.Contact(0, l.previousX, l.stageY)
        assertTrue(controls.update(listOf(previous), 900, 600).previousStage)
        assertFalse(controls.update(listOf(previous), 900, 600).previousStage)
        controls.reset()
        assertTrue(controls.update(listOf(previous), 900, 600).previousStage)
        controls.update(emptyList(), 900, 600)
        assertTrue(controls.update(listOf(NativeTouchControls.Contact(0, l.nextX, l.stageY)), 900, 600).nextStage)
    }

    @Test fun invalidGeometryAndDuplicatePointersFailFast() {
        val controls = NativeTouchControls()
        assertThrows(IllegalArgumentException::class.java) { controls.layout(0, 100) }
        assertThrows(IllegalArgumentException::class.java) {
            controls.update(
                listOf(NativeTouchControls.Contact(0, 1f, 2f), NativeTouchControls.Contact(0, 3f, 4f)),
                100, 100,
            )
        }
    }
}
