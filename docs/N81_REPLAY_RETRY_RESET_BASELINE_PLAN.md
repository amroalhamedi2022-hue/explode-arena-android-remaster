# N81 — Validate LOST/DRAW retry against the canonical initial replay frame

Date: 2026-10-28  
PR: #2 Native Kotlin/libGDX; keep Draft/open.

## Engineering question

Can a replay comparator accept a LOST/DRAW → PLAYING tick-0 transition that resets only the clock while retaining stale actor positions, health, bombs, remote owners, or flames?

## Why source inspection alone is insufficient

The comparator validates serialized temporal sequences. The regression must exercise valid full-roster traces and adversarially mutate one field in the retry frame, proving the rejection occurs before a misleading matching-trace result.

## Production change

- Capture the first validated frame independently for each compared trace.
- Allow LOST/DRAW → PLAYING at tick 0 only when that trace began from a PLAYING tick-0 frame and every serialized field matches that initial frame exactly.
- Return retry-reset-baseline-missing when a trace lacks a valid initial baseline, and retry-reset-mutation when the reset frame retains or introduces stale state.
- Preserve frozen terminal holds and the existing 41-stage/1-, 2-, and 4-spawn deterministic replay coverage.

## Acceptance criteria

1. An exact initial-frame reset after LOST and DRAW is accepted.
2. Changing actor state on the retry frame is rejected at that frame with retry-reset-mutation.
3. A retry with no valid initial PLAYING/tick-0 baseline is rejected.
4. Same-tick terminal holds remain valid only when byte-identical; terminal tick advancement remains rejected.
5. Native Kotlin/JUnit and Mainline JS/Python PR checks pass on the exact source/test commit.
6. No APK build/package/upload, new dependencies, external assets, merge, or main write.
7. Android device gameplay and Symbian/EKA2L1 parity remain NOT TESTED.

## Verification — 2026-10-28

- Exact source/test commit: 3127508c2f9350b76a4e01d970ab80b75e495c35.
- PASS — Native Kotlin/JUnit, job native-source-check: [CI #40740125636](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40740125636); log reports BUILD SUCCESSFUL in 40s.
- PASS — Mainline JS/Python, job source-checks: [CI #40740125935](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40740125935).
- Artifact queries: 0. No APK build/package/upload.
- Android gameplay and Symbian/EKA2L1 parity: NOT TESTED.
- PR #2 remains Draft/open; PR #1 remains separate. No merge, main write, external assets, or dependency changes.
- Google Drive LIVE sync: SYNC FAILED — this runtime has no Google Drive/Docs connector; no Drive read/write/read-back is claimed.
