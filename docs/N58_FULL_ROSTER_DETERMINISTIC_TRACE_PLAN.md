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

- Source commit: f9ea8faedb2689be760c9d819ba41c3c3aa9ecc2; integration-test commit: 130fcb397cc3485c2ae9a51744e22de3b20297a1; final source tweak: 6c16a203ff2fbbcb81f6234ae9b9e8795eb75ba6.
- Exact tested PR head: 6c16a203ff2fbbcb81f6234ae9b9e8795eb75ba6.
- Native Kotlin/JUnit CI #40595056469: PASS; log reports BUILD SUCCESSFUL in 52s.
- Mainline JS/Python CI #40595056470: PASS on the same SHA.
- APK artifacts: 0. Device/audio/HD art/EKA2L1: NOT TESTED.
- The evidence-only docs commit that follows is separately rechecked.
- Device/audio/HD art/EKA2L1: NOT TESTED.
