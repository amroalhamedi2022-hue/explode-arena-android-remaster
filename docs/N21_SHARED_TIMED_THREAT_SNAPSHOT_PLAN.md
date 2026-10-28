# N21 — Shared timed-threat snapshot for native bot decisions

Date: 2026-10-26. PR #2 remains open Draft; collision PR #1 remains separate.

## Engineering question

NativeBotController.decide() runs every fixed simulation tick for each living opponent. It separately called predictedTimedChainThreats() and predictedImminentTimedBlastCells(), each repeating the same timed chain-graph prediction and blast-ray work. With up to three bots, this multiplied the same calculation in the hottest decision path. Can both consumers use one snapshot without changing predicted threats, cell deadlines, or simulation state?

## Acceptance criteria

1. Add one immutable result carrying the timed chain threats and timestamped blast cells from a single prediction pass.
2. Update the bot controller to obtain both views from that single snapshot per decision.
3. Keep the existing public helper behavior compatible for other callers.
4. Regression compares snapshot threats and cells with the legacy helper results for a timed bomb that chains into a remote bomb.
5. Verify prediction does not mutate bombs, flames, or the next bomb ID.
6. Run Native Kotlin/JUnit and Mainline source checks on the exact source+test SHA; no APK packaging, upload, release, merge, or main write.
7. Keep Symbian/EKA2L1 timing and physical-device performance explicitly NOT TESTED.

## Baseline

- Branch: planning/native-kotlin-libgdx-migration-20261023.
- Baseline PR #2 head: 2aed138a30b964d08361ba3578ee7f1c02dc226e.
- Latest verified Native/Mainline checks at baseline: runs 40256584216 / 40256584213, PASS.
- Licensed-source review: rechecked official libGDX/gdx-ai Apache-2.0 project; its generic pathfinding APIs do not provide this simulation-specific timed-chain snapshot. No code copied and no dependency added.

## Implementation status

## Implementation and CI result — SOURCE ACCEPTANCE PASS (2026-10-26)

- Initial implementation/test commit: `15d75f79bd64d2e1ea330aa2a28466d6020a55df`.
Follow-up source commit keeps the legacy blast-cell helper independent as a real parity oracle: `f3000302a5c4ba7f4063f3918005b116824607cd`.
Final source+test SHA: `f3000302a5c4ba7f4063f3918005b116824607cd`.
- `NativeBombSimulation.TimedThreatSnapshot` now returns threat owners and timestamped cells together; `NativeBotController.decide()` consumes that one snapshot rather than launching two identical timed-chain prediction passes.
- Regression proves the snapshot matches both existing helper views for a timed bomb that chains into a remote bomb, and proves bombs, flames, and next ID are unchanged.
- **- **Native Kotlin/JUnit: PASS** on final SHA `f3000302a5c4ba7f4063f3918005b116824607cd`: [run 40262196746](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40262196746).
- **Mainline source checks: PASS** on the same SHA: [run 40262196734](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40262196734).
- Earlier initial attempt on the previous implementation SHA hit Maven Central HTTP 429 before tests; the failed job was rerun successfully. The final follow-up SHA above passed without a code-related failure.
- Native artifacts: none; no APK packaging/build/upload.
- Xiaomi Pad 5 runtime and measured frame-time improvement: **NOT TESTED**. Symbian/EKA2L1 timing parity: **NOT TESTED**.
- PR #2 remains Draft and unmerged; collision PR #1 remains separate.
