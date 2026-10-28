package com.explodearena.remaster.core

import kotlin.math.floor

/**
 * Native port of the PROVISIONAL web/core.js bomb-cell and blast rules.
 * This is a deterministic Kotlin simulation slice, NOT evidence of Symbian
 * fuse timing, explosion range, bomb ownership or damage behavior.
 *
 * A caller advances this object exactly once per FixedStepClock tick (60 Hz).
 */
class NativeBombSimulation(
    stage: StageData,
    private val bombLimit: Int = 1,
    private val bombFuseSeconds: Double = 1.6,
    private val blastLifetimeSeconds: Double = 0.45,
    private val bombRange: Int = 3,
    initialBombId: Long = 1L,
) {
    // Own a deep snapshot: List<IntArray> is not deeply immutable, and callers
    // may retain/mutate either the list or its row arrays after construction.
    private val stage: StageData = stage.detachedCopy()

    data class Bomb(val id: Long, val owner: String, val x: Double, val y: Double, val remaining: Double?, val remote: Boolean = false)
    data class Flame(val cells: List<Spawn>, val remaining: Double)
    private val activeBombs = mutableListOf<Bomb>()
    private val activeFlames = mutableListOf<Flame>()
    // Flames removed at the end of the most recent tick still occupied their
    // cells during that tick's damage sample. Rendering only uses activeFlames.
    private var expiredFlamesDuringLastTick: List<Flame> = emptyList()
    private val remoteOwners = mutableSetOf<String>()
    private var nextId = initialBombId

    init {
        require(initialBombId > 0L) { "Invalid initial bomb id" }
        require(bombLimit in 1..8 && bombRange in 1..12)
        require(bombFuseSeconds.isFinite() && bombFuseSeconds in 0.1..20.0)
        require(blastLifetimeSeconds.isFinite() && blastLifetimeSeconds in 0.05..5.0)
    }

    /**
     * Fail closed when arena movement/collision and bomb blasts were given
     * different stage definitions. A matching stage ID alone is not enough:
     * custom/test stages can reuse an ID with a different collision grid.
     */
    internal fun requireStageDefinition(candidate: StageData) {
        val sameSpawns = stage.spawns.size == candidate.spawns.size &&
            stage.spawns.indices.all { index ->
                val expected = stage.spawns[index]
                val actual = candidate.spawns[index]
                expected.x == actual.x && expected.y == actual.y
            }
        require(
            stage.id == candidate.id &&
                stage.image == candidate.image &&
                stage.width == candidate.width &&
                stage.height == candidate.height &&
                stage.obstacleX == candidate.obstacleX &&
                stage.obstacleY == candidate.obstacleY &&
                sameSpawns &&
                stage.grid.size == candidate.grid.size &&
                stage.grid.indices.all { index ->
                    stage.grid[index].contentEquals(candidate.grid[index])
                }
        ) { "Bomb simulation stage does not match arena stage" }
    }

    val nextBombId: Long get() = nextId
    val remoteDetonatorOwners: List<String> get() = remoteOwners.sorted()
    val bombs: List<Bomb> get() = activeBombs.toList()
    val flames: List<Flame> get() = activeFlames.toList()

    /**
     * Remote power is only granted explicitly by a future verified pickup
     * handler or tests. Do not infer pickup identity from sprite colors.
     * The no-fuse rule and keypad 8 were reported from original Symbian;
     * blast/chain rules and multi-bomb targeting remain provisional.
     */
    fun setRemoteDetonator(owner: String, enabled: Boolean): Boolean {
        if (owner != "player" && owner != "player2") return false
        if (enabled) remoteOwners.add(owner) else remoteOwners.remove(owner)
        return true
    }

    fun hasRemoteDetonator(owner: String): Boolean = owner in remoteOwners

    /** Return false if owner has reached the limit or the bomb tile is blocked/occupied. */
    fun place(
        actor: Actor,
        owner: String = "player",
        otherActors: List<Actor> = emptyList(),
    ): Boolean {
        require(owner in setOf("player", "player2", "bot", "bot2", "bot3"))
        require(actor.x.isFinite() && actor.y.isFinite())
        require(otherActors.none { it === actor } &&
            otherActors.all { it.x.isFinite() && it.y.isFinite() }) {
            "Invalid bomb-placement actor blockers"
        }
        // Do not rely only on the snapped bomb cell: a caller can reach this
        // method outside the canonical tick (for example replay or AI tests),
        // and the actor's own 10px hitbox may already overlap terrain.
        // A passBomb exemption is valid only for the current owner.
        val graceBomb = actor.passBomb?.let { id -> activeBombs.firstOrNull { it.id == id } }
        if (graceBomb != null && graceBomb.owner != owner) return false
        if (StageCollision.blocked(
                stage, actor.x, actor.y, StageCollision.ACTOR_RADIUS,
                activeBombs, actor.passBomb,
            ) || StageCollision.blockedByActors(actor.x, actor.y, otherActors)) {
            return false
        }
        if (activeBombs.count { it.owner == owner } >= bombLimit) return false
        val x = cellCenter(actor.x)
        val y = cellCenter(actor.y)
        if (StageCollision.blocked(stage, x, y, 3.0) ||
            activeBombs.any { it.x == x && it.y == y }) return false
        // Reserve Long.MAX_VALUE as the exhausted sentinel: post-incrementing
        // it would wrap the next bomb identity to a negative value.
        check(nextId < Long.MAX_VALUE) { "Bomb ID counter exhausted" }
        val remote = owner in remoteOwners
        val bomb = Bomb(nextId, owner, x, y, if (remote) null else bombFuseSeconds, remote)
        // The bomb's 16px cell can overlap a nearby actor even when the placer
        // itself is clear. Refuse that placement before allocating an ID; this
        // keeps the next canonical tick from starting inside a foreign bomb.
        if (otherActors.any { StageCollision.touchesBomb(bomb, it.x, it.y) }) return false
        nextId++
        activeBombs.add(bomb)
        // Match provisional JS: the placing actor can leave its own bomb once.
        if (StageCollision.touchesBomb(bomb, actor.x, actor.y)) actor.passBomb = bomb.id
        return true
    }

    /** Advance fuses/flames one simulation tick and detonate chain reactions. */
    fun tick(): Int {
        // A bomb placed into an already burning cell must join the chain
        // reaction on the next simulation tick, even if it is a remote bomb
        // with no fuse. Sample BEFORE expiring flames: a flame still active
        // when the bomb was placed must not silently miss its final tick.
        // This is a native chain-reaction consistency rule; the exact
        // original Symbian timing still needs EKA2L1 verification.
        val ignitedIds = activeBombs.filter { bomb ->
            activeFlames.any { flame ->
                flame.cells.any { cell -> blastHits(cell, bomb.x, bomb.y, 0.0) }
            }
        }.map { it.id }
        val dt = FixedStepClock.STEP_SECONDS
        val decrementedFlames = activeFlames.map { it.copy(remaining = it.remaining - dt) }
        expiredFlamesDuringLastTick = decrementedFlames.filter { it.remaining <= 1e-10 }
        activeFlames.clear()
        activeFlames.addAll(decrementedFlames.filter { it.remaining > 1e-10 })
        val updated = activeBombs.map { bomb ->
            if (bomb.remote) bomb else bomb.copy(remaining = requireNotNull(bomb.remaining) - dt)
        }
        activeBombs.clear()
        activeBombs.addAll(updated)
        return detonate(ignitedIds + updated.filter { it.remaining != null && it.remaining <= 1e-10 }.map { it.id })
    }

    /**
     * Keypad 8 detonates ALL currently active remote bombs owned by this player.
     * This follows the user's original Symbian gameplay observation: remote
     * bombs detonate together, not one-by-one. Timed bombs placed before the
     * pickup remain timed, but may still explode via a normal chain reaction.
     * Exact explosion timing and range still require EKA2L1 verification.
     */
    fun detonateRemote(owner: String = "player"): Boolean {
        if (owner != "player" && owner != "player2") return false
        val ids = activeBombs.filter { it.owner == owner && it.remote }.map { it.id }
        if (ids.isEmpty()) return false
        return detonate(ids) >= 1
    }

    private fun detonate(ids: Collection<Long>): Int {
        val queue = java.util.ArrayDeque<Long>()
        ids.forEach { queue.addLast(it) }
        val detonated = mutableSetOf<Long>()
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (id in detonated) continue
            val bomb = activeBombs.firstOrNull { it.id == id } ?: continue
            detonated.add(id)
            val cells = explosionCells(bomb.x, bomb.y)
            activeFlames.add(Flame(cells, blastLifetimeSeconds))
            for (other in activeBombs) {
                if (other.id !in detonated && cells.any { blastHits(it, other.x, other.y, 0.0) }) {
                    queue.addLast(other.id)
                }
            }
        }
        if (detonated.isNotEmpty()) activeBombs.removeAll { it.id in detonated }
        return detonated.size
    }

    fun isBurning(x: Double, y: Double, radius: Double = 0.0): Boolean {
        require(x.isFinite() && y.isFinite() && radius.isFinite() && radius >= 0)
        return activeFlames.any { flame -> flame.cells.any { blastHits(it, x, y, radius) } }
    }

    /**
     * Damage-sample occupancy after tick(). Includes active flames and flames
     * whose lifetime reached zero at this tick boundary: those cells were
     * hazardous throughout the interval even though they are no longer drawn.
     * Call immediately after tick(), before advancing the next simulation tick.
     */
    fun isBurningDuringLastTick(
        x: Double, y: Double, radius: Double = 0.0,
    ): Boolean {
        require(x.isFinite() && y.isFinite() && radius.isFinite() && radius >= 0)
        return isBurning(x, y, radius) || expiredFlamesDuringLastTick.any { flame ->
            flame.cells.any { blastHits(it, x, y, radius) }
        }
    }

    /**
     * Predict the current provisional bomb footprint without placing a bomb.
     * Used by the native bot to look for a reachable refuge before planting.
     * This does not predict chain reactions or establish Symbian blast parity.
     */
    fun wouldBlastHitFrom(
        bombX: Double, bombY: Double, x: Double, y: Double,
        radius: Double = StageCollision.ACTOR_RADIUS,
    ): Boolean {
        require(bombX.isFinite() && bombY.isFinite() && x.isFinite() && y.isFinite())
        require(radius.isFinite() && radius >= 0.0)
        return explosionCells(cellCenter(bombX), cellCenter(bombY))
            .any { blastHits(it, x, y, radius) }
    }

    /**
     * Snapshot the predicted blast footprint of currently placed bombs for
     * bounded AI searches. Compute the rays once per plan, not once for every
     * BFS cell. This is conservative provisional behavior, not Symbian timing.
     */
    fun predictedActiveBlastCells(): List<Spawn> =
        activeBombs.flatMap { explosionCells(it.x, it.y) }

    /**
     * Predict automatic ignition deadlines from BOTH timed fuses and already
     * burning cells. tick() samples active flames before expiring them, so a
     * bomb placed into fire will ignite on the NEXT fixed tick even if the
     * flame has less than one tick of lifetime left. The same deadline then
     * propagates through transitive bomb-to-bomb chain reactions.
     *
     * Remote bombs without a fuse or flame contact remain unscheduled. Manual
     * remote detonation cannot be predicted. These are native provisional
     * rules, not verified Symbian EKA2L1 timings.
     */
    data class TimedChainThreat(val bomb: Bomb, val secondsUntilBlast: Double)

    fun predictedTimedChainThreats(withinSeconds: Double): List<TimedChainThreat> =
        predictTimedChainThreats(activeBombs, withinSeconds)

    /**
     * Predict a hypothetical timed placement without mutating simulation state.
     * This includes chain reactions between the proposed bomb and already
     * active bombs, including remote bombs ignited by a timed blast or flame.
     * It is a planning aid for the provisional native AI, not Symbian parity.
     */
    fun predictedPlacementTimedBlastCells(
        bombX: Double,
        bombY: Double,
        withinSeconds: Double,
    ): List<TimedBlastCell> {
        require(bombX.isFinite() && bombY.isFinite())
        require(withinSeconds.isFinite() && withinSeconds >= 0.0)
        val proposed = Bomb(
            id = Long.MIN_VALUE,
            owner = "bot",
            x = cellCenter(bombX),
            y = cellCenter(bombY),
            remaining = bombFuseSeconds,
            remote = false,
        )
        return predictTimedChainThreats(activeBombs + proposed, withinSeconds).flatMap { threat ->
            explosionCells(threat.bomb.x, threat.bomb.y).map { cell ->
                TimedBlastCell(cell, threat.secondsUntilBlast)
            }
        }
    }

    private fun predictTimedChainThreats(
        bombsToPredict: List<Bomb>,
        withinSeconds: Double,
    ): List<TimedChainThreat> {
        require(withinSeconds.isFinite() && withinSeconds >= 0.0)
        val earliest = mutableMapOf<Long, Double>()
        val pending = java.util.ArrayDeque<Bomb>()
        fun schedule(bomb: Bomb, deadline: Double) {
            if (deadline > withinSeconds) return
            val previous = earliest[bomb.id]
            if (previous == null || deadline < previous) {
                earliest[bomb.id] = deadline
                pending.addLast(bomb)
            }
        }
        for (bomb in bombsToPredict) {
            bomb.remaining?.let { schedule(bomb, it) }
            if (activeFlames.any { flame ->
                    flame.cells.any { cell -> blastHits(cell, bomb.x, bomb.y, 0.0) }
                }) {
                schedule(bomb, FixedStepClock.STEP_SECONDS)
            }
        }
        while (pending.isNotEmpty()) {
            val bomb = pending.removeFirst()
            val deadline = earliest.getValue(bomb.id)
            val footprint = explosionCells(bomb.x, bomb.y)
            for (other in bombsToPredict) {
                if (other.id == bomb.id) continue
                if (footprint.any { blastHits(it, other.x, other.y, 0.0) }) {
                    schedule(other, deadline)
                }
            }
        }
        return bombsToPredict.mapNotNull { bomb ->
            earliest[bomb.id]?.let { TimedChainThreat(bomb, it) }
        }
    }

    /**
     * Timestamped blast footprint for time-aware path planning. Keep the
     * deadline attached to each cell: a route may cross a distant future
     * blast safely, but must not enter one after its ignition deadline.
     * Ray geometry is computed once per bomb, never inside the BFS loop.
     * These are provisional native timings, not Symbian parity evidence.
     */
    data class TimedBlastCell(val cell: Spawn, val secondsUntilBlast: Double)

    /** The ray geometry for one predicted threat, calculated once per snapshot. */
    data class TimedThreatFootprint(
        val threat: TimedChainThreat,
        val cells: List<Spawn>,
    )

    /**
     * One immutable view of a timed-threat calculation. Bot decisions need
     * the threat list, per-threat ray geometry and deduplicated timestamped
     * cells. All are derived in one chain-graph walk and one ray calculation
     * per threat, then shared by the roster for this tick.
     */
    data class TimedThreatSnapshot(
        val threats: List<TimedChainThreat>,
        val cells: List<TimedBlastCell>,
        val threatFootprints: List<TimedThreatFootprint> = emptyList(),
    )

    fun predictedTimedThreatSnapshot(withinSeconds: Double): TimedThreatSnapshot {
        val threats = predictTimedChainThreats(activeBombs, withinSeconds)
        // Calculate each bomb's ray geometry once, then reuse it both for the
        // deduplicated spatial index input and each bot's escape-threat lookup.
        val footprints = threats.map { threat ->
            TimedThreatFootprint(threat, explosionCells(threat.bomb.x, threat.bomb.y).toList())
        }
        val earliestByCell = linkedMapOf<Spawn, Double>()
        for (footprint in footprints) {
            for (cell in footprint.cells) {
                val previous = earliestByCell[cell]
                if (previous == null || footprint.threat.secondsUntilBlast < previous) {
                    earliestByCell[cell] = footprint.threat.secondsUntilBlast
                }
            }
        }
        val cells = earliestByCell.map { (cell, deadline) ->
            TimedBlastCell(cell, deadline)
        }
        return TimedThreatSnapshot(threats, cells, footprints)
    }

    fun predictedTimedBlastCells(withinSeconds: Double): List<TimedBlastCell> =
        predictedTimedChainThreats(withinSeconds).flatMap { threat ->
            explosionCells(threat.bomb.x, threat.bomb.y).map { cell ->
                TimedBlastCell(cell, threat.secondsUntilBlast)
            }
        }

    /**
     * Imminent automatic explosions INCLUDING active-flame ignition and
     * transitive bomb chains. An isolated remote bomb has no countdown.
     * Active flames themselves are checked separately by the caller.
     */
    fun predictedImminentTimedBlastCells(withinSeconds: Double): List<Spawn> =
        predictedTimedBlastCells(withinSeconds).map { it.cell }

    private fun explosionCells(x: Double, y: Double): List<Spawn> {
        val cx = floor(x / BOMB_CELL).toInt()
        val cy = floor(y / BOMB_CELL).toInt()
        val hit = mutableListOf(Spawn(cx * BOMB_CELL + 8.0, cy * BOMB_CELL + 8.0))
        for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            for (n in 1..bombRange) {
                val px = (cx + dx * n) * BOMB_CELL + 8.0
                val py = (cy + dy * n) * BOMB_CELL + 8.0
                if (px < 0 || py < 0 || px >= stage.width || py >= stage.height) break
                // A 16px blast step can skip an 8px obstacle when the stage
                // grid has an offset. Sample the entire ray segment rather
                // than just its endpoint, so flames cannot tunnel through
                // narrow walls. Symbian blast parity remains unverified.
                val startX = (cx + dx * (n - 1)) * BOMB_CELL + 8.0
                val startY = (cy + dy * (n - 1)) * BOMB_CELL + 8.0
                val pathBlocked = (1..4).any { substep ->
                    val fraction = substep / 4.0
                    val sx = startX + (px - startX) * fraction
                    val sy = startY + (py - startY) * fraction
                    StageCollision.blocked(stage, sx, sy, 3.0)
                }
                if (pathBlocked) break
                hit.add(Spawn(px, py))
            }
        }
        return hit
    }

    companion object {
        const val BOMB_CELL = 16
        fun cellCenter(value: Double): Double = floor(value / BOMB_CELL) * BOMB_CELL + BOMB_CELL / 2.0
        fun blastHits(cell: Spawn, x: Double, y: Double, radius: Double = 7.0): Boolean =
            kotlin.math.abs(cell.x - x) < 8 + radius && kotlin.math.abs(cell.y - y) < 8 + radius
    }
}
