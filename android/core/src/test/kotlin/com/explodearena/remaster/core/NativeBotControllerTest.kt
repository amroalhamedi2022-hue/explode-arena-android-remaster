package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeBotControllerTest {
    private fun stage(withWall: Boolean = false) = StageData(
        id = 0, image = "test.png", width = 128, height = 128,
        obstacleX = 0, obstacleY = 0,
        grid = List(16) { row ->
            IntArray(16) { col -> if (withWall && col == 6 && row < 12) 751 else 0 }
        },
        spawns = listOf(Spawn(20.0, 20.0), Spawn(76.0, 20.0)),
    )

    @Test fun approachesPlayerOnOpenArena() {
        val arena = stage()
        val choice = NativeBotController().decide(
            arena, Actor(20.0, 20.0), Actor(76.0, 20.0),
            NativeBombSimulation(arena), 0,
        )
        assertEquals(1.0, choice.dx, 0.0)
        assertEquals(0.0, choice.dy, 0.0)
        assertFalse(choice.placeBomb)
    }

    @Test fun routesAroundWallRatherThanWalkingIntoIt() {
        val arena = stage(withWall = true)
        val choice = NativeBotController().decide(
            arena, Actor(36.0, 20.0), Actor(76.0, 20.0),
            NativeBombSimulation(arena), 0,
        )
        assertEquals(0.0, choice.dx, 0.0)
        assertEquals(1.0, choice.dy, 0.0)
    }

    @Test fun closeOpponentTriggersProvisionalBombOnlyWithEscapeAndCapacity() {
        val arena = stage()
        val bot = Actor(40.0, 40.0)
        val player = Actor(56.0, 40.0)
        val sim = NativeBombSimulation(arena)
        val controller = NativeBotController()
        assertTrue(controller.decide(arena, bot, player, sim, 0).placeBomb)
        assertTrue(sim.place(bot, "bot"))
        assertFalse(controller.decide(arena, bot, player, sim, 1).placeBomb)
    }

    @Test fun neverSelectsMovementIntoActiveFlame() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.place(Actor(40.0, 40.0)))
        repeat(96) { sim.tick() }
        val bot = Actor(24.0, 24.0)
        val choice = NativeBotController().decide(arena, bot, Actor(76.0, 24.0), sim, 0)
        val nextX = bot.x + choice.dx * NativeBotController.SPEED * FixedStepClock.STEP_SECONDS
        val nextY = bot.y + choice.dy * NativeBotController.SPEED * FixedStepClock.STEP_SECONDS
        assertFalse(sim.isBurning(nextX, nextY, StageCollision.ACTOR_RADIUS))
    }

    @Test fun refusesBombInCorridorWithNoReachableBlastRefuge() {
        val arena = StageData(
            id = 0, image = "corridor.png", width = 128, height = 128,
            obstacleX = 0, obstacleY = 0,
            grid = List(16) { row ->
                IntArray(16) { col ->
                    if (row in 4..6 && col in 3..10) 0 else 751
                }
            },
            spawns = listOf(Spawn(40.0, 44.0), Spawn(56.0, 44.0)),
        )
        val sim = NativeBombSimulation(arena)
        val decision = NativeBotController().decide(
            arena, Actor(40.0, 44.0), Actor(56.0, 44.0), sim, 0,
        )
        assertFalse(decision.placeBomb)
    }

    @Test fun botFleesOwnBombBeforeItsProvisionalFuseExpires() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        val bot = Actor(40.0, 40.0)
        val target = Actor(56.0, 40.0)
        val controller = NativeBotController()
        assertTrue(sim.place(bot, "bot"))
        repeat(75) { tick ->
            val choice = controller.decide(arena, bot, target, sim, tick.toLong())
            assertFalse(choice.placeBomb)
            StageCollision.move(
                arena, bot, choice.dx, choice.dy,
                NativeBotController.SPEED, FixedStepClock.STEP_SECONDS, sim.bombs,
            )
            sim.tick()
        }
        assertFalse(
            "Bot must reach a refuge before the 96-tick fuse",
            sim.wouldBlastHitFrom(40.0, 40.0, bot.x, bot.y),
        )
    }

    @Test fun escapeRefugeMustAvoidEveryActiveBombFootprint() {
        // The only potential refuge from the first bomb is the bottom
        // corridor row; a second bomb covers it. No speculative escape.
        val corridor = StageData(
            id = 0, image = "test.png", width = 128, height = 128,
            obstacleX = 0, obstacleY = 0,
            grid = List(16) { row ->
                IntArray(16) { col ->
                    if (row in 4..7 && col in 3..10) 0 else 751
                }
            },
            spawns = listOf(Spawn(40.0, 44.0), Spawn(56.0, 60.0)),
        )
        val sim = NativeBombSimulation(corridor)
        val bot = Actor(40.0, 44.0)
        assertTrue(sim.place(bot, "bot"))
        assertTrue(sim.place(Actor(56.0, 60.0), "player"))
        val threats = sim.predictedActiveBlastCells()
        assertTrue(threats.isNotEmpty())
        assertTrue(threats.any {
            NativeBombSimulation.blastHits(it, 44.0, 60.0, StageCollision.ACTOR_RADIUS)
        })
        assertNull(NativeBotEscapeRoute.firstDirection(corridor, bot, sim, 40.0, 40.0))
    }

    @Test fun provisionalBlastPredictionDistinguishesRayFromDiagonalRefuge() {
        val sim = NativeBombSimulation(stage())
        assertTrue(sim.wouldBlastHitFrom(40.0, 40.0, 56.0, 40.0))
        assertFalse(sim.wouldBlastHitFrom(40.0, 40.0, 64.0, 64.0))
    }

    @Test fun lateEscapeCannotClaimRefugeBeyondRemainingFuseTravel() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        val bot = Actor(40.0, 40.0)
        assertTrue(sim.place(bot, "bot"))
        // At placement there is time to escape an open arena.
        assertNotNull(NativeBotEscapeRoute.firstDirection(arena, bot, sim, 40.0, 40.0))
        // The provisional 1.6s fuse is 96 ticks. After 92 ticks, less
        // than five movement pixels remain; a distant refuge is impossible.
        repeat(92) { sim.tick() }
        assertTrue(sim.bombs.single().remaining!! > 0.0)
        assertNull(NativeBotEscapeRoute.firstDirection(arena, bot, sim, 40.0, 40.0))
    }

    @Test fun remoteBombWithoutFuseDoesNotInheritTimedEscapeDeadline() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        val bot = Actor(40.0, 40.0)
        // Remote owners are intentionally limited to players; use the same
        // escape planner directly for a player-owned remote bomb.
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(bot, "player"))
        repeat(200) { sim.tick() }
        assertNull(sim.bombs.single().remaining)
        assertNotNull(NativeBotEscapeRoute.firstDirection(arena, bot, sim, 40.0, 40.0))
    }
}
