package com.explodearena.remaster.core

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/**
 * N32: verifies the damage window at the end of a flame's lifetime against
 * all 41 checked-in stage records. This is native JVM evidence only, not
 * Symbian/EKA2L1 timing parity or an Android-device test.
 */
class NativeFlameDamageTimingTest {
    private fun originalStages(): List<StageData> {
        val file = File("../../web/assets/stages.js")
        assertTrue("Missing real stage catalog: ${file.absolutePath}", file.isFile)
        return StageCatalog.parse(file.readText(Charsets.UTF_8))
    }

    private fun safeBombCell(stage: StageData): Spawn {
        for (y in 8 until stage.height step 16) {
            for (x in 8 until stage.width step 16) {
                val px = x.toDouble()
                val py = y.toDouble()
                if (!StageCollision.blocked(stage, px, py, StageCollision.ACTOR_RADIUS)) {
                    return Spawn(px, py)
                }
            }
        }
        throw AssertionError("Arena ${stage.id} has no valid 16px bomb cell for a 5px actor")
    }

    @Test fun all41ArenasDamageActorsOnTheFinalFlameTick() {
        val stages = originalStages()
        assertEquals((0..40).toList(), stages.map { it.id })
        assertEquals(
            mapOf(1 to 18, 2 to 2, 4 to 21),
            stages.groupingBy { it.spawns.size }.eachCount(),
        )

        for (stage in stages) {
            val cell = safeBombCell(stage)
            val simulation = NativeBombSimulation(
                stage,
                bombFuseSeconds = 0.1,
                blastLifetimeSeconds = 0.05,
                bombRange = 1,
            )
            assertTrue("arena ${stage.id}: safe bomb cell should accept placement",
                simulation.place(Actor(cell.x, cell.y), "player"))

            // Six 60Hz ticks expire the shortened 0.1s fuse.
            repeat(5) { tick ->
                assertEquals("arena ${stage.id}: early detonation at tick ${tick + 1}",
                    0, simulation.tick())
            }
            assertEquals("arena ${stage.id}: expected detonation at tick 6", 1, simulation.tick())
            assertTrue("arena ${stage.id}: new flame must be damage-active on detonation tick",
                simulation.isBurningDuringLastTick(cell.x, cell.y, StageCollision.ACTOR_RADIUS))

            // A 0.05s flame remains active for two more ticks, then is removed
            // from the renderable flame list at the third tick boundary.
            repeat(2) { simulation.tick() }
            assertTrue("arena ${stage.id}: flame should still be active before expiry",
                simulation.isBurning(cell.x, cell.y, StageCollision.ACTOR_RADIUS))
            simulation.tick()
            assertTrue("arena ${stage.id}: expired flame must not remain renderable",
                simulation.flames.isEmpty())
            assertFalse("arena ${stage.id}: expired flame should not block current-state queries",
                simulation.isBurning(cell.x, cell.y, StageCollision.ACTOR_RADIUS))
            assertTrue("arena ${stage.id}: final tick must still count for damage",
                simulation.isBurningDuringLastTick(cell.x, cell.y, StageCollision.ACTOR_RADIUS))

            val vitality = NativeVitality(
                initialHealth = 1,
                initialInvulnerabilitySeconds = 0.0,
                hitInvulnerabilitySeconds = 0.0,
            )
            assertTrue("arena ${stage.id}: final-tick flame damage should be registered",
                vitality.tick(simulation.isBurningDuringLastTick(
                    cell.x, cell.y, StageCollision.ACTOR_RADIUS,
                )))
            assertTrue("arena ${stage.id}: final-tick damage should defeat one-HP actor",
                vitality.defeated)

            simulation.tick()
            assertFalse("arena ${stage.id}: expired-flame damage window must clear next tick",
                simulation.isBurningDuringLastTick(cell.x, cell.y, StageCollision.ACTOR_RADIUS))
        }
    }

    @Test fun damageWindowIncludesAFlameCreatedByTheCurrentDetonation() {
        val stage = StageData(
            id = 0, image = "test.png", width = 128, height = 64,
            obstacleX = 0, obstacleY = 0,
            grid = List(8) { IntArray(16) },
            spawns = listOf(Spawn(24.0, 24.0)),
        )
        val simulation = NativeBombSimulation(
            stage, bombFuseSeconds = 0.1, blastLifetimeSeconds = 0.05, bombRange = 1,
        )
        assertTrue(simulation.place(Actor(24.0, 24.0)))
        repeat(5) { simulation.tick() }
        assertEquals(1, simulation.tick())
        assertTrue(simulation.isBurning(24.0, 24.0))
        assertTrue(simulation.isBurningDuringLastTick(24.0, 24.0))
    }
}
