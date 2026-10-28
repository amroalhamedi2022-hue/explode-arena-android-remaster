# N17 — Stage catalog identity and geometry integrity

Date: 2026-10-26. PR #2 remains Draft; PR #1 collision branch remains separate.

## Engineering question
Can the native parser accidentally accept a stage record whose ID points at another map image, whose collision grid is shifted beyond the arena bounds, whose grid contains a negative/fractional/non-numeric tile, or whose spawn lies outside the arena? N16 bounded record sizes, but the image regex alone still permits a valid-looking wrong map filename, and integer conversion alone may normalize malformed tile values.

## Acceptance criteria
1. Every stage ID 0..40 must reference its matching `assets/maps/map_NNN.png` path.
2. Collision geometry must be wholly aligned with the stage dimensions before grid rows are allocated; reject shifted origins and malformed cell values.
3. Spawn coordinates must be finite and within the corresponding stage bounds; preserve original coordinates (native runtime spawn resolver may still reposition obstructed 5px hitboxes).
4. The actual source inventory still parses to 41 ordered arenas with the established spawn distribution 18/2/21 for 1/2/4 spawns.
5. JUnit regressions reject each tampered case and existing 41-arena spawn/bot/bomb integration tests remain green on the exact code/test SHA.
6. Run only the free source/JVM check `gradle --project-dir android :core:test --stacktrace --no-daemon`; no APK packaging, artifact upload, merge or main-branch write.

## Why source tests are sufficient for this question
This is a parser/data-contract question: whether malformed metadata is rejected before the native runtime consumes it. The existing no-APK Kotlin/JUnit workflow exercises the actual StageCatalog and real `web/assets/stages.js` fixture. Android packaging cannot prove original Symbian behavior and is unnecessary for these acceptance criteria.

## Baseline
- PR #2 branch before N17: `fe44edb9c791e1e32cfeb222288a45acfa7450ae`
- N16 source+41-arena sweep: `ce46a8bc1d285bb654eeb92e3bcad9f3f9738039`; Native Kotlin/JUnit run 40232581270 PASS and Mainline run 40232581282 PASS.
- Physical Xiaomi Pad 5, actual rendering/audio, and Symbian EKA2L1 parity remain NOT TESTED.
- Google Drive LIVE sync remains blocked in this runtime because Google Drive/Docs connector tools are unavailable; do not claim sync until read-back verification is possible.

## Result — N17 SOURCE ACCEPTANCE PASS (2026-10-26)
- Production parser commit: `3258cb9f73dde6471c63295032be02eb2fb75e87` (stage-to-map identity, geometry bounds, integral/nonnegative numeric grid cells, and spawn bounds).
- Regression commit / exact source+test SHA: `230848f38f013b8a12a67b50d37297a1abf57150`.
- **PASS:** Native Kotlin/JUnit actual Gradle step `gradle --project-dir android :core:test --stacktrace --no-daemon`, run [40242074635](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40242074635), job `native-source-check`, step `Test native Kotlin core without Android packaging` SUCCESS.
- **PASS:** Mainline validation on the same exact SHA, run [40242074624](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40242074624).
- Regressions cover wrong map identity, shifted collision origin, out-of-bounds spawn, negative/fractional/non-numeric grid cell, plus the existing 41-real-arena inventory and spawn-layout integration checks.
- **Artifacts: []** No APK assembled or uploaded. Android/Xiaomi Pad 5 rendering/audio, full gameplay and Symbian EKA2L1 parity remain **NOT TESTED**.
- Google Drive LIVE Handoff/INDEX: **SYNC FAILED** because Google Drive/Docs connector functions are unavailable in this runtime. GitHub docs are the verified recovery checkpoint; no Drive sync is claimed.

## Next
Alternate to App Commander for its next functional milestone, then return to Explode Arena for native runtime/device acceptance planning. Do not merge PRs or write to main.
