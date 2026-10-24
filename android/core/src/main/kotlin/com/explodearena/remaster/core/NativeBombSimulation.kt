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
    private val stage: StageData,
    private val bombLimit: Int = 1,
    private val bombFuseSeconds: Double = 1.6,
    private val blastLifetimeSeconds: Double = 0.45,
    private val bombRange: Int = 3,
) {
    data class Bomb(val id: Long, val owner: String, val x: Double, val y: Double, val remaining: Double?, val remote: Boolean = false)
    data class Flame(val cells: List<Spawn>, val remaining: Double)
    private val activeBombs = mutableListOf<Bomb>()
    private val activeFlames = mutableListOf<Flame>()
    private val remoteOwners = mutableSetOf<String>()
    private var nextId = 1L

    init {
        require(bombLimit in 1..8 && bombRange in 1..12)
        require(bombFuseSeconds.isFinite() && bombFuseSeconds in 0.1..20.0)
        require(blastLifetimeSeconds.isFinite() && blastLifetimeSeconds in 0.05..5.0)
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
    fun place(actor: Actor, owner: String = "player"): Boolean {
        require(owner == "player" || owner == "player2" || owner == "bot")
        require(actor.x.isFinite() && actor.y.isFinite())
        if (activeBombs.count { it.owner == owner } >= bombLimit) return false
        val x = cellCenter(actor.x)
        val y = cellCenter(actor.y)
        if (StageCollision.blocked(stage, x, y, 3.0) ||
            activeBombs.any { it.x == x && it.y == y }) return false
        val remote = owner in remoteOwners
        val bomb = Bomb(nextId++, owner, x, y, if (remote) null else bombFuseSeconds, remote)
        activeBombs.add(bomb)
        // Match provisional JS: the placing actor can leave its own bomb once.
        if (StageCollision.touchesBomb(bomb, actor.x, actor.y)) actor.passBomb = bomb.id
        return true
    }

    /** Advance fuses/flames one simulation tick and detonate chain reactions. */
    fun tick(): Int {
        val dt = FixedStepClock.STEP_SECONDS
        val flamesRemaining = activeFlames.map { it.copy(remaining = it.remaining - dt) }
            .filter { it.remaining > 1e-10 }
        activeFlames.clear()
        activeFlames.addAll(flamesRemaining)
        val updated = activeBombs.map { bomb ->
            if (bomb.remote) bomb else bomb.copy(remaining = requireNotNull(bomb.remaining) - dt)
        }
        activeBombs.clear()
        activeBombs.addAll(updated)
        return detonate(updated.filter { it.remaining != null && it.remaining <= 1e-10 }.map { it.id })
    }

    /** Keypad 8 requests the one owned remote bomb; ambiguous multi-bomb order is NOT guessed. */
    fun detonateRemote(owner: String = "player"): Boolean {
        if (owner != "player" && owner != "player2") return false
        val candidates = activeBombs.filter { it.owner == owner && it.remote }
        if (candidates.size != 1) return false
        return detonate(listOf(candidates.single().id)) >= 1
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
