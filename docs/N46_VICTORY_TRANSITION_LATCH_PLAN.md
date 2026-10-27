# N46 — Latch stage progression at victory start

**Date:** 2026-10-27  
**Repository:** `amroalhamedi2022-hue/explode-arena-android-remaster`  
**PR:** #2 — remain Draft; no merge.  
**Status:** source and regression tests committed; CI pending.

## Engineering question

Can the caller's `hasNextStage` value changing during the 90-tick victory hold turn a final arena into an advance, or prevent an advance that was valid when victory began?

## Acceptance criteria

1. `NativeRoundFlow` captures stage availability on the exact tick that enters `WON`.
2. If no next arena exists at victory start, later `hasNextStage=true` values cannot wrap the campaign.
3. If a next arena exists at victory start, later `hasNextStage=false` values cannot suppress the one transition signal.
4. The transition signal is emitted once only, after the configured hold; `COMPLETE` and `ADVANCING` remain terminal for that round.
5. JVM unit tests cover both flag-change directions.
6. A canonical `NativeArenaTick` integration test exercises the last-arena case through the same production tick ordering.
7. Run `gradle --project-dir android :core:test --stacktrace --no-daemon` on the exact source/test SHA. No APK packaging, asset upload, release, merge or main write.

## Implementation

- Added a per-round latched `hasNextStageAtVictory` value when `PLAYING` first resolves to `WON`.
- The victory-hold completion uses the captured value, not a later render-loop argument.
- Added a bidirectional unit regression and a canonical-tick integration regression using a synthetic 2-spawn final arena.

## Source and license review

This is a local state-transition correction in existing Kotlin code. No third-party code or game assets were copied or added. The existing libGDX dependency is Apache-2.0 licensed; the framework is used as a dependency, not copied into the public repository. See the [official libGDX repository](https://github.com/libgdx/libgdx).

## Evidence

- Source/test SHA: pending.
- Native Kotlin/JUnit CI: pending.
- Mainline CI: pending.
- Xiaomi Pad 5 and Symbian/EKA2L1: **NOT TESTED**.
- Original game asset provenance is unchanged; no new assets uploaded.
