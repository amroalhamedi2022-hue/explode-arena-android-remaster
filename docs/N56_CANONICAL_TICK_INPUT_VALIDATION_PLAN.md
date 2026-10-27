# N56 — validate the canonical arena tick before mutation

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft/open, no merge.

## Engineering question

Can a malformed tick or movement vector partially mutate gameplay before the engine rejects it? Before this change, a negative tick could move the player first and only then be rejected by a bot controller in a multi-spawn arena. A solo arena had no bot controller to reject the negative tick at all.

## Implementation

- Validate the tick is non-negative and both movement axes are finite and normalized to [-1, 1] at the entry to NativeArenaTick.advance.
- Perform validation before the terminal-state early return and before player movement, bot planning, bomb/fuse advancement, damage, or round resolution.
- Preserve the normal renderer contract: keyboard/touch vectors are already clamped to [-1, 1].

## Acceptance criteria

1. A negative tick throws before player/bot positions, health, bombs, flames, or round state change.
2. NaN, infinity, and out-of-range movement axes fail atomically even in a solo arena with no bot controller.
3. Existing 41-arena, bomb, bot, replay, round-flow, and lifecycle source tests remain green.
4. Native Kotlin/JUnit and Mainline JS/Python checks pass on the exact source/test SHA and on the docs-updated PR head.
5. No APK packaging/artifact, copyrighted asset addition, dependency change, merge, main write, or auto-merge.

## Evidence ledger

- Production: android/core/src/main/kotlin/com/explodearena/remaster/core/NativeArenaTick.kt.
- New integration regression: NativeArenaTickInputValidationTest.kt.
- Physical Android play and Symbian/EKA2L1 parity remain NOT TESTED.
- Google Drive LIVE sync must be verified separately; GitHub documentation is not proof of Drive sync.

## Verification ledger — source/test head

- Production commit: d63cda61015f893a04d8e1b9365636d778a0c40d.
- Integration-test commit: 386df8575d43891ba276f1bba895a036560420a4.
- **PASS — Native Kotlin/JUnit on PR head 2b3d8cc558d36192c26bfe2adb888f24ddd945a8:** [CI #40581633764](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40581633764); job completed successfully, no APK packaging.
- **PASS — Mainline JS/Python on the same PR head:** [CI #40581633692](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40581633692).
- The docs/acceptance-ledger append creates a new PR head; final-head recheck will be recorded in the PR conversation.
