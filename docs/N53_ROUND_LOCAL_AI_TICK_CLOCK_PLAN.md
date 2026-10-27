# N53 — Deterministic AI tick phase per arena attempt

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft/open, no merge.

## Engineering question

Does a retry or stage replacement begin bot planning at the same local tick phase, or does it inherit the process-wide fixed-step tick count from previous play? The renderer passed `FixedStepClock.ticks` to `NativeArenaTick`, while `installFreshRound` rebuilt bot controllers but only cleared the clock accumulator. Since the controller replans every six ticks, the first route-planning deadline could depend on how long the previous attempt ran.

## Implementation

- Added `NativeRoundTickClock`, a zero-based monotonic tick sequence scoped to one attempt.
- Added a fresh clock to `NativeArenaRoundStateFactory` output, so retry and stage replacement create independent AI timing state alongside fresh bots/bombs/health.
- Wired the native libGDX render loop to pass the round-local tick to `NativeArenaTick`; the process-wide `FixedStepClock` remains responsible for frame pacing and lifecycle pause handling.
- Added all-41-arena regression coverage for IDs and 1/2/4 spawn distribution, zero-based fresh attempts, and independence between two round clocks.

## Acceptance criteria

1. Every factory-created attempt begins its AI tick at 0 and advances monotonically within that attempt.
2. A fresh attempt does not inherit the previous attempt's six-tick replan phase.
3. The global frame clock remains unchanged; pause/resume and terminal-round gates retain their existing behavior.
4. Native Kotlin/JUnit and Mainline JS/Python checks pass on the exact source/test SHA; docs-updated PR-head recheck must also pass.
5. No APK packaging/signing/upload, new assets/dependencies, merge, main write, or auto-merge.

## Verification ledger

- **Source + regression-test SHA:** `3212f031d2dd1c20bb036060f9c083d495bfa957`.
- **PASS — Native Kotlin/JUnit:** [CI #40559115392](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40559115392), step `Test native Kotlin core without Android packaging` succeeded.
- **PASS — Mainline JS/Python:** [CI #40559115582](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40559115582) succeeded on the same source/test head.
- **Artifacts:** 0 on both runs; no APK packaging was invoked.
- **Docs-updated PR-head recheck:** pending after this plan/ledger commit.
- Android device rendering, physical-device behavior, and Symbian/EKA2L1 parity: **NOT TESTED**.
- Google Drive LIVE sync: **SYNC FAILED** — no Google Drive/Docs connector is available in this runtime; no LIVE read/write or folder metadata verification is claimed.
