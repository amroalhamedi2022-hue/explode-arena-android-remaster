# Build status — 2026-10-18 (Mainline 0.21 Alpha)

- Browser / offline HTML: regression tests run in Chromium desktop and simulated mobile-size viewport; local two-player controls and game logic included.
- 41 stage assets, 464 original object records and 106 marker references remain available for fidelity work.
- Shared Android WebView source: synced from web and checked byte-for-byte. WebView origin restricted; manifest requests no INTERNET permission.
- Private GitHub repository and Actions workflows: **ACTIVE**. Source validation and the Android build run successfully on `main`.
- Android Debug APK: **BUILT AND SIGNATURE-VERIFIED** by GitHub Actions on 2026-10-18. The artifact contains `app-debug.apk`, its SHA-256 checksum and the `apksigner --print-certs` report. This proves compilation and package signing, not device compatibility.
- Actual Android device/emulator: **NOT TESTED**.
- Game mechanics matched against EKA2L1: **NOT VERIFIED**. Local two-player mode is new, provisional practice behavior, not faithful Bluetooth implementation.
- HD asset remaster: **NOT STARTED**.

- 0.21: 42 original NPS explosion sprites wired into the renderer; all 15 original WAVs and original MOD source preserved. MOD-derived MP3 playback is opt-in (not tested on actual Android).
- Optional deterministic practice bot navigation and bomb planting added; replay/determinism regression tested. Original Symbian enemy logic remains unverified.

## 2026-10-28 — N58 full-roster deterministic arena trace

- Source: f9ea8faedb2689be760c9d819ba41c3c3aa9ecc2; integration tests: 130fcb397cc3485c2ae9a51744e22de3b20297a1; final source tweak: 6c16a203ff2fbbcb81f6234ae9b9e8795eb75ba6.
- Native Kotlin/JUnit CI #40595056469 PASS; Mainline JS/Python CI #40595056470 PASS on PR head 6c16a203ff2fbbcb81f6234ae9b9e8795eb75ba6.
- The new trace covers all 41 stage records and their real 1/2/4-spawn layouts; 180-tick four-spawn determinism and invalid-input atomicity passed.
- APK artifacts/build: 0. Device gameplay, audio, HD art fidelity, and Symbian/EKA2L1 parity remain NOT TESTED.
- No external or unlicensed game assets were added.


## 2026-10-28 — N59 pause-gate frame-time contract (CI pending)

- Production: NativePauseGate.advance now rejects NaN, infinity and negative frame deltas before the paused fast path, so pause state cannot mask malformed frame timing.
- Regression: NativePauseGateTest.pausedGateRejectsInvalidFrameDeltasBeforeThePauseShortCircuit verifies invalid inputs do not tick or invoke callbacks while backgrounded.
- Plan: docs/N59_PAUSE_GATE_DELTA_VALIDATION_PLAN.md.
- Source/test/docs commit: ff5e64f46c92f1fedbc72d048521523e5c19c7f5. Native Kotlin/JUnit CI #40598732190 PASS; Mainline JS/Python CI #40598732425 PASS on this exact SHA.
- Native job ran the core test task successfully; artifact query returned 0. No APK packaging/upload.
- Android lifecycle/device and Symbian/EKA2L1 parity remain NOT TESTED. Final docs-head recheck: PENDING.
- No assets/dependencies added, no merge or main write.


## 2026-10-28 N60 — fixed-step clock exhaustion (source/test and docs-head CI PASS)
- Production change: reject a fixed-step tick when the counter is already Long.MAX_VALUE, before invoking gameplay; add a non-negative internal initial counter seam for boundary tests. Round AI tick clock keeps its overflow guard and now has near-limit regression coverage.
- Tests: FixedStepClockTest covers the last representable tick, fail-before-callback on the next tick, zero-delta behavior at exhaustion, and invalid initial state; NativeRoundTickClockTest covers near-limit failure and negative initial state.
- Source/test commit cdd56104db6c886102c6e31afb32946f5d997abb: Native Kotlin/JUnit CI #40605994566 PASS (BUILD SUCCESSFUL in 53s); Mainline JS/Python CI #40605994534 PASS. Native artifact query: 0.
- Docs-head commit cc975afb85df5b3b4b07a9c5aaa3c9e00f2d2386: Native Kotlin/JUnit CI #40606358590 PASS; Mainline JS/Python CI #40606358711 PASS. Artifact query: 0.
- No APK build/package/upload; artifacts 0. Device lifecycle and Symbian/EKA2L1 parity: NOT TESTED.


