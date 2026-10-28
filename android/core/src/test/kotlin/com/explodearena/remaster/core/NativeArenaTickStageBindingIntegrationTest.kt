package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/** N75: the arena tick and bomb simulation must share the same full stage definition. */
class NativeArenaTickStageBindingIntegrationTest {
    private fun stage(wallColumn: Int = -1) = StageData(
        id = 0,
        image = "assets/maps/map_000.png",
        width = 96,
        height = 64,
        obstacleX = 0,
        obstacleY = 0,
        grid = List(8) { IntArray(12) { col -> if (col == wallColumn) 751 else 0 } },
        spawns = listOf(Spawn(16.0, 16.0), Spawn(72.0, 24.0)),
    )

    @Test fun canonicalArenaTickRejectsMismatchedSimulationBeforeAnyMutationAndCanRetrySameTick() {
        val arenaStage = stage()
        // Same ID/dimensions/spawns, but a different collision grid.
        val simulationStage = stage(wallColumn = 4)
        val player = Actor(16.0, 16.0)
        val bot = NativeOpponentRoster.BotSlot("bot", Actor(72.0, 24.0))
        val roster = NativeOpponentRoster(listOf(bot))
        val wrongSimulation = NativeBombSimulation(simulationStage)
        assertTrue(wrongSimulation.place(player, owner = "player"))
        val fuseBefore = wrongSimulation.bombs.single().remaining
        val playerBefore = Actor(player.x, player.y, player.passBomb)
        val botBefore = Actor(bot.actor.x, bot.actor.y, bot.actor.passBomb)
        val playerVitality = NativeVitality()
        val playerHealthBefore = playerVitality.health
        val botHealthBefore = bot.vitality.health
        val round = NativeRoundFlow()

        assertThrows(IllegalArgumentException::class.java) {
            NativeArenaTick.advance(
                arenaStage, player, playerVitality, roster, wrongSimulation, round,
                tick = 4L, dx = 1.0, dy = 0.0, hasNextStage = true,
            )
        }

        assertEquals(playerBefore.x, player.x, 0.0)
        assertEquals(playerBefore.y, player.y, 0.0)
        assertEquals(playerBefore.passBomb, player.passBomb)
        assertEquals(botBefore.x, bot.actor.x, 0.0)
        assertEquals(botBefore.y, bot.actor.y, 0.0)
        assertEquals(botBefore.passBomb, bot.actor.passBomb)
        assertEquals(fuseBefore, wrongSimulation.bombs.single().remaining)
        assertEquals(playerHealthBefore, playerVitality.health)
        assertEquals(botHealthBefore, bot.vitality.health)
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)

        // The rejected tick was not reserved by either canonical clock.
        player.passBomb = null
        val matchingSimulation = NativeBombSimulation(arenaStage)
        val retry = NativeArenaTick.advance(
            arenaStage, player, playerVitality, roster, matchingSimulation, round,
            tick = 4L, dx = 0.0, dy = 0.0, hasNextStage = true,
        )
        assertFalse(retry.playerWalking)
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)
    }

    @Test fun directRosterTickAlsoRejectsMismatchedSimulationBeforeFuseOrTickReservation() {
        val arenaStage = stage()
        val player = Actor(16.0, 16.0)
        val bot = NativeOpponentRoster.BotSlot("bot", Actor(72.0, 24.0))
        val roster = NativeOpponentRoster(listOf(bot))
        val wrongSimulation = NativeBombSimulation(stage(wallColumn = 4))
        assertTrue(wrongSimulation.place(player, owner = "player"))
        val fuseBefore = wrongSimulation.bombs.single().remaining
        val botBefore = Actor(bot.actor.x, bot.actor.y, bot.actor.passBomb)

        assertThrows(IllegalArgumentException::class.java) {
            roster.advanceSimulationTick(arenaStage, player, wrongSimulation, tick = 9L)
        }

        assertEquals(fuseBefore, wrongSimulation.bombs.single().remaining)
        assertEquals(botBefore.x, bot.actor.x, 0.0)
        assertEquals(botBefore.y, bot.actor.y, 0.0)
        assertEquals(botBefore.passBomb, bot.actor.passBomb)

        player.passBomb = null
        val matchingSimulation = NativeBombSimulation(arenaStage)
        val retry = roster.advanceSimulationTick(arenaStage, player, matchingSimulation, tick = 9L)
        assertEquals(0, retry.bombsDetonated)
    }

    @Test fun structurallyEquivalentStageCopiesRemainCompatible() {
        val simulation = NativeBombSimulation(stage())
        simulation.requireStageDefinition(stage())
    }

    @Test fun callerMutationCannotChangeSimulationGeometryAndRejectedTickCanRetryAfterRepair() {
        val base = stage()
        val mutableGrid = base.grid.map { it.copyOf() }.toMutableList()
        val mutableSpawns = base.spawns.toMutableList()
        val callerStage = base.copy(grid = mutableGrid, spawns = mutableSpawns)
        val simulation = NativeBombSimulation(callerStage)
        val player = Actor(16.0, 16.0)
        val bot = NativeOpponentRoster.BotSlot("bot", Actor(72.0, 24.0))
        val roster = NativeOpponentRoster(listOf(bot))
        val vitality = NativeVitality()
        val round = NativeRoundFlow()

        // The 16px bomb cell includes grid cell [3][3]. Mutating the caller's
        // row after construction must not change the simulation's captured map.
        mutableGrid[3][3] = 751
        mutableSpawns[0] = Spawn(8.0, 8.0)
        assertTrue("bomb placement must use the detached stage snapshot", simulation.place(player))
        val fuseBefore = simulation.bombs.single().remaining

        assertThrows(IllegalArgumentException::class.java) {
            NativeArenaTick.advance(
                callerStage, player, vitality, roster, simulation, round,
                tick = 12L, dx = 0.0, dy = 0.0, hasNextStage = true,
            )
        }
        assertEquals(16.0, player.x, 0.0)
        assertEquals(16.0, player.y, 0.0)
        assertEquals(fuseBefore, simulation.bombs.single().remaining)
        assertEquals(NativeRoundFlow.State.PLAYING, round.state)

        // Repair both caller-owned structures. The rejected tick was not
        // reserved, so the same tick index is accepted and advances one fuse step.
        mutableGrid[3][3] = 0
        mutableSpawns[0] = base.spawns[0]
        NativeArenaTick.advance(
            callerStage, player, vitality, roster, simulation, round,
            tick = 12L, dx = 0.0, dy = 0.0, hasNextStage = true,
        )
        assertEquals(
            requireNotNull(fuseBefore) - FixedStepClock.STEP_SECONDS,
            requireNotNull(simulation.bombs.single().remaining),
            1e-9,
        )
    }
}
