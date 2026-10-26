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

Production and regression changes committed together. CI result pending; acceptance ledger will be appended after the matching SHA is checked.
