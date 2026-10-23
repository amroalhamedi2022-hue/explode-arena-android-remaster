package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeVitalityTest {
    @Test fun initialInvulnerabilityThenFirstHit() {
        val health = NativeVitality()
        repeat(59) { assertFalse(health.tick(burning = true)) }
        var hit = false
        repeat(2) { hit = health.tick(burning = true) || hit }
        assertTrue(hit)
        assertEquals(2, health.health)
    }

    @Test fun repeatedFlameCannotDrainAllHealthInOneTick() {
        val health = NativeVitality(initialInvulnerabilitySeconds = 0.0)
        assertTrue(health.tick(true))
        repeat(60) { assertFalse(health.tick(true)) }
        assertEquals(2, health.health)
        repeat(50) { health.tick(false) }
        assertTrue(health.tick(true))
        assertEquals(1, health.health)
    }

    @Test fun threeSeparatedHitsCauseDefeatWithoutNegativeHealth() {
        val health = NativeVitality(initialInvulnerabilitySeconds = 0.0, hitInvulnerabilitySeconds = 0.0)
        repeat(3) { assertTrue(health.tick(true)) }
        assertTrue(health.defeated)
        repeat(100) { assertFalse(health.tick(true)) }
        assertEquals(0, health.health)
    }

    @Test fun rejectsInvalidDamageRules() {
        assertThrows(IllegalArgumentException::class.java) { NativeVitality(initialHealth = 0) }
        assertThrows(IllegalArgumentException::class.java) { NativeVitality(hitInvulnerabilitySeconds = Double.NaN) }
    }
}
