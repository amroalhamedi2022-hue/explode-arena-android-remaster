# N68 — Bounded native collision substeps

Date: 2026-10-28

## Finding
`StageCollision.move` derives its collision-loop count from `ceil(distance / 3)`. The previous travel guard was proportional to the declared stage pixel dimensions. Because `StageData` validates the grid shape but does not impose a maximum dimension, a malformed/custom stage with very large dimensions could permit an impractically large collision loop.

## Implementation
- Add a fixed upper bound of 4,096 collision substeps per movement request.
- Centralize travel-bound and substep-count validation in `StageCollision.movementStepCount`.
- Use the same non-mutating preflight in `NativeArenaTick` before reserving the canonical tick, so rejection does not consume a tick or mutate player/world state.
- Preserve the existing <=3-pixel substep algorithm for accepted movements; do not change game assets or infer new Symbian mechanics.

## Acceptance criteria
1. A pathological custom stage + extreme finite speed is rejected before actor movement, bomb/flame mutation, vitality changes, or round/tick reservation.
2. The same canonical tick can be retried with a normal speed after the rejected request.
3. Existing movement, collision, 41-stage catalog, roster, bomb, and round regressions remain green.
4. No APK packaging/upload; Android device and EKA2L1 parity remain separate gates.

## Evidence
- Source + regression-test SHA: `5970814c4a3e02b66286f36821f5188b0b1101d5`.
- Native Kotlin/JUnit CI: [#40653227950](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40653227950) — PASS, `:core:test`, build successful in 40s.
- Mainline JS/Python PR CI: [#40653227854](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40653227854) — PASS.
- Artifact count: 0. APK packaging/upload: NOT PERFORMED.
- Android device gameplay and Symbian/EKA2L1 parity: NOT TESTED.
- No external assets or copied third-party code added; PR #1 collision work remains separate.
- Final documentation-head CI is recorded in the PR verification comment added after this plan update.
