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

    private const val FLAME_CELL_STEP = 16.0

    /**
     * Active flame transit is limited to the connected footprint containing
     * the bot at escape start. A blanket burning exemption can otherwise
     * send a bot through a second, unrelated explosion while fleeing the first.
     *
     * Flame cells are aligned to the 16px bomb grid. Actor-hit footprints
     * overlap for neighboring cells (including diagonals), so a bounded
     * 8-neighbor flood fill finds the continuous danger region in O(cells).
     */
    internal fun connectedBurningEscapeCells(
        simulation: NativeBombSimulation,
        startX: Double,
        startY: Double,
    ): Set<Spawn> {
        if (!startX.isFinite() || !startY.isFinite()) return emptySet()
        val cells = simulation.flames.flatMap { it.cells }.toSet()
        if (cells.isEmpty()) return emptySet()

        val component = LinkedHashSet<Spawn>()
        val pending = java.util.ArrayDeque<Spawn>()
        for (cell in cells) {
            if (NativeBombSimulation.blastHits(
                    cell, startX, startY, StageCollision.ACTOR_RADIUS,
                ) && component.add(cell)
            ) pending.addLast(cell)
        }
        while (pending.isNotEmpty()) {
            val cell = pending.removeFirst()
            for (dx in -1..1) for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val neighbor = Spawn(
                    cell.x + dx * FLAME_CELL_STEP,
                    cell.y + dy * FLAME_CELL_STEP,
                )
                if (neighbor in cells && component.add(neighbor)) pending.addLast(neighbor)
            }
        }
        return component
    }

    internal fun isWithinBurningEscapeComponent(
        component: Set<Spawn>,
        x: Double,
        y: Double,
    ): Boolean = x.isFinite() && y.isFinite() && component.any {
        NativeBombSimulation.blastHits(it, x, y, StageCollision.ACTOR_RADIUS)
    }


    /**
     * Returns the first movement direction (1=right, 2=down, 3=left, 4=up)
     * toward a reachable cell outside the predicted blast, 0 if already safe,
     * or null when no bounded refuge is found. A null result forbids planting.
     */
    // While escaping a flame already underfoot, the bot must be allowed to
    // traverse burning intermediate points; only the destination is a refuge.
    // ignoreProposedBomb is for flame-only escapes with no hypothetical bomb.
    fun firstDirection(
        stage: StageData,
        bot: Actor,
        simulation: NativeBombSimulation,
        bombX: Double,
        bombY: Double,
        otherActors: List<Actor> = emptyList(),
        allowBurningTransit: Boolean = false,
        ignoreProposedBomb: Boolean = false,
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
        // A bot may cross the connected fire region it is already standing in,
        // but must not treat an unrelated flame as a permissible transit lane.
        val escapeFlameComponent = if (allowBurningTransit) {
            connectedBurningEscapeCells(simulation, bot.x, bot.y)
        } else emptySet()
        val burningBlocks = { x: Double, y: Double ->
            simulation.isBurning(x, y, StageCollision.ACTOR_RADIUS) &&
                !(allowBurningTransit &&
                    isWithinBurningEscapeComponent(escapeFlameComponent, x, y))
        }
        // An opponent's timed bomb can explode sooner than the proposed or
        // owned bomb. When its ray already covers the bot, use the EARLIEST
        // relevant fuse as the travel deadline; otherwise BFS may claim a
        // refuge reachable only after the opponent's explosion. Distant timed
        // bombs do not constrain this escape, and remote bombs have no fuse.
        // This is conservative provisional AI, not Symbian timing evidence.
        // Cache deadlines AND footprints. Checking only whether the bot
        // starts inside a ray misses routes that cross a different, soon-to-
        // explode ray on the way to an otherwise safe endpoint.
        val proposedCellX = NativeBombSimulation.cellCenter(bombX)
        val proposedCellY = NativeBombSimulation.cellCenter(bombY)
        val alreadyPlacedAtProposal = simulation.bombs.any {
            it.x == proposedCellX && it.y == proposedCellY
        }
        // Before a bot places a bomb, include that hypothetical bomb's own
        // fuse and any chain it would trigger. During escape from an already
        // placed bomb, keep its real remaining fuse instead of resetting it.
        val timedCells = if (!ignoreProposedBomb && !alreadyPlacedAtProposal) {
            simulation.predictedPlacementTimedBlastCells(bombX, bombY, 20.0)
        } else {
            simulation.predictedTimedBlastCells(20.0)
        }
        val earliestThreatFuse = timedCells.asSequence()
            .filter { NativeBombSimulation.blastHits(
                it.cell, bot.x, bot.y, StageCollision.ACTOR_RADIUS,
            ) }
            .map { it.secondsUntilBlast }
            .minOrNull()
        val entersBlastTooLate = { x: Double, y: Double, travelSeconds: Double ->
            timedCells.any { threat ->
                travelSeconds + FUSE_MARGIN_SECONDS >= threat.secondsUntilBlast &&
                    NativeBombSimulation.blastHits(
                        threat.cell, x, y, StageCollision.ACTOR_RADIUS,
                    )
            }
        }
        val travelBudget = earliestThreatFuse?.let { remaining ->
            (remaining - FUSE_MARGIN_SECONDS).coerceAtLeast(0.0) * NativeBotController.SPEED
        }
        val threatenedByOtherBomb = { x: Double, y: Double ->
            activeThreats.any { cell ->
                NativeBombSimulation.blastHits(cell, x, y, StageCollision.ACTOR_RADIUS)
            }
        }
        if ((ignoreProposedBomb || !simulation.wouldBlastHitFrom(bombX, bombY, bot.x, bot.y)) &&
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
                // Every traversed cell must be reachable before its own
                // explosion deadline, not merely the final refuge cell.
                if (entersBlastTooLate(x, y, distance / NativeBotController.SPEED)) continue
                if (StageCollision.blocked(stage, x, y, StageCollision.ACTOR_RADIUS,
                        simulation.bombs, bot.passBomb) ||
                    StageCollision.blockedByActors(x, y, otherActors) ||
                    burningBlocks(x, y)) continue
                // Cell-center BFS can otherwise propose a first step that is
                // physically blocked from the bot's current sub-cell position.
                if (cell == start) {
                    val stepX = bot.x + dc[direction] * NativeBotController.SPEED * FixedStepClock.STEP_SECONDS
                    val stepY = bot.y + dr[direction] * NativeBotController.SPEED * FixedStepClock.STEP_SECONDS
                    if (entersBlastTooLate(stepX, stepY, FixedStepClock.STEP_SECONDS)) continue
                    if (StageCollision.blocked(stage, stepX, stepY, StageCollision.ACTOR_RADIUS,
                            simulation.bombs, bot.passBomb) ||
                        StageCollision.blockedByActors(stepX, stepY, otherActors) ||
                        burningBlocks(stepX, stepY)) continue
                }
                seen[next] = true
                first[next] = if (cell == start) direction + 1 else first[cell]
                depth[next] = depth[cell] + 1
                travelDistance[next] = distance
                if ((ignoreProposedBomb || !simulation.wouldBlastHitFrom(bombX, bombY, x, y)) &&
                    !threatenedByOtherBomb(x, y) &&
                    !simulation.isBurning(x, y, StageCollision.ACTOR_RADIUS)) {
                    return first[next]
                }
                queue[tail++] = next
            }
        }
        return null
    }
}
