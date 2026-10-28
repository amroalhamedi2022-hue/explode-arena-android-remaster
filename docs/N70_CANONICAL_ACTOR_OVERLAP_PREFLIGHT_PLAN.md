# N70 — Canonical actor-overlap preflight

Date: 2026-10-28

## Engineering question (before CI)
Can the production `NativeArenaTick` reject a playing-state world in which the player and a living opponent overlap, or two living opponents overlap, **before** reserving the canonical tick or mutating positions, vitality, bomb fuses/flames, roster state, or round flow?

Source inspection is not enough: the current preflight checks finite positions and terrain occupancy, but does not compare the complete starting actor set for pairwise overlap. The regression must demonstrate fail-before-mutation and same-tick retry after repair.

## Implementation
- Extend the existing non-mutating PLAYING preflight to check each player/opponent and opponent/opponent pair with `StageCollision.blockedByActors`.
- Preserve strict edge-contact semantics (touching edges are allowed; positive-area overlap is rejected).
- Run the check before `roundFlow.reserveCanonicalTick`.
- Add canonical-tick integration regressions for player/bot overlap and bot/bot overlap. Include a live bomb where possible to prove a rejected request does not age its fuse, then repair the actor and reuse the same tick.

## Acceptance criteria
1. Player/living-opponent overlap is rejected before any gameplay mutation.
2. Living-opponent overlap is rejected before any gameplay mutation.
3. Bomb/flame state, health, actor positions, round state, and canonical tick remain unchanged on rejection.
4. Repairing the overlap allows the exact same tick to succeed.
5. All existing 41-arena, collision, bot, bomb, and round tests pass.
6. No APK build/package/upload. Android device gameplay and Symbian/EKA2L1 parity remain separate NOT TESTED gates.

## Rights and boundaries
No game art/audio/assets or dependencies are added. PR #1 remains separate. No merge, main-branch write, or paid service.

## Evidence

- Production change commit: `eb1c96336f50cf3d7d80ea2f1cf7a981af21c3df`.
- Regression-test commit / source-test CI head: `ed32f55a9ffae0ddc5feb5b188dc19378a9224e6`.
- **PASS — Native Kotlin/JUnit:** [CI #40663938262](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40663938262), `:core:test` completed successfully.
- **PASS — Mainline JS/Python PR CI:** [CI #40663938114](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40663938114), source checks completed successfully.
- Final documentation-head recheck: pending after this evidence update.
- Native artifacts: 0 at source-test stage. No APK build/package/upload.
- Android device gameplay and Symbian/EKA2L1 parity: NOT TESTED.
- No assets or dependencies added; PR #1 remains separate; no merge or main-branch write.
