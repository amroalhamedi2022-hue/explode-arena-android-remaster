# N36 — Bot final-flame round-outcome integration

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft, no merge.

## Engineering question

Does a bot that is killed by the final active flame interval resolve the round in the same canonical simulation tick, and does the victory hold emit exactly one stage-advance signal? Source ordering alone is insufficient because bomb expiry, roster damage sampling and round flow interact at a fixed-step boundary.

## Acceptance criteria

1. Exercise the production `NativeArenaTick.advance`, `NativeOpponentRoster`, `NativeBombSimulation`, `NativeVitality` and `NativeRoundFlow` together; no copied simulation in the test.
2. A bot-owned timed bomb detonates after the configured fuse, its final flame interval defeats a one-health bot, and the player remains alive.
3. The same tick enters `WON`; a one-tick victory hold emits one next-stage signal exactly once.
4. A final-arena case does not wrap to arena zero.
5. Existing 41-arena integration and native Kotlin/JUnit plus Mainline CI pass on the updated PR head.
6. No APK packaging, artifacts, release, merge, main write, or auto-merge.

## Implementation

- Add a production-path integration regression for the bot's final-flame damage boundary and round outcome.
- Add a final-arena terminal-state regression using the same canonical tick path.
- Keep the existing 41-stage catalog as the fixture; the compact combat arena exists only in the test and adds no game assets.

## Source/license review

This extends project-owned Kotlin/JUnit code and existing in-repository simulation. No third-party source, assets, dependency, permission, or service is introduced. All collision/fuse/AI rules remain provisional pending independent Symbian/EKA2L1 evidence.

## Verification ledger

- **PASS — exact source/test/PR head:** `8b49f7666061d534efe27afcfa9948e01cd3ff33`.
- **PASS — Native Kotlin/JUnit:** [run #40446349623](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40446349623), `:core:test` completed successfully.
- **PASS — Mainline:** [run #40446349648](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40446349648), source/stage/resource checks passed.
- Artifacts: **0** confirmed; no APK packaging occurred.
- Physical device, Xiaomi Pad 5, audio/visual play, and EKA2L1 parity: **NOT TESTED**.
