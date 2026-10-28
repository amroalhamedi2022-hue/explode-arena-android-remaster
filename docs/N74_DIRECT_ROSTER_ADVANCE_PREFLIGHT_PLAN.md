# N74 — Direct roster movement preflight

Date: 2026-10-28  
PR: #2 Native Kotlin/libGDX; keep Draft/open.

## Engineering question

Can callers of `NativeOpponentRoster.advance` bypass the complete start-world validation added to the canonical `advanceSimulationTick` path, mutating bot animation, AI, bomb placement, movement, or vitality from an invalid actor/bomb/terrain state?

## Why source inspection alone is not enough

The public movement-only entry point is also used by deterministic tests and focused simulation paths. It previously checked only that the tick was non-negative before changing defeated-bot animation state and planning/moving living bots. A direct-call integration test must prove invalid worlds are rejected before any mutation and that the same requested tick works after the world is repaired.

## Implementation

- Keep the public `advance` entry point, but run the shared non-mutating `validateStartState` before any bot-state mutation.
- Move the existing movement body to a private validated implementation.
- Have `advanceSimulationTick` call that private implementation after its own canonical preflight/reservation, avoiding a duplicate full-world scan in the 60Hz production path.
- Add direct-entry regressions for terrain overlap, unauthorized bomb overlap, actor overlap, unchanged fuse/animation/health/position, and same-tick retry after repair.

## Acceptance criteria

1. Invalid direct movement throws before bot animation, health, position, bomb quota, or fuse state changes.
2. The same requested tick succeeds after repairing the invalid world.
3. Valid owner exit-grace remains permitted; foreign bomb overlap is rejected.
4. Existing 41-arena, 1/2/4-spawn, fuse, AI, replay, and canonical tick tests remain green.
5. Free CI runs `:core:test` only; no APK packaging, no artifact upload, no new dependencies or assets.
6. Android device gameplay and Symbian/EKA2L1 parity remain **NOT TESTED**.
7. PR #1 collision work remains separate; no merge or `main` write.

## Evidence

- Exact source/test/docs commit: `89c35d0667fafea93c4857762f8479b80cc71372`.
- **PASS — Native Kotlin/JUnit:** [CI #40692516957](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40692516957), `:core:test`, `BUILD SUCCESSFUL in 53s`.
- **PASS — Mainline JS/Python PR CI:** [#40692516813](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40692516813).
- Native workflow artifact query: **0**. No APK build/package/upload.
- Android device gameplay and Symbian/EKA2L1 parity: **NOT TESTED**.
- The evidence append triggers a final documentation-head CI recheck; its result will be recorded in the PR conversation to avoid a docs-only commit loop.
- Google Drive LIVE sync: **SYNC FAILED** — no Drive/Docs connector is exposed in this runtime; no sync is claimed.
