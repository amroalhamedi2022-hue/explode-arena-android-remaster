# N59 — Validate fixed-step frame deltas before pause short-circuit

## Question
Can a caller pass NaN, infinity, or a negative frame delta to the production pause gate and have it silently accepted only because the game is paused?

## Change
NativePauseGate.advance now enforces the same finite, non-negative frame-time contract as FixedStepClock.advance before returning early for paused/backgrounded states. No simulation tick, fuse, bot, or round state may advance on invalid input.

## Acceptance criteria
- NaN, positive/negative infinity, and negative delta are rejected while backgrounded.
- Rejected inputs leave the fixed-step tick count unchanged and do not call the tick callback.
- A valid zero delta while paused returns zero and does not call the callback.
- Existing pause/resume/fuse/lifecycle regressions remain green.
- No APK packaging, assets, dependencies, or main-branch writes.

## Evidence
- Source/test/docs commit ff5e64f46c92f1fedbc72d048521523e5c19c7f5: Native Kotlin/JUnit CI #40598732190 PASS and Mainline JS/Python CI #40598732425 PASS. Artifact query: 0. Final docs-head recheck: PENDING.
- Device lifecycle and Symbian/EKA2L1 parity: NOT TESTED.
