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
        if (!x.isFinite() || !y.isFinite()) return -1
        val col = floor((x - stage.obstacleX) / CELL).toInt()
        val row = floor((y - stage.obstacleY) / CELL).toInt()
        if (col !in 0 until stage.gridWidth || row !in 0 until stage.gridHeight) return -1
        return stage.grid[row][col]
    }

    fun blocked(
        stage: StageData, x: Double, y: Double, radius: Double = ACTOR_RADIUS,
        bombs: List<NativeBombSimulation.Bomb> = emptyList(), ignoreBombId: Long? = null,
    ): Boolean {
        // Invalid coordinates must never be interpreted as a traversable
        // tile (floor(NaN).toInt() otherwise silently aliases tile zero).
        if (!x.isFinite() || !y.isFinite() || !radius.isFinite() || radius < 0) return true
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
        require(speed.isFinite() && dt.isFinite() && speed >= 0.0 && dt >= 0.0 && dx.isFinite() && dy.isFinite())
        require(actor.x.isFinite() && actor.y.isFinite()) { "Invalid actor position" }
        val length = hypot(dx, dy)
        require(length.isFinite()) { "Movement direction magnitude overflow" }
        // Validate the full request before changing even transient actor state.
        // Clear stale grace when standing still after a bomb detonates.
        val prior = actor.passBomb
        if (prior != null && bombs.none { it.id == prior && touchesBomb(it, actor.x, actor.y) }) {
            actor.passBomb = null
        }
        if (length == 0.0 || dt == 0.0) return
        val distance = speed * dt
        // A malformed replay/input must not overflow the step count or hang
        // the render thread in billions of collision iterations. Normal
        // 60 Hz movement is unchanged; the bound is relative to arena size.
        val maxTravel = max(stage.width, stage.height).toDouble() * 2.0
        require(distance.isFinite() && distance <= maxTravel) { "Movement exceeds safe arena bounds" }
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
        if (!x.isFinite() || !y.isFinite() || !radius.isFinite() || radius < 0.0 ||
            stage.width <= radius * 2 + 3 || stage.height <= radius * 2 + 3) return null
        val bx = x.coerceIn(radius + 1, stage.width - radius - 2.0)
        val by = y.coerceIn(radius + 1, stage.height - radius - 2.0)
        if (!blocked(stage, bx, by, radius)) return Spawn(bx, by)
        // The obstacle grid is not necessarily aligned to world origin.
        // Search its actual cell centers, including half-cell stage offsets.
        // A world-aligned search could miss the only walkable spawn cell.
        val ox = floor((bx - stage.obstacleX) / CELL).toInt()
        val oy = floor((by - stage.obstacleY) / CELL).toInt()
        val candidates = ArrayList<Pair<Int, Int>>(stage.gridWidth * stage.gridHeight)
        for (row in 0 until stage.gridHeight) for (col in 0 until stage.gridWidth) {
            candidates.add(col to row)
        }
        candidates.sortWith(compareBy<Pair<Int, Int>> {
            max(kotlin.math.abs(it.first.toLong() - ox), kotlin.math.abs(it.second.toLong() - oy))
        }.thenBy { it.second }.thenBy { it.first })
        for ((col, row) in candidates) {
            if (stage.grid[row][col] != 0) continue
            val px = stage.obstacleX + col * CELL + CELL / 2.0
            val py = stage.obstacleY + row * CELL + CELL / 2.0
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
