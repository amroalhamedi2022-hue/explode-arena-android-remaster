# N23 — Spatial index for timed blast footprints in native bot search

Date: 2026-10-26. Explode Arena PR #2 remains open Draft; collision PR #1 stays separate.

## Engineering question

N22 coalesced overlapping timed-blast cells, but the bot's chase BFS and escape BFS still linearly scanned those cells for every candidate. Can we replace those repeated scans with a bounded 16px-grid lookup while preserving the exact hitbox boundary and earliest ignition deadline?

## Acceptance criteria

1. Index only finite 16px-aligned cells and non-negative finite deadlines; duplicate cells retain the earliest deadline.
2. Queries preserve the strict geometry of `NativeBombSimulation.blastHits` and support radii from 0 through the native 5px actor radius.
3. Each valid query checks no more than four possible cell keys; no per-query cell/list allocation.
4. Use the index in chase BFS, timed escape deadline checks, and active-bomb footprint checks. Keep the one-off cached-direction safety check unchanged to avoid building an index every fixed tick.
5. Tests compare index membership against the existing linear hitbox oracle across positive/negative cells and exact boundaries, and cover earliest deadlines, duplicates, empty input, invalid coordinates, alignment and radius bounds.
6. Run `gradle --project-dir android :core:test` and Mainline source validation on the exact source+test SHA. No APK packaging, artifact upload, release, merge or main write.

## Reference and licensing review

Reviewed the existing in-repository `BurningEscapeIndex` as the closest tested design precedent and the official libGDX Apache-2.0 project/performance guidance. The implementation reuses the project's own 16px grid conventions; no third-party code, game assets or dependencies are copied or added.

## Baseline

- PR #2 branch: `planning/native-kotlin-libgdx-migration-20261023`.
- Baseline PR head: `3db46723a28e65a1cf4576b663528b144108b63c`.
- N22 exact source/test SHA: `23afa7bc5d9e2a5ce4edc8123fff8dee89402776`; Native and Mainline source checks PASS as recorded in the acceptance ledger.
- Android device, real-time performance and Symbian/EKA2L1 parity remain NOT TESTED.

## Implementation

Pending source/JUnit CI. No APK build is requested because this change's question is exact deterministic equivalence of spatial lookups versus the existing linear hitbox oracle; device packaging cannot establish that equivalence.
