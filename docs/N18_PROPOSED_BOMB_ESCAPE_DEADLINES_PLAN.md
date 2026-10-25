# N18 — Proposed-bomb escape deadline and chain prediction

Date: 2026-10-26. PR #2 remains Draft; collision PR #1 remains separate.

## Engineering question
Before an opponent places a timed bomb, the escape planner checks that the final refuge lies outside the hypothetical blast, but its timed-cell snapshot contains only bombs already present in the simulation. Can the route incorrectly cross the proposed bomb's own future ray (or a remote/timed bomb triggered by that proposed blast) after the fuse deadline and still select a refuge?

## Acceptance criteria
1. A pure prediction API must evaluate the hypothetical placement without mutating active bombs, next-ID state, actor pass-bomb grace, flames, or remote-owner state.
2. The prediction must include the proposed bomb's fuse deadline, currently active timed bombs, active-flame ignition, and transitive chain reactions among the proposed and active bombs within a bounded horizon.
3. NativeBotEscapeRoute must use this prediction only for a hypothetical placement; an already placed escape bomb must retain its actual remaining fuse, and flame-only escape must not invent a new bomb.
4. A short-fuse regression must reject a route that reaches a safe endpoint only by crossing the proposed blast after its deadline; a normal-fuse route remains viable when its path fits the safety budget.
5. A chain regression must show a proposed bomb propagating its deadline to an otherwise isolated remote bomb it would hit.
6. Existing Native Kotlin/JUnit and Mainline checks must pass on the exact source+test SHA. Run source tests only; no APK packaging, artifact upload, merge, release, or main write.

## Licensed reference review
Reviewed libGDX and libgdx/gdx-ai repository metadata: Apache-2.0. gdx-ai provides general pathfinding but not this arena's fuse/deadline/chain semantics, so the existing bounded BFS and NativeBombSimulation remain the implementation base. No third-party code or game assets will be copied.

## Baseline
- PR #2 current head before N18: d9c569b57b38fb3bd77a700c26a29ea872afd8e9 (N17 docs head); N17 source/test SHA 230848f38f013b8a12a67b50d37297a1abf57150.
- Native Kotlin/JUnit run 40242074635 PASS and Mainline run 40242074624 PASS on N17 source/test SHA.
- Physical Xiaomi Pad 5, full gameplay, original Symbian/EKA2L1 timing parity remain NOT TESTED.
- Google Drive LIVE sync has been unavailable in this runtime; no Drive sync will be claimed without read-back verification.

## Result
Pending source changes and exact-SHA CI verification.

## Next
Continue from the latest PR #2 head. Do not merge PRs or write to main.
