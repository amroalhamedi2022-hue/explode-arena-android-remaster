# N41 — Latched terminal world freeze across native arenas

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft, no merge.

## Engineering question

After a round enters LOST or DRAW, can any subsequent catch-up tick still move actors, run bot decisions, age a fuse/flame, place/detonate a bomb, or re-resolve the outcome? The same explicit latch must protect ADVANCING/COMPLETE while leaving WON's victory-hold clock live.

## Production change

- Added `NativeRoundFlow.isLatched` for LOST, DRAW, ADVANCING and COMPLETE. WON is intentionally not latched because the victory hold must continue.
- `NativeArenaTick.advance` now stops opponent walking and returns before AI, movement, bomb/fuse updates, damage sampling or round resolution whenever the outcome/transition is latched.
- No stage data, bomb rule, AI heuristic, art, audio or original assets changed.

## Acceptance criteria

1. Parse the existing 41-stage source and preserve IDs 0–40 and spawn distribution 18/2/21.
2. For every arena, place a live player bomb, enter LOST, then run 120 production ticks; player position/owner-exit grace, all bot positions, bomb ID/owner/fuse and terminal state must remain unchanged. Bot walking animation must stop.
3. Exercise DRAW with a live bomb and verify the fuse does not age and the result remains DRAW.
4. Verify WON continues its victory-hold ticks; ADVANCING and COMPLETE become latched and cannot emit a second transition.
5. Run native Kotlin/JUnit `:core:test` and Mainline JS/Python checks on the exact source/test SHA, then recheck the docs-updated PR head. Confirm zero artifacts.
6. No APK packaging, upload, release, merge, main write, copyrighted asset upload or auto-merge.

## Status

- Source/test SHA: `240a539b8da45db783f5dd66d51930f47798441f`.
- **PASS — exact source/test SHA:** Native Kotlin/JUnit [run #40467325784](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40467325784), `:core:test`, successful; Mainline JS/Python [run #40467325594](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40467325594), successful.
- **PASS — docs-updated PR head `91ca494e7a4f4063bc8eb9b568a68b9c4e8ecd00`:** Native [run #40467466189](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40467466189) and Mainline [run #40467466030](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40467466030), both successful. A subsequent docs-only ledger update will trigger one more PR-head recheck; its result will be recorded in the PR discussion.
- Xiaomi Pad 5, human-played rounds and Symbian/EKA2L1 parity: **NOT TESTED**.
- Google Drive LIVE sync: **SYNC FAILED** if no Drive/Docs connector is available; no sync claim without read-back and folder metadata verification.
