# N29 — Full-fuse integration sweep across all 41 native arenas

Date: 2026-10-26. Explode Arena PR #2 remains open Draft; collision PR #1 remains separate.

## Engineering question

The existing all-arena native sweep exercised only eight 60 Hz ticks per stage. That proves deterministic early movement/bomb placement, but it does not cross the provisional 1.6-second fuse boundary or cover the post-detonation flame window. Can every one of the 41 real Mainline stage records run through a complete native bomb fuse and post-blast simulation window without invalid positions, terrain/actor overlap, per-owner quota violations, or a bomb that never detonates?

## Existing-source review

- Reused the repository's current `StageCatalog`, `NativeSpawnLayout`, `NativeOpponentRoster`, `NativeBombSimulation`, `NativeVitality`, and `StageCollision` APIs rather than writing a parallel simulation.
- The existing `NativeArenaSweepTest` already covers 8-tick deterministic behavior and reversed roster order; the new test extends coverage to 130 ticks and the fuse/flame lifecycle.
- No external implementation was copied, no dependency was added, and no game art/audio was changed or uploaded.

## Acceptance criteria

1. Read the real `web/assets/stages.js` catalog and require stage IDs 0–40 plus the known 1/2/4-spawn distribution.
2. For each arena, resolve safe spawns, create the actual native bot roster and bomb simulation, and place a player bomb.
3. Run 130 fixed simulation ticks (96 ticks to the provisional 1.6-second fuse plus a post-blast window).
4. At each tick, assert finite player/bot coordinates, no solid-terrain collision, no living-actor overlap, and no per-owner bomb quota violation.
5. Assert at least one detonation occurred and the initial bomb is no longer active after the window.
6. Repeat representative one-, two-, and four-spawn arenas and compare complete final snapshots for determinism.
7. Run native `:core:test` and Mainline JS/Python validation on the exact test commit, then recheck CI on the docs-updated PR head.
8. No APK packaging, artifact upload, release, merge, auto-merge, main write, or copyrighted asset upload.

## Result — INTEGRATION TEST PASS

- Added `android/core/src/test/kotlin/com/explodearena/remaster/core/NativeArenaFuseIntegrationTest.kt`.
- Test blob SHA: `11a78ba2afb2b4c84180700ad35adc2c5c042fdf`.
- Exact test commit: `c62218cb16bd048cc498f9f754effd9103393193`.
- **PASS Native Kotlin/JUnit:** [Actions run 40364236670](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40364236670), real `:core:test`, log **BUILD SUCCESSFUL in 51s**, 4 actionable tasks executed.
- **PASS Mainline JS/Python:** [Actions run 40364236639](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40364236639) on the same commit.
- The new integration test and existing suite both pass. This is a source/JVM result, not device or Symbian parity evidence.
- **Artifacts: 0.** No APK packaging/build/upload was run.

## Explicit limits

- The 1.6-second fuse, 0.45-second flame lifetime, collision radii, and AI remain provisional native behavior.
- **NOT TESTED:** Symbian/EKA2L1 parity, actual Android Activity/touch delivery, real audio playback, graphics fidelity, FPS/RAM on Xiaomi Pad 5, or a full human-played match.
- No production gameplay source changed in N29; this milestone increases end-to-end acceptance coverage and does not claim new game mechanics.
- **Google Drive SYNC FAILED:** no Drive/Docs connector is available in this runtime, so LIVE documents were not read or written. This GitHub record is not a Drive sync.
