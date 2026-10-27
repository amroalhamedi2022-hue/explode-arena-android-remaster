package com.explodearena.remaster.core

import kotlin.math.floor

/**
 * Provisional native single-opponent controller. Uses bounded 8px-grid BFS
 * against the same collision and live-flame model as the Kotlin simulation.
 * This is NOT a reconstruction of original Symbian AI or game modes.
 */
/**
 * Only a bomb with an automatic fuse is an owned-bomb escape target.
 * Isolated remote bombs have no deadline; a remote bomb reached by a timed
 * chain is still represented by [NativeBotThreatContext] and remains a threat.
 */
internal fun selectOwnedTimedBomb(
    bombs: List<NativeBombSimulation.Bomb>,
    owner: String,
): NativeBombSimulation.Bomb? =
    bombs.firstOrNull { it.owner == owner && !it.remote && it.remaining != null }

class NativeBotController(private val owner: String = "bot") {
    init { require(owner in setOf("bot", "bot2", "bot3")) }
    data class Decision(val dx: Double, val dy: Double, val placeBomb: Boolean)

    private var cachedDirection = 0
    private var lastPlanTick = Long.MIN_VALUE
    private var wasEscaping = false
    private var lastThreatKeys: List<NativeBotThreatContext.ThreatKey>? = null

    /** Last path-planning tick, exposed internally for deterministic scheduling regressions. */
    internal val lastPlannedTick: Long get() = lastPlanTick

    fun decide(
        stage: StageData,
        bot: Actor,
        target: Actor,
        simulation: NativeBombSimulation,
        tick: Long,
        otherActors: List<Actor> = emptyList(),
    ): Decision = decideInternal(stage, bot, target, simulation, tick, otherActors, null)

    /** Roster-only entry point: every bot in one tick shares the same read-only context. */
    internal fun decideWithContext(
        stage: StageData,
        bot: Actor,
        target: Actor,
        simulation: NativeBombSimulation,
        tick: Long,
        otherActors: List<Actor>,
        threatContext: NativeBotThreatContext,
    ): Decision = decideInternal(stage, bot, target, simulation, tick, otherActors, threatContext)

