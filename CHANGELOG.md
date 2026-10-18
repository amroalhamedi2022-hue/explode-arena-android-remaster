# Mainline changelog

## 0.21 Alpha — 2026-10-18
- Replaced prototype rectangle blasts with 14-frame original NPS sprite sequences for center, horizontal and vertical flame segments (timing remains provisional).
- Bundled all 15 extracted original WAV effects, kept original six-channel MOD in source assets, and added opt-in, offline, loopable MP3 preview converted from that MOD; added UI sounds and mobile-safe playback control.
- Replaced purely wandering demo bot with deterministic grid-based pursuit, finite bomb ownership and rudimentary retreat. This is NOT a Symbian-accurate AI implementation.
- Extended offline/browser and source tests, including deterministic bot navigation and embedded soundtrack validation.
- No APK has been built; GitHub Actions workflow remains unexecuted until authorized.

## 0.20 Alpha — 2026-10-18
- Added optional local two-player practice to the existing simulation (not a separate prototype): independent controls, bombs, health and result state, touch D-pads and keyboard/gamepad input.
- Validated and centralized development gameplay rules; ensured bomb limits are per owner.
- Hardened the local-only Android WebView, blocked unknown assets from network fallthrough, enabled safe local preference persistence.
- Added private GitHub Actions APK-build workflow, pending first execution.
- Expanded deterministic, UI, offline and Android source-sync regression coverage.

## 0.19 Alpha
- Initial persistent mainline: 41 original stage previews, single-player, experimental opponent, original-direction sprite animation, bomb chaining, fixed-time simulation and offline WebView source.
