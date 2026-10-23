package com.explodearena.remaster.core

import kotlin.math.hypot
import kotlin.math.min

/**
 * Native Android multi-touch controls in screen pixels (origin at TOP-left).
 * Pure Kotlin so pointer transitions and screen-size math can be regression-tested.
 * This is a provisional ergonomic control layout, NOT original Symbian keypad parity.
 */
class NativeTouchControls {
    data class Contact(val pointer: Int, val x: Float, val y: Float)
    data class Frame(
        val dx: Float = 0f,
        val dy: Float = 0f,
        val placeBomb: Boolean = false,
        val previousStage: Boolean = false,
        val nextStage: Boolean = false,
    )
    data class Layout(
        val padX: Float, val padY: Float,
        val bombX: Float, val bombY: Float,
        val previousX: Float, val nextX: Float, val stageY: Float,
        val radius: Float,
    )

    private var downPointers = emptySet<Int>()

    fun reset() { downPointers = emptySet() }

    fun layout(width: Int, height: Int): Layout {
        require(width > 0 && height > 0)
        val radius = (min(width, height) * 0.085f).coerceIn(24f, 64f)
        return Layout(
            padX = width * 0.20f, padY = height * 0.79f,
            bombX = width * 0.82f, bombY = height * 0.79f,
            previousX = width * 0.12f, nextX = width * 0.88f,
            stageY = height * 0.11f, radius = radius,
        )
    }

    fun update(contacts: List<Contact>, width: Int, height: Int): Frame {
        val l = layout(width, height)
        require(contacts.all { it.x.isFinite() && it.y.isFinite() && it.pointer >= 0 })
        require(contacts.map { it.pointer }.distinct().size == contacts.size)
        val newContacts = contacts.filter { it.pointer !in downPointers }
        downPointers = contacts.map { it.pointer }.toSet()

        fun distance(x: Float, y: Float, contact: Contact): Float =
            hypot(contact.x - x, contact.y - y)
        fun justPressed(x: Float, y: Float): Boolean =
            newContacts.any { distance(x, y, it) <= l.radius * 1.25f }

        val movementTouch = contacts
            .filter { distance(l.padX, l.padY, it) <= l.radius * 1.8f }
            .minByOrNull { distance(l.padX, l.padY, it) }
        var dx = 0f
        var dy = 0f
        if (movementTouch != null) {
            val offsetX = movementTouch.x - l.padX
            val offsetY = movementTouch.y - l.padY
            val magnitude = hypot(offsetX, offsetY)
            if (magnitude >= l.radius * 0.25f) {
                val scale = min(1f, l.radius / magnitude) / l.radius
                dx = offsetX * scale
                dy = offsetY * scale
            }
        }
        return Frame(
            dx = dx,
            dy = dy,
            placeBomb = justPressed(l.bombX, l.bombY),
            previousStage = justPressed(l.previousX, l.stageY),
            nextStage = justPressed(l.nextX, l.stageY),
        )
    }
}
