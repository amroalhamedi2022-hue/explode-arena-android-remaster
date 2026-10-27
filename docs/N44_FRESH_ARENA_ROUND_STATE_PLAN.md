# N44 — One fresh-state factory for arena transitions and retry

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft, no merge.

## Engineering question

Stage navigation and retry duplicated construction of the player, bomb simulation, vitality, opponent roster and round-flow state. Could the production renderer use one factory for both paths, prepare a target round before replacing the active arena, and prove that no mutable gameplay state leaks between attempts across the original 41 stage records?

## Source and license review

- Reviewed the existing ExplodeArenaNativeGame.selectStage() / retryStage() paths and the project's pure Kotlin simulation types before changing the design.
- Reviewed the official [libGDX repository](https://github.com/libgdx/libgdx), licensed Apache-2.0, as the framework reference. No third-party implementation was copied and no game art/audio/assets were added or modified.
- Stage data and collision/AI semantics remain provisional JS-mainline ports; this work does not claim Symbian/EKA2L1 parity.

## Acceptance criteria

1. create(), stage navigation and retry use NativeArenaRoundStateFactory.
2. A target round is constructed before the active texture/state is replaced; failure to construct the new state cannot partially reset the current round.
3. Each factory result owns independent player, bomb simulation, vitality, opponent roster and round-flow objects.
4. Retry begins with no prior bombs/flames, full default vitality, fresh bot controllers and PLAYING round state.
5. A source integration test parses all 41 original stage records and checks expected IDs, spawn counts, player spawn resolution and independent bot identity.
6. Native :core:test and existing Mainline checks pass on the exact source/test SHA. No APK packaging, signing or artifact upload.

## Implementation

- Added NativeArenaRoundState / NativeArenaRoundStateFactory.
- Routed native initialization, stage navigation and retry through installFreshRound.
- Stage construction now happens before disposing the old texture, and input quarantine remains enabled for stage changes/retry.
- Added NativeArenaRoundStateFactoryIntegrationTest for all 41 arenas and a reset regression with an active bomb and latched LOST outcome.

## Verification — 2026-10-27

- **Source/test SHA:** ac8edd6404619707088d4989887fadc76477c2f9.
- **PASS — Native Kotlin/JUnit:** [run #40484435074](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40484435074), job native-source-check, source-only :core:test.
- **PASS — Mainline JS/Python:** [run #40484435050](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40484435050), associated with the same source/test commit.
- **Artifacts:** 0 confirmed for both runs; the Native workflow runs tests only and does not assemble an APK.
- **NOT TESTED:** Android device lifecycle/rendering, Xiaomi Pad 5/HyperOS, original Symbian/EKA2L1 stage-transition parity, performance and audio.
- PR #2 remains Draft/open; PR #1 collision work remains separate. No merge, main write, release or copyrighted-asset upload.
- **Google Drive LIVE:** SYNC FAILED — this runtime exposes no Google Drive/Docs connector; no LIVE read/write or folder metadata verification is claimed.
