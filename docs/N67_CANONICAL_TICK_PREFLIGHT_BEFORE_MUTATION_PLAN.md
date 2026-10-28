# N67 — Canonical tick preflight before world mutation

Date: 2026-10-28. Explode Arena PR #2 remains open/Draft; collision PR #1 remains separate.

## Engineering question

The roster rejected duplicate/out-of-order simulation ticks, but the rejection occurred only after `NativeArenaTick` had moved the player. The separate victory-hold clock also had no monotonic tick guard, so a repeated render/fixed-step identifier could consume more than one victory-hold tick.

## Production change

- Added a non-mutating monotonic tick preflight to the round clock and opponent roster.
- `NativeArenaTick` validates the player position, current tick, roster tick, blockers, and movement distance before reserving the round clock or moving any actor.
- The round/presentation clock now consumes each canonical tick at most once, including the frozen victory hold and latched terminal states.
- The roster retains its own reserve-before-mutation guard as defense in depth.

## Acceptance criteria

1. Replaying a gameplay tick is rejected before player/bot positions, bomb/flame lists, vitality, or round state change.
2. The next strictly greater tick still runs after a rejected duplicate.
3. Replaying a victory-hold tick cannot fast-forward the transition; the next unique tick advances the hold exactly once.
4. Existing native JVM tests and 41-arena integration suite remain green.
5. CI runs `gradle --project-dir android :core:test` only; no APK packaging/upload.

## Evidence

- Production + integration-test SHA: `a57caa1ef14a8ffef635a8e03d3e6c8b3f4f361b`.
- **PASS:** Native Kotlin/JUnit CI [#40646233882](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40646233882), `:core:test`, BUILD SUCCESSFUL in 50s.
- **PASS:** Mainline PR CI [#40646233855](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40646233855) and push CI [#40646228550](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40646228550) on the same source/test SHA.
- Native artifact query: 0. No APK build/package/upload.
- Android device gameplay and Symbian/EKA2L1 parity: **NOT TESTED**.
- Final documentation-head CI recheck: pending at plan creation; will be recorded in PR conversation after it completes.
- Source was written against the existing project architecture; no external game code/assets copied or added. No merge/main write.

## Google Drive

**SYNC FAILED** — this runtime exposes no Google Drive/Docs connector, so the LIVE handoff/index could not be read, written, or verified.
