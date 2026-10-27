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
- Source and regression test added in the N59 PR commit; CI: PENDING.
- Device lifecycle and Symbian/EKA2L1 parity: NOT TESTED.
