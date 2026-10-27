# N38 — Four-spawn last-opponent round outcome

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft, no merge.

## Engineering question

In a four-spawn arena, do already-defeated opponents stop blocking space without prematurely winning the round, and does the final living opponent's timed bomb resolve the round exactly once through the canonical production tick? Source inspection alone is insufficient because roster alive-count semantics, blocking actors, bomb fuse/flame timing, vitality and stage progression interact.

## Acceptance criteria

1. Use a 4-spawn arena fixture with three distinct bot owners and the production `NativeArenaTick.advance` path.
2. Prove that two defeated bots no longer block space and the remaining living bot prevents a premature round outcome.
3. Let the final living bot's real timed bomb/fuse/flame path defeat it; do not copy the simulation logic into the test.
4. Keep the distant player alive and assert the round resolves to `WON` then `ADVANCING`.
5. Assert exactly one stage-advance signal and no outcome while the final bot is alive.
6. Run Native Kotlin/JUnit `:core:test` and Mainline CI on the source/test head; zero artifacts and no APK packaging.
7. Do not alter `main`, merge either PR, upload original assets, publish a release, or auto-merge.

## Implementation

- Add `fourSpawnRoundWaitsForLastLivingBotBeforeAdvancingExactlyOnce` to `NativeArenaTickAllArenasIntegrationTest.kt`.
- The test combines roster state, dead-bot blocking behavior, the timed-bomb production path and the one-shot round transition.

## Open-source and license review

- The project already uses libGDX, whose official repository identifies the framework as Apache-2.0: https://github.com/libgdx/libgdx
- No third-party code or copyrighted assets are copied. Only project-owned Kotlin/JUnit and a test-only arena are used.
- All combat, fuse and bot rules remain provisional until independently compared with the original Symbian game in EKA2L1.

## Verification ledger

- Engineering question and acceptance criteria recorded before source-only CI.
- **PASS — exact source/test SHA:** `df96e41790cb521eaab15e130284b718bcf31294`.
- **PASS — Native Kotlin/JUnit:** [run #40452935519](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40452935519), `:core:test`, BUILD SUCCESSFUL in 33s.
- **PASS — Mainline source checks:** [run #40452935490](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40452935490).
- **Artifacts: 0 confirmed**; no APK packaging/upload.

- APK/artifacts: **NOT RUN / 0 expected**.
- Physical device and Symbian/EKA2L1 parity: **NOT TESTED**.
- Google Drive LIVE sync: **SYNC FAILED** — no Drive/Docs connector is exposed; no LIVE handoff/index read/write or folder metadata verification is claimed.
