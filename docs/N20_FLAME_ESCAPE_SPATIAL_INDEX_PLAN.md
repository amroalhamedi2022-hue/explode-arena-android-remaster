# N20 — Indexed active-flame escape membership

Date: 2026-10-26. PR #2 remains open Draft; collision PR #1 remains separate.

## Engineering question

N19 correctly limits active-flame transit to the connected flame component under the bot. However, each candidate movement cell calls a linear `component.any { blastHits(...) }` scan. In a bounded BFS with up to 20,000 arena cells and multiple active bombs/flames, that repeats the full component scan for every visited cell and can turn a bounded path search into O(gridCells × flameCells) hit-tests. Can we preserve the exact existing actor-hitbox semantics while bounding each membership query to at most four spatial-index lookups?

## Acceptance criteria

1. Build an immutable index only from flame cells produced by `NativeBombSimulation`, whose centers lie on its 16px bomb-cell lattice.
2. Return the same membership result as the existing linear `NativeBombSimulation.blastHits` reference for all finite positions, including strict hitbox boundaries.
3. Bound each query to the two nearest lattice columns × two nearest rows (at most four candidate keys), with early rejection outside indexed bounds.
4. Non-finite coordinates return false; no simulation state, bombs, flames, or IDs are mutated by route planning.
5. Wire the index once per `firstDirection` call; preserve normal movement, disconnected-flame exclusion, timed-deadline checks and unlit-refuge validation.
6. Add a dense grid and boundary regression comparing indexed results against the linear oracle; retain the active-flame and all-41-arena core suites.
7. Run Native Kotlin/JUnit and Mainline checks on the exact source+test SHA. Source-only CI only: no APK packaging/build, artifact upload, release, merge or main write.

## Licensed-source review

Rechecked the official `libgdx/gdx-ai` repository's Apache-2.0 LICENSE. Its generic pathfinding utilities do not provide this exact 16px flame-cell / 5px actor-footprint membership contract, so no external code is copied and no new dependency is justified. This is a small local spatial index over existing simulation data.

## Baseline

- PR #2 branch: `planning/native-kotlin-libgdx-migration-20261023`.
- Baseline PR head before N20: `914a45a7b655824bbd748215cbfa632b988bb5cc`.
- N19 exact source/test SHA: `9ebc27a34cedab8e671923181084ba2bddb7d2fc`; Native run 40251474221 PASS and Mainline run 40251474309 PASS.
- Device runtime and Symbian/EKA2L1 parity remain NOT TESTED.
- Google Drive LIVE sync is unavailable in this runtime; no Drive sync will be claimed without verified read-back.

## Implementation

- Production change: `BurningEscapeIndex` maps lattice coordinates to packed integer keys and checks only the nearest two columns and rows. The existing linear helper remains as a readable reference oracle.
- Production commit: `7da45dad01f70f06f55dc01067345f986b502406`.
- Regression commit / exact source+test SHA: `2a41a99bc4b40ee689a58899509abf969e425d66`.
- Test: dense 2px sampling around the flame footprint plus values just inside/on/outside the strict `8 + ACTOR_RADIUS` boundaries; compares every result with the linear reference and rejects NaN/Infinity.

## CI result

PENDING at the time this plan was created:
- Native Kotlin source checks run 40256273436.
- Mainline source checks run 40256273461.

## Device / delivery gates

- Xiaomi Pad 5 runtime: NOT TESTED.
- Original Symbian/EKA2L1 AI timing and flame-hitbox parity: NOT TESTED.
- APK/artifact: none; source-only CI policy applies.
- PR #2 remains Draft; no merge or main write.
