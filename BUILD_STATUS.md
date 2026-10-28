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
