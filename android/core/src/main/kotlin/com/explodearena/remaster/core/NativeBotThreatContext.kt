package com.explodearena.remaster.core

/**
 * Immutable per-tick view of imminent timed threats shared by all living bots.
 *
 * The context is deliberately created by NativeOpponentRoster.advance rather
 * than cached on a controller: bomb fuses and flames change every tick. Its
 * snapshot and spatial index are read-only during the planning phase. The
 * 0.75-second horizon is a provisional native heuristic, not Symbian timing.
 */
internal class NativeBotThreatContext private constructor(
    val snapshot: NativeBombSimulation.TimedThreatSnapshot,
    val cells: List<Spawn>,
    val imminentIndex: TimedBlastIndex,
    val threatFootprints: List<NativeBombSimulation.TimedThreatFootprint>,
) {
    companion object {
        const val DEFAULT_HORIZON_SECONDS = 0.75

        fun create(
            simulation: NativeBombSimulation,
            horizonSeconds: Double = DEFAULT_HORIZON_SECONDS,
        ): NativeBotThreatContext {
            val snapshot = simulation.predictedTimedThreatSnapshot(horizonSeconds)
            val cells = snapshot.cells.map { it.cell }
            val index = TimedBlastIndex.fromCells(cells)
            return NativeBotThreatContext(snapshot, cells, index, snapshot.threatFootprints)
        }
    }
}
