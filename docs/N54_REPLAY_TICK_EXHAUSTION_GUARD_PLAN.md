# N54 — Fail-closed replay tick exhaustion

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft/open, no merge.

## Engineering question

Can a replay session reach `Long.MAX_VALUE`, overflow its tick counter to a negative value, and mutate bombs/movement/health before returning an invalid trace? Normal play cannot practically reach this boundary, but the replay harness is used as a deterministic evidence tool; a wrapped tick must never masquerade as a valid new sequence.

## Implementation

- Added an optional non-negative `initialTick` for explicit replay checkpoint/resume setup and boundary testing; the default remains zero.
- Reject negative initial ticks at construction.
- Check for tick exhaustion **before** bomb placement, movement, fuse advancement, or health mutation.
- Keep the existing replay schema and ordinary frame sequencing unchanged.

## Acceptance criteria

1. Default sessions still start at tick zero and existing deterministic replay comparisons remain unchanged.
2. Negative initial ticks are rejected.
3. A step at `Long.MAX_VALUE` fails with `IllegalStateException` before changing the actor, bombs, flames, or other trace state.
4. Native Kotlin/JUnit and Mainline JS/Python pass on the exact source/test SHA and on the final docs-updated PR head.
5. No APK packaging/signing/upload, new assets/dependencies, merge, main write, or auto-merge.

## Verification ledger

- **Source + regression-test commit:** `e2364165398de10eaab6240b4af39ad8e4a8a8fe`.
- **PASS — Mainline JS/Python on source/test commit:** [CI #40563943359](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40563943359).
- **Native Kotlin/JUnit:** run [#40563943582](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40563943582) was queued/pending when this ledger entry was prepared; no PASS is claimed for that run yet.
- Final docs-head recheck will be recorded in the PR conversation after both current workflows finish.
- Device and Symbian/EKA2L1 behavior: **NOT TESTED**.
- APK artifacts: **0 expected**; this workflow is source/JVM-only.
- No third-party game code/assets copied.

