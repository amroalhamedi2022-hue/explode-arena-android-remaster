package com.explodearena.remaster.core

import kotlin.math.floor

/**
 * Provisional native single-opponent controller. Uses bounded 8px-grid BFS
 * against the same collision and live-flame model as the Kotlin simulation.
 * This is NOT a reconstruction of original Symbian AI or game modes.
 */
class NativeBotController(private val owner: String = "bot") {
    init { require(owner in setOf("bot", "bot2", "bot3")) }
    data class Decision(val dx: Double, val dy: Double, val placeBomb: Boolean)

    private var cachedDirection = 0
    private var lastPlanTick = Long.MIN_VALUE
    private var wasEscaping = false

    fun decide(
        stage: StageData,
        bot: Actor,
        target: Actor,
        simulation: NativeBombSimulation,
        tick: Long,
        otherActors: List<Actor> = emptyList(),
    ): Decision {
        require(bot.x.isFinite() && bot.y.isFinite() && target.x.isFinite() && target.y.isFinite())
        require(tick >= 0)
        // Replan at 10Hz, immediately if the cached move becomes unsafe.
        val dx = doubleArrayOf(0.0, 1.0, 0.0, -1.0, 0.0)
        val dy = doubleArrayOf(0.0, 0.0, 1.0, 0.0, -1.0)
        // A bot that is already inside an imminent timed ray must seek a
        // refuge; a bot outside it must not chase the player INTO that ray.
        // Snapshot rays once per decision, not for every BFS candidate.
        // Remote bombs are excluded: they have no countdown.
        val imminentThreats = simulation.predictedTimedChainThreats(IMMINENT_FUSE_SECONDS)
        val imminentCells = simulation.predictedImminentTimedBlastCells(IMMINENT_FUSE_SECONDS)
        val imminentBomb = imminentThreats
            .filter { simulation.wouldBlastHitFrom(it.bomb.x, it.bomb.y, bot.x, bot.y) }
            .minByOrNull { it.secondsUntilBlast }?.bomb
        val ownBomb = simulation.bombs.firstOrNull { it.owner == owner }
        val escapeBomb = ownBomb ?: imminentBomb
        val escaping = escapeBomb != null
        val isImminent = { x: Double, y: Double ->
            imminentCells.any { NativeBombSimulation.blastHits(
                it, x, y, StageCollision.ACTOR_RADIUS,
            ) }
        }
        val canStep = { direction: Int ->
            val nx = bot.x + dx[direction] * SPEED * FixedStepClock.STEP_SECONDS
            val ny = bot.y + dy[direction] * SPEED * FixedStepClock.STEP_SECONDS
            !StageCollision.blocked(stage, nx, ny, StageCollision.ACTOR_RADIUS,
                simulation.bombs, bot.passBomb) &&
                !StageCollision.blockedByActors(nx, ny, otherActors) &&
                !simulation.isBurning(nx, ny, StageCollision.ACTOR_RADIUS) &&
                (escaping || !isImminent(nx, ny))
        }
        if (escaping || escaping != wasEscaping ||
            lastPlanTick == Long.MIN_VALUE || tick < lastPlanTick ||
            tick - lastPlanTick >= REPLAN_TICKS || !canStep(cachedDirection)) {
            // While our bomb is live, seek a refuge outside its predicted
            // blast instead of continuing to chase the player into the flame.
            cachedDirection = if (escapeBomb != null) {
                NativeBotEscapeRoute.firstDirection(
                    stage, bot, simulation, escapeBomb.x, escapeBomb.y, otherActors,
                ) ?: 0
            } else plan(stage, bot, target, simulation, otherActors, imminentCells)
            lastPlanTick = tick
            wasEscaping = escaping
        }
        if (!canStep(cachedDirection)) cachedDirection = 0
        val near = kotlin.math.abs(bot.x - target.x) <= 20.0 &&
            kotlin.math.abs(bot.y - target.y) <= 20.0
        // An immediate open neighbor is NOT proof of a viable escape: a
        // corridor can have no refuge outside the bomb's actual ray footprint.
        val refuge = if (near && !escaping && bot.passBomb == null) {
            NativeBotEscapeRoute.firstDirection(stage, bot, simulation, bot.x, bot.y, otherActors)
        } else null
        val place = near && refuge != null && refuge != 0 && bot.passBomb == null &&
            !escaping && canStep(refuge) &&
            !simulation.isBurning(bot.x, bot.y, StageCollision.ACTOR_RADIUS)
        // A bomb must be followed by the escape plan immediately. Returning
        // the cached chase direction here spent the first fuse tick moving
        // toward the target, despite having just verified a different refuge.
        val movement = if (place) requireNotNull(refuge) else cachedDirection
        return Decision(dx[movement], dy[movement], place)
    }

    private fun plan(
        stage: StageData,
        bot: Actor,
        target: Actor,
        simulation: NativeBombSimulation,
        otherActors: List<Actor>,
        imminentCells: List<Spawn>,
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
                    StageCollision.blockedByActors(x, y, otherActors) ||
                    simulation.isBurning(x, y, StageCollision.ACTOR_RADIUS) ||
                    imminentCells.any { NativeBombSimulation.blastHits(
                        it, x, y, StageCollision.ACTOR_RADIUS,
                    ) }) continue
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
        // Conservative native AI heuristic: evaluate danger within 45 ticks.
        // Not a measured Symbian AI reaction window.
        private const val IMMINENT_FUSE_SECONDS = 0.75
        private const val MAX_GRID_CELLS = 20_000L
    }
}
