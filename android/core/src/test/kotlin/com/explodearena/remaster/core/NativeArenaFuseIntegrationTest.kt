package com.explodearena.remaster.core

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/**
 * N29: end-to-end JVM integration across every real arena for a complete
 * provisional bomb fuse and post-detonation simulation window. This does not
 * prove Symbian/EKA2L1 timing or Android rendering/audio behavior.
 */
class NativeArenaFuseIntegrationTest {
    private data class BotState(
        val owner: String,
        val x: Double,
        val y: Double,
        val health: Int,
        val defeated: Boolean,
        val passBomb: Long?,
    )

    private data class FinalState(
        val playerX: Double,
        val playerY: Double,
        val playerHealth: Int,
        val playerDefeated: Boolean,
        val bots: List<BotState>,
        val bombs: List<NativeBombSimulation.Bomb>,
        val flames: List<NativeBombSimulation.Flame>,
        val nextBombId: Long,
        val detonations: Int,
    )

    private fun exercise(stage: StageData): FinalState {
        val resolved = NativeSpawnLayout.resolve(stage)
        val player = Actor(resolved.first().x, resolved.first().y)
        val playerVitality = NativeVitality()
        val roster = NativeOpponentRoster.create(stage, player)
        val simulation = NativeBombSimulation(stage)
        assertTrue("arena ${stage.id}: initial player bomb must be placeable",
            simulation.place(player, "player"))

        var detonations = 0
        // 96 ticks reaches the provisional 1.6s fuse; 130 ticks also spans
        // the 0.45s flame lifetime so post-blast cleanup is exercised.
        repeat(130) { frame ->
            detonations += simulation.tick()
            playerVitality.tick(
                simulation.isBurning(player.x, player.y, StageCollision.ACTOR_RADIUS),
            )
            roster.advance(stage, player, simulation, frame.toLong())

            assertTrue("arena ${stage.id}: player position must remain finite",
                player.x.isFinite() && player.y.isFinite())
            assertFalse("arena ${stage.id}: player entered blocked terrain",
                StageCollision.blocked(stage, player.x, player.y,
                    StageCollision.ACTOR_RADIUS, simulation.bombs, player.passBomb))
            assertFalse("arena ${stage.id}: bot overlapped player",
                StageCollision.blockedByActors(player.x, player.y, roster.blockingActors()))

            val living = roster.bots.filterNot { it.vitality.defeated }
            for (slot in living) {
                assertTrue("arena ${stage.id}/${slot.owner}: position must remain finite",
                    slot.actor.x.isFinite() && slot.actor.y.isFinite())
                assertFalse("arena ${stage.id}/${slot.owner}: bot entered blocked terrain",
                    StageCollision.blocked(stage, slot.actor.x, slot.actor.y,
                        StageCollision.ACTOR_RADIUS, simulation.bombs, slot.actor.passBomb))
                assertFalse("arena ${stage.id}/${slot.owner}: bot overlaps another living actor",
                    StageCollision.blockedByActors(
                        slot.actor.x, slot.actor.y,
                        listOf(player) + living.filter { it !== slot }.map { it.actor },
                    ))
            }
            for (owner in listOf("player", "player2", "bot", "bot2", "bot3")) {
                assertTrue("arena ${stage.id}: bomb quota exceeded for $owner",
                    simulation.bombs.count { it.owner == owner } <= 1)
            }
        }

        assertTrue("arena ${stage.id}: complete fuse window must detonate a bomb", detonations > 0)
        assertFalse("arena ${stage.id}: initial bomb should not remain after 130 ticks",
            simulation.bombs.any { it.id == 1L })
        return FinalState(
            player.x, player.y, playerVitality.health, playerVitality.defeated,
            roster.bots.sortedBy { it.owner }.map {
                BotState(it.owner, it.actor.x, it.actor.y, it.vitality.health,
                    it.vitality.defeated, it.actor.passBomb)
            },
            simulation.bombs.sortedBy { it.id },
            simulation.flames.map { it.copy(cells = it.cells.toList()) },
            simulation.nextBombId,
            detonations,
        )
    }

    @Test fun all41RealArenasCompleteFuseAndPostBlastIntegrationWindow() {
        val file = File("../../web/assets/stages.js")
        assertTrue("Missing real stage catalog: ${file.absolutePath}", file.isFile)
        val stages = StageCatalog.parse(file.readText(Charsets.UTF_8))
        assertEquals((0..40).toList(), stages.map { it.id })
        assertEquals(mapOf(1 to 18, 2 to 2, 4 to 21),
            stages.groupingBy { it.spawns.size }.eachCount())

        for (stage in stages) {
            val result = exercise(stage)
            assertEquals("arena ${stage.id}: roster must preserve all native opponents",
                stage.spawns.size - 1, result.bots.size)
        }
    }

    @Test fun fullFuseStateIsReproducibleForSingleTwoAndFourSpawnArenas() {
        val file = File("../../web/assets/stages.js")
        assertTrue(file.isFile)
        val stages = StageCatalog.parse(file.readText(Charsets.UTF_8)).associateBy { it.id }
        for (id in listOf(0, 26, 40)) {
            assertEquals("arena $id: repeated 130-tick simulation diverged",
                exercise(stages.getValue(id)), exercise(stages.getValue(id)))
        }
    }
}
