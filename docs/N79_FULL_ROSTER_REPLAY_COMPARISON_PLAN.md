# N79 — Full-roster native replay comparison

Date: 2026-10-28  
PR: #2 Native Kotlin/libGDX; keep Draft/open.

## Engineering question

Can the complete 1/2/4-spawn native replay emitted by `NativeArenaReplaySession` be compared as a validated trace, with useful first-field diagnostics, rather than relying only on per-frame string equality?

## Why source inspection alone is insufficient

The serializer and comparator must agree on the actual full-roster format. Tests need to exercise production-generated traces from all 41 stage records, as well as malformed-but-identical traces, bot divergence, tick discontinuities, and oversized input. A comparator that merely returns equality for two strings could otherwise silently accept a malformed trace.

## Production and test changes

- Added `NativeArenaReplayComparison` for the existing `EA-ARENA-PROVISIONAL-v1` full-roster trace; the legacy single-player `EA-NATIVE-PROVISIONAL-v2` comparator remains separate.
- Validate schema, line/frame bounds, stage and round state, tick progression including terminal holds and retry resets, player/bot records, remote-owner membership, pass-bomb IDs, bomb IDs/owners/fuses, and flame cells.
- Report the first differing field, reject identical malformed recordings, and bound diagnostic previews.
- Integrated the comparator into the 180-tick four-spawn replay and the 41-stage / 30-tick-per-stage production replay test.

## Acceptance criteria

1. Full-roster production traces from all 41 stage records compare successfully when produced by identical sessions.
2. Four-spawn traces cover player, all bots, remote ownership, bombs and flames.
3. A bot-field change reports `bots` at the first differing frame.
4. Identical malformed traces, repeated active ticks, empty traces, and oversized lines are rejected.
5. Existing native core and Mainline source checks remain green.
6. CI remains source-only: no APK build/package/upload, no external game assets, no new dependencies, no merge or `main` write.
7. Device gameplay and original Symbian/EKA2L1 parity remain **NOT TESTED**.

## Verification — 2026-10-28

- Production + regression-test SHA: `265ea6054269512917a1e8c392968ad0cc33c780`.
- **PASS:** Native Kotlin/JUnit [CI #40732809179](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40732809179), `:core:test`, **BUILD SUCCESSFUL in 59s**.
- **PASS:** Mainline JS/Python source checks [CI #40732809023](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40732809023).
- Artifact query: **0**. No APK build/package/upload.
- The final documentation-head recheck will be recorded in the PR conversation after it completes.
- Android device gameplay and Symbian/EKA2L1 parity: **NOT TESTED**. PR #1 remains separate; no merge or `main` write.
- Google Drive LIVE sync: **SYNC FAILED** — no Google Drive/Docs connector is exposed in this runtime; no Drive read/write/read-back or folder metadata verification is claimed.
