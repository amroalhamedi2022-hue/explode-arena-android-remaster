# N73 — Canonical bot-tick start-state preflight

Date: 2026-10-28  
PR: #2 Native Kotlin/libGDX; keep Draft/open.

## Engineering question

Can a direct call to `NativeOpponentRoster.advanceSimulationTick` bypass the whole-world preflight in `NativeArenaTick`, allowing a bot already inside terrain, a foreign bomb, or another actor to age fuses and mutate AI/vitality state?

## Why source inspection alone is not enough

The normal libGDX path validates player, bot, terrain, bomb ownership grace and pairwise actor overlap before moving the player. The roster's direct/replay entry point previously validated the player and tick sequence only. Deterministic integration fixtures are required to prove a rejected direct tick changes neither the world nor the tick reservation, and that the same tick can be retried after repairing the start state.

## Implementation

- Centralize the non-mutating start-world preflight in `NativeOpponentRoster.validateStartState`.
- Call it from both `NativeArenaTick.advance` before player movement and `NativeOpponentRoster.advanceSimulationTick` before tick reservation.
- Validate finite actor coordinates, terrain overlap, unauthorized bomb overlap, owner-specific exit grace, and pairwise overlap among the player and living bots.
- Add direct-entry integration regressions for wall overlap, foreign-bomb overlap, actor overlap, unchanged fuse/state, and same-tick retry.

## Acceptance criteria

1. Invalid direct/replay start state throws before actor movement, bot damage, bomb/fuse mutation, or tick reservation.
2. A same-tick retry succeeds after the invalid actor is repaired.
3. Valid owner-exit grace remains permitted; foreign-bomb overlap is rejected.
4. Existing 41-arena, 1/2/4-spawn, bomb, AI, round-flow and replay tests remain green.
5. Free CI runs `:core:test` only; no APK packaging, no external assets/dependencies, no main write or merge.
6. Android-device gameplay and Symbian/EKA2L1 parity remain **NOT TESTED**.

## Evidence

- Production source commits: `e60e9e9ea563b52be1a0606ad69d8303ec996670` (roster preflight) and `67f9e10aefb5bd6078e5b44270f1a385b77deae3` (canonical tick integration).
- Integration test commit: `e2512d92a6e8261259378a494d49e03d9f5969c9`; final tested PR head: same SHA.
- **PASS — Native Kotlin/JUnit:** [CI #40684199692](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40684199692), `:core:test`, BUILD SUCCESSFUL in 45s.
- **PASS — Mainline PR CI:** [#40684199700](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40684199700).
- Native artifacts: 0. No APK build/package/upload.
- PR #1 collision work remains separate.
- Google Drive LIVE sync: **SYNC FAILED** if this runtime exposes no Drive/Docs connector; no sync claim without read-back verification.
