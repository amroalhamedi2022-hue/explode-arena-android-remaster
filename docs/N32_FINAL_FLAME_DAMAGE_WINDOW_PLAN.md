# N32 — Consistent final-flame damage window

**Date:** 2026-10-27  
**PR:** #2 Native Kotlin/libGDX  
**Status before CI:** implementation and regression tests committed; CI pending.  
**Production source changes:** Yes. No assets or third-party implementation code added.

## Engineering question

Does a native actor receive damage when it occupies a flame cell during the last 60Hz interval, even though the flame reaches zero lifetime and is removed from the renderable flame list at that interval's boundary? Do the player, opponent roster, and deterministic replay harness use the same boundary semantics?

Source inspection showed that `NativeBombSimulation.tick()` removed expired flames before callers sampled `NativeVitality`. The source therefore strongly suggested a missed final damage interval, but source inspection alone could not establish the floating-point boundary behavior or confirm it across the checked-in 41-stage data set. The required evidence is executable JVM integration coverage.

## Implementation

- `NativeBombSimulation` now records flame cells that expire during the most recently completed tick separately from renderable active flames.
- `isBurningDuringLastTick` is the explicit post-`tick()` damage query: it includes currently active flames and flames that expired at that exact boundary. Normal `isBurning` and the renderable `flames` list still exclude expired flames.
- The native game tick now moves the player and opponents against the same pre-tick world, advances bomb/fuse/flame state, then applies player and opponent damage using the same completed-tick sample. This avoids letting bots move after the flame boundary but before damage is decided.
- The roster exposes a separate post-tick damage phase; the replay harness uses the same damage query.
- Added a 41-arena integration test with shortened legal timers, exact detonation checks, final-tick damage, render-state cleanup and next-tick expiry of the damage window. Added a bot-roster regression.

## Acceptance criteria

1. A flame created by the current detonation is damage-active on that tick.
2. A flame removed from `flames` at its expiry boundary still counts for damage on that final interval.
3. The following tick clears the expired-flame damage window.
4. The above boundary is tested on all 41 ordered source stages, with the 18/2/21 spawn distribution asserted.
5. Player, bot roster and replay code use the same post-tick damage semantics; bots move before the bomb tick and apply damage afterward.
6. `gradle --project-dir android :core:test --stacktrace --no-daemon` passes on the exact source/test commit and the final PR head. No APK packaging, artifact upload, release, main-branch write or auto-merge.

## Open-source/license review

- Reviewed the official [EKA2L1 repository](https://github.com/EKA2L1/EKA2L1), which is GPL-3.0. It remains a behavioral/reference tool only; no EKA2L1 code is copied into this project.
- Reviewed the MIT-licensed [Bomberman_libGdx example](https://github.com/yichen0831/Bomberman_libGdx). Its game architecture/assets are not a suitable drop-in for this project's original 41-stage data and existing native simulation; no source or assets are reused.
- The Explode Arena repository root has no discoverable `LICENSE` file in the current branch. This change adds only project code, tests and documentation; it adds no original art/audio or other assets, and makes no claim that those assets are redistributable.

## Limitations

This is a consistency fix for the current provisional native timing model. It does **not** establish Symbian/EKA2L1 fuse, damage or AI parity. Android-device gameplay, audio, rendering, real FPS/RAM, and full human-played matches remain **NOT TESTED**. Source CI is not a device test.
