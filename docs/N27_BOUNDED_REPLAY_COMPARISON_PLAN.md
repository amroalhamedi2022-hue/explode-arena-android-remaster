# N27 — Bounded native replay comparison input

Date: 2026-10-26  
Status: source acceptance PASS on exact source/test commit 13c7647e2b8473c5ff9c3443e1b76f4d22319a80.

## Engineering question

Can replay comparison reject oversized trace lines before splitting/allocating field arrays and bound the number of frames it consumes, while preserving exact first-divergence reporting for valid in-bound traces?

Source inspection alone is insufficient: the critical properties are allocation ordering and iterator consumption. They need executable tests using oversized strings and lazy counting iterables.

## Planned implementation

- Check each raw line length before calling `split('|')`; cap one line at 512 Ki characters.
- Do not retain or echo an oversized untrusted line in the returned divergence; report only the configured limit and observed length.
- Cap comparison at 100,000 frames. If either input contains another frame, return `frame-limit` without consuming that extra frame.
- Preserve existing schema, field, tick continuity, empty-trace, first-difference, and unequal-frame-count behavior for traces within the cap.
- Keep this as a pure JVM/source change. Do not assemble/package/upload an APK.

## Acceptance criteria

1. Oversized expected and actual lines are rejected before parsing; the divergence contains lengths, not the large payload.
2. Two lazy traces longer than the cap produce a `frame-limit` divergence at frame index 100,000.
3. Neither iterator consumes more than 100,000 frames when the cap is exceeded.
4. Existing valid deterministic replays, malformed-trace rejection, first-field divergence, empty traces, and short frame-count mismatch tests remain green.
5. Run the actual free native workflow (`:core:test`) and Mainline JS/Python validation on the resulting source/test checkpoint; report exact SHA and run URLs.
6. Device playback, EKA2L1 parity, and performance are not inferred from source CI.

## Safety and scope

No new dependency, copied implementation, game art/audio, APK packaging, release, merge, auto-merge, or write to `main`. PR #2 remains separate from collision PR #1.

## Verification ledger — 2026-10-26

- **Production + test commit:** `13c7647e2b8473c5ff9c3443e1b76f4d22319a80`.
- **PASS Native Kotlin/JUnit:** [CI run 40328225806](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40328225806), actual `gradle --project-dir android :core:test --stacktrace --no-daemon`; log: **BUILD SUCCESSFUL in 46s**, 4 actionable tasks executed.
- **PASS Mainline JS/Python:** [CI run 40328225489](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40328225489) on the same PR head.
- The added lazy-iterator regression verifies exactly 100,000 frames are consumed from each input and that the comparator reports `frame-limit` without calling `next()` on the remaining tail. Oversized-line regressions cover expected and actual sides and assert the payload is not echoed in the divergence.
- **Artifacts: zero.** The workflow runs JVM source/tests only; no APK packaging or upload.
- **NOT TESTED:** Android device playback, Xiaomi Pad 5 performance, full match, and Symbian/EKA2L1 parity.
- PR #2 remains Draft; collision PR #1 and `main` are untouched.
- **Google Drive LIVE sync: SYNC FAILED** — Drive/Docs connector unavailable in this runtime; no LIVE document read/write or folder verification is claimed.
