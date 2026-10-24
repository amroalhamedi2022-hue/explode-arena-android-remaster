package com.explodearena.remaster.core

import kotlin.math.floor

/**
 * Provisional native single-opponent controller. Uses bounded 8px-grid BFS
 * against the same collision and live-flame model as the Kotlin simulation.
 * This is NOT a reconstruction of original Symbian AI or game modes.
 */
class NativeBotController {
    data class Decision(val dx: Double, val dy: Double, val placeBomb: Boolean)

    private var cachedDirection = 0
    private var lastPlanTick = Long.MIN_VALUE

    fun decide(
        stage: StageData,
        bot: Actor,
        target: Actor,
        simulation: NativeBombSimulation,
        tick: Long,
    ): Decision {
        require(bot.x.isFinite() && bot.y.isFinite() && target.x.isFinite() && target.y.isFinite())
        require(tick >= 0)
        // Replan at 10Hz, immediately if the cached move becomes unsafe.
        val dx = doubleArrayOf(0.0, 1.0, 0.0, -1.0, 0.0)
        val dy = doubleArrayOf(0.0, 0.0, 1.0, 0.0, -1.0)
        val canStep = { direction: Int ->
            val nx = bot.x + dx[direction] * SPEED * FixedStepClock.STEP_SECONDS
            val ny = bot.y + dy[direction] * SPEED * FixedStepClock.STEP_SECONDS
            !StageCollision.blocked(stage, nx, ny, StageCollision.ACTOR_RADIUS,
                simulation.bombs, bot.passBomb) &&
                !simulation.isBurning(nx, ny, StageCollision.ACTOR_RADIUS)
        }
        if (lastPlanTick == Long.MIN_VALUE || tick < lastPlanTick ||
            tick - lastPlanTick >= REPLAN_TICKS || !canStep(cachedDirection)) {
            cachedDirection = plan(stage, bot, target, simulation)
            lastPlanTick = tick
        }
        if (!canStep(cachedDirection)) cachedDirection = 0
        val near = kotlin.math.abs(bot.x - target.x) <= 20.0 &&
            kotlin.math.abs(bot.y - target.y) <= 20.0
        val canEscape = (1..4).any { canStep(it) }
        val place = near && canEscape && bot.passBomb == null &&
            simulation.bombs.none { it.owner == "bot" } &&
            !simulation.isBurning(bot.x, bot.y, StageCollision.ACTOR_RADIUS)
        return Decision(dx[cachedDirection], dy[cachedDirection], place)
    }

    private fun plan(
        stage: StageData,
        bot: Actor,
        target: Actor,
        simulation: NativeBombSimulation,
    ): Int {
        val w = stage.gridWidth
        val h = stage.gridHeight
        val size = w.toLong() * h.toLong()
        // Avoid allocating unbounded arrays if a malformed stage slips through.
        if (size <= 0 || size > MAX_GRID_CELLS) return 0
        val startCol = floor((bot.x - stage.obstacleX) / StageCollision.CELL).toInt()
        val startRow = floor((bot.y - stage.obstacleY) / StageCollision.CELL).toInt()
        if (startCol !in 0 until w || startRow !in 0 until h) return 0
        val start = startRow * w + startCol
        val seen = BooleanArray(size.toInt())
        val first = IntArray(size.toInt())
        val queue = IntArray(size.toInt())
        var head = 0
        var tail = 1
        queue[0] = start
        seen[start] = true
        var best = start
        var bestDistance = distanceSq(bot.x, bot.y, target.x, target.y)
        // Deterministic tie-break: right, down, left, up.
        val cols = intArrayOf(1, 0, -1, 0)
        val rows = intArrayOf(0, 1, 0, -1)
        while (head < tail) {
            val cell = queue[head++]
            val col = cell % w
            val row = cell / w
            for (i in 0..3) {
                val nc = col + cols[i]
                val nr = row + rows[i]
                if (nc !in 0 until w || nr !in 0 until h) continue
                val index = nr * w + nc
                if (seen[index]) continue
                val x = stage.obstacleX + nc * StageCollision.CELL + StageCollision.CELL / 2.0
                val y = stage.obstacleY + nr * StageCollision.CELL + StageCollision.CELL / 2.0
                if (StageCollision.blocked(stage, x, y, StageCollision.ACTOR_RADIUS,
                        simulation.bombs, bot.passBomb) ||
                    simulation.isBurning(x, y, StageCollision.ACTOR_RADIUS)) continue
                seen[index] = true
                first[index] = if (cell == start) i + 1 else first[cell]
                queue[tail++] = index
                val distance = distanceSq(x, y, target.x, target.y)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = index
                }
            }
        }
        return first[best]
    }

    private fun distanceSq(x: Double, y: Double, tx: Double, ty: Double): Double =
        (x - tx) * (x - tx) + (y - ty) * (y - ty)

    companion object {
        const val SPEED = 54.0
        private const val REPLAN_TICKS = 6L
        private const val MAX_GRID_CELLS = 20_000L
    }
}
