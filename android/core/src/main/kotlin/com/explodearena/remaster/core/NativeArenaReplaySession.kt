package com.explodearena.remaster.core

/**
 * Headless deterministic replay of the production native arena tick, including
 * the complete 1/2/4-spawn roster. All rules remain provisional until compared
 * with original Symbian EKA2L1 evidence; matching runs prove regression
 * determinism only, not original-game parity.
 */
internal data class NativeArenaReplayInput(
    val dx: Double = 0.0,
    val dy: Double = 0.0,
    val placeBomb: Boolean = false,
    val remoteDetonate: Boolean = false,
)

internal data class NativeArenaReplayActor(
    val owner: String,
    val x: Double,
    val y: Double,
    val health: Int,
    val invulnerableSeconds: Double,
    val defeated: Boolean,
    val passBomb: Long?,
    val walking: Boolean?,
    val facing: Int?,
)

internal data class NativeArenaReplayFrame(
    val stageId: Int,
    val ticksInRound: Long,
    val roundState: String,
    val player: NativeArenaReplayActor,
    val bots: List<NativeArenaReplayActor>,
    val remoteOwners: List<String>,
    val nextBombId: Long,
    val bombs: List<NativeBombSimulation.Bomb>,
    val flames: List<NativeBombSimulation.Flame>,
) {
    /** Stable exact-double trace for all gameplay actors and timed hazards. */
    fun traceLine(): String = buildString {
        fun bits(value: Double): String =
            java.lang.Long.toHexString(java.lang.Double.doubleToLongBits(value))
        fun actorLine(actor: NativeArenaReplayActor): String = buildString {
            append(actor.owner).append(',')
            append(bits(actor.x)).append(',').append(bits(actor.y)).append(',')
            append(actor.health).append(',').append(bits(actor.invulnerableSeconds)).append(',')
            append(actor.defeated).append(',')
            append(actor.passBomb?.toString() ?: "none").append(',')
            append(actor.walking?.toString() ?: "na").append(',')
            append(actor.facing?.toString() ?: "na")
        }

        append("EA-ARENA-PROVISIONAL-v1|").append(stageId).append('|').append(ticksInRound)
        append("|round=").append(roundState)
        append("|player=").append(actorLine(player))
        append("|bots=").append(bots.size)
        for (bot in bots) append(';').append(actorLine(bot))
        append("|remoteOwners=").append(remoteOwners.joinToString(","))
        append("|nextBombId=").append(nextBombId)
        append("|bombs=").append(bombs.size)
        for (bomb in bombs.sortedBy { it.id }) {
            append(';').append(bomb.id).append(',').append(bomb.owner)
            append(',').append(bits(bomb.x)).append(',').append(bits(bomb.y))
            append(',').append(bomb.remaining?.let(::bits) ?: "remote")
            append(',').append(bomb.remote)
        }
        append("|flames=").append(flames.size)
        for (flame in flames) {
            append(';').append(bits(flame.remaining)).append(':')
            for (cell in flame.cells.sortedWith(compareBy<Spawn> { it.y }.thenBy { it.x })) {
                append(bits(cell.x)).append(',').append(bits(cell.y)).append('/')
            }
        }
    }
}

/**
 * Drives the same NativeArenaTick used by the libGDX renderer without a GPU.
 * Input is validated before any action. A bomb button retries LOST/DRAW rounds,
 * matching NativeFrameCommands; WON ticks only the victory hold, while terminal
 * ADVANCING/COMPLETE states stay frozen. This harness intentionally models one
 * selected stage; stage navigation is a separate UI action.
 */
internal class NativeArenaReplaySession(
    private val stage: StageData,
    private val hasNextStage: Boolean = true,
) {
    private var round: NativeArenaRoundState = NativeArenaRoundStateFactory.create(stage)
    private var ticksInRound = 0L

    fun setRemoteDetonator(owner: String = "player", enabled: Boolean): Boolean =
        round.simulation.setRemoteDetonator(owner, enabled)

    fun snapshot(): NativeArenaReplayFrame = NativeArenaReplayFrame(
        stageId = stage.id,
        ticksInRound = ticksInRound,
        roundState = round.roundFlow.state.name,
        player = NativeArenaReplayActor(
            owner = "player",
            x = round.player.x,
            y = round.player.y,
            health = round.playerVitality.health,
            invulnerableSeconds = round.playerVitality.invulnerableSeconds,
            defeated = round.playerVitality.defeated,
            passBomb = round.player.passBomb,
            walking = null,
            facing = null,
        ),
        bots = round.opponents.bots.sortedBy { it.owner }.map { slot ->
            NativeArenaReplayActor(
                owner = slot.owner,
                x = slot.actor.x,
                y = slot.actor.y,
                health = slot.vitality.health,
                invulnerableSeconds = slot.vitality.invulnerableSeconds,
                defeated = slot.vitality.defeated,
                passBomb = slot.actor.passBomb,
                walking = slot.walking,
                facing = slot.facing,
            )
        },
        remoteOwners = round.simulation.remoteDetonatorOwners.toList(),
        nextBombId = round.simulation.nextBombId,
        bombs = round.simulation.bombs.toList(),
        flames = round.simulation.flames.map { it.copy(cells = it.cells.map { cell -> cell.copy() }) },
    )

    fun step(input: NativeArenaReplayInput): NativeArenaReplayFrame {
        require(
            input.dx.isFinite() && input.dy.isFinite() &&
                input.dx in -1.0..1.0 && input.dy in -1.0..1.0
        ) { "Arena replay input must be finite and normalized" }

        if (round.roundFlow.canRetry) {
            if (input.placeBomb) {
                round = NativeArenaRoundStateFactory.create(stage)
                ticksInRound = 0L
            }
            return snapshot()
        }
        if (round.roundFlow.isLatched) return snapshot()
        check(ticksInRound < Long.MAX_VALUE) { "Arena replay tick counter exhausted" }

        val playing = round.roundFlow.isPlaying
        if (playing && input.placeBomb) round.simulation.place(round.player)
        if (playing && input.remoteDetonate) round.simulation.detonateRemote()

        val tick = round.tickClock.nextTick()
        NativeArenaTick.advance(
            stage = stage,
            player = round.player,
            playerVitality = round.playerVitality,
            opponents = round.opponents,
            simulation = round.simulation,
            roundFlow = round.roundFlow,
            tick = tick,
            dx = if (playing) input.dx else 0.0,
            dy = if (playing) input.dy else 0.0,
            hasNextStage = hasNextStage,
        )
        ticksInRound++
        return snapshot()
    }
}
