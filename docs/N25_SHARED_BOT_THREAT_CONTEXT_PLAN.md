# N25 — Share immutable timed-threat planning across the opponent roster

Date: 2026-10-26. Explode Arena PR #2 remains open Draft; collision PR #1 stays independent.

## Engineering question

N21 reduced duplicate prediction work inside one bot decision, but each living bot still independently walks the same timed-chain graph, emits the same imminent blast-cell set, and builds the same spatial index during the same fixed 60Hz roster tick. With up to three opponents, can the roster compute one immutable threat context and share it without changing any decision or collision behavior?

## Acceptance criteria

1. A roster tick with at least one living bot computes one TimedThreatSnapshot for the existing provisional 0.75-second imminent horizon, before any bot plans or places a bomb.
2. The same immutable snapshot and one prebuilt TimedBlastIndex are passed to every bot decision in that roster tick. Common imminent-threat checks and chase BFS do not rebuild the 0.75-second context/index per bot. Escape-route-specific indexes for hypothetical placements or the longer route horizon remain local because their inputs differ.
3. Standalone NativeBotController.decide(...) remains source-compatible and builds a fresh context for callers outside a roster. Context is never cached across ticks, so bomb fuse/flame changes cannot leave stale threat data.
4. Preserve exact NativeBombSimulation.blastHits strict-boundary behavior, earliest-deadline snapshot semantics, stable owner-ordered bomb placement, and N24 reversed-roster determinism.
5. New Kotlin/JUnit tests compare shared-context decisions with independently constructed contexts for multiple bots, compare the shared index with the linear hitbox oracle over grid/boundary samples, and verify planning does not mutate bombs, flames, next ID, or remote-owner state.
6. The existing 180-tick four-spawn reversed-order integration and all-41-arena sweep still pass; source changes must not alter their deterministic snapshots or 18/2/21 spawn distribution.
7. Run actual gradle --project-dir android :core:test --stacktrace --no-daemon and Mainline JS/Python validation on the exact source/test SHA. No APK packaging/build, artifact upload, release, merge, auto-merge, or main-branch write.

## Why source tests answer this question

The defect is repeated Kotlin computation and shared-state correctness inside a fixed simulation tick. JVM tests execute the production controller, bomb predictor, spatial index, and roster directly. APK packaging or device rendering cannot establish that the same immutable context is reused without changing decisions; Android profiling remains a separate future performance gate.

## Source and license review

Reviewed the existing in-repository N21 TimedThreatSnapshot, N23 TimedBlastIndex, and N24 roster planning as the closest implementation precedents. The change composes these project-owned primitives; no new dependency, copied third-party implementation, game asset, or external API is needed. Existing libGDX/gdx-ai license review records Apache-2.0 for those framework repositories; no framework code is copied in N25.

## Baseline

- PR #2 branch: planning/native-kotlin-libgdx-migration-20261023.
- Current PR head before N25: 626a0ab1adefc0b6dfdeccd462ee6c015a91bb4b (documentation-only descendant of N24 source/test SHA b31657ef20127901a22931dc402278572eb71e28).
- N24 Native Kotlin/JUnit run 40282530487 PASS and Mainline run 40282530545 PASS on exact N24 source/test SHA.
- Xiaomi Pad 5 runtime, measured performance, full match, and original Symbian/EKA2L1 AI/timing parity remain NOT TESTED.
- Google Drive LIVE sync must be verified separately; do not claim sync without read-back.

## Status

Implementation is in progress on PR #2. The shared context is created once in NativeOpponentRoster.advance and passed to every living controller; standalone controller calls retain a fresh-context path. Exact-SHA CI is pending. No APK is built because this is a source-level shared-snapshot and deterministic-decision question.