    private fun decideInternal(
        stage: StageData,
        bot: Actor,
        target: Actor,
        simulation: NativeBombSimulation,
        tick: Long,
        otherActors: List<Actor>,
        providedContext: NativeBotThreatContext?,
    ): Decision {
        require(bot.x.isFinite() && bot.y.isFinite() && target.x.isFinite() && target.y.isFinite())
        require(tick >= 0)
        // Standalone callers receive a fresh context; a roster supplies one
        // shared context computed before any bot plans or places a bomb.
        val threatContext = providedContext ?: NativeBotThreatContext.create(simulation)
        val imminentIndex = threatContext.imminentIndex
        // Compare threat identity/geometry, not countdown values: fuses change
        // every tick, but a newly appearing/disappearing chain threat must
        // invalidate the cached escape plan immediately.
        // The roster builds this canonical list once per simulation tick and
        // shares it with every controller, avoiding per-bot map/sort churn.
        val threatKeys = threatContext.threatKeys
        val threatSetChanged = lastThreatKeys != threatKeys
        // Keep expensive bounded escape/path searches at 10Hz while stable.
        // The live threat index and next-step safety check still run every tick,
        // so a newly unsafe cached step triggers an immediate replan.
        // Threat owners and timestamped cells come from the same prediction.
        // The roster shares that immutable prediction across all living bots.
        // Remote bombs are excluded: they have no countdown.
        val imminentBomb = threatContext.threatFootprints
            .filter { footprint ->
                footprint.cells.any { cell ->
                    NativeBombSimulation.blastHits(
                        cell, bot.x, bot.y, StageCollision.ACTOR_RADIUS,
                    )
                }
            }
            .minByOrNull { it.threat.secondsUntilBlast }?.threat?.bomb
        val ownBomb = selectOwnedTimedBomb(simulation.bombs, owner)
        val escapeBomb = ownBomb ?: imminentBomb
        // Standing in an active flame is an escape emergency even after the
        // detonating bomb has disappeared. Do not freeze until flame expiry.
        val escapingFlame = simulation.isBurning(bot.x, bot.y, StageCollision.ACTOR_RADIUS)
        val escaping = escapeBomb != null || escapingFlame
        val isImminent = { x: Double, y: Double ->
            imminentIndex.contains(x, y, StageCollision.ACTOR_RADIUS)
        }
        val canStep = { direction: Int ->
            val nx = bot.x + DIRECTION_X[direction] * SPEED * FixedStepClock.STEP_SECONDS
            val ny = bot.y + DIRECTION_Y[direction] * SPEED * FixedStepClock.STEP_SECONDS
            !StageCollision.blocked(stage, nx, ny, StageCollision.ACTOR_RADIUS,
                simulation.bombs, bot.passBomb) &&
                !StageCollision.blockedByActors(nx, ny, otherActors) &&
                (escapingFlame || !simulation.isBurning(nx, ny, StageCollision.ACTOR_RADIUS)) &&
                (escaping || !isImminent(nx, ny))
        }
        if (escaping != wasEscaping || threatSetChanged ||
            lastPlanTick == Long.MIN_VALUE || tick < lastPlanTick ||
            tick - lastPlanTick >= REPLAN_TICKS || !canStep(cachedDirection)) {
            // While our bomb is live, seek a refuge outside its predicted
            // blast instead of continuing to chase the player into the flame.
            cachedDirection = if (escaping) {
                NativeBotEscapeRoute.firstDirection(
                    stage, bot, simulation, escapeBomb?.x ?: bot.x, escapeBomb?.y ?: bot.y,
                    otherActors, allowBurningTransit = escapingFlame,
                    ignoreProposedBomb = escapeBomb == null,
                ) ?: 0
            } else plan(stage, bot, target, simulation, otherActors, imminentIndex)
            lastPlanTick = tick
            wasEscaping = escaping
            lastThreatKeys = threatKeys
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
        return Decision(DIRECTION_X[movement], DIRECTION_Y[movement], place)
    }

    private fun plan(
        stage: StageData,
        bot: Actor,
        target: Actor,
        simulation: NativeBombSimulation,
        otherActors: List<Actor>,
        imminentIndex: TimedBlastIndex,
    ): Int {
        val w = stage.gridWidth
        val h = stage.gridHeight
        val size = w.toLong() * h.toLong()
        // Avoid allocating unbounded arrays if a malformed stage slips through.
        if (size <= 0 || size > MAX_GRID_CELLS) return 0
        // The immutable index was built once for this context/tick, not once per bot BFS.
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
        while (head < tail) {
            val cell = queue[head++]
            val col = cell % w
            val row = cell / w
            for (i in 0..3) {
                val nc = col + GRID_COLS[i]
                val nr = row + GRID_ROWS[i]
                if (nc !in 0 until w || nr !in 0 until h) continue
                val index = nr * w + nc
                if (seen[index]) continue
                val x = stage.obstacleX + nc * StageCollision.CELL + StageCollision.CELL / 2.0
                val y = stage.obstacleY + nr * StageCollision.CELL + StageCollision.CELL / 2.0
                if (StageCollision.blocked(stage, x, y, StageCollision.ACTOR_RADIUS,
                        simulation.bombs, bot.passBomb) ||
                    StageCollision.blockedByActors(x, y, otherActors) ||
                    simulation.isBurning(x, y, StageCollision.ACTOR_RADIUS) ||
                    imminentIndex.contains(x, y, StageCollision.ACTOR_RADIUS)) continue
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

        // Shared direction tables avoid arrays per bot decision at 60 Hz.
        private val DIRECTION_X = doubleArrayOf(0.0, 1.0, 0.0, -1.0, 0.0)
        private val DIRECTION_Y = doubleArrayOf(0.0, 0.0, 1.0, 0.0, -1.0)
        private val GRID_COLS = intArrayOf(1, 0, -1, 0)
        private val GRID_ROWS = intArrayOf(0, 1, 0, -1)
    }
}
