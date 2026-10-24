package com.explodearena.remaster.core

/**
 * Headless deterministic trace harness for the N02/N03 Kotlin migration.
 *
 * Input ordering intentionally follows the CURRENT native vertical slice:
 * plant/detonate at frame input, then move, advance bombs, and apply damage.
 * These rules are PROVISIONAL and must be compared against original Symbian
 * EKA2L1 evidence before being described as original game behavior.
 *
 * No libGDX graphics, WebView, Android framework or APK is required.
 */
data class NativeReplayInput(
    val dx: Double = 0.0,
    val dy: Double = 0.0,
    val placeBomb: Boolean = false,
    val remoteDetonate: Boolean = false,
)

data class NativeReplayFrame(
    val stageId: Int,
    val tick: Long,
    val actorX: Double,
    val actorY: Double,
    val health: Int,
    val invulnerableSeconds: Double,
    val passBomb: Long?,
    val remoteDetonatorOwners: List<String>,
    val nextBombId: Long,
    val bombs: List<NativeBombSimulation.Bomb>,
    val flames: List<NativeBombSimulation.Flame>,
) {
    /**
     * Locale-independent exact-double trace line for comparing native runs.
     * Includes timers, owners, bomb IDs and all flame cells so a mismatch
     * cannot be concealed by an identical final actor position.
     */
    fun traceLine(): String = buildString {
        fun bits(value: Double): String =
            java.lang.Long.toHexString(java.lang.Double.doubleToLongBits(value))
        append("EA-NATIVE-PROVISIONAL-v2|").append(stageId).append('|').append(tick)
        append('|').append(bits(actorX)).append(',').append(bits(actorY))
        append('|').append(health).append(',').append(bits(invulnerableSeconds))
        append("|pass=").append(passBomb?.toString() ?: "none")
        append("|remoteOwners=").append(remoteDetonatorOwners.joinToString(","))
        append("|nextBombId=").append(nextBombId)
        append("|bombs=").append(bombs.size)
        for (bomb in bombs) {
            append(';').append(bomb.id).append(',').append(bomb.owner)
            append(',').append(bits(bomb.x)).append(',').append(bits(bomb.y))
            append(',').append(bomb.remaining?.let { bits(it) } ?: "remote")
            append(',').append(bomb.remote)
        }
        append("|flames=").append(flames.size)
        for (flame in flames) {
            append(';').append(bits(flame.remaining)).append(':')
            for (cell in flame.cells) {
                append(bits(cell.x)).append(',').append(bits(cell.y)).append('/')
            }
        }
    }
}

class NativeReplaySession(
    private val stage: StageData,
    spawn: Spawn,
    bombLimit: Int = 1,
) {
    private val actor: Actor
    private val bombSimulation = NativeBombSimulation(stage, bombLimit = bombLimit)
    private val vitality = NativeVitality()
    private var ticks = 0L

    init {
        require(spawn.x.isFinite() && spawn.y.isFinite()) { "Invalid replay spawn" }
        val safe = StageCollision.nearestFree(stage, spawn.x, spawn.y)
            ?: throw IllegalArgumentException("Stage has no valid replay spawn")
        actor = Actor(safe.x, safe.y)
    }

    fun setRemoteDetonator(owner: String = "player", enabled: Boolean): Boolean =
        bombSimulation.setRemoteDetonator(owner, enabled)

    fun snapshot(): NativeReplayFrame = NativeReplayFrame(
        stageId = stage.id,
        tick = ticks,
        actorX = actor.x,
        actorY = actor.y,
        health = vitality.health,
        invulnerableSeconds = vitality.invulnerableSeconds,
        passBomb = actor.passBomb,
        remoteDetonatorOwners = bombSimulation.remoteDetonatorOwners,
        nextBombId = bombSimulation.nextBombId,
        bombs = bombSimulation.bombs,
        flames = bombSimulation.flames.map { flame ->
            flame.copy(cells = flame.cells.map { it.copy() })
        },
    )

    fun step(input: NativeReplayInput): NativeReplayFrame {
        // Reject malformed traces before mutating ANY simulation state.
        require(input.dx.isFinite() && input.dy.isFinite() &&
            input.dx in -1.0..1.0 && input.dy in -1.0..1.0) {
            "Replay direction must be finite and normalized"
        }
        if (!vitality.defeated) {
            if (input.placeBomb) bombSimulation.place(actor)
            if (input.remoteDetonate) bombSimulation.detonateRemote()
            StageCollision.move(
                stage, actor, input.dx, input.dy, 77.0,
                FixedStepClock.STEP_SECONDS, bombSimulation.bombs,
            )
            bombSimulation.tick()
            vitality.tick(
                bombSimulation.isBurning(actor.x, actor.y, StageCollision.ACTOR_RADIUS),
            )
        }
        ticks++
        return snapshot()
    }
}
