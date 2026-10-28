# N65 — Canonical simulation tick monotonicity

Date: 2026-10-28. Explode Arena PR #2 remains open Draft; PR #1 remains separate.

## Engineering question

The canonical advanceSimulationTick path rejected negative ticks but accepted a repeated or older non-negative tick. A stale callback or integration caller could therefore advance bomb fuses/flames and opponent movement more than once for the same logical 60 Hz step. The current render loop is expected to supply a monotonic per-round sequence, so the roster should enforce that contract itself.

## Acceptance criteria

1. The first canonical tick may be any non-negative value; every subsequent tick for that roster must be strictly greater than the previously reserved tick.
2. Duplicate and out-of-order ticks fail before actor movement, bomb fuse/flame advancement, damage, or round-state mutation.
3. Reserve the tick before mutable gameplay work; if an internal operation fails partway through, the same tick cannot be replayed to duplicate partial effects.
4. A fresh roster has a fresh sequence, consistent with the per-attempt NativeRoundTickClock.
5. A headless integration regression with a solo roster and live bomb proves duplicate/backward ticks leave the fuse unchanged and the next greater tick advances it exactly once.
6. Existing 41-arena, 1/2/4-spawn deterministic integration tests remain green.

## Validation boundary

- CI runs gradle --project-dir android :core:test only; no APK packaging or artifact upload.
- Android device gameplay, audio/HD fidelity, and Symbian/EKA2L1 parity remain NOT TESTED.
- No external game assets are added; no merge or main write.
