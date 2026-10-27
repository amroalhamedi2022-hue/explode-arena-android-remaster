# N58 — Full-roster deterministic arena replay

**Date:** 2026-10-28  
**PR:** #2 — Native Kotlin/libGDX; keep Draft/open, no merge.

## Engineering question

Do repeated native simulation runs produce the same full gameplay trace across 1-, 2-, and 4-spawn arenas when we record every bot, bomb, fuse, flame, remote owner, health/invulnerability state, and round transition—not just the player position?

## Implementation

- Add a headless replay harness that drives the same NativeArenaTick used by the libGDX renderer.
- Validate input before mutation; preserve retry-only LOST/DRAW behavior and victory-hold ticks.
- Record all bot actors in stable owner order, exact double bits, bomb IDs/fuses, flame cells, remote ownership, and round state.
- Exercise a 180-tick four-spawn script and the real 41-stage catalog with the actual 18 one-spawn, 2 two-spawn, and 21 four-spawn layouts.
- No original game assets or third-party code copied or added.

## Acceptance

1. Two identical four-spawn scripts produce byte-identical per-tick traces including bots, bomb identities, and flame cells.
2. Invalid input leaves the complete snapshot unchanged.
3. All 41 actual stage records produce matching 30-tick traces in two independent sessions.
4. Native Kotlin/JUnit passes on the exact source/test SHA and again on the docs-updated PR head.
5. This proves determinism of the current provisional port only; it does not prove Symbian/EKA2L1 parity.
6. No APK build/artifact.

## Evidence

- Source/test SHA: pending.
- Final PR head: pending.
- Native Kotlin/JUnit CI: pending.
- Mainline JS/Python: check actual workflow trigger; no claim until verified.
- Device/audio/HD art/EKA2L1: NOT TESTED.
