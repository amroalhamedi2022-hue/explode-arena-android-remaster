# N22 — Coalesce overlapping timed-threat cells

Date: 2026-10-26. PR #2 remains open Draft; collision PR #1 stays separate.

## Engineering question

A timed-chain snapshot may contain the same 16px blast cell from multiple bomb rays. The native bot only needs one danger lookup for that cell, using its earliest predicted ignition deadline. Can we remove duplicate cell hit-tests without losing any threatened cells or delaying the earliest deadline?

## Acceptance criteria

1. The snapshot preserves the exact timed-chain threat list.
2. Every cell from the legacy timestamped blast view remains present in the snapshot.
3. Snapshot cells are unique and each carries the minimum deadline among all threats covering that cell.
4. Snapshot creation does not mutate bombs, flames, next bomb ID, or remote-owner state.
5. Run `gradle --project-dir android :core:test` and Mainline source validation on the exact source/test SHA. No APK packaging, artifact upload, release, merge, or main write.
6. Android/Xiaomi Pad 5 performance and original Symbian/EKA2L1 parity remain NOT TESTED.

## Implementation

`NativeBombSimulation.predictedTimedThreatSnapshot` now coalesces overlapping `Spawn` cells in insertion order using a linked map, retaining the earliest deadline for each cell. The legacy `predictedTimedBlastCells` helper remains unchanged as an independent regression oracle. No third-party code or dependency was copied/added; this is a bounded change to existing Kotlin logic.

## Regression

The existing timed-chain snapshot test now checks set parity against the legacy helper, uniqueness, earliest-deadline retention, and simulation immutability for a timed bomb that chains into a remote bomb. The fixture intentionally creates overlapping rays.

## Status at implementation

- Production commit: `42a32970d35d00b187a10b7e91513c4b0acc8730`.
- Test commit / source+test SHA: `23afa7bc5d9e2a5ce4edc8123fff8dee89402776`.
- Native Kotlin/JUnit and Mainline CI: pending at the initial checkpoint; do not claim PASS until both exact-SHA runs complete.
- Device runtime, profiling, 41-arena end-to-end play and EKA2L1 parity: NOT TESTED.
- No APK was built; source/JUnit can answer the deterministic cell-set question.
