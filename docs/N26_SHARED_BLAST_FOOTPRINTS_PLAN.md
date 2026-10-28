# N26 — Shared per-bomb blast footprints

Date: 2026-10-26  
Status: source acceptance PASS on exact source/test SHA 19c65522ea75533089cf4630bb86d2454876c153.

## Engineering question

Can the native roster reuse each predicted bomb's exact blast-ray geometry across all living bots in one simulation tick, while preserving the prior escape-threat decision and leaving simulation state untouched?

Source inspection alone is insufficient: strict hitbox boundaries and rays stopped by offset 8px obstacle cells need an executable oracle comparison, and the standalone controller must remain behaviorally equivalent to the roster's shared-context path.

## Implementation

- Extend `NativeBombSimulation.TimedThreatSnapshot` with one precomputed footprint for each predicted timed-chain threat.
- Compute each footprint once during snapshot creation and reuse those exact cells both for the deduplicated spatial-index input and the per-bot escape lookup; do not cache snapshots across ticks because fuse/flame state changes.
- Select an escape threat by testing the cached footprint with the same strict `blastHits` rule, replacing per-bot recomputation of `explosionCells`.
- Keep `NativeBombSimulation.wouldBlastHitFrom` as the independent oracle used by tests and standalone callers.
- Do not change fuse deadlines, blast geometry, ownership, movement, or Symbian-parity claims.

## Acceptance criteria

1. Each context footprint corresponds in order to exactly one threat in its snapshot.
2. Across a grid of actor positions, cached footprint hits equal `wouldBlastHitFrom`, including wall truncation and strict hitbox edges.
3. Shared-context decisions remain equal to standalone-controller decisions for each bot.
4. Existing roster-order independence, 1/2/4-spawn, bomb collision and timed-chain tests remain green.
5. Run the free source-only native workflow (`:core:test`) on the exact source/test SHA; no APK packaging, upload, release or device-test claim.

## Status ledger

- Source and regression test: included in the N26 implementation and follow-up geometry-reuse commits.
- **PASS Native Kotlin/JUnit:** [run 40310546626](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40310546626), exact SHA above; `BUILD SUCCESSFUL in 54s`, 4 actionable tasks.
- **PASS Mainline JS/Python:** [run 40310546805](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40310546805), same SHA.
- Artifacts: zero; no APK packaging.
- Device/EKA2L1 parity and performance profiling: NOT TESTED.
- Google Drive LIVE sync: SYNC FAILED if the Drive/Docs connector remains unavailable; do not infer sync from GitHub docs.
