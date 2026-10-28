# N34 — Canonical opponent simulation tick and damage boundary

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX (keep Draft; no merge)

## Engineering question

Can the native game loop and all-41-arena integration test share one production entry point that guarantees this exact fixed-step order for opponent simulation: plan/move, advance bomb fuses/flames once, then sample bot damage against the just-finished interval? The previous order was repeated at each caller, making it possible for a future caller to forget the post-tick damage sample or accidentally advance the bomb simulation twice.

## Acceptance criteria

1. NativeOpponentRoster.advanceSimulationTick performs one roster advance with inline damage disabled, calls NativeBombSimulation.tick() exactly once, then samples bot damage using isBurningDuringLastTick.
2. The method returns explicit bomb-placement and detonation counts so the renderer can play effects without ticking the simulation itself.
3. ExplodeArenaNativeGame uses the canonical entry point and does not separately tick bombs or apply bot damage a second time.
4. The real web/assets/stages.js catalog still parses as ordered stage IDs 0..40, with 18/2/21 arenas containing 1/2/4 spawns.
5. The integration test exercises the canonical entry point for 120 fixed steps on every arena and continues checking independent bot/controller/actor instances, terrain/bomb collisions, and actor separation.
6. Exact PR head source/test SHA and CI are checked. No APK packaging, artifacts, release, merge, main write, or auto-merge.

## Implementation

- Added NativeOpponentRoster.SimulationTickResult and advanceSimulationTick.
- Routed the libGDX render/simulation loop through the new method; sound playback uses the returned counts.
- Reworked NativeOpponentRosterAllArenasIntegrationTest to use the same canonical production tick rather than duplicating its internal sequence.

## Evidence

- Production roster commit: 958c661e92e135ea4b3a9c2c50c982ba5ff45508.
- Native game-loop integration commit: ecddbc5eefe58461e6b19b6bdb8ab1998ef46ba5.
- Exact source/test head: f81c88c8504ac52487b0adc83e096ec1653cdc98.
- **PASS — Native Kotlin/JUnit:** [run #40434769974](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40434769974), :core:test, BUILD SUCCESSFUL in 50s.
- **PASS — Mainline:** [run #40434769977](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40434769977) on the same source/test head.
- **Artifacts: 0**; this workflow performs JVM source tests only, no APK packaging.
- Device launch, Xiaomi Pad 5 behavior, Symbian/EKA2L1 parity, audio and visual acceptance: **NOT TESTED**.

## Source/license review

This is a refactor of the repository's existing Kotlin simulation and game loop. No external implementation or art/audio was copied, no dependencies or permissions were added, and no new original assets were uploaded.
