# N57 — reject invalid opponent ticks before any roster mutation

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft/open, no merge.

## Engineering question

Can a direct caller advance bomb fuses on an invalid negative tick in a solo arena? Before this change, `NativeOpponentRoster.advance` returned early when no bots existed, before any controller validated the tick. `advanceSimulationTick` then advanced the bomb simulation anyway. The main `NativeArenaTick` already rejects negative ticks, but the roster's canonical integration entry point must be safe independently.

## Production change

- Validate `tick >= 0` at the very start of `NativeOpponentRoster.advance`, before clearing defeated-bot animation state or returning for an empty roster.
- Preserve normal one-, two-, and four-spawn behavior and the existing fixed-step order.
- Do not change fuse durations, movement, collision, AI decisions, or asset data.

## Regression / acceptance criteria

1. A negative tick passed through `advanceSimulationTick` with an empty (solo-arena) roster throws before a timed bomb fuse or flame state advances.
2. The same contract applies to populated rosters before bot animation or planning state can mutate.
3. Existing 41-arena, bomb/fuse, AI, replay, collision, and round-flow tests remain green.
4. Native Kotlin/JUnit and Mainline JS/Python CI both pass on the docs-updated PR head.
5. No APK packaging/artifacts, new game assets, dependencies, merge, main write, or auto-merge.

## Evidence ledger

- Production commit: `83ce94da22374e7a516f8cc446a820c9f6c8bcac`.
- Integration regression commit: `4f329e943e1c2d8297224ecd20f4808de0e7cd6d`.
- Test: `NativeOpponentRosterTest.negativeTickCannotAdvanceSoloArenaBombFuses`.
- **PASS on this docs-updated head:** Native Kotlin/JUnit [run 40590981279](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40590981279) and Mainline JS/Python [run 40590981370](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40590981370). Native log: `BUILD SUCCESSFUL in 48s`; artifacts: **0**. This verification-ledger update creates a new head; its recheck will be recorded in PR #2 conversation to avoid an endless docs/CI commit loop.
- Android device play and Symbian/EKA2L1 parity: **NOT TESTED**.
- Copyrighted art/audio were not added or modified.
- Google Drive LIVE sync must be independently verified; GitHub documentation is not proof of Drive sync.
