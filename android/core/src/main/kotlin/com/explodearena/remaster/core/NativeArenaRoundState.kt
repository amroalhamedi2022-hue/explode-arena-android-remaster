package com.explodearena.remaster.core

/**
 * A complete, fresh simulation state for one arena attempt.
 *
 * The native renderer uses this factory for both stage navigation and retry so
 * bombs, flames, health, bot controllers, and round outcome cannot leak across
 * attempts. The mechanics remain provisional until matched to Symbian/EKA2L1.
 */
internal data class NativeArenaRoundState(
    val player: Actor,
    val simulation: NativeBombSimulation,
    val playerVitality: NativeVitality,
    val opponents: NativeOpponentRoster,
    val roundFlow: NativeRoundFlow,
)

/** Builds every mutable gameplay subsystem from the selected stage's spawn data. */
internal object NativeArenaRoundStateFactory {
    fun create(stage: StageData): NativeArenaRoundState {
        val spawn = NativeSpawnLayout.resolve(stage).first()
        val player = Actor(spawn.x, spawn.y)
        val simulation = NativeBombSimulation(stage)
        val vitality = NativeVitality()
        val opponents = NativeOpponentRoster.create(stage, player)
        return NativeArenaRoundState(
            player = player,
            simulation = simulation,
            playerVitality = vitality,
            opponents = opponents,
            roundFlow = NativeRoundFlow(),
        )
    }
}
