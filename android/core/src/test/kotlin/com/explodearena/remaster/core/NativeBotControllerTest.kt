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

    @Test fun bombPlacementUsesRefugeDirectionImmediatelyInsteadOfChasing() {
        val arena = stage()
        val bot = Actor(40.0, 40.0)
        // Chase would head down toward this close target, but the predicted
        // bomb blast requires a perpendicular escape first.
        val target = Actor(40.0, 56.0)
        val sim = NativeBombSimulation(arena)
        val refuge = NativeBotEscapeRoute.firstDirection(arena, bot, sim, bot.x, bot.y)
        assertNotNull("A valid pre-placement escape must exist", refuge)
        val choice = NativeBotController().decide(arena, bot, target, sim, 0)
        assertTrue(choice.placeBomb)
        val expectedDx = when (refuge) { 1 -> 1.0; 3 -> -1.0; else -> 0.0 }
        val expectedDy = when (refuge) { 2 -> 1.0; 4 -> -1.0; else -> 0.0 }
        assertEquals(expectedDx, choice.dx, 0.0)
        assertEquals(expectedDy, choice.dy, 0.0)
        assertFalse("Do not take the chase step on a planting tick",
            choice.dx == 0.0 && choice.dy == 1.0)
    }

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

    @Test fun alreadySafeCheckMustNotIgnoreAnActiveFlame() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.place(Actor(40.0, 40.0)))
        repeat(96) { sim.tick() }
        val bot = Actor(56.0, 40.0)
        assertTrue(sim.isBurning(bot.x, bot.y, StageCollision.ACTOR_RADIUS))
        // Diagonal to the proposed bomb, with no other active bomb left:
        // the previous early-return incorrectly said "already safe".
        assertFalse(sim.wouldBlastHitFrom(104.0, 104.0, bot.x, bot.y))
        assertTrue(sim.bombs.isEmpty())
        val direction = NativeBotEscapeRoute.firstDirection(arena, bot, sim, 104.0, 104.0)
        assertNotEquals("Standing in active fire cannot be a safe refuge", 0, direction ?: -1)
    }

    @Test fun alreadySafeAndNotBurningStillReturnsZero() {
        val arena = stage()
        val direction = NativeBotEscapeRoute.firstDirection(
            arena, Actor(24.0, 24.0), NativeBombSimulation(arena), 104.0, 104.0,
        )
        assertEquals(0, direction)
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

    @Test fun imminentOpponentBombOverridesChaseWithAReachableEscape() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        // Near the edge of the ray, the bot has a feasible short escape
        // even though the bomb has only 0.6s left.
        val bot = Actor(40.0, 52.0)
        assertTrue(sim.place(Actor(56.0, 40.0), "player"))
        repeat(60) { sim.tick() } // 0.6s left; inside the provisional ray.
        assertTrue(sim.wouldBlastHitFrom(56.0, 40.0, bot.x, bot.y))
        val escape = NativeBotEscapeRoute.firstDirection(arena, bot, sim, 56.0, 40.0)
        assertNotNull("The threat must have a reachable refuge", escape)
        val choice = NativeBotController().decide(arena, bot, Actor(76.0, 40.0), sim, 60)
        val direction = when {
            choice.dx > 0 -> 1
            choice.dy > 0 -> 2
            choice.dx < 0 -> 3
            choice.dy < 0 -> 4
            else -> 0
        }
        assertEquals(escape, direction)
        assertFalse("Do not plant a second bomb while escaping", choice.placeBomb)
    }

    @Test fun safeBotDoesNotStepIntoAnImminentBlastWhileChasing() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.place(Actor(56.0, 24.0), "player"))
        repeat(60) { sim.tick() }
        val bot = Actor(42.5, 40.0)
        assertFalse(sim.wouldBlastHitFrom(56.0, 24.0, bot.x, bot.y))
        val choice = NativeBotController().decide(arena, bot, Actor(80.0, 40.0), sim, 60)
        val nx = bot.x + choice.dx * NativeBotController.SPEED * FixedStepClock.STEP_SECONDS
        val ny = bot.y + choice.dy * NativeBotController.SPEED * FixedStepClock.STEP_SECONDS
        assertFalse("Chase must not enter the imminent ray",
            sim.wouldBlastHitFrom(56.0, 24.0, nx, ny))
        assertFalse(choice.placeBomb)
    }

    @Test fun remoteBombWithoutFuseDoesNotCausePrematureEscape() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(56.0, 40.0), "player"))
        repeat(120) { sim.tick() }
        assertNull(sim.bombs.single().remaining)
        val choice = NativeBotController().decide(
            arena, Actor(40.0, 40.0), Actor(80.0, 40.0), sim, 120,
        )
        val timed = NativeBombSimulation(arena)
        assertTrue(timed.place(Actor(56.0, 40.0), "player"))
        val timedChoice = NativeBotController().decide(
            arena, Actor(40.0, 40.0), Actor(80.0, 40.0), timed, 0,
        )
        assertEquals("Remote no-fuse should follow the same route as a non-urgent timed bomb",
            timedChoice, choice)
        assertFalse(choice.placeBomb)
    }

    @Test fun fullFuseBombDoesNotTriggerPrematureRetreat() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.place(Actor(56.0, 40.0), "player"))
        val choice = NativeBotController().decide(
            arena, Actor(40.0, 40.0), Actor(80.0, 40.0), sim, 0,
        )
        val remote = NativeBombSimulation(arena)
        assertTrue(remote.setRemoteDetonator("player", true))
        assertTrue(remote.place(Actor(56.0, 40.0), "player"))
        val remoteChoice = NativeBotController().decide(
            arena, Actor(40.0, 40.0), Actor(80.0, 40.0), remote, 0,
        )
        assertEquals("Long-fuse bomb must not trigger the imminent-threat mode",
            remoteChoice, choice)
        assertFalse(choice.placeBomb)
    }

    @Test fun imminentOpponentBlastSuppressesBotBombPlanting() {
        val arena = stage()
        val sim = NativeBombSimulation(arena)
        assertTrue(sim.place(Actor(72.0, 40.0), "player"))
        repeat(60) { sim.tick() }
        val bot = Actor(40.0, 40.0)
        assertTrue(sim.wouldBlastHitFrom(72.0, 40.0, bot.x, bot.y))
        val choice = NativeBotController().decide(arena, bot, Actor(56.0, 40.0), sim, 60)
        assertFalse(choice.placeBomb)
    }

    @Test fun isolatedRemoteBombDoesNotInterruptBotPursuit() {
        val arena = stage()
        val sim = NativeBombSimulation(arena, bombLimit = 2)
        assertTrue(sim.setRemoteDetonator("player", true))
        assertTrue(sim.place(Actor(120.0, 120.0), "player"))
        val choice = NativeBotController().decide(
            arena, Actor(24.0, 24.0), Actor(88.0, 24.0), sim, 0,
        )
        assertEquals(1.0, choice.dx, 0.0)
        assertEquals(0.0, choice.dy, 0.0)
        assertFalse(choice.placeBomb)
    }

    @Test fun ownedRemoteBombIsNeverSelectedAsAnAutomaticFuseEscapeTarget() {
        val remote = NativeBombSimulation.Bomb(
            id = 7, owner = "bot", x = 40.0, y = 40.0, remaining = null, remote = true,
        )
        assertNull(selectOwnedTimedBomb(listOf(remote), "bot"))
    }

    @Test fun ownedTimedBombRemainsEscapeTargetWhenRemoteBombAlsoExists() {
        val remote = NativeBombSimulation.Bomb(
            id = 7, owner = "bot", x = 40.0, y = 40.0, remaining = null, remote = true,
        )
        val timed = NativeBombSimulation.Bomb(
            id = 8, owner = "bot", x = 56.0, y = 40.0, remaining = 0.5, remote = false,
        )
        assertEquals(timed, selectOwnedTimedBomb(listOf(remote, timed), "bot"))
    }

    @Test fun remoteBombReachedByTimedChainRemainsAnEscapeThreat() {
        val openArena = stage()
        // A wall blocks the direct chase step, so the bot must take a safe perpendicular refuge.
        val arena = openArena.copy(grid = openArena.grid.mapIndexed { row, cells ->
            cells.copyOf().also { if (row == 5) it[13] = 751 }
        })
        val sim = NativeBombSimulation(arena, bombLimit = 2, bombRange = 1)
        assertTrue(sim.setRemoteDetonator("player2", true))
        val bot = Actor(88.0, 40.0)
        assertTrue(sim.place(Actor(72.0, 40.0), "player2"))
        assertTrue(sim.place(Actor(56.0, 40.0), "player"))
        repeat(60) { sim.tick() }
        assertTrue(
            "Timed blast must schedule the remote bomb through the chain",
            sim.predictedTimedChainThreats(0.75).any {
                it.bomb.owner == "player2" && it.bomb.remote
            },
        )
        val refuge = NativeBotEscapeRoute.firstDirection(arena, bot, sim, 72.0, 40.0)
        assertNotNull("A refuge exists outside both predicted blast footprints", refuge)
        assertTrue("The bot is in the timed-chain footprint and must move to escape", refuge != 0)
        val expectedDx = when (refuge) { 1 -> 1.0; 3 -> -1.0; else -> 0.0 }
        val expectedDy = when (refuge) { 2 -> 1.0; 4 -> -1.0; else -> 0.0 }
        val choice = NativeBotController().decide(arena, bot, Actor(120.0, 40.0), sim, 60)
        assertEquals("Controller must follow the independently computed refuge", expectedDx, choice.dx, 0.0)
        assertEquals("Controller must follow the independently computed refuge", expectedDy, choice.dy, 0.0)
        assertFalse(choice.placeBomb)
        assertFalse("The bot must not continue directly into the timed chain", choice.dx == 1.0 && choice.dy == 0.0)
    }

}
