# N78 — Bounded collision-grid shape preflight

Date: 2026-10-28  
PR: #2 Native Kotlin/libGDX; keep Draft/open.

## Engineering question

Can the native collision path keep failing closed for malformed mutable grid rows without scanning every row on every player/bot collision probe?

## Why source inspection alone is insufficient

The collision path is called repeatedly by movement substeps and bot route checks. A regression test with a counting mutable list is needed to prove that a hot probe reads only the header and rows it intersects, while malformed touched rows still fail closed.

## Production change

- Capture the intended grid width/height when StageData is constructed.
- Validate list size and first-row shape in O(1), then validate each row immediately before reading it.
- Keep tile/block checks safe for list clearing, first-row corruption, and a shortened row that is actually touched.
- Make nearest-free search return null rather than indexing a malformed row.
- Preserve offset-grid semantics; do not infer that every grid footprint must align to the nominal world rectangle.

## Acceptance criteria

1. A valid collision probe does not iterate the entire grid list; the regression bounds list reads.
2. A malformed touched row returns the blocked/out-of-bounds result and never throws an index exception.
3. A cleared list or changed first-row shape fails closed.
4. nearestFree returns null for a malformed row encountered during its search.
5. Existing stage catalog, 41-arena, movement, bomb, opponent, and replay suites remain green.
6. Free CI runs `:core:test` only. No APK build/package/upload, asset additions, new dependencies, merge, or main write.
7. Android device gameplay and Symbian/EKA2L1 parity remain NOT TESTED; PR #1 stays separate.

## Evidence

Source/test SHA and CI: pending. Final-head verification: pending.  
Google Drive LIVE sync: SYNC FAILED if the Drive/Docs connector is unavailable; no sync is claimed without read-back verification.
