# N45 — Freeze the final-arena completion clock

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft, no merge.  
**Status:** source/test CI PASS; independent docs-head recheck pending.

## Engineering question

Does the production input arbiter continue feeding fixed-step ticks after the last arena reaches `COMPLETE`, even though the round-flow contract says only `PLAYING` and the provisional victory hold may advance? Source review found the arbiter used an unconditional `advanceWorld = true` outside pause/navigation/retry. That keeps the clock waking after the final result and can make future render-loop work accidentally mutate a completed round.

## Acceptance criteria

1. The production game passes `NativeRoundFlow.advancesPresentationClock` to the command arbiter.
2. `PLAYING` and `WON` keep advancing; bomb placement/detonation still require `isPlaying`.
3. `LOST`, `DRAW`, `ADVANCING`, and `COMPLETE` cannot advance the fixed-step clock through ordinary idle frames.
4. An integration-style JUnit test proves the victory hold advances, the final transition reaches `COMPLETE`, and 40 subsequent idle frames do not advance the clock.
5. Native Kotlin/JUnit and Mainline JS/Python source checks pass on the exact production/test SHA; zero APK artifacts.
6. No `main` write, merge, release, auto-merge, APK packaging, or new copyrighted assets.

## Implementation

- Added an explicit `advancesClock` input to `NativeFrameCommands.resolve`, defaulting to `isPlaying` for safe callers.
- Wired the live libGDX render loop to `roundFlow.advancesPresentationClock`; this preserves the victory hold while freezing final/latched states.
- Added a regression covering both the allowed WON clock and frozen COMPLETE clock. Existing input tests explicitly opt into the victory-hold clock where appropriate.

## Source/license review

This is a focused correction to existing project logic. No external implementation or dependency was copied or added; no original game art/audio files changed.

## Verified result

- **PASS — exact source/test SHA:** `8993ddfa9cce684850e8cf2f464531524e62c2de`.
- **PASS — Native Kotlin/JUnit:** [run #40491767525](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40491767525), `:core:test`, BUILD SUCCESSFUL in 57s.
- **PASS — Mainline JS/Python:** [run #40491767514](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40491767514).
- **Artifacts: 0**; no APK packaging, signing or upload.
- Device, visual freeze, Android runtime behavior, and Symbian/EKA2L1 parity remain **NOT TESTED**.
- Google Drive LIVE sync: **SYNC FAILED** — no Drive/Docs connector is exposed in this runtime.
