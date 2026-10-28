# N24 — Order-independent multi-opponent fixed-step integration

Date: 2026-10-26. Explode Arena PR #2 remains Draft; collision PR #1 stays independent.

## Engineering question

The 1/2/4-spawn native roster has separate controllers, health and bomb ownership, but its old loop fully decided, placed, moved and damaged one bot before the next bot decided. Could changing the list order change another bot's information and the resulting 60Hz state? That is an order-bias bug in a multi-opponent simulation, independent of whether the provisional AI matches original Symbian.

## Acceptance criteria

1. Every living bot decides from the same immutable start-of-tick actor-position snapshot; one bot cannot observe another bot's movement from the same tick.
2. Successful bomb placements commit in stable owner order; owner-specific bomb limits and owner exit grace remain intact.
3. All candidate moves are calculated before any bot position is committed. Every candidate sees the same pre-move actor blockers and the complete bomb set for that tick.
4. If candidate endpoints overlap, fail closed by cancelling both conflicting moves; do not create an actor overlap. Defeated opponents remain nonblocking and do not act.
5. Every living opponent receives exactly one vitality tick after its committed move. Successful bomb count is preserved for renderer sound feedback.
6. Native Kotlin/JUnit integration verifies identical four-spawn state after 180 ticks when the roster list is reversed, including actor positions, facing, health, bomb ownership/IDs/fuses and no terrain/actor overlap.
7. The all-41-real-arena sweep verifies deterministic snapshots and reversed roster order for every arena with 2 or 4 source spawns, while preserving the 18/2/21 distribution for 1/2/4 spawns.
8. Run the actual free source/JUnit workflow and Mainline JS/Python checks on the exact code/test SHA. No APK packaging/build, artifact upload, release, merge or main-branch write.

## Why source tests answer this question

The defect concerns Kotlin object ordering, deterministic AI decisions, collision and bomb simulation. The existing source-only Gradle workflow exercises the actual core code and all 41 checked-in arena records. APK packaging cannot establish list-order invariance, and device rendering/audio or original Symbian parity are separate gates.

## Open-source/license review

Rechecked the official libGDX and libgdx/gdx-ai LICENSE files; both are Apache-2.0. Their generic engine/pathfinding APIs do not provide this game's per-owner bomb/fuse simulation or multi-bot snapshot contract. This change extends the existing in-repository Kotlin implementation; no external code, dependency, or original game asset is copied or added.

## Baseline

- PR #2 head before N24: 611d9b0ee2c2154f735fd07b04287ff61a5833b7 — N23 timed-blast spatial index.
- Native source CI run 40271907325 PASS and Mainline run 40271907354 PASS on that baseline.
- Xiaomi Pad 5 runtime, full gameplay, and Symbian/EKA2L1 AI/timing parity remain NOT TESTED.
- Google Drive LIVE sync has been unavailable in this runtime; no Drive sync is claimed without verified read-back.

## Implementation and CI result — SOURCE ACCEPTANCE PASS

- Production: NativeOpponentRoster.advance now separates shared-snapshot planning, stable bomb-placement commit, candidate movement calculation, overlap conflict handling, and final position/vitality commit.
- Tests: NativeOpponentRosterTest runs a four-spawn scenario for 180 ticks in normal and reversed roster order; NativeArenaSweepTest extends all-41 arena integration and reversed-order checks for every multi-bot arena.
- Exact production + regression source/test SHA: b31657ef20127901a22931dc402278572eb71e28.
- **PASS:** Native Kotlin/JUnit actual Gradle step in [run 40282530487](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40282530487), job 121438779605. The step executed `gradle --project-dir android :core:test --stacktrace --no-daemon` and logged **BUILD SUCCESSFUL in 47s**.
- **PASS:** Mainline JS/Python validation in [run 40282530545](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40282530545) on the same exact SHA.
- Native workflow artifacts: **[] (zero)**. No APK packaging/build or upload was performed.
- **NOT TESTED:** Xiaomi Pad 5 runtime, full playable match, audio/graphics lifecycle, measured device performance and original Symbian/EKA2L1 AI/timing parity.
- PR #2 remains open Draft; PR #1 and main are untouched. No merge or auto-merge.
- **Google Drive SYNC FAILED:** the Drive/Docs connector is not available in this runtime; LIVE documents were not read or written.
