# N37 — Mutual final-flame defeat resolves as DRAW

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft, no merge.

## Engineering question

When the player and the last living opponent are both defeated by the same bomb's final active-flame interval, does the canonical production tick resolve a DRAW instead of a win/loss, and can the victory-hold logic accidentally emit a stage transition?

Source inspection alone is insufficient because movement, fuse expiry, the final-flame sample, two independent vitality objects, and round resolution happen in a strict order inside the shared fixed-step path.

## Acceptance criteria

1. Use NativeArenaTick.advance with real NativeOpponentRoster, NativeBombSimulation, NativeVitality, and NativeRoundFlow; do not reproduce their logic in the test.
2. Arrange a compact test-only arena so one timed blast's last active interval hits both a one-health player and a one-health final bot.
3. Assert both vitality objects become defeated on the same canonical tick and round state becomes DRAW.
4. Assert no stage-advance signal is emitted during subsequent ticks and retry remains available.
5. Preserve the existing 41-arena/120-tick integration, N36 bot-final-flame victory tests, and source-only Native/Mainline checks.
6. No APK packaging, artifacts, release, merge, main write, or auto-merge.

## Implementation

- Add one production-path integration regression to NativeArenaTickAllArenasIntegrationTest.kt.
- Only test fixtures are added; no copyrighted assets, external source, dependencies, permissions, or services are introduced.

## Source/license review

This is a regression test against project-owned Kotlin simulation and the existing JUnit dependency. No third-party code or assets are copied. Combat and timing remain provisional until independently matched against Symbian/EKA2L1.

## Verification ledger

- Engineering question and acceptance criteria recorded before the test run.
- Source/test SHA: PENDING.
- Native Kotlin/JUnit and Mainline PR-head recheck: PENDING.
- Artifacts: expected 0; no APK build.
- Xiaomi Pad 5, human-played round, original Symbian/EKA2L1 parity: NOT TESTED.
