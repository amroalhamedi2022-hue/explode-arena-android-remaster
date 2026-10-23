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
    data class Bomb(val id: Long, val owner: String, val x: Double, val y: Double, val remaining: Double)
    data class Flame(val cells: List<Spawn>, val remaining: Double)
    private val activeBombs = mutableListOf<Bomb>()
    private val activeFlames = mutableListOf<Flame>()
    private var nextId = 1L

    init {
        require(bombLimit in 1..8 && bombRange in 1..12)
        require(bombFuseSeconds.isFinite() && bombFuseSeconds in 0.1..20.0)
        require(blastLifetimeSeconds.isFinite() && blastLifetimeSeconds in 0.05..5.0)
    }

    val bombs: List<Bomb> get() = activeBombs.toList()
    val flames: List<Flame> get() = activeFlames.toList()

    /** Return false if owner has reached the limit or the bomb tile is blocked/occupied. */
    fun place(actor: Actor, owner: String = "player"): Boolean {
        require(owner == "player" || owner == "player2" || owner == "bot")
        require(actor.x.isFinite() && actor.y.isFinite())
        if (activeBombs.count { it.owner == owner } >= bombLimit) return false
        val x = cellCenter(actor.x)
        val y = cellCenter(actor.y)
        if (StageCollision.blocked(stage, x, y, 3.0) ||
            activeBombs.any { it.x == x && it.y == y }) return false
        val bomb = Bomb(nextId++, owner, x, y, bombFuseSeconds)
        activeBombs.add(bomb)
        // Match provisional JS: the placing actor can leave its own bomb once.
        if (StageCollision.touchesBomb(bomb, actor.x, actor.y)) actor.passBomb = bomb.id
        return true
    }

    /** Advance fuses/flames one simulation tick and detonate all chain reactions. */
    fun tick(): Int {
        val dt = FixedStepClock.STEP_SECONDS
        val flamesRemaining = activeFlames.map { it.copy(remaining = it.remaining - dt) }
            .filter { it.remaining > 1e-10 }
        activeFlames.clear()
        activeFlames.addAll(flamesRemaining)
        val updated = activeBombs.map { it.copy(remaining = it.remaining - dt) }
        activeBombs.clear()
        activeBombs.addAll(updated)
        val queue = java.util.ArrayDeque<Long>()
        updated.filter { it.remaining <= 1e-10 }.forEach { queue.addLast(it.id) }
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

    private fun explosionCells(x: Double, y: Double): List<Spawn> {
        val cx = floor(x / BOMB_CELL).toInt()
        val cy = floor(y / BOMB_CELL).toInt()
        val hit = mutableListOf(Spawn(cx * BOMB_CELL + 8.0, cy * BOMB_CELL + 8.0))
        for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            for (n in 1..bombRange) {
                val px = (cx + dx * n) * BOMB_CELL + 8.0
                val py = (cy + dy * n) * BOMB_CELL + 8.0
                if (px < 0 || py < 0 || px >= stage.width || py >= stage.height) break
                if (StageCollision.blocked(stage, px, py, 3.0)) break
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
