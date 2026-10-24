package com.explodearena.remaster.core

import kotlin.math.max

/**
 * Kotlin port of the current Mainline's PROVISIONAL 3-HP/1.7s damage rule.
 * It does not establish the original Symbian game's health or invulnerability.
 * Called once per fixed 60 Hz simulation tick, AFTER bomb/flame updates.
 */
class NativeVitality(
    initialHealth: Int = 3,
    initialInvulnerabilitySeconds: Double = 1.0,
    private val hitInvulnerabilitySeconds: Double = 1.7,
) {
    var health: Int = initialHealth
        private set
    var invulnerableSeconds: Double = initialInvulnerabilitySeconds
        private set
    val defeated: Boolean get() = health == 0

    init {
        require(initialHealth in 1..99)
        require(initialInvulnerabilitySeconds.isFinite() && initialInvulnerabilitySeconds in 0.0..10.0)
        require(hitInvulnerabilitySeconds.isFinite() && hitInvulnerabilitySeconds in 0.0..10.0)
    }

    /** Returns true exactly once for each registered damage event. */
    fun tick(burning: Boolean): Boolean {
        if (defeated) return false
        invulnerableSeconds = max(0.0, invulnerableSeconds - FixedStepClock.STEP_SECONDS)
        if (!burning || invulnerableSeconds > 0.0) return false
        health--
        invulnerableSeconds = hitInvulnerabilitySeconds
        return true
    }
}
