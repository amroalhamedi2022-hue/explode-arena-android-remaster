# N23 — Spatial index for timed blast footprints in native bot search

Date: 2026-10-26. Explode Arena PR #2 remains open Draft; collision PR #1 stays separate.

## Engineering question

N22 coalesced overlapping timed-blast cells, but the bot's chase BFS and escape BFS still linearly scanned those cells for every candidate. Can we replace those repeated scans with a bounded 16px-grid lookup while preserving the exact hitbox boundary and earliest ignition deadline?

## Acceptance criteria

1. Index only finite 16px-aligned cells and non-negative finite deadlines; duplicate cells retain the earliest deadline in a bounded primitive open-addressed table.
2. Queries preserve the strict geometry of `NativeBombSimulation.blastHits` and support radii from 0 through the native 5px actor radius.
3. Each valid query checks exactly four possible cell slots with primitive arrays, no boxed keys or per-query cell/list allocation.
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

## CI result — SOURCE ACCEPTANCE PASS (2026-10-26)

- Initial native run on intermediate SHA `60aa5ea1762f003917dd2a499163d1783b9448ed` failed test compilation because two nullable `Double?` results were passed to JUnit's primitive double assertion. Fixed with `requireNotNull` in test commit `98d47752048bf29194dbc2d0e717068ec972deeb`.
- **PASS on exact source+test SHA `98d47752048bf29194dbc2d0e717068ec972deeb`:** [Native Kotlin/JUnit run 40271739066](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40271739066), task `gradle --project-dir android :core:test`, `BUILD SUCCESSFUL in 43s`.
- **PASS:** [Mainline source validation run 40271739122](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40271739122) on the same SHA.
- Independent geometry cross-check exercised 4,225 boundary/grid coordinate cases with zero mismatches against the linear hitbox oracle; the corresponding Kotlin/JUnit regression is part of `:core:test`.
- **Artifacts: 0.** No APK packaging, artifact upload or release.
- Xiaomi Pad 5 runtime/profiling, 41-arena end-to-end playthrough and original Symbian/EKA2L1 parity remain NOT TESTED.
- PR #2 remains open Draft; collision PR #1 and `main` untouched. Google Drive LIVE sync remains SYNC FAILED because this runtime has no Drive/Docs connector; no Drive write/read-back is claimed.


