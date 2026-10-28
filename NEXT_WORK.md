# Next milestones — one mainline, no disposable prototypes

1. **Test the verified debug APK on Xiaomi Pad 5 / Android phone**, including installation, launch, WebView audio, gamepads, orientation, pause/resume and multi-touch. Capture any crash/logcat output and screen recordings before changing gameplay constants.
2. **Gameplay equivalence**: use existing EKA2L1 footage when necessary to measure bomb fuse, speed, blast reach, collision, sprite directions, AI, player spawn and layering. Replace provisional constants with grounded equivalents.
3. **Original mechanics and modes**: menus, opponents, bonuses, rule sets, local/network multiplayer where feasible; distinguish original Bluetooth from modern optional local/online modes.
4. **Faithful HD artwork and sound** using original recovered NPS/BRK maps and sprites; keep original-pixel rendering as optional comparison and verify source-to-HD mapping.
5. **Publishable personal release**: code and source in private GitHub, reproducible APK builds, regression comparisons, app performance/stability and user acceptance.

The debug APK is now a verified build artifact. Do not claim physical-device compatibility, graphics remastering or Symbian behavior equivalence without corresponding tests and comparisons.

0.21 completed: original NPS blast visuals and optional original soundtrack preview; practice bot pursuit and offensive bombs. Validate original blast timing/audio length against EKA2L1 before marking faithful.


## 2026-10-28 — N71 source checkpoint

- Corrected canonical actor/bomb start-state validation and prevented bomb placement over another living actor.
- Final production/test SHA: `5a6b21dfc9571e67164457520df5a3a5ce36e5bb`.
- **PASS:** Native Kotlin/JUnit [#40670102041](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40670102041); Mainline PR [#40670102080](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40670102080); Mainline push [#40670091795](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40670091795). Native artifacts: 0; no APK build.
- Device gameplay and Symbian/EKA2L1 parity remain **NOT TESTED**.
- Next: device-side gameplay capture and EKA2L1 comparison before replacing provisional timing/collision/AI rules; continue native feature work without importing unlicensed assets.


## 2026-10-28 — N75 source checkpoint
- Source/test commit: `dff3a69fd394b5b8dbb38a6d12000eff5b472603`.
- Native Kotlin/JUnit CI #40703078096 PASS; Mainline PR CI #40703078131 PASS. Native artifacts: 0.
- Arena and bomb simulation must share the same structural stage definition; mismatch is rejected before player/bot/fuse/round mutation.
- Next: prioritize a controlled Android gameplay run and EKA2L1 comparison before claiming native/Symbian parity; continue source-level milestones without adding unlicensed assets.
