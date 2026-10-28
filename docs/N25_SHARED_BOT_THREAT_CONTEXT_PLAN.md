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

## Implementation and exact-SHA verification — SOURCE ACCEPTANCE PASS

- Production: NativeBotThreatContext packages the existing N21 coalesced timed-threat snapshot and one N23 TimedBlastIndex. NativeOpponentRoster.advance creates one context before planning and passes that same instance to every living controller. NativeBotController.decide remains source-compatible; its standalone path creates a fresh context. The context is never cached across ticks.
- The shared 0.75-second context/index serves imminent checks and chase BFS. Escape-route-specific indexes for proposed placements or the longer route horizon remain local because their inputs differ; no behavior was silently removed.
- Regression tests compare shared and standalone decisions for all three bot owners, compare indexed membership with the linear blastHits oracle over dense grid/boundary samples, assert simulation immutability, verify a fresh context sees fuse changes, reject invalid horizons, and count exactly one context creation per living roster tick (zero when all bots are defeated).
- Exact production + test SHA: 4e71fa9e35a292fab0be19bfaa1af662aacf8404.
- **PASS:** Native Kotlin/JUnit [run 40297267749](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40297267749), job 121486356992. Actual step ran `gradle --project-dir android :core:test --stacktrace --no-daemon`; log: **BUILD SUCCESSFUL in 36s**, 4 actionable tasks executed.
- **PASS:** Mainline JS/Python validation [run 40297267719](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40297267719) on the same exact source/test SHA.
- N24 180-tick four-spawn roster-reversal integration and all-41-arena sweep remained in the same `:core:test` suite and passed. Native workflow artifacts: **[] (zero)**. No APK packaging/build/upload, release, merge, auto-merge, or main write.
- **NOT TESTED:** measured Xiaomi Pad 5 performance, Android runtime, complete match, audio/graphics lifecycle, and original Symbian/EKA2L1 AI/timing parity. This is source acceptance only.
- Official libGDX and gdx-ai LICENSE files were re-read from their repositories; both are Apache-2.0. N25 copies no external implementation or assets and adds no dependency.
- PR #2 remains open Draft; collision PR #1 remains separate. Google Drive LIVE sync remains SYNC FAILED because no Drive/Docs connector is available in this runtime; no Drive read-back or write is claimed.