## 2026-10-28 N61 — canonical tick speed validation (source/test and docs-head CI PASS)
- Production change: `NativeArenaTick.advance` now validates finite, non-negative `playerSpeed` before the latched-round early return, so malformed input cannot clear terminal bot animation state.
- Integration regression: NaN, positive infinity, and negative speed are rejected while the round is LOST; actor positions, bot walking state, bomb count, and round outcome must remain unchanged.
- Plan: `docs/N61_CANONICAL_TICK_SPEED_VALIDATION_PLAN.md`.
- Source/test commit `3f831dc44adb9ac52d2e397d7243202e9ab77f6d`: Native Kotlin/JUnit CI #40611588324 PASS (`BUILD SUCCESSFUL in 49s`); Mainline JS/Python CI #40611588341 PASS. Native artifacts: 0.
- Docs-updated head `5acb09802eb3391c54392381863e86380092820c`: Native Kotlin/JUnit CI #40611991238 PASS (`BUILD SUCCESSFUL in 50s`), Mainline JS/Python CI #40611991210 PASS; artifact count 0.
- Android device and Symbian/EKA2L1 parity: NOT TESTED. No APK packaging or upload.


## 2026-10-28 N64 — roster membership immutability (CI pending)
- Prevented a retained caller MutableList from bypassing roster owner/state invariants after construction; simulation methods now use a private copied roster and the accessor returns a detached snapshot.
- Added a regression for post-construction mutation of both the input list and a returned mutable snapshot.
- Plan: docs/N64_ROSTER_MEMBERSHIP_IMMUTABILITY_PLAN.md. Source/test + final-head CI: pending.
- Device gameplay and Symbian/EKA2L1 parity: NOT TESTED. APK artifacts: 0. No merge or main write.

## 2026-10-28 N64 — roster membership immutability — source/test PASS
- Source/test SHA: 6d8817a9b52dec61e641c47771f33a3ea476f8a6.
- Native Kotlin/JUnit CI #40629249497 PASS; Mainline JS/Python CI #40629249500 PASS. Native task is :core:test only; artifacts 0.
- Input MutableList and returned mutable snapshots are detached from the roster's private validated membership.
- Android device and Symbian/EKA2L1 parity: NOT TESTED. A documentation-only final-head recheck follows.

## 2026-10-28 N65 — canonical simulation tick monotonicity

- Source/test/plan commit: be81a164c2ac3d86778bbe8d58d7e397f4556337.
- PASS: native Kotlin/JUnit :core:test — CI #40634566362; Mainline JS/Python source checks — CI #40634566314 (PR) and #40634561086 (push), all on the same source/test SHA.
- The canonical roster tick now rejects duplicate/backward tick indices before gameplay mutation and reserves each tick before mutable work. Regression verifies a live bomb fuse does not age on rejected ticks, then advances on the next greater tick.
- Native source-test artifact query: 0. No APK packaging/build/upload was performed.
- Device gameplay, Android lifecycle, original audio/HD fidelity and Symbian/EKA2L1 parity: NOT TESTED.
- Google Drive LIVE sync: SYNC FAILED — no Google Drive/Docs connector is exposed in this execution environment. No LIVE read/write or folder metadata verification is claimed.
- No external game assets added; PR #1 remains separate; no merge or main write. Documentation-only follow-up does not change source behavior.

## 2026-10-28 N67 — canonical tick preflight and victory-hold monotonicity

- Source/test SHA: a57caa1ef14a8ffef635a8e03d3e6c8b3f4f361b.
- PASS: Native Kotlin/JUnit CI #40646233882; Mainline PR CI #40646233855; Mainline push CI #40646228550. All source/test runs succeeded; Native `:core:test` completed in 50s.
- Duplicate ticks are rejected before player/world mutation; round/presentation clock also prevents a repeated tick from consuming the victory hold twice. Added two canonical-tick integration regressions.
- Artifact query: 0. No APK packaging/build/upload.
- Android device gameplay and Symbian/EKA2L1 parity: NOT TESTED. No external game assets. PR #1 remains separate; no merge or main write.
- Google Drive LIVE sync: SYNC FAILED — no Drive/Docs connector is exposed.


## 2026-10-28 — N69 canonical actor terrain-position preflight

- Source/test SHA: b68d34d409398e2ad98889c93e8206c431b9bc36.
- Native Kotlin/JUnit CI #40660972234 PASS; Mainline PR CI #40660972227 PASS. Native task: :core:test only; artifacts 0.
- Player/opponent positions overlapping solid terrain are rejected before canonical tick reservation and movement; same-tick repair/retry regressions pass.
- Android device gameplay and Symbian/EKA2L1 parity: NOT TESTED. No APK packaging, external assets, merge or main write.


## 2026-10-28 N75 — stage/simulation binding consistency
- Source/test SHA: `dff3a69fd394b5b8dbb38a6d12000eff5b472603`.
- **PASS:** Native Kotlin/JUnit CI #40703078096 and Mainline PR CI #40703078131.
- Mismatched stage definitions now fail during non-mutating preflight; regression covers same-ID/different-grid rejection, no partial mutation, direct roster API and same-tick retry.
- APK artifacts: 0. Device gameplay and Symbian/EKA2L1 parity: NOT TESTED.
