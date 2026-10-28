package com.explodearena.remaster.core

/**
 * Native opponents for 1-, 2- and 4-spawn arenas. Each opponent owns a
 * separate provisional AI, vitality and bomb quota. Not Symbian mode parity.
 */
internal class NativeOpponentRoster(
    bots: List<BotSlot>,
    private val threatContextFactory: (NativeBombSimulation) -> NativeBotThreatContext =
        { simulation -> NativeBotThreatContext.create(simulation) },
) {
    // Snapshot the caller-owned collection before validating roster invariants.
    private val slots: List<BotSlot> = bots.toList()

    /** A detached snapshot; callers cannot mutate validated internal membership. */
    val bots: List<BotSlot> get() = slots.toMutableList()

    class BotSlot(
        val owner: String,
        val actor: Actor,
        val vitality: NativeVitality = NativeVitality(),
        val controller: NativeBotController = NativeBotController(owner),
    ) {
        var facing = 2
        var walking = false
    }

    init {
        require(slots.all { it.owner == it.controller.ownerId }) {
            "Opponent controller owner must match its roster owner"
        }
        require(slots.map { it.owner }.toSet().size == slots.size) {
            "Opponent owner IDs must be unique"
        }
        for (firstIndex in slots.indices) {
            for (secondIndex in firstIndex + 1 until slots.size) {
                val first = slots[firstIndex]
                val second = slots[secondIndex]
                require(first.actor !== second.actor) {
                    "Each opponent must own an independent actor"
                }
                require(first.vitality !== second.vitality) {
                    "Each opponent must own independent vitality"
                }
                require(first.controller !== second.controller) {
                    "Each opponent must own an independent controller"
                }
            }
        }
    }

    val present: Boolean get() = slots.isNotEmpty()
    val aliveCount: Int get() = slots.count { !it.vitality.defeated }
    val allDefeated: Boolean get() = present && aliveCount == 0

    fun stopWalking() { slots.forEach { it.walking = false } }

    /** Only living opponents occupy space; dead sprites never trap the player. */
    fun blockingActors(): List<Actor> =
        slots.filterNot { it.vitality.defeated }.map { it.actor }

    /**
     * Advance one provisional 60Hz opponent tick without roster-order bias.
     *
     * Every living bot first decides from the same immutable actor-position
     * snapshot and the same timed-threat context. The context is created once
     * before planning and discarded after this tick; it is never cached across
     * fuse/flame updates. Successful placements are then committed in stable
     * owner order, and all movement candidates are computed against the same
     * pre-move actor positions and the now-complete bomb set. Positions are
     * committed only after every candidate has been calculated.
     *
     * These AI and timing rules remain provisional; this is not Symbian parity.
     */
    fun advance(
        stage: StageData,
        player: Actor,
        simulation: NativeBombSimulation,
        tick: Long,
        applyDamage: Boolean = true,
    ): Int {
        // Validate before touching bot animation state or taking the solo-arena
        // early return. advanceSimulationTick must not age fuses on an invalid tick.
        require(tick >= 0L) { "Invalid opponent tick" }
        slots.filter { it.vitality.defeated }.forEach { it.walking = false }
        val living = slots.filterNot { it.vitality.defeated }.sortedBy { it.owner }
        if (living.isEmpty()) return 0

        val playerAtStart = Actor(player.x, player.y, player.passBomb)
        val actorsAtStart = living.associateWith { slot ->
            Actor(slot.actor.x, slot.actor.y, slot.actor.passBomb)
        }
        val blockersAtStart = living.associateWith { slot ->
            listOf(playerAtStart) + living
                .filter { it !== slot }
                .map { actorsAtStart.getValue(it) }
        }

        // One chain prediction and one immutable spatial index serve all slots.
        // Planning is read-only, so this pre-placement context is valid for
        // every decision in this tick and cannot observe a peer's new bomb.
        val threatContext = threatContextFactory(simulation)
        val decisions = living.associateWith { slot ->
            slot.controller.decideWithContext(
                stage = stage,
                bot = actorsAtStart.getValue(slot),
                target = playerAtStart,
                simulation = simulation,
                tick = tick,
                otherActors = blockersAtStart.getValue(slot),
                threatContext = threatContext,
            )
        }

        // Commit all successful placements before moving anyone. Sorting by
        // owner makes bomb IDs and replay snapshots stable if roster order changes.
        var placed = 0
        for (slot in living) {
            if (decisions.getValue(slot).placeBomb && simulation.place(slot.actor, slot.owner)) {
                placed++
            }
        }

        // If any placement succeeded, the pre-placement decisions are stale for
        // escape planning: a peer's newly committed bomb may now cut across a
        // cached refuge. Replan movement once against the complete bomb set.
        // The second pass is movement-only and carries pass-bomb grace forward;
        // it cannot add another bomb after the deterministic placement phase.
        val movementDecisions = if (placed == 0) {
            decisions
        } else {
            val postPlacementContext = threatContextFactory(simulation)
            living.associateWith { slot ->
                val start = actorsAtStart.getValue(slot)
                slot.controller.decideWithContext(
                    stage = stage,
                    bot = Actor(start.x, start.y, slot.actor.passBomb),
                    target = playerAtStart,
                    simulation = simulation,
                    tick = tick,
                    otherActors = blockersAtStart.getValue(slot),
                    threatContext = postPlacementContext,
                    allowBombPlacement = false,
                )
            }
        }

        // Candidate movements all use the same actor snapshot and complete bomb
        // set. No candidate uses a peer's already committed movement as extra info.
        val candidates = living.associateWith { slot ->
            val decision = movementDecisions.getValue(slot)
            Actor(slot.actor.x, slot.actor.y, slot.actor.passBomb).also { candidate ->
                StageCollision.move(
                    stage, candidate, decision.dx, decision.dy,
                    NativeBotController.SPEED, FixedStepClock.STEP_SECONDS,
                    simulation.bombs, blockersAtStart.getValue(slot),
                )
            }
        }

        // Fail closed if two independently calculated endpoints nevertheless
        // overlap (for example, on a tightly packed custom test arena).
        val conflicts = mutableSetOf<BotSlot>()
        for (i in living.indices) {
            for (j in i + 1 until living.size) {
                val first = living[i]
                val second = living[j]
                val a = candidates.getValue(first)
                val b = candidates.getValue(second)
                if (StageCollision.blockedByActors(a.x, a.y, listOf(b))) {
                    conflicts.add(first)
                    conflicts.add(second)
                }
            }
        }

        for (slot in living) {
            val actor = slot.actor
            val oldX = actor.x
            val oldY = actor.y
            val candidate = candidates.getValue(slot)
            if (slot in conflicts) {
                candidate.x = oldX
                candidate.y = oldY
                // A bomb may have been placed this tick; preserve its owner
                // exit grace if the conservative conflict gate cancels motion.
                candidate.passBomb = actor.passBomb
            }
            actor.x = candidate.x
            actor.y = candidate.y
            actor.passBomb = candidate.passBomb
            slot.walking = actor.x != oldX || actor.y != oldY
            if (slot.walking) {
                val decision = movementDecisions.getValue(slot)
                slot.facing = when {
                    decision.dx > 0 -> 3
                    decision.dx < 0 -> 1
                    decision.dy > 0 -> 2
                    else -> 0
                }
            }
            if (applyDamage) {
                tickDamage(
                    slot,
                    simulation.isBurning(actor.x, actor.y, StageCollision.ACTOR_RADIUS),
                )
            }
        }
        return placed
    }

    data class SimulationTickResult(val bombsPlaced: Int, val bombsDetonated: Int)

    // A roster instance consumes each canonical simulation tick at most once.
    // Reserve before mutable gameplay work so a failed partial tick cannot be
    // replayed and accidentally age fuses or move bots twice.
    private var lastSimulationTick: Long? = null

    /**
     * Canonical fixed-step order for bot gameplay: plan/move against the pre-tick
     * world, advance fuses and flames once, then sample bot damage against the
     * entire just-finished interval (including flames expiring at this boundary).
     * The game loop and all-arena integration tests share this entry point so
     * callers cannot accidentally sample bot damage before the bomb update.
     */
    /** Non-mutating tick preflight used by the outer canonical step before player movement. */
    internal fun validateNextSimulationTick(tick: Long) {
        require(tick >= 0L) { "Invalid opponent simulation tick" }
        val previousTick = lastSimulationTick
        require(previousTick == null || tick > previousTick) {
            "Opponent simulation tick must increase strictly"
        }
    }

    fun advanceSimulationTick(
        stage: StageData,
        player: Actor,
        simulation: NativeBombSimulation,
        tick: Long,
    ): SimulationTickResult {
        // Even a solo arena must reject malformed player coordinates before reserving a tick
        // or advancing bombs; the empty-opponent fast path otherwise bypasses collision checks.
        require(player.x.isFinite() && player.y.isFinite()) { "Invalid player position" }
        validateNextSimulationTick(tick)
        // Reserve before mutating any actor/bomb/vitality state. If an internal
        // operation fails after partial work, the same tick cannot be replayed.
        lastSimulationTick = tick

        val placed = advance(stage, player, simulation, tick, applyDamage = false)
        val detonated = simulation.tick()
        applyDamageForLastTick(simulation)
        return SimulationTickResult(placed, detonated)
    }

    /**
     * A defeat is also an animation-state transition. Clear walking on the same
     * damage sample that removes the bot from gameplay; waiting for the next
     * fixed tick can render one stale walking frame during a terminal hold.
     */
    private fun tickDamage(slot: BotSlot, burning: Boolean) {
        slot.vitality.tick(burning)
        if (slot.vitality.defeated) slot.walking = false
    }

    /** Apply damage after tick(), including flame cells that expired at its boundary. */
    fun applyDamageForLastTick(simulation: NativeBombSimulation) {
        for (slot in slots) {
            tickDamage(
                slot,
                simulation.isBurningDuringLastTick(
                    slot.actor.x, slot.actor.y, StageCollision.ACTOR_RADIUS,
                ),
            )
        }
    }

    companion object {
        private val owners = listOf("bot", "bot2", "bot3")

        fun create(stage: StageData, player: Actor): NativeOpponentRoster {
            val resolved = NativeSpawnLayout.resolve(stage)
            val occupied = mutableListOf(Spawn(player.x, player.y))
            val bots = resolved.drop(1).mapIndexed { index, safe ->
                check(occupied.none {
                    kotlin.math.abs(it.x - safe.x) < 2 * StageCollision.ACTOR_RADIUS &&
                        kotlin.math.abs(it.y - safe.y) < 2 * StageCollision.ACTOR_RADIUS
                }) { "Arena ${stage.id} has overlapping opponent spawns" }
                occupied.add(safe)
                BotSlot(owners[index], Actor(safe.x, safe.y))
            }
            return NativeOpponentRoster(bots)
        }
    }
}
