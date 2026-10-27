# N48 — Chained remote-bomb escape regression fixture correction

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft/open, no merge.

## Engineering question

Does the chained-remote-bomb AI regression actually provide a reachable refuge, and does the controller choose the independently computed refuge direction? The previous fixture revision moved the wall to row 5, column 12 while placing the bot at (88, 40). That configuration blocked the corridor needed to leave the remote bomb's predicted footprint, so the test correctly failed at its "refuge exists" assertion. This was a test-fixture geometry error, not evidence that the production AI had passed.

## Correction

- Move the wall one grid cell farther to row 5, column 13. It still constrains a direct chase route while leaving a perpendicular refuge reachable within the provisional fuse deadline.
- Require the independent escape search to return a non-zero direction; a non-null zero direction is not sufficient proof of escape.
- Assert that NativeBotController.decide returns the same direction as the independently computed refuge.
- Keep the chain prediction assertion: a timed bomb must still schedule the remote bomb in its chain footprint.
- Do not change production AI rules or claim Symbian parity from this fixture.

## Acceptance criteria

1. The timed bomb's chain prediction contains the player2-owned remote bomb.
2. The bot has a non-zero reachable refuge outside both predicted blast footprints.
3. The controller does not plant another bomb and follows the computed refuge rather than continuing the direct chase.
4. gradle --project-dir android :core:test --stacktrace --no-daemon passes on the exact source/test SHA.
5. Mainline JS/Python checks pass; no APK packaging/signing/upload or artifacts.
6. The docs-updated PR head is rechecked. The native workflow now includes docs/** and README_AR.md paths to ensure this evidence is rerun after acceptance-ledger changes.

## Verification ledger

- **Initial attempt — FAIL:** commit 6fd4b8831c9558df93dda8a09af6482c317f3b65, Native CI [#40518948655](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40518948655). The chained remote-bomb escape test failed because the wall placement left no reachable refuge.
- **Corrected source/test SHA:** ec9a6f37811710e6d5996a7bf22ca42869a8c6c9.
- **PASS — Native Kotlin/JUnit:** [#40520940594](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40520940594).
- **PASS — Mainline JS/Python:** [#40520940530](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40520940530).
- Artifacts: **0** on both runs.
- Xiaomi Pad 5, Symbian/EKA2L1 AI parity, real-time visual behavior: **NOT TESTED**.
- Google Drive LIVE sync: **SYNC FAILED** — this runtime has no Google Drive/Docs connector; no LIVE read/write or folder metadata verification is claimed.
- No copyrighted assets added, no merge, no main write, no release or auto-merge.
