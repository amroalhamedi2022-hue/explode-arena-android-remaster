# N39 — Freeze lost/draw rounds until retry

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft, no merge.

## Engineering question

After the production round flow enters LOST or DRAW, do idle frames and remote-only input continue advancing the fixed-step clock even though the arena is terminal? Source inspection identifies that the command arbiter always returns `advanceWorld = true` outside pause/navigation/retry. A regression must prove the clock stays frozen while the victory-hold state remains advanceable.

## Acceptance criteria

1. A lost or drawn round accepts the explicit retry action only; idle and remote-only frames resolve to an empty command.
2. The terminal result remains stable while repeated frames run; the fixed-step clock does not advance and therefore cannot age gameplay timers.
3. A bomb/tap retry still resolves to `Decision(retry = true)` and does not advance the old round in the same frame.
4. WON/victory-hold frames still advance when `canRetry == false`, preserving the one-shot stage transition.
5. Run Native Kotlin/JUnit `:core:test` and Mainline JS/Python source checks on the exact source/test SHA; zero APK artifacts and no Android packaging.
6. No `main` write, merge, release, original asset upload, or auto-merge.

## Implementation

- `NativeFrameCommands.resolve` now returns an empty decision for retryable terminal states after honoring the explicit retry input.
- Added a JVM integration-style regression for both LOST and DRAW that checks idle/remote-only frames, the retry command, and 40 frozen frame attempts against `FixedStepClock`.
- Existing victory-hold tests continue to require clock advancement for non-retryable WON frames.

## Scope and evidence boundaries

This is native input/clock safety, not a claim about original Symbian menu behavior. It changes no movement, bomb, AI, collision, fuse, blast or damage rules. Physical device and Symbian/EKA2L1 parity remain **NOT TESTED**.
