# N76 — Detached native stage snapshot ownership

Date: 2026-10-28  
PR: #2 Native Kotlin/libGDX; keep Draft/open.

## Engineering question

Can a caller mutate a `StageData` grid row or spawn list after constructing `NativeBombSimulation`, thereby changing the geometry the bomb engine uses or making the N75 stage-binding check compare a mutated object against itself?

## Why source inspection alone is not enough

`StageData` exposes a Kotlin `List<IntArray>`: the list interface is read-only, but each row is mutable and callers may retain the original mutable list. A focused integration test must prove that the simulation owns detached rows/spawns, rejects a mutated caller stage before a canonical tick changes any state, and can retry the same tick after the caller restores a matching definition.

## Implementation

- Deep-copy every grid row and copy the spawn list when the bomb simulation is constructed.
- Keep N75's structural comparison against that private snapshot.
- Exercise caller mutation of both a grid cell and the original spawn list.
- Prove bomb placement still uses the captured geometry; a mismatched arena tick must fail before player/bot movement or fuse aging, and the same tick must succeed after repair.

## Acceptance criteria

1. The simulation's collision/placement geometry cannot be changed through caller-owned grid arrays after construction.
2. A grid or spawn mutation on the caller's original stage is rejected by N75 preflight, even when the caller passes the same `StageData` instance.
3. Rejection leaves player position, bomb fuse and round state unchanged.
4. Restoring the stage allows the same canonical tick index to succeed and advance the fuse exactly one fixed step.
5. Structurally equivalent stage copies remain compatible; the 41-stage and 1/2/4-spawn suites remain green.
6. Free CI runs `:core:test` only; no APK build/package/upload, assets, new dependencies, merge or `main` write.
7. Android device gameplay and Symbian/EKA2L1 parity remain **NOT TESTED**. PR #1 collision work remains separate.

## Evidence

Source/test SHA and CI: pending. Final docs-head recheck: pending.
Google Drive LIVE sync: **SYNC FAILED** if no Drive/Docs connector is available; do not claim sync without read-back verification.
