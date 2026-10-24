package com.explodearena.remaster.core

/**
 * Native opponents for 1-, 2- and 4-spawn arenas. Each opponent owns a
 * separate provisional AI, vitality and bomb quota. Not Symbian mode parity.
 */
internal class NativeOpponentRoster private constructor(val bots: List<BotSlot>) {
    class BotSlot(
        val owner: String,
        val actor: Actor,
        val vitality: NativeVitality = NativeVitality(),
        val controller: NativeBotController = NativeBotController(owner),
    ) {
        var facing = 2
        var walking = false
    }

    val present: Boolean get() = bots.isNotEmpty()
    val aliveCount: Int get() = bots.count { !it.vitality.defeated }
    val allDefeated: Boolean get() = present && aliveCount == 0

    fun stopWalking() { bots.forEach { it.walking = false } }

    /** Number of bombs successfully placed by all opponents this fixed tick. */
    fun advance(stage: StageData, player: Actor, simulation: NativeBombSimulation, tick: Long): Int {
        var placed = 0
        for (slot in bots) {
            if (slot.vitality.defeated) {
                slot.walking = false
                continue
            }
            val turn = NativeBotTurn.advance(
                stage, slot.actor, player, simulation, slot.controller, tick, slot.owner,
            )
            slot.walking = turn.moved
            if (turn.moved) {
                slot.facing = when {
                    turn.decision.dx > 0 -> 3
                    turn.decision.dx < 0 -> 1
                    turn.decision.dy > 0 -> 2
                    else -> 0
                }
            }
            if (turn.bombPlaced) placed++
            slot.vitality.tick(simulation.isBurning(
                slot.actor.x, slot.actor.y, StageCollision.ACTOR_RADIUS,
            ))
        }
        return placed
    }

    companion object {
        private val owners = listOf("bot", "bot2", "bot3")

        fun create(stage: StageData, player: Actor): NativeOpponentRoster {
            require(stage.spawns.size in setOf(1, 2, 4)) { "Unsupported native spawn count" }
            val occupied = mutableListOf(Spawn(player.x, player.y))
            val bots = stage.spawns.drop(1).mapIndexed { index, spawn ->
                val safe = StageCollision.nearestFree(stage, spawn.x, spawn.y)
                    ?: error("Arena ${stage.id} has no free opponent spawn ${index + 1}")
                check(occupied.none {
                    kotlin.math.abs(it.x - safe.x) < 2 * StageCollision.ACTOR_RADIUS &&
                        kotlin.math.abs(it.y - safe.y) < 2 * StageCollision.ACTOR_RADIUS
                }) { "Arena ${stage.id} has overlapping opponent spawns" }
                occupied.add(safe)
                BotSlot(owners[index], Actor(safe.x, safe.y))
            }
            return NativeOpponentRoster(bots)
        }
    }
}
