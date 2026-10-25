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

## Result
PENDING source implementation and exact-SHA CI.
