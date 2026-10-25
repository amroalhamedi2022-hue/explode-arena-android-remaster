package com.explodearena.remaster.core

import kotlin.math.abs

/**
 * Resolves every original arena spawn against the same 5px hitbox used by
 * StageCollision.move. Some source spawns are legal for a 4px point check but
 * collide as soon as the actual native actor starts moving.
 *
 * Repositioning is deterministic and local to the native runtime. It does NOT
 * establish original Symbian spawn behavior or alter the 41 source records.
 */
internal object NativeSpawnLayout {
    fun resolve(stage: StageData): List<Spawn> {
        require(stage.spawns.size in setOf(1, 2, 4)) {
            "Arena ${stage.id} has unsupported spawn count ${stage.spawns.size}"
        }
        val radius = StageCollision.ACTOR_RADIUS
        val positions = stage.spawns.mapIndexed { index, spawn ->
            StageCollision.nearestFree(stage, spawn.x, spawn.y, radius)
                ?: error("Arena ${stage.id} has no walkable spawn ${index + 1} for ${radius}px actor")
        }
        for ((index, position) in positions.withIndex()) {
            check(!StageCollision.blocked(stage, position.x, position.y, radius)) {
                "Arena ${stage.id} resolved spawn ${index + 1} is obstructed"
            }
            check(positions.take(index).none { previous ->
                abs(previous.x - position.x) < 2 * radius &&
                    abs(previous.y - position.y) < 2 * radius
            }) { "Arena ${stage.id} has overlapping resolved native spawns" }
        }
        return positions
    }
}
