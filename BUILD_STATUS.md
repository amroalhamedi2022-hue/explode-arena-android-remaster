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
