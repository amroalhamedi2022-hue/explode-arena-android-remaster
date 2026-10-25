# N16 — Native 41-arena catalog integrity and deterministic integration sweep
Date: 2026-10-25. PR #2 Draft; PR #1 collision branch unchanged.

## Engineering question and acceptance gates
1. The native StageCatalog rejects malformed/oversized source, invalid image paths, non-sequential IDs, oversized/mismatched collision grids and nonfinite/unsupported spawn metadata **before per-stage grid allocation**.
2. The real, existing 41 Mainline arenas still load in exact order (18 solo, 2 two-spawn, 21 four-spawn); no new or modified original game assets.
3. For **each** of the 41 real arenas, independently constructed native player/opponent rosters, collision and bomb simulation run eight 60Hz fixed ticks, cross the six-tick bot replan boundary, remain in walkable terrain, do not overlap living actors, and reproduce identical snapshots on a second run.
4. Kotlin/JUnit `:core:test` and Mainline JS/Python validation PASS on the exact code/test SHA. No APK needed for a source-only parsing/determinism question.
5. Xiaomi Pad 5 native launch, actual stage texture/audio loading, full 41-stage playthrough and Symbian EKA2L1 parity require **physical-device and original-game evidence**: NOT TESTED.

## Why this test, why no APK build?
StageCatalog is used by the real native libGDX entrypoint; the existing source-level tests check individual records and spawn locations but did not run the bot+bomb+collision integration across all original maps. A source-level deterministic JUnit sweep can detect data-dependent crashes and state divergence without Android packaging. The free existing native workflow executes only `:core:test` and deliberately does **not** assemble or publish an APK. Real graphics, audio and lifecycle still require later targeted Android/device tests; no source PASS can replace them.

## Open-source and license check
Reviewed upstream libGDX Apache-2.0 LICENSE at https://github.com/libgdx/libgdx/blob/master/LICENSE and gdx-ai Apache-2.0 LICENSE at https://github.com/libgdx/gdx-ai/blob/master/LICENSE . The project already uses libGDX JsonReader and its own deterministic native AI; adding an AI/parser dependency would increase complexity and not improve this bounded validation. Reused existing internal classes, no copied third-party code, new dependency, or new copyrighted assets.

## Source changes
- `StageCatalog.kt`: 1,000,000-character input cap; exactly 41 ordered records; image path allowlist; bounded stage dimensions and 20,000 collision cells; 8px geometry consistency; row-width checks before allocating row arrays; supported 1/2/4 spawn counts and finite coordinates.
- `StageCatalogTest.kt`: four additional JUnit regressions for input cap, path and ID tampering, oversized/mismatched grid geometry, malformed spawns.
- `NativeArenaSweepTest.kt`: one full-inventory deterministic 41-stage integration regression, including 8 native ticks per independent run per stage, bot collisions and bomb state.
- Production SHA `98e74e694a0a03656ac340ee81f1b1ecd0cbe57a`; parser tests SHA `f3504a9d7024be149a637bff7d814c315a0cd706`; sweep test SHA `ce46a8bc1d285bb654eeb92e3bcad9f3f9738039`.

## Verification at initial documentation
- Native CI https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40232581270 — IN PROGRESS at write time. Do not claim PASS until verified.
- Mainline CI https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40232581282 — IN PROGRESS at write time.
- No APK or artifact, no main write, no PR merge, no new game assets.
- Google Drive LIVE Handoff/Index: **SYNC FAILED** in this execution environment because Google Drive connector functions are not available in the connected tool registry (and the installed skill list is empty). No claim of a successful Drive write or verification. Preserve this GitHub checkpoint for later Drive recovery.
- NEXT: resolve exact-SHA CI results and any failing test, sync LIVE Google Docs once connector returns, then alternate to App Commander.
