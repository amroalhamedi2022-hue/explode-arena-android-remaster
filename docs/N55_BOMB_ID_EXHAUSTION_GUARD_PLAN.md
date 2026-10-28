# N55 — Fail-closed native bomb identity exhaustion

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft/open, no merge.

## Engineering question

`NativeBombSimulation.place` previously allocated bomb IDs with `nextId++`. If the counter reached `Long.MAX_VALUE`, the increment could wrap to a negative ID and corrupt deterministic replay identity/order. This is a boundary guard for the headless simulation; it does not claim a realistic gameplay counter can reach this value.

## Implementation

- Added a positive `initialBombId` checkpoint parameter to the bomb simulation and replay harness; the normal default remains `1L`.
- Reserve `Long.MAX_VALUE` as an exhausted sentinel and fail before adding a bomb or setting the actor's pass-bomb grace.
- Propagate the checkpoint through `NativeReplaySession` so the same production step can be exercised by integration tests.
- Keep ordinary bomb placement, remote detonation, fuse, blast and collision rules unchanged.

## Acceptance criteria

1. Zero/negative initial bomb IDs are rejected.
2. At exhaustion, placement fails with a stable exception before changing the bomb list, actor pass-bomb state, or replay trace/tick.
3. Existing bomb simulation, replay determinism and remote-bomb regressions remain green.
4. Native Kotlin/JUnit and Mainline JS/Python pass on the exact source/test SHA.
5. Re-run both checks on the docs-updated PR head; no APK packaging, artifacts, new dependencies/assets, merge, main write or auto-merge.

## Verification ledger

- Production files: `NativeBombSimulation.kt` and `NativeReplaySession.kt`.
- Unit regressions: `NativeBombSimulationTest.kt`; replay integration regression: `NativeReplaySessionTest.kt`.
- **Source + test SHA:** `ead28891a9b7a4825ce94d42af942bfda901506e`.
- **PASS — Native Kotlin/JUnit:** [run #40572909270](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40572909270).
- **PASS — Mainline JS/Python:** [run #40572909353](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40572909353).
- **PASS — previous docs-updated PR head:** `a219fdd13296b7243f9653b71e9a4d7efa19bdd2`; Native Kotlin/JUnit [#40573367593](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40573367593) and Mainline [#40573367562](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40573367562) both succeeded. This status/ledger append creates a new PR head; its final recheck is tracked in the PR conversation.
- **NOT TESTED:** Xiaomi Pad 5 / Android gameplay, Symbian EKA2L1 parity, native full-game completeness.
- APK artifacts: **0**. No original game assets or third-party code added.
