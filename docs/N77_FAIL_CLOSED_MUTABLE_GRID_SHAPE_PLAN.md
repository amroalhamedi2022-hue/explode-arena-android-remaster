# N77 — Fail-closed collision queries after mutable grid-shape corruption

Date: 2026-10-28  
PR: #2 Native Kotlin/libGDX; keep Draft/open.

## Engineering question

Can direct collision callers crash or read an invalid subset of the arena if a caller mutates the retained StageData.grid list or one of its IntArray rows after StageData construction?

## Why source inspection alone is not enough

StageData checks row shape only in its constructor, but Kotlin's read-only List interface does not detach a caller-owned mutable list and IntArray rows remain mutable. A focused regression must mutate those aliases after construction and exercise the production StageCollision.tile and StageCollision.blocked APIs.

## Change

- Fail closed when grid dimensions no longer match the arena's pixel dimensions.
- Validate the selected row before indexing it; a ragged row returns the blocked/out-of-bounds sentinel rather than throwing.
- Keep valid-stage collision rules unchanged and avoid copying the entire grid on every 60 Hz collision probe.
- Add regressions for a shortened first row, a shortened overlapped row, and a cleared caller-owned grid list.

## Acceptance criteria

1. tile returns -1 and blocked returns true for an empty or dimensionally inconsistent grid.
2. A ragged row cannot cause ArrayIndexOutOfBoundsException or be interpreted as a traversable cell.
3. Existing 41-stage catalog, movement, bomb, bot and replay suites remain green.
4. Free CI runs :core:test only; no APK build/package/upload, asset additions, new dependencies, merge or main write.
5. Android device gameplay and Symbian/EKA2L1 parity remain NOT TESTED. PR #1 collision work remains separate.

## Evidence

Source/test SHA and CI: pending. Final docs-head recheck: pending.  
Google Drive LIVE sync must be marked SYNC FAILED if the Drive/Docs connector is unavailable; never claim sync without read-back verification.


## Source/test evidence — 2026-10-28

- Exact production + regression-test head: `3c365a22e049c25dc3285937bee4b9d43851e120`.
- Initial CI runs caught two compatibility assumptions and were corrected before acceptance: [#40713396837](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40713396837) exposed offset-grid fixtures; [#40713946085](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40713946085) showed that offset grids may extend beyond the nominal world rectangle in existing collision fixtures. The final guard validates row consistency only and preserves those established offset-grid semantics.
- **PASS:** Native Kotlin/JUnit [#40714472657](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40714472657), `:core:test`, BUILD SUCCESSFUL in 57s.
- **PASS:** Mainline PR CI [#40714472754](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40714472754).
- Artifact query: **0**. No APK build/package/upload; no assets or dependencies added.
- Android device gameplay and Symbian/EKA2L1 parity: **NOT TESTED**. PR #1 remains separate; no merge or main write.
- Final documentation-head recheck: pending PR verification comment.
- Google Drive LIVE sync: **SYNC FAILED** — no Google Drive/Docs connector is exposed in this runtime; no read/write/read-back is claimed.
