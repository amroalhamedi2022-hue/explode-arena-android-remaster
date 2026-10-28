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

    @Test fun backgroundHeldFingerCannotResumeOrPlantBombUntilFullyReleased() {
        val controls = NativeTouchControls()
        val l = controls.layout(1200, 800)
        val pause = NativeTouchControls.Contact(0, l.pauseX, l.stageY)
        val bomb = NativeTouchControls.Contact(1, l.bombX, l.bombY)
        controls.update(listOf(pause), 1200, 800)
        controls.suppressUntilRelease()
        repeat(3) {
            val stale = controls.update(listOf(pause, bomb), 1200, 800)
            assertEquals(NativeTouchControls.Frame(), stale)
        }
        // Lifting only one finger is insufficient: both must be up.
        assertEquals(NativeTouchControls.Frame(), controls.update(listOf(bomb), 1200, 800))
        assertEquals(NativeTouchControls.Frame(), controls.update(emptyList(), 1200, 800))
        val fresh = controls.update(listOf(pause), 1200, 800)
        assertTrue(fresh.pauseToggle)
        assertFalse(fresh.placeBomb)
        assertFalse(controls.update(listOf(pause), 1200, 800).pauseToggle)
    }

    @Test fun remoteKeyEightTouchIsIndependentDebouncedAndAllowsMovement() {
        val controls = NativeTouchControls()
        val l = controls.layout(1200, 800)
        val moving = NativeTouchControls.Contact(0, l.padX + l.radius, l.padY)
        val remote = NativeTouchControls.Contact(1, l.remoteX, l.remoteY)
        val bomb = NativeTouchControls.Contact(2, l.bombX, l.bombY)
        val first = controls.update(listOf(moving, remote), 1200, 800)
        assertTrue(first.detonateRemote)
        assertFalse(first.placeBomb)
        assertTrue(first.dx > 0f)
        assertFalse(controls.update(listOf(moving, remote), 1200, 800).detonateRemote)
        val second = controls.update(listOf(moving, remote, bomb), 1200, 800)
        assertTrue(second.placeBomb)
        assertFalse(second.detonateRemote)
        controls.update(listOf(moving, bomb), 1200, 800)
        assertTrue(controls.update(listOf(moving, remote, bomb), 1200, 800).detonateRemote)
    }

    @Test fun remoteAndBombHitAreasDoNotOverlapAtPhoneAndTabletSizes() {
        val controls = NativeTouchControls()
        for ((width, height) in listOf(320 to 800, 800 to 1200, 1200 to 800)) {
            val l = controls.layout(width, height)
            val separation = kotlin.math.hypot(
                (l.bombX - l.remoteX).toDouble(), (l.bombY - l.remoteY).toDouble(),
            )
            assertTrue("overlap on ${width}x${height}", separation > l.radius * 2.5f)
        }
    }
}
