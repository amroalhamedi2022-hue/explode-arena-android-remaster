package com.explodearena.remaster.core

import kotlin.math.floor
import kotlin.math.abs

/**
 * Bounded deterministic escape search for the PROVISIONAL native bot.
 * It uses the native bomb ray/wall model, not the HTML/WebView renderer.
 * Fuse, blast and movement parity with Symbian/EKA2L1 remain unverified.
 */
internal object NativeBotEscapeRoute {
    private const val MAX_CELLS = 20_000L
    // 8 cells at 8px each take about 1.19s at 54px/s, leaving a conservative
    // margin before the provisional 1.6s fuse (not a guarantee under obstacles).
    private const val MAX_STEPS = 8
    // Reserve nine ticks for turning, sub-cell alignment and tick ordering.
    // This is a conservative provisional safety margin, not Symbian timing.
    private const val FUSE_MARGIN_SECONDS = 0.15
    private val dc = intArrayOf(1, 0, -1, 0)
    private val dr = intArrayOf(0, 1, 0, -1)

    /**
     * Returns the first movement direction (1=right, 2=down, 3=left, 4=up)
     * toward a reachable cell outside the predicted blast, 0 if already safe,
     * or null when no bounded refuge is found. A null result forbids planting.
     */
    fun firstDirection(
        stage: StageData,
        bot: Actor,
        simulation: NativeBombSimulation,
        bombX: Double,
        bombY: Double,
        otherActors: List<Actor> = emptyList(),
    ): Int? {
        val width = stage.gridWidth
        val height = stage.gridHeight
        val cellCount = width.toLong() * height
        if (cellCount <= 0 || cellCount > MAX_CELLS ||
            !bot.x.isFinite() || !bot.y.isFinite() ||
            !bombX.isFinite() || !bombY.isFinite()) return null
        // A refuge outside our own blast can still be inside another active
        // bomb's ray. Snapshot all active footprints once to avoid recomputing
        // obstacle-ray traversal for every visited BFS cell.
        val activeThreats = simulation.predictedActiveBlastCells()
        // An opponent's timed bomb can explode sooner than the proposed or
        // owned bomb. When its ray already covers the bot, use the EARLIEST
        // relevant fuse as the travel deadline; otherwise BFS may claim a
        // refuge reachable only after the opponent's explosion. Distant timed
        // bombs do not constrain this escape, and remote bombs have no fuse.
        // This is conservative provisional AI, not Symbian timing evidence.
        val earliestThreatFuse = simulation.predictedTimedChainThreats(20.0).asSequence()
            .filter { simulation.wouldBlastHitFrom(
                it.bomb.x, it.bomb.y, bot.x, bot.y,
            ) }
            .map { it.secondsUntilBlast }
            .minOrNull()
        val travelBudget = earliestThreatFuse?.let { remaining ->
            (remaining - FUSE_MARGIN_SECONDS).coerceAtLeast(0.0) * NativeBotController.SPEED
        }
        val threatenedByOtherBomb = { x: Double, y: Double ->
            activeThreats.any { cell ->
                NativeBombSimulation.blastHits(cell, x, y, StageCollision.ACTOR_RADIUS)
            }
        }
        if (!simulation.wouldBlastHitFrom(bombX, bombY, bot.x, bot.y) &&
            !threatenedByOtherBomb(bot.x, bot.y) &&
            !simulation.isBurning(bot.x, bot.y, StageCollision.ACTOR_RADIUS)) return 0

        val startCol = floor((bot.x - stage.obstacleX) / StageCollision.CELL).toInt()
        val startRow = floor((bot.y - stage.obstacleY) / StageCollision.CELL).toInt()
        if (startCol !in 0 until width || startRow !in 0 until height) return null

        val seen = BooleanArray(cellCount.toInt())
        val first = IntArray(cellCount.toInt())
        val depth = IntArray(cellCount.toInt())
        val travelDistance = DoubleArray(cellCount.toInt())
        val queue = IntArray(cellCount.toInt())
        val start = startRow * width + startCol
        var head = 0
        var tail = 1
        queue[0] = start
        seen[start] = true

        while (head < tail) {
            val cell = queue[head++]
            if (depth[cell] >= MAX_STEPS) continue
            val col = cell % width
            val row = cell / width
            for (direction in 0..3) {
                val nc = col + dc[direction]
                val nr = row + dr[direction]
                if (nc !in 0 until width || nr !in 0 until height) continue
                val next = nr * width + nc
                if (seen[next]) continue
                val x = stage.obstacleX + nc * StageCollision.CELL + StageCollision.CELL / 2.0
                val y = stage.obstacleY + nr * StageCollision.CELL + StageCollision.CELL / 2.0
                // The bot moves on cardinal axes; Manhattan distance is a
                // conservative travel-time bound for its sub-cell offset.
                val distance = travelDistance[cell] + if (cell == start) {
                    abs(x - bot.x) + abs(y - bot.y)
                } else StageCollision.CELL.toDouble()
                if (travelBudget != null && distance > travelBudget) continue
                if (StageCollision.blocked(stage, x, y, StageCollision.ACTOR_RADIUS,
                        simulation.bombs, bot.passBomb) ||
                    StageCollision.blockedByActors(x, y, otherActors) ||
                    simulation.isBurning(x, y, StageCollision.ACTOR_RADIUS)) continue
                // Cell-center BFS can otherwise propose a first step that is
                // physically blocked from the bot's current sub-cell position.
                if (cell == start) {
                    val stepX = bot.x + dc[direction] * NativeBotController.SPEED * FixedStepClock.STEP_SECONDS
                    val stepY = bot.y + dr[direction] * NativeBotController.SPEED * FixedStepClock.STEP_SECONDS
                    if (StageCollision.blocked(stage, stepX, stepY, StageCollision.ACTOR_RADIUS,
                            simulation.bombs, bot.passBomb) ||
                        StageCollision.blockedByActors(stepX, stepY, otherActors) ||
                        simulation.isBurning(stepX, stepY, StageCollision.ACTOR_RADIUS)) continue
                }
                seen[next] = true
                first[next] = if (cell == start) direction + 1 else first[cell]
                depth[next] = depth[cell] + 1
                travelDistance[next] = distance
                if (!simulation.wouldBlastHitFrom(bombX, bombY, x, y) &&
                    !threatenedByOtherBomb(x, y)) {
                    return first[next]
                }
                queue[tail++] = next
            }
        }
        return null
    }
}
