# N66 — Validate player coordinates before canonical simulation ticks

Date: 2026-10-28. Explode Arena PR #2 remains open Draft; collision PR #1 remains separate.

## Engineering question

In a one-spawn arena the opponent roster is empty. The canonical tick path calls the roster advance method, whose empty-living-roster fast path returns before collision/movement code validates the player coordinates. A NaN or infinite coordinate could therefore pass through the canonical gate and allow bomb timers to advance, unlike a multi-opponent arena that reaches coordinate validation during movement.

## Production change

- `NativeOpponentRoster.advanceSimulationTick` now requires finite player X/Y before validating/reserving the tick or touching the simulation.
- This check applies to solo and bot arenas, and is fail-fast: malformed input does not consume the tick number.
- No game assets, dependencies, renderer, APK workflow, or Symbian claims changed.

## Acceptance criteria

1. NaN X and infinite Y are rejected before the tick is reserved.
2. A live bomb's fuse remains unchanged after each rejected attempt.
3. The same tick number succeeds once coordinates are repaired and advances the fuse exactly once.
4. Existing 41-stage and 1/2/4-spawn integration regressions continue to pass.
5. CI is source-only: `gradle --project-dir android :core:test`; no APK packaging/upload.

## Evidence

- Production + regression test commit: 5ec9c3fed00dbb465ddb45a5acbe8475bd8c1d16.
- PASS — Native Kotlin/JUnit source-only CI #40640834426 on docs-head SHA bfe7fecb1dd9e125041bfe85ed9db172e91c19bb; job `native-source-check` completed successfully.
- PASS — Mainline JS/Python CI #40640834344 on the same docs-head; source-checks completed successfully. Mainline push CI #40640828161 also PASS on source/test commit 5ec9c3fed00dbb465ddb45a5acbe8475bd8c1d16.
- Native job log contains BUILD SUCCESSFUL: true. Native artifacts: 0.
- Android device gameplay and Symbian/EKA2L1 parity: NOT TESTED.
- Open-source/API reference checked: Kotlin `Double.isFinite()` semantics at https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/is-finite.html. No third-party implementation copied.
- No merge, main write, paid service, or external game asset upload.
