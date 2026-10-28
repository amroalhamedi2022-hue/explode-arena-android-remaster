# N80 — Frozen terminal replay-frame invariant

Date: 2026-10-28  
PR: #2 Native Kotlin/libGDX; keep Draft/open.

## Engineering question

Does the full-roster replay comparator reject terminal traces that keep the same tick while mutating actor/hazard state, or that continue advancing ticks after a latched terminal state?

## Why source inspection alone is insufficient

The comparator is a parser plus temporal state validator. A trace may be byte-identical between two recordings and still be structurally impossible. Regression tests must exercise production-generated frames and adversarial trace edits, while preserving the legitimate LOST/DRAW retry reset.

## Production and regression changes

- Validate frozen states (LOST, DRAW, ADVANCING, COMPLETE) before the ordinary monotonic-tick rule.
- Permit a same-tick terminal hold only when the complete serialized frame is unchanged.
- Permit only the documented LOST/DRAW → PLAYING reset at tick 0; reject tick advancement or state mutation after other latched terminal frames.
- Add regressions for terminal actor mutation, tick advancement after COMPLETE, and unchanged terminal holds.

## Acceptance criteria

1. Identical frozen terminal frames compare successfully.
2. Any field mutation on a repeated terminal tick reports `terminal-hold-mutation`.
3. A terminal state cannot advance its tick and reports `terminal-state-advanced`.
4. LOST/DRAW retry at tick 0 remains allowed.
5. Existing full-roster replay tests across the 41 stage records and 1/2/4-spawn traces remain green.
6. Source-only CI; no APK packaging/upload, no external assets, no dependency changes, no merge or `main` write.
7. Android gameplay and Symbian/EKA2L1 parity remain **NOT TESTED**.

## Verification status

- Source/test SHA: pending commit.
- Native Kotlin/JUnit CI: PENDING.
- Mainline JS/Python CI: PENDING.
- Artifacts: expected 0; no APK build requested.
- Google Drive LIVE sync: SYNC FAILED if the runtime has no Drive/Docs connector; no sync claim without read-back.
