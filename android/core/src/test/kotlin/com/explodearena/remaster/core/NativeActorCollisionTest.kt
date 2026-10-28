package com.explodearena.remaster.core

import java.io.File
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

/**
 * Actor-to-actor movement integration, using the same provisional 5px square
 * hitboxes as native spawn resolution. Symbian/EKA2L1 parity NOT TESTED.
 */
class NativeActorCollisionTest {
    private fun arena(spawns: List<Spawn> = listOf(Spawn(40.0, 40.0))) = StageData(
        id = 14, image = "open.png", width = 160, height = 160,
        obstacleX = 0, obstacleY = 0, grid = List(20) { IntArray(20) },
        spawns = spawns,
    )

    private fun assertNoOverlaps(actors: List<Actor>) {
        for (i in actors.indices) for (j in i + 1 until actors.size) {
            assertFalse("actors $i/$j overlap at ${actors[i]} and ${actors[j]}",
                StageCollision.blockedByActors(actors[i].x, actors[i].y, listOf(actors[j])))
        }
    }

    @Test fun actorHitboxesTouchAtExactEdgeButCannotOverlap() {
        val other = Actor(60.0, 40.0)
        assertFalse(StageCollision.blockedByActors(50.0, 40.0, listOf(other)))
        assertTrue(StageCollision.blockedByActors(50.0001, 40.0, listOf(other)))
        assertFalse(StageCollision.blockedByActors(50.0001, 50.0, listOf(other)))
        assertTrue(StageCollision.blockedByActors(Double.NaN, 40.0, listOf(other)))
    }

    @Test fun playerAndOpponentCannotWalkThroughEachOtherInSameFixedStepModel() {
        val stage = arena()
        val player = Actor(40.0, 40.0)
        val bot = Actor(60.0, 40.0)
        StageCollision.move(stage, player, 1.0, 0.0, 77.0, 0.5,
            otherActors = listOf(bot))
        assertTrue(player.x > 40.0)
        assertTrue(player.x <= 50.0)
        StageCollision.move(stage, bot, -1.0, 0.0, 77.0, 0.5,
            otherActors = listOf(player))
        assertNoOverlaps(listOf(player, bot))
        // A diagonal approach may slide along the other actor, but never
        // penetrate its square hitbox on either axis.
        val diagonal = Actor(40.0, 40.0)
        StageCollision.move(stage, diagonal, 1.0, 1.0, 77.0, 0.5,
            otherActors = listOf(Actor(55.0, 55.0)))
        assertFalse(StageCollision.blockedByActors(diagonal.x, diagonal.y,
            listOf(Actor(55.0, 55.0))))
    }

    @Test fun invalidOrSelfBlockersFailBeforePositionOrBombGraceMutation() {
        val stage = arena()
        val player = Actor(40.0, 40.0, passBomb = 12L)
        assertThrows(IllegalArgumentException::class.java) {
            StageCollision.move(stage, player, 1.0, 0.0, 77.0,
                FixedStepClock.STEP_SECONDS, otherActors = listOf(player))
        }
        assertThrows(IllegalArgumentException::class.java) {
            StageCollision.move(stage, player, 1.0, 0.0, 77.0,
                FixedStepClock.STEP_SECONDS,
                otherActors = listOf(Actor(Double.NaN, 40.0)))
        }
        assertEquals(40.0, player.x, 0.0)
        assertEquals(40.0, player.y, 0.0)
        assertEquals(12L, player.passBomb)
    }

    @Test fun deadBotNoLongerBlocksPlayerMovement() {
        val stage = arena(listOf(Spawn(40.0, 40.0), Spawn(60.0, 40.0)))
        val player = Actor(40.0, 40.0)
        val roster = NativeOpponentRoster.create(stage, player)
        assertEquals(1, roster.blockingActors().size)
        repeat(350) { roster.bots.single().vitality.tick(true) }
        assertTrue(roster.bots.single().vitality.defeated)
        assertTrue(roster.blockingActors().isEmpty())
        StageCollision.move(stage, player, 1.0, 0.0, 77.0, 0.5,
            otherActors = roster.blockingActors())
        assertTrue("defeated bot must not be a solid obstacle", player.x > 60.0)
    }

    @Test fun fourActorNativeRoundNeverStacksLivePlayerAndBots() {
        val stage = arena(listOf(
            Spawn(24.0, 24.0), Spawn(48.0, 24.0),
            Spawn(24.0, 48.0), Spawn(48.0, 48.0),
        ))
        val player = Actor(24.0, 24.0)
        val roster = NativeOpponentRoster.create(stage, player)
        val bombs = NativeBombSimulation(stage)
        repeat(60) { tick ->
            StageCollision.move(stage, player, 1.0, 0.0, 77.0,
                FixedStepClock.STEP_SECONDS, bombs.bombs, roster.blockingActors())
            roster.advance(stage, player, bombs, tick.toLong())
            assertNoOverlaps(listOf(player) + roster.blockingActors())
            bombs.tick()
        }
    }

    @Test fun all41OriginalStageRostersStartAndAdvanceWithoutActorOverlap() {
        val source = File("../../web/assets/stages.js")
        assertTrue("Missing source stage catalog", source.isFile)
        val stages = StageCatalog.parse(source.readText(Charsets.UTF_8))
        assertEquals(41, stages.size)
        for (stage in stages) {
            val resolved = NativeSpawnLayout.resolve(stage)
            val player = Actor(resolved.first().x, resolved.first().y)
            val roster = NativeOpponentRoster.create(stage, player)
            val bombs = NativeBombSimulation(stage)
            roster.advance(stage, player, bombs, 0L)
            assertNoOverlaps(listOf(player) + roster.blockingActors())
        }
    }
}
