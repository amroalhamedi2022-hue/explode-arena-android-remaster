package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeOpponentRosterTest {
    private fun stage(count: Int): StageData = StageData(
        id = 14, image = "assets/maps/map_014.png", width = 160, height = 160,
        obstacleX = 0, obstacleY = 0, grid = List(20) { IntArray(20) },
        spawns = listOf(
            Spawn(24.0, 24.0), Spawn(120.0, 24.0),
            Spawn(24.0, 120.0), Spawn(120.0, 120.0),
        ).take(count),
    )

    @Test fun createsIndependentOneTwoAndFourSpawnRosters() {
        for ((spawns, bots) in listOf(1 to 0, 2 to 1, 4 to 3)) {
            val roster = NativeOpponentRoster.create(stage(spawns), Actor(24.0, 24.0))
            assertEquals(bots, roster.bots.size)
            assertEquals(bots > 0, roster.present)
            assertFalse(roster.allDefeated)
            assertEquals(bots, roster.aliveCount)
            assertEquals(listOf("bot", "bot2", "bot3").take(bots), roster.bots.map { it.owner })
            assertEquals(bots, roster.bots.map { it.actor.x to it.actor.y }.toSet().size)
        }
    }

    @Test fun eachOpponentHasIndependentBombLimit() {
        val arena = stage(4)
        val roster = NativeOpponentRoster.create(arena, Actor(24.0, 24.0))
        val sim = NativeBombSimulation(arena)
        roster.bots.forEach { assertTrue(sim.place(it.actor, it.owner)) }
        assertEquals(3, sim.bombs.size)
        assertEquals(setOf("bot", "bot2", "bot3"), sim.bombs.map { it.owner }.toSet())
        roster.bots.forEach { assertFalse(sim.place(it.actor, it.owner)) }
        assertTrue(sim.place(Actor(56.0, 56.0), "player"))
        assertEquals(4, sim.bombs.size)
    }

    @Test fun victoryWaitsForEveryOpponentToBeDefeated() {
        val arena = stage(4)
        val roster = NativeOpponentRoster.create(arena, Actor(24.0, 24.0))
        val flow = NativeRoundFlow(victoryHoldTicks = 2)
        for (index in 0..2) {
            repeat(350) { roster.bots[index].vitality.tick(true) }
            assertTrue(roster.bots[index].vitality.defeated)
            assertEquals(2 - index, roster.aliveCount)
            assertEquals(index == 2, roster.allDefeated)
            flow.tick(false, roster.allDefeated, roster.present, true)
            assertEquals(
                if (index == 2) NativeRoundFlow.State.WON else NativeRoundFlow.State.PLAYING,
                flow.state,
            )
        }
        assertFalse(flow.tick(false, roster.allDefeated, roster.present, true))
        assertTrue(flow.tick(false, roster.allDefeated, roster.present, true))
    }

    @Test fun defeatedOpponentNeverWalksOrPlacesNewBombs() {
        val arena = stage(2)
        val roster = NativeOpponentRoster.create(arena, Actor(24.0, 24.0))
        val bot = roster.bots.single()
        bot.walking = true
        repeat(350) { bot.vitality.tick(true) }
        assertTrue(bot.vitality.defeated)
        assertEquals(0, roster.advance(arena, Actor(24.0, 24.0), NativeBombSimulation(arena), 0))
        assertFalse(bot.walking)
    }

    @Test fun rejectsUnsupportedAndOverlappingSpawns() {
        assertThrows(IllegalArgumentException::class.java) {
            NativeOpponentRoster.create(stage(3), Actor(24.0, 24.0))
        }
        val overlap = stage(2).copy(spawns = listOf(Spawn(24.0, 24.0), Spawn(24.0, 24.0)))
        assertThrows(IllegalStateException::class.java) {
            NativeOpponentRoster.create(overlap, Actor(24.0, 24.0))
        }
    }

    private data class BotState(
        val owner: String,
        val x: Double,
        val y: Double,
        val health: Int,
        val defeated: Boolean,
        val walking: Boolean,
        val facing: Int,
        val passBomb: Long?,
    )

    private data class BombState(
        val id: Long,
        val owner: String,
        val x: Double,
        val y: Double,
        val remaining: Double?,
        val remote: Boolean,
    )

    private data class FourSpawnSnapshot(
        val bots: List<BotState>,
        val bombs: List<BombState>,
        val nextBombId: Long,
    )

    private fun runFourSpawnRoster(reverseOrder: Boolean): FourSpawnSnapshot {
        val arena = stage(4)
        val player = Actor(24.0, 24.0)
        val initial = NativeOpponentRoster.create(arena, player)
        val slots = initial.bots.map { slot ->
            NativeOpponentRoster.BotSlot(
                slot.owner, Actor(slot.actor.x, slot.actor.y, slot.actor.passBomb),
            )
        }
        val roster = NativeOpponentRoster(if (reverseOrder) slots.reversed() else slots)
        val sim = NativeBombSimulation(arena)

        repeat(180) { tick ->
            sim.tick()
            roster.advance(arena, player, sim, tick.toLong())
            val living = roster.blockingActors()
            for (slot in roster.bots.filterNot { it.vitality.defeated }) {
                assertFalse("bot ${slot.owner} entered terrain", StageCollision.blocked(
                    arena, slot.actor.x, slot.actor.y, StageCollision.ACTOR_RADIUS,
                    sim.bombs, slot.actor.passBomb,
                ))
                assertFalse("bot ${slot.owner} overlapped player or another living bot",
                    StageCollision.blockedByActors(
                        slot.actor.x, slot.actor.y,
                        listOf(player) + living.filterNot { it === slot.actor },
                    ))
            }
        }

        return FourSpawnSnapshot(
            bots = roster.bots.sortedBy { it.owner }.map {
                BotState(
                    it.owner, it.actor.x, it.actor.y, it.vitality.health,
                    it.vitality.defeated, it.walking, it.facing, it.actor.passBomb,
                )
            },
            bombs = sim.bombs.sortedBy { it.id }.map {
                BombState(it.id, it.owner, it.x, it.y, it.remaining, it.remote)
            },
            nextBombId = sim.nextBombId,
        )
    }

    @Test fun fourSpawnBotTicksAreIndependentOfRosterIterationOrder() {
        assertEquals(
            "Reordering the roster must not change AI decisions, movement, damage or bomb IDs",
            runFourSpawnRoster(reverseOrder = false),
            runFourSpawnRoster(reverseOrder = true),
        )
    }
}
