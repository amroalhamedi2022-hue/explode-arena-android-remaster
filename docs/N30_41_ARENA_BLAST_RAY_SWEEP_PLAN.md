# N30 — 41-arena blast-ray geometry sweep

**Date:** 2026-10-26  
**PR:** #2 Native Kotlin/libGDX  
**Status:** SOURCE TEST ADDED; CI VERIFICATION PENDING  
**Production gameplay source changed:** No in this slice; this milestone adds an independent integration oracle before any ray implementation changes.

## Engineering question

Does the current provisional native bomb ray produce the same set of flame cells as a denser independent one-pixel segment sampler for every valid 16px bomb placement across the 41 real Mainline arena records, including offset obstacle grids and map edges?

A few hand-picked wall fixtures are not sufficient to establish coverage across the real catalog. The test is intended to expose thin-wall tunneling, incorrect map-boundary termination, duplicate flame cells, and regressions caused by arena-specific obstacle offsets.

## Acceptance criteria

1. Read the checked-in `web/assets/stages.js` catalog through `StageCatalog.parse`; assert IDs 0–40 and the 18/2/21 distribution of 1/2/4-spawn arenas.
2. Enumerate every 16px cell center within each stage's dimensions.
3. For each center where the shared collision contract permits bomb placement, detonate an explicitly granted remote bomb to avoid timing-dependent fuse behavior.
4. Compare the production flame-cell set against a separate reference ray sampler that samples each one-pixel segment, rather than the production implementation's four substeps per cell.
5. Assert there are no duplicate flame cells and require more than 1,000 valid real-map placements to be checked.
6. Run the actual Gradle `:core:test` workflow on the exact source/test commit, then repeat on the documentation-updated PR head. No APK/package task is permitted for this question.

## Scope and limitations

- The reference shares `StageCollision.blocked` as the arena collision contract but uses a denser sampling interval; it is independent of the production ray-step loop, not an independent proof of Symbian geometry.
- This checks provisional JS-derived native blast geometry only. It does not establish the original Symbian fuse, blast range, damage, or EKA2L1 parity.
- No original assets, dependencies, paid services, APKs, releases, or main-branch writes are part of this milestone.
- Physical-device graphics/audio/performance and full human-played rounds remain NOT TESTED.

## Expected evidence

Record source/test SHA, test blob SHA, CI run URL and result, latest docs-updated PR-head SHA and CI result, and whether the actual test count exceeded 1,000. Any failure must be documented as FAIL, not silently weakened.
