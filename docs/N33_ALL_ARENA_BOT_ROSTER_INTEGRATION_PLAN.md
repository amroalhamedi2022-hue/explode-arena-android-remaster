# N33 — All-41-arena opponent roster integration

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX (keep Draft; no merge)

## Engineering question

Does the current independent-opponent roster remain within valid terrain and keep living actors separated when it repeatedly plans and moves through every one of the 41 real source arenas, including 1-, 2-, and 4-spawn layouts?

Synthetic arena tests and source inspection cannot prove interactions across the actual stage grid offsets, obstacles, and source spawn positions. This milestone adds an executable JVM integration test against `web/assets/stages.js` and drives the native roster/simulation for 120 fixed 60Hz ticks per arena. It is not a device build or a Symbian parity claim.

## Acceptance criteria

1. The actual stage catalog contains ordered IDs 0..40 with spawn distribution 18/2/21 for 1/2/4 spawns.
2. Each stage creates exactly `spawnCount - 1` opponents with distinct owner IDs, controller instances, and actor instances.
3. During 120 fixed-step ticks per arena, every living opponent remains outside blocking terrain/bombs and no living opponent overlaps the player or another living opponent.
4. Each tick uses the production roster, bomb simulation, native collision and post-tick damage APIs; the test does not duplicate their algorithms.
5. `gradle --project-dir android :core:test --stacktrace --no-daemon` passes on the exact source/test commit.
6. No APK packaging, Android build, artifact upload, release, main write, merge, or auto-merge.

## Source/license review

- The test uses only the repository's existing stage data and Kotlin production APIs; it adds no art, audio, or third-party implementation.
- Existing reference checks in N32 remain: EKA2L1 (GPL-3.0) and Bomberman_libGdx (MIT) were reviewed but not copied.
- Original gameplay timing and AI remain provisional pending EKA2L1/Symbian observation.

## Implementation

- Added `NativeOpponentRosterAllArenasIntegrationTest.kt`.
- The test runs each real stage through roster construction, independent bot planning, fixed-step movement, bomb updates, and post-tick damage for 120 ticks, asserting terrain and actor separation at every tick.
- **PASS — exact source/test SHA:** `f75588927aa633a0941f0816646413576697a420`.
- **PASS — Native Kotlin/JUnit:** [run #40429833635](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40429833635), job `native-source-check`, `:core:test`, **BUILD SUCCESSFUL in 52s**, 4 actionable tasks.
- **PASS — Mainline:** [run #40429833469](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40429833469) on the same SHA.
- **Artifacts: 0 confirmed**; no Android/ APK packaging occurred.
- Android device, EKA2L1 parity, graphics/audio and complete human-played rounds: **NOT TESTED**.
- A docs-only follow-up head will be rechecked through Mainline CI and recorded in the PR thread without another source edit.
