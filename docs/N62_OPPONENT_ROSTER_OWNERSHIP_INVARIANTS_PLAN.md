# N62 — Opponent roster ownership invariants

**Date:** 2026-10-28  
**PR:** #2 — Native Kotlin/libGDX; keep Draft and unmerged.

## Engineering question

Can malformed native opponent rosters silently share an owner bomb quota or alias the same mutable actor, vitality, or AI controller across multiple bots?

## Why source inspection alone is insufficient

The invariant is enforced at roster construction, but its failure behavior and the canonical 4-spawn factory need executable JVM regressions. The test calls the actual production constructor/factory and uses referential identity checks rather than data-class structural equality.

## Implementation

- Reject duplicate owner IDs at construction so two slots cannot silently share a per-owner bomb quota or create owner-key ambiguity.
- Reject shared Actor, NativeVitality, or NativeBotController instances between slots.
- Preserve valid 1/2/4-spawn creation and keep all gameplay/timing semantics provisional pending EKA2L1 comparison.
- Add JVM tests for duplicate owners, shared mutable references, and the canonical three-opponent factory.

## Acceptance criteria

1. Duplicate owner IDs fail before the first simulation tick.
2. A shared actor, vitality object, or controller fails closed.
3. The real 4-spawn factory returns three uniquely named bots with independent mutable state.
4. Existing 41-arena tick/replay, collision, bomb and round-flow tests remain green.
5. Native Kotlin/JUnit CI and Mainline JS/Python CI pass on the exact docs-updated PR head; no APK build/package, asset upload, merge, main-branch write or auto-merge.

## Reference review

- Reviewed Kotlin's official distinction between structural equality (==) and referential equality (===): https://kotlinlang.org/docs/equality.html. The invariant concerns shared object identity, not equal values.
- No third-party implementation or dependency was copied or added.

## Verification ledger

- Production change: NativeOpponentRoster constructor validates unique owner IDs and independent actor/vitality/controller references.
- Regression suite: NativeOpponentRosterInvariantTest.kt exercises malformed constructor inputs and the actual 4-spawn factory.
- Exact source/test and docs-updated PR-head CI: PENDING.
- Android device / Xiaomi Pad 5 and Symbian EKA2L1 parity: NOT TESTED.
- APK artifacts: 0 expected; no packaging requested.
- Google Drive LIVE sync: SYNC FAILED if the Drive/Docs connector is unavailable; no sync claim without document read-back and folder metadata verification.
