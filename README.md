# Explode Arena — Single Mainline (0.21 alpha)

Continuously developed, private-source Android faithful-remaster project based on user-owned Symbian SIS research. **One codebase, no repeated disposable prototypes.** Original extracted art/audio and 41 stage renderings are used as references; collision, timers, AI and bomb rules remain experimental until EKA2L1 comparison.

### Implemented in this update

- Same fixed-60Hz seeded simulation with improved per-owner bomb accounting and configurable/validated development rules.
- **Optional local two-player practice**: independent player movement and bombs, P1/P2 health, elimination/win/draw logic and separate touch D-pads/bomb buttons. Keyboard P1 arrows/Space, P2 WASD/F, or two gamepads. This is **not** verified original Bluetooth multiplayer or a complete game mode.
- Original 41 stage previews, directional character animations and selected original WAV sounds.
- Android WebView source restricted to `appassets.androidplatform.net`, no INTERNET permission, missing assets return 404; local preferences via DOM storage.
- Private Android debug APK GitHub Actions workflow (`workflow_dispatch` and pushes to `main`) ready **but not executed**. It uses JDK 17, Gradle 8.7, SDK 35 and validates the APK if compilation succeeds.
- Expanded deterministic and browser tests, standalone HTML containing all assets, and exact source/Android resource sync test.

See `README_AR.md` and `BUILD_STATUS.md` for current limitations, run instructions and future work. **There is no built Android APK in this package.**

## Mainline 0.21
Original 42 NPS blast animation frames, all 15 original WAV effects and an optional MOD-derived music preview (local only). The practice bot now navigates and plants bombs with provisional behavior. Build/tests still require actual Android verification.
