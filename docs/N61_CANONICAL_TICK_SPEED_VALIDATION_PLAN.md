# N61 — Validate canonical movement speed before terminal-round fast paths

## Engineering question
Does the canonical native arena tick reject non-finite or negative player speed before *any* mutable subsystem is touched, including the terminal-round early return?

## Why a source-only inspection is insufficient
The invariant spans the production tick coordinator, opponent animation state, player position, and live bomb state. A focused Kotlin/JUnit regression can execute the real coordinator with a latched LOST round and prove validation ordering without building or packaging Android.

## Implementation
- Validate `playerSpeed` as finite and non-negative alongside normalized input and the tick number, before checking whether the round is latched.
- Add an integration regression that calls the production `NativeArenaTick.advance` with NaN, positive infinity, and negative speed after the round has latched LOST.
- Assert each invalid call throws before clearing bot walking, moving actors, changing the bomb set, or changing the round outcome.

## Acceptance criteria
- NaN, positive infinity, and negative player speed are rejected before the terminal fast path mutates any state.
- The player's and bot's positions, bot walking flag, bomb count, and LOST outcome remain unchanged after every rejected call.
- Existing 41-arena canonical tick, collision, bomb, round-flow, and lifecycle regressions continue to pass.
- Native Kotlin/JUnit source CI and Mainline JS/Python CI pass; no APK packaging, asset upload, dependency change, merge, or main-branch write.

## Evidence
- Production source commit: `781820215360137489aefdc7a2b2b159d780bd1c`.
- Source + regression-test commit: `3f831dc44adb9ac52d2e397d7243202e9ab77f6d`.
- Final evidence/docs head before this result update: `92eb506b502984ff71d971b4af17228c056b62f4`.
- Native Kotlin/JUnit CI #40611588324: PASS — `:core:test`, `BUILD SUCCESSFUL in 49s`, 0 artifacts.
- Mainline JS/Python CI #40611588341: PASS — all workflow steps completed successfully.
- Android device, full gameplay, and Symbian/EKA2L1 parity: NOT TESTED.
- Final docs-updated PR-head CI after this evidence commit: PENDING until the workflow result is observed.
- APK artifacts: 0; no APK build requested.
