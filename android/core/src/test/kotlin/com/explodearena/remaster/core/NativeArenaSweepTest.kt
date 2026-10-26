package com.explodearena.remaster.core

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/**
 * N16/N24: deterministic native source-level integration sweep of all 41 real
 * Mainline arenas. This is NOT an EKA2L1 parity, graphics, sound, Android
 * device, or full-match playthrough test.
 */
class NativeArenaSweepTest {
    private data class Snapshot(
        val bots: List<Spawn>,
        val health: List<Int>,
        val bombs: List<NativeBombSimulation.Bomb>,
        val living: Int,
        val nextBombId: Long,
    )

    private fun roster(stage: StageData, player: Actor, reverseOrder: Boolean): NativeOpponentRoster {
        val initial = NativeOpponentRoster.create(stage, player)
        if (!reverseOrder) return initial
        val reversed = initial.bots.reversed().map { slot ->
            NativeOpponentRoster.BotSlot(
                slot.owner, Actor(slot.actor.x, slot.actor.y, slot.actor.passBomb),
            )
        }
        return NativeOpponentRoster(reversed)
    }

    private fun simulate(stage: StageData, reverseOrder: Boolean = false): Pair<Snapshot, Boolean> {
        val safe = NativeSpawnLayout.resolve(stage)
        val player = Actor(safe.first().x, safe.first().y)
        val roster = roster(stage, player, reverseOrder)
        val simulation = NativeBombSimulation(stage)
        val playerBombPlaced = simulation.place(player, "player")

        // Exercise the real 60Hz bot/collision/bomb integration, including
        // the six-tick bot replan boundary, without waiting for full fuse expiry.
        repeat(8) { frame ->
            simulation.tick()
            val placed = roster.advance(stage, player, simulation, frame.toLong())
            assertTrue("arena ${stage.id}: invalid bot placement count", placed in 0..roster.bots.size)
            for (slot in roster.bots) {
                val actor = slot.actor
                assertTrue("arena ${stage.id}: nonfinite bot position",
                    actor.x.isFinite() && actor.y.isFinite())
                assertFalse("arena ${stage.id}: bot entered solid terrain",
                    StageCollision.blocked(stage, actor.x, actor.y, StageCollision.ACTOR_RADIUS,
                        simulation.bombs, actor.passBomb))
                val others = listOf(player) + roster.bots
                    .filter { it !== slot && !it.vitality.defeated }.map { it.actor }
                assertFalse("arena ${stage.id}: bot overlapped a living actor",
                    StageCollision.blockedByActors(actor.x, actor.y, others))
            }
        }
        return Snapshot(
            roster.bots.sortedBy { it.owner }.map { Spawn(it.actor.x, it.actor.y) },
            roster.bots.sortedBy { it.owner }.map { it.vitality.health },
            simulation.bombs.sortedBy { it.id },
            roster.aliveCount,
            simulation.nextBombId,
        ) to playerBombPlaced
    }

    @Test fun all41RealArenasRunDeterministicNativeBotAndBombSteps() {
        val file = File("../../web/assets/stages.js")
        assertTrue("Missing real stage catalog: ${file.absolutePath}", file.isFile)
        val stages = StageCatalog.parse(file.readText(Charsets.UTF_8))
        assertEquals((0..40).toList(), stages.map { it.id })
        assertEquals(mapOf(1 to 18, 2 to 2, 4 to 21),
            stages.groupingBy { it.spawns.size }.eachCount())
        var successfulPlayerBombs = 0
        for (stage in stages) {
            val (first, placed) = simulate(stage)
            val (second, placedAgain) = simulate(stage)
            assertEquals("arena ${stage.id}: placement not deterministic", placed, placedAgain)
            assertEquals("arena ${stage.id}: native 60Hz snapshot diverged", first, second)
            assertEquals(stage.spawns.size - 1, first.bots.size)
            if (stage.spawns.size > 1) {
                val (reversed, reversedPlaced) = simulate(stage, reverseOrder = true)
                assertEquals("arena ${stage.id}: roster order changed bomb placement", placed, reversedPlaced)
                assertEquals("arena ${stage.id}: roster order changed native state", first, reversed)
            }
            if (placed) successfulPlayerBombs++
        }
        assertTrue("At least one real arena must exercise a placed bomb",
            successfulPlayerBombs > 0)
    }
}
