package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

/** N62: malformed rosters must not alias owner quotas or mutable bot state. */
class NativeOpponentRosterInvariantTest {
    private fun fourSpawnStage() = StageData(
        id = 7,
        image = "test.png",
        width = 128,
        height = 64,
        obstacleX = 0,
        obstacleY = 0,
        grid = List(8) { IntArray(16) },
        spawns = listOf(
            Spawn(24.0, 24.0),
            Spawn(88.0, 24.0),
            Spawn(24.0, 40.0),
            Spawn(88.0, 40.0),
        ),
    )

    @Test fun duplicateOwnerIdsAreRejectedBeforeAnyTickCanShareBombQuota() {
        assertThrows(IllegalArgumentException::class.java) {
            NativeOpponentRoster(
                listOf(
                    NativeOpponentRoster.BotSlot("bot", Actor(24.0, 24.0)),
                    NativeOpponentRoster.BotSlot("bot", Actor(88.0, 24.0)),
                ),
            )
        }
    }

    @Test fun sharedActorReferenceIsRejected() {
        val actor = Actor(24.0, 24.0)
        assertThrows(IllegalArgumentException::class.java) {
            NativeOpponentRoster(
                listOf(
                    NativeOpponentRoster.BotSlot("bot", actor),
                    NativeOpponentRoster.BotSlot("bot2", actor),
                ),
            )
        }
    }

    @Test fun sharedVitalityReferenceIsRejected() {
        val vitality = NativeVitality()
        assertThrows(IllegalArgumentException::class.java) {
            NativeOpponentRoster(
                listOf(
                    NativeOpponentRoster.BotSlot("bot", Actor(24.0, 24.0), vitality),
                    NativeOpponentRoster.BotSlot("bot2", Actor(88.0, 24.0), vitality),
                ),
            )
        }
    }

    @Test fun sharedControllerReferenceIsRejected() {
        val controller = NativeBotController("bot")
        assertThrows(IllegalArgumentException::class.java) {
            NativeOpponentRoster(
                listOf(
                    NativeOpponentRoster.BotSlot("bot", Actor(24.0, 24.0), controller = controller),
                    NativeOpponentRoster.BotSlot("bot2", Actor(88.0, 24.0), controller = controller),
                ),
            )
        }
    }

    @Test fun mismatchedControllerOwnerIsRejectedBeforeGameplay() {
        assertThrows(IllegalArgumentException::class.java) {
            NativeOpponentRoster(
                listOf(
                    NativeOpponentRoster.BotSlot(
                        "bot",
                        Actor(24.0, 24.0),
                        controller = NativeBotController("bot2"),
                    ),
                ),
            )
        }
    }

    @Test fun canonicalFourSpawnFactoryCreatesIndependentStateForEveryBot() {
        val player = Actor(24.0, 24.0)
        val roster = NativeOpponentRoster.create(fourSpawnStage(), player)
        assertEquals(listOf("bot", "bot2", "bot3"), roster.bots.map { it.owner })
        for (firstIndex in roster.bots.indices) {
            for (secondIndex in firstIndex + 1 until roster.bots.size) {
                val first = roster.bots[firstIndex]
                val second = roster.bots[secondIndex]
                assertNotSame(first.actor, second.actor)
                assertNotSame(first.vitality, second.vitality)
                assertNotSame(first.controller, second.controller)
            }
        }
    }

    @Test fun rosterMembershipCannotBeMutatedThroughInputOrReturnedList() {
        val original = NativeOpponentRoster.BotSlot("bot", Actor(24.0, 24.0))
        val callerOwned = mutableListOf(original)
        val roster = NativeOpponentRoster(callerOwned)

        // The constructor validated one slot; later caller changes must not
        // silently bypass owner uniqueness or introduce a new simulation actor.
        callerOwned.add(NativeOpponentRoster.BotSlot("bot", Actor(88.0, 24.0)))
        assertEquals(listOf("bot"), roster.bots.map { it.owner })
        assertEquals(1, roster.blockingActors().size)

        // The public read-only Kotlin type is not a JVM immutability boundary.
        // Even a caller that casts/mutates the returned JVM list gets a detached copy.
        @Suppress("UNCHECKED_CAST")
        val exposed = roster.bots as MutableList<NativeOpponentRoster.BotSlot>
        exposed.add(NativeOpponentRoster.BotSlot("bot2", Actor(72.0, 40.0)))
        assertEquals(listOf("bot"), roster.bots.map { it.owner })
        assertEquals(1, roster.blockingActors().size)
    }

}
