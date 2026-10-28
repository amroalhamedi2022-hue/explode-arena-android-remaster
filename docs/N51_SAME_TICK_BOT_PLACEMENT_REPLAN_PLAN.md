# N51 — Replan bot movement after same-tick bomb placements

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft/open, no merge.

## Engineering question

When two or more bots decide to place bombs in the same roster tick, are their escape/movement decisions recalculated against the complete set of bombs actually committed before movement? The current two-phase loop computes every decision from the pre-placement snapshot, then commits all placements; collision checks see the new bombs, but blast-route decisions can still be stale for that tick.

## Planned production change

- Preserve deterministic pre-placement decisions and stable owner-order placement commits.
- If at least one placement succeeds, create one fresh post-placement threat context and recompute movement-only decisions for all living bots against the complete bomb set.
- Carry the placing actor's pass-bomb grace into the post-placement snapshot so it can leave its own newly placed bomb.
- Disable new placements during the second pass; this pass may revise movement only, never add a second unplanned placement.
- Keep candidate movement and final overlap rejection based on the same pre-move actor snapshot.

## Acceptance criteria

1. No placement: exactly one threat context is built and the existing single-pass behavior is preserved.
2. One or more placements: exactly one additional context is built after all placements, and that context sees the complete committed bomb set.
3. The second pass cannot place additional bombs; each successful placer retains its own pass-bomb grace while planning its escape.
4. Same-tick placements remain deterministic under roster-order reversal; movement candidates remain collision-safe.
5. Native Kotlin/JUnit, including all 41 arenas and 1/2/4-spawn integration, and Mainline JS/Python checks pass on the exact source/test SHA; recheck the docs-updated PR head.
6. No APK packaging/signing/upload, new dependency/assets, merge, main write, or auto-merge.

## Source/licensing boundary

This is a correction to project-owned Kotlin simulation ordering. No external AI implementation or game assets are copied. All timing and bot behavior remain provisional until compared with Symbian/EKA2L1.

## Verification ledger

- **Exact source/test SHA:** `6ba42eb14fa92c62ece1807fa3396f9cd69a90f2`.
- **PASS — Native Kotlin/JUnit:** [CI #40545607851](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40545607851), `:core:test`; source-only workflow, no APK packaging.
- **PASS — Mainline JS/Python:** [CI #40545607886](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40545607886), same PR head; all existing mainline checks passed.
- **PASS — documentation-updated PR-head recheck:** head `be37932eb9b6ab14280d1aa9e054e9982a53a469`, [Native CI #40545892255](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40545892255) and [Mainline CI #40545892333](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40545892333) both PASS. The final recheck after this verification-note commit will be recorded in the PR comment; no further repository edits are planned.
- The new regression `sameTickPlacementsRefreshThreatContextBeforeMovement` passed; the existing roster-order and all-41-arena/1-2-4-spawn integration suites passed in the same Native run.
- Device and Symbian/EKA2L1: **NOT TESTED**.
- APK artifacts: **0 expected**.
- Google Drive LIVE sync: **SYNC FAILED** if no Drive/Docs connector is exposed; do not claim sync without verified read-after-write.
