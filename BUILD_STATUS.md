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
