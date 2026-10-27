# N35 — Canonical full-arena simulation tick

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft, no merge.

## Engineering question

Can the real libGDX loop and an integration test share one production fixed-step that orders player movement, independent bot movement, exactly one bomb/fuse update, player and bot damage sampling, and round resolution identically? Source inspection alone cannot prove Kotlin compilation or the interaction of the real 41-stage data catalog with this shared entry point.

## Acceptance criteria

1. A single `NativeArenaTick.advance` production method owns the complete fixed-step order.
2. The libGDX render loop calls that method once per FixedStepClock callback; it must not separately move the player, tick bombs, apply damage, or resolve round state.
3. The returned result carries player walking state, bomb placement/detonation counts, and the one-shot next-stage signal for rendering/audio.
4. A source integration test parses the existing `web/assets/stages.js` catalog and runs 120 deterministic fixed steps on all 41 arenas through the same production method.
5. Each step checks player and living-bot terrain/bomb safety plus actor separation; a separate regression proves player damage on a flame's final active tick reaches round flow in that same tick.
6. Exact PR-head Kotlin/JUnit CI and Mainline CI pass; no APK packaging, artifact upload, release, merge, main write, or auto-merge.

## Implementation

- Added `NativeArenaTick` as the single production orchestration entry point.
- Replaced the duplicated fixed-step body in `ExplodeArenaNativeGame` with the canonical method and retained sound/stage side effects from its result.
- Added `NativeArenaTickAllArenasIntegrationTest` against the real 41-stage catalog and a final-flame player-damage regression.

## Source/license review

This refactors the project's own Kotlin/libGDX simulation; no third-party source, assets, dependencies, permissions, or external service were added. Original art/audio were not copied or newly uploaded. All mechanics remain provisional pending independent Symbian/EKA2L1 observation.

## Status

- Source and integration CI: pending until the updated PR head is checked.
- Physical device, Xiaomi Pad 5, audio/visual play, and EKA2L1 parity: NOT TESTED.
- Artifacts/APK: none permitted for this milestone.
