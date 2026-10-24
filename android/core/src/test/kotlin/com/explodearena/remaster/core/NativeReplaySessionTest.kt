package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class NativeReplaySessionTest {
    private fun stage() = StageData(
        id = 7,
        image = "assets/maps/map_007.png",
        width = 128,
        height = 64,
        obstacleX = 0,
        obstacleY = 0,
        grid = List(8) { IntArray(16) },
        spawns = listOf(Spawn(24.0, 24.0)),
    )

    private fun scriptedRun(): List<String> {
        val replay = NativeReplaySession(stage(), Spawn(24.0, 24.0))
        return (0 until 125).map { tick ->
            replay.step(NativeReplayInput(
                dx = if (tick in 1..24) 1.0 else 0.0,
                placeBomb = tick == 0 || tick == 110,
            )).traceLine()
        }
    }

    @Test fun identicalInputProducesByteIdenticalTraceIncludingBombsAndDamage() {
        val first = scriptedRun()
        val second = scriptedRun()
        assertEquals(125, first.size)
        assertEquals(first, second)
        assertTrue(first.first().contains("|bombs=1"))
        assertTrue(first.any { it.contains("|flames=1") })
        assertTrue(first.last().startsWith("EA-NATIVE-PROVISIONAL-v2|7|125|"))
    }

    @Test fun replayChangesWhenMovementInputChanges() {
        val a = NativeReplaySession(stage(), Spawn(24.0, 24.0))
        val b = NativeReplaySession(stage(), Spawn(24.0, 24.0))
        val left = a.step(NativeReplayInput(dx = -1.0)).traceLine()
        val right = b.step(NativeReplayInput(dx = 1.0)).traceLine()
        assertNotEquals(left, right)
    }

    @Test fun malformedInputDoesNotMutateReplayState() {
        val replay = NativeReplaySession(stage(), Spawn(24.0, 24.0))
        val before = replay.snapshot()
        for (bad in listOf(Double.NaN, Double.POSITIVE_INFINITY, 1.01, -1.01)) {
            assertThrows(IllegalArgumentException::class.java) {
                replay.step(NativeReplayInput(dx = bad, placeBomb = true))
            }
            assertEquals(before, replay.snapshot())
        }
    }

    @Test fun remoteBombRemainsUntilExplicitDetonation() {
        val replay = NativeReplaySession(stage(), Spawn(24.0, 24.0))
        assertTrue(replay.setRemoteDetonator(enabled = true))
        val placed = replay.step(NativeReplayInput(placeBomb = true))
        assertEquals(1, placed.bombs.size)
        assertTrue(placed.bombs.single().remote)
        repeat(120) { replay.step(NativeReplayInput()) }
        assertEquals(1, replay.snapshot().bombs.size)
        val detonated = replay.step(NativeReplayInput(remoteDetonate = true))
        assertTrue(detonated.bombs.isEmpty())
        assertTrue(detonated.flames.isNotEmpty())
    }
    @Test fun traceIncludesHiddenCollisionGraceAndFutureBombIdentity() {
        val replay = NativeReplaySession(stage(), Spawn(24.0, 24.0))
        val initial = replay.snapshot()
        assertNull(initial.passBomb)
        assertEquals(1L, initial.nextBombId)
        val planted = replay.step(NativeReplayInput(placeBomb = true))
        assertEquals(planted.bombs.single().id, planted.passBomb)
        assertEquals(2L, planted.nextBombId)
        assertTrue(planted.traceLine().contains("|pass=1|"))
        assertTrue(planted.traceLine().contains("|nextBombId=2|"))
        repeat(20) { replay.step(NativeReplayInput(dx = 1.0)) }
        assertNull(replay.snapshot().passBomb)
    }

    @Test fun traceDistinguishesRemotePowerBeforeBombPlacement() {
        val baseline = NativeReplaySession(stage(), Spawn(24.0, 24.0))
        val remote = NativeReplaySession(stage(), Spawn(24.0, 24.0))
        assertEquals(baseline.snapshot().traceLine(), remote.snapshot().traceLine())
        assertTrue(remote.setRemoteDetonator(enabled = true))
        assertNotEquals(baseline.snapshot().traceLine(), remote.snapshot().traceLine())
        assertEquals(listOf("player"), remote.snapshot().remoteDetonatorOwners)
        assertTrue(remote.snapshot().traceLine().contains("|remoteOwners=player|"))
    }

}
