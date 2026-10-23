package com.explodearena.remaster.core

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Provisional JS-mainline compatible values; these are NOT verified Symbian
 * movement, collision, or timing rules. All positions use original y-down pixels.
 */
data class Spawn(val x: Double, val y: Double)

data class StageData(
    val id: Int,
    val image: String,
    val width: Int,
    val height: Int,
    val obstacleX: Int,
    val obstacleY: Int,
    val grid: List<IntArray>,
    val spawns: List<Spawn>,
) {
    val gridHeight: Int get() = grid.size
    val gridWidth: Int get() = grid.firstOrNull()?.size ?: 0

    init {
        require(width > 0 && height > 0 && grid.isNotEmpty())
        require(gridWidth > 0 && grid.all { it.size == gridWidth })
    }
}

data class Actor(var x: Double, var y: Double, var passBomb: Long? = null)

object StageCollision {
    const val CELL = 8
    const val ACTOR_RADIUS = 5.0

    fun tile(stage: StageData, x: Double, y: Double): Int {
        val col = floor((x - stage.obstacleX) / CELL).toInt()
        val row = floor((y - stage.obstacleY) / CELL).toInt()
        if (col !in 0 until stage.gridWidth || row !in 0 until stage.gridHeight) return -1
        return stage.grid[row][col]
    }

    fun blocked(
        stage: StageData, x: Double, y: Double, radius: Double = ACTOR_RADIUS,
        bombs: List<NativeBombSimulation.Bomb> = emptyList(), ignoreBombId: Long? = null,
    ): Boolean {
        if (x - radius < 0 || y - radius < 0 || x + radius >= stage.width || y + radius >= stage.height) return true
        for (dx in doubleArrayOf(-radius, radius)) for (dy in doubleArrayOf(-radius, radius)) {
            if (tile(stage, x + dx, y + dy) != 0) return true
        }
        // Mirrors the current PROVISIONAL JS touchesBomb (7px half-size),
        // not yet a verified Symbian collision rule.
        return bombs.any { it.id != ignoreBombId && touchesBomb(it, x, y, radius) }
    }

    fun touchesBomb(bomb: NativeBombSimulation.Bomb, x: Double, y: Double, radius: Double = ACTOR_RADIUS): Boolean =
        kotlin.math.abs(bomb.x - x) < 7.0 + radius &&
            kotlin.math.abs(bomb.y - y) < 7.0 + radius

    /** JS-compatible axis-separated movement and <=3 pixel substeps. */
    fun move(
        stage: StageData, actor: Actor, dx: Double, dy: Double, speed: Double, dt: Double,
        bombs: List<NativeBombSimulation.Bomb> = emptyList(),
    ) {
        require(speed >= 0.0 && dt >= 0.0 && dx.isFinite() && dy.isFinite())
        // Clear stale grace even when standing still after the bomb detonates.
        val prior = actor.passBomb
        if (prior != null && bombs.none { it.id == prior && touchesBomb(it, actor.x, actor.y) }) {
            actor.passBomb = null
        }
        val length = hypot(dx, dy)
        if (length == 0.0 || dt == 0.0) return
        val distance = speed * dt
        val steps = max(1, ceil(distance / 3.0).toInt())
        val sx = dx / length * distance / steps
        val sy = dy / length * distance / steps
        repeat(steps) {
            if (!blocked(stage, actor.x + sx, actor.y, ACTOR_RADIUS, bombs, actor.passBomb)) actor.x += sx
            if (!blocked(stage, actor.x, actor.y + sy, ACTOR_RADIUS, bombs, actor.passBomb)) actor.y += sy
            // Owner may walk OUT of a newly placed bomb, but cannot re-enter.
            val passing = actor.passBomb
            if (passing != null && bombs.none { it.id == passing && touchesBomb(it, actor.x, actor.y) }) {
                actor.passBomb = null
            }
        }
    }

    fun nearestFree(stage: StageData, x: Double, y: Double, radius: Double = 4.0): Spawn? {
        val bx = x.coerceIn(radius + 1, stage.width - radius - 2.0)
        val by = y.coerceIn(radius + 1, stage.height - radius - 2.0)
        if (!blocked(stage, bx, by, radius)) return Spawn(bx, by)
        val ox = floor(bx / CELL).toInt()
        val oy = floor(by / CELL).toInt()
        val limit = ceil(max(stage.width, stage.height) / CELL.toDouble()).toInt()
        for (r in 1..limit) for (dy in -r..r) for (dx in -r..r) {
            if (max(kotlin.math.abs(dx), kotlin.math.abs(dy)) != r) continue
            val px = (ox + dx) * CELL + CELL / 2.0
            val py = (oy + dy) * CELL + CELL / 2.0
            if (!blocked(stage, px, py, radius)) return Spawn(px, py)
        }
        return null
    }
}

/** LCG matches JS Math.imul(n, 1664525) + 1013904223 >>> 0. */
class SeededRng(seed: Long) {
    var state: Int = seed.toInt()
        private set

    fun nextUnsigned(): Long {
        state = state * 1664525 + 1013904223
        return state.toLong() and 0xffffffffL
    }
}

/** 60 Hz deterministic update independent of render frame rate. */
class FixedStepClock {
    companion object {
        const val STEP_SECONDS = 1.0 / 60.0
        const val MAX_FRAME_SECONDS = 0.25
    }

    private var accumulator = 0.0
    var ticks: Long = 0
        private set

    fun advance(deltaSeconds: Double, tick: () -> Unit): Int {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0) { "Invalid frame time" }
        accumulator += min(deltaSeconds, MAX_FRAME_SECONDS)
        var steps = 0
        while (accumulator + 1e-12 >= STEP_SECONDS && steps < 15) {
            tick()
            accumulator -= STEP_SECONDS
            steps++
            ticks++
        }
        return steps
    }
}
