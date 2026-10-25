# N15 — Native bot active-flame escape (provisional)
Date: 2026-10-25
PR: #2 (Draft); no main merge, no new game assets.

## Observed source defect
NativeBotController.canStep and NativeBotEscapeRoute.firstDirection reject every intermediate point that is still burning. A bot already inside a live flame cannot leave it because a 54px/s move advances only 0.9px per 60Hz tick, while the flame footprint spans many pixels. It freezes even if a reachable safe cell exists.

## Acceptance questions and gates
1. A bot already burning can choose a nonzero cardinal escape move through the burning intermediate cells to a genuinely non-burning refuge; no fictitious bomb is added.
2. Existing own-bomb and timed chain deadlines still constrain escape; an isolated remote bomb has no automatic fuse.
3. Normal chase/planting cannot intentionally enter flames; bot never plants while burning.
4. Deterministic Kotlin JUnit regression and existing Mainline tests pass on exact code SHA.
5. Native Android/Xiaomi Pad 5, original EKA2L1 flame/hitbox timing and game-wide parity: NOT TESTED until physical device evidence.

## CI engineering test question, before source changes
Does the new native Kotlin escape planner + controller integration typecheck, pass JVM regressions, and remain compatible with the existing Android/libGDX app entrypoint? Source inspection/JVM-only checks cannot prove the Android module compiles against the changed call signature. The repository's existing native CI may execute :app:assembleDebug internally as a compile gate; no APK upload/release, no artifact delivery and no physical-device claim. Only run existing free source checks/CI; do not dispatch separate APK build.

## Open source/license review
Checked https://github.com/libgdx/gdx-ai (Apache-2.0, maintained libGDX ecosystem, general A*/steering). This defect needs the project's already integrated deterministic, time-aware bomb/flame BFS and 60Hz collision model; importing gdx-ai adds dependency and behavior mismatch without fixing flame transit. Adapt existing code; no third-party code copied and no new dependencies/assets.

## Verified outcome (2026-10-25)
- Production commits: escape route 55eca1394eef5cca1c0e89264cf6eb5855359a73 ; controller 5abe6fd719adb8a51abccc273e572c8ed032b8f8.
- Five Kotlin JUnit regression cases including repeated fixed-step movement to an unlit refuge: exact tested code SHA 4b1d0a11a388ce016f73ed6da6dfd6cfd974b6fd.
- PASS Native Kotlin :core:test (job native-source-check, actual Gradle test step SUCCESS): https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40223095067
- PASS Mainline JS/Python source CI on same SHA: https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40223095023
- Native artifacts: zero. Workflow is core source check WITHOUT Android packaging; no APK was built or delivered in this run. Android module compatibility from an assembled APK is NOT TESTED, despite the pre-recorded engineering question; do not overstate coverage.
- Gate count 4/5: code, five regressions, Native JUnit CI, Mainline CI PASS; fifth gate physical Android/EKA2L1 parity NOT TESTED. Whole-project % unknown. No public asset uploads, main changes or merge.
- NEXT: alternate to App Commander; then independently validate flame escape with real Android device and original EKA2L1 evidence.
