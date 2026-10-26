# N31 — Independent 41-arena blast collision oracle

**Date:** 2026-10-26  
**PR:** #2 Native Kotlin/libGDX  
**Status:** PASS — Native source/JUnit and Mainline CI passed on the docs-updated PR head  
**Production gameplay source changed:** No. This milestone hardens the integration oracle before any production collision/ray changes.

## Engineering question

Does the N30 blast-ray sweep remain valid when the reference ray does not call the production `StageCollision.blocked` helper?

N30 used a denser one-pixel ray sampler, but its wall query delegated to the same production collision routine used by `NativeBombSimulation`. That shared dependency could let a defect in tile-overlap indexing make both sides agree incorrectly. N31 removes that common implementation from the oracle.

## Acceptance criteria

1. Parse the checked-in `web/assets/stages.js` and assert ordered arena IDs 0–40 and the 18/2/21 distribution for 1/2/4 spawns.
2. Compute reference wall occupancy directly from `StageData.grid`, `obstacleX/Y`, tile size 8px, world bounds, and the reference radius; do not call `StageCollision.blocked` from the reference path.
3. Trace four cardinal rays with a one-pixel segment sampler and local 16px bomb-cell constants.
4. For every independently traversable real-map bomb cell, require the production simulation to accept placement and compare its detonation footprint to the independent reference.
5. Reject duplicate production flame cells and require more than 1,000 valid real-map placements.
6. Run the actual `gradle --project-dir android :core:test` workflow on the exact test commit and again on the docs-updated PR head. No APK or Android package build is needed for this source-geometry question.

## Scope and limitations

- The oracle independently evaluates raw grid occupancy, but it still uses the checked-in stage data and the project's intended 8px/16px geometry assumptions.
- This is evidence about the provisional native implementation only; it does not prove original Symbian/EKA2L1 blast geometry, fuse timing, damage or AI parity.
- No art/audio assets, new dependencies, APKs, releases, paid services or main-branch writes are part of this milestone.
- Real Android device, rendering, sound and full-match behavior remain NOT TESTED.

## Evidence

- Test source commit: `1a069b0e0e0fed7b55a728d34fd8defd2e9059f5`.
- Test blob: `07dfd6f39237d8626cf0a16f4b2003a7ee6633a2`.
- The test-only commit's first run (#40399638446) was cancelled by the branch concurrency group after documentation was added; it is not counted as PASS.
- **PASS — Native Kotlin/JUnit on docs-updated head `9b02708ff028d92f7214a6beaca6e50dde7ec8fd`:** [run #40399730989](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40399730989), Gradle `:core:test` step completed successfully; 0 artifacts.
- **PASS — Mainline on the same docs-updated head:** [run #40399731014](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40399731014), all source/resource checks succeeded.
- The final acceptance-ledger update will create a docs-only head; that head still requires its own CI recheck.
