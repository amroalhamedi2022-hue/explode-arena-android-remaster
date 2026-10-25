# N15 — Native bot active-flame escape (provisional)
Date: 2026-10-25
PR: #2 (Draft); no main merge, no new game assets.

## Observed source defect
NativeBotController.canStep and NativeBotEscapeRoute.firstDirection reject every intermediate point that is still burning. A bot already inside a live flame cannot leave it because a 54px/s move advances only 0.9px per 60Hz tick, while the flame footprint spans many pixels. It freezes even if a reachable safe cell exists.

## Acceptance questions and gates
1. A bot already burning can choose a nonzero cardinal escape move through the burning intermediate cells to a genuinely non-burning refuge; no fictitious bomb is added.
2. Existing own-bomb and timed chain deadlines still constrain escape; an isolated remote bomb has no automatic fuse.
3. Normal chase/planting cannot intentionally enter flames; bot never plants while burning.
4. Deterministic Kotlin JUnit regression and existing Mainline tests pass on exact code SHA.
5. Native Android/Xiaomi Pad 5, original EKA2L1 flame/hitbox timing and game-wide parity: NOT TESTED until physical device evidence.

## CI engineering test question, before source changes
Does the new native Kotlin escape planner + controller integration typecheck, pass JVM regressions, and remain compatible with the existing Android/libGDX app entrypoint? Source inspection/JVM-only checks cannot prove the Android module compiles against the changed call signature. The repository's existing native CI may execute :app:assembleDebug internally as a compile gate; no APK upload/release, no artifact delivery and no physical-device claim. Only run existing free source checks/CI; do not dispatch separate APK build.

## Open source/license review
Checked https://github.com/libgdx/gdx-ai (Apache-2.0, maintained libGDX ecosystem, general A*/steering). This defect needs the project's already integrated deterministic, time-aware bomb/flame BFS and 60Hz collision model; importing gdx-ai adds dependency and behavior mismatch without fixing flame transit. Adapt existing code; no third-party code copied and no new dependencies/assets.

## Pending
Production changes, tests and CI result must be recorded separately. No PASS claimed in this pre-implementation plan.
