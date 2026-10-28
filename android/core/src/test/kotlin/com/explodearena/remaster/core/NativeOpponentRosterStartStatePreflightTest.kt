package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/** N73: direct/replay bot ticks must validate the full start world before consuming a tick. */
class NativeOpponentRosterStartStatePreflightTest {
    private fun stage(wallColumn: Int = -1) = StageData(
        id = 0,
        image = "test.png",
        width = 128,
        height = 64,
        obstacleX = 0,
        obstacleY = 0,
        grid = List(8) { IntArray(16) { col -> if (col == wallColumn) 751 else 0 } },
        spawns = listOf(Spawn(88.0, 24.0), Spawn(24.0, 24.0)),
    )

    @Test fun directBotTickRejectsTerrainOverlapBeforeFuseOrTickMutationAndCanRetrySameTick() {
        val arena = stage(wallColumn = 4) // solid x=32..39
        val player = Actor(88.0, 24.0)
        val bot = NativeOpponentRoster.BotSlot("bot", Actor(31.0, 24.0))
        val roster = NativeOpponentRoster(listOf(bot))
        val simulation = NativeBombSimulation(arena)
        assertTrue(simulation.place(player, "player"))
        val fuseBefore = simulation.bombs.single().remaining
        val playerBefore = Actor(player.x, player.y, player.passBomb)
        val botBefore = Actor(bot.actor.x, bot.actor.y, bot.actor.passBomb)
        val playerHealthBefore = NativeVitality().health
        val botHealthBefore = bot.vitality.health

        assertThrows(IllegalArgumentException::class.java) {
            roster.advanceSimulationTick(arena, player, simulation, tick = 17L)
        }

        assertEquals(playerBefore.x, player.x, 0.0)
        assertEquals(playerBefore.y, player.y, 0.0)
        assertEquals(playerBefore.passBomb, player.passBomb)
        assertEquals(botBefore.x, bot.actor.x, 0.0)
        assertEquals(botBefore.y, bot.actor.y, 0.0)
        assertEquals(botBefore.passBomb, bot.actor.passBomb)
        assertEquals(fuseBefore, simulation.bombs.single().remaining)
        assertEquals(1, simulation.bombs.size)
        assertEquals(botHealthBefore, bot.vitality.health)
        assertEquals(playerHealthBefore, NativeVitality().health)

        // Repair the actor and retry the exact same tick: the failed preflight
        // must not have reserved it or advanced the bomb fuse.
        bot.actor.x = 24.0
        val result = roster.advanceSimulationTick(arena, player, simulation, tick = 17L)
        assertEquals(0, result.bombsDetonated)
        assertTrue(requireNotNull(simulation.bombs.firstOrNull { it.owner == "player" }).remaining!! < fuseBefore!!)
    }

    @Test fun directBotTickRejectsForeignBombOverlapBeforeFuseAgingAndAllowsOwnerGrace() {
        val arena = stage()
        val player = Actor(24.0, 24.0)
        val simulation = NativeBombSimulation(arena)
        assertTrue(simulation.place(player, "player"))
        // Keep the player's existing bomb in the world while moving the player
        // away; only the bot now illegally overlaps that bomb.
        player.x = 88.0
        val bot = NativeOpponentRoster.BotSlot("bot", Actor(24.0, 24.0))
        val roster = NativeOpponentRoster(listOf(bot))
        val fuseBefore = simulation.bombs.single().remaining

        assertThrows(IllegalArgumentException::class.java) {
            roster.advanceSimulationTick(arena, player, simulation, tick = 4L)
        }
        assertEquals(fuseBefore, simulation.bombs.single().remaining)
        assertEquals(1, simulation.bombs.size)

        // Move the bot clear of the foreign bomb. The same tick remains available.
        bot.actor.x = 40.0
        val result = roster.advanceSimulationTick(arena, player, simulation, tick = 4L)
        assertEquals(0, result.bombsDetonated)
        assertTrue(simulation.bombs.single().remaining!! < fuseBefore!!)
    }

    @Test fun directBotTickRejectsOverlappingActorStartsWithoutChangingEitherActor() {
        val arena = stage()
        val player = Actor(40.0, 24.0)
        val bot = NativeOpponentRoster.BotSlot("bot", Actor(48.0, 24.0))
        val roster = NativeOpponentRoster(listOf(bot))
        val simulation = NativeBombSimulation(arena)
        val playerBefore = player.x to player.y
        val botBefore = bot.actor.x to bot.actor.y

        assertThrows(IllegalArgumentException::class.java) {
            roster.advanceSimulationTick(arena, player, simulation, tick = 0L)
        }
        assertEquals(playerBefore.first, player.x, 0.0)
        assertEquals(playerBefore.second, player.y, 0.0)
        assertEquals(botBefore.first, bot.actor.x, 0.0)
        assertEquals(botBefore.second, bot.actor.y, 0.0)
        assertTrue(simulation.bombs.isEmpty())

        bot.actor.x = 72.0
        roster.advanceSimulationTick(arena, player, simulation, tick = 0L)
    }
}
