# N75 — Bind the bomb simulation to the canonical arena stage

Date: 2026-10-28  
Status: implementation committed; CI pending at plan creation.

## Engineering question

Can a caller accidentally advance player/opponent movement against one `StageData` while the `NativeBombSimulation` calculates placement, collision and blast geometry from a different stage?

Source inspection found that both objects accepted stage data independently and `NativeOpponentRoster.validateStartState` did not assert that the supplied stage matched the stage captured by the bomb simulation. Stage IDs alone are not sufficient because tests/custom stages can reuse an ID with different geometry.

## Change

- Add a structural stage-definition check covering ID, image path, dimensions, obstacle origin, spawn coordinates, grid row count and every grid row's cell contents.
- Run it at the start-state preflight shared by canonical arena ticks, direct roster ticks and the movement-only entry point.
- Reject mismatches before player movement, bot decisions, bomb fuse aging, health updates or tick reservation.
- Allow a separately constructed but structurally equivalent stage definition.
- No external assets, dependencies or copied gameplay implementation.

## Acceptance criteria

1. Same ID with a different collision grid is rejected.
2. Canonical `NativeArenaTick.advance` leaves player/bot positions, pass-bomb grace, health, bomb fuse and round state unchanged on rejection.
3. Direct `advanceSimulationTick` rejects before consuming the tick or aging fuses.
4. Repairing the caller to use a matching simulation allows the exact same tick index to succeed.
5. Structurally equivalent stage copies remain compatible.
6. `:core:test` and Mainline PR source CI pass on the exact source/test commit; docs-head recheck follows.
7. APK packaging/artifacts remain zero; Android device gameplay and Symbian/EKA2L1 parity remain NOT TESTED.

## Scope boundary

This is an internal native-world consistency invariant, not evidence of original Symbian collision/blast parity. PR #1 collision work remains separate. No merge or `main` write.
