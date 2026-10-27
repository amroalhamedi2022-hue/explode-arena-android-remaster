# N49 — Bound native bot escape replanning without weakening per-tick safety

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft/open, no merge.

## Engineering question

The controller's escape branch replanned on every 60 Hz simulation tick even though the normal navigation cadence is six ticks (10 Hz). NativeBotEscapeRoute.firstDirection builds timed-blast indexes and performs bounded grid search; doing that for every living bot every frame creates avoidable CPU work, especially in four-spawn arenas. The current source and a deterministic scheduling regression are needed to distinguish a safe cadence optimization from simply skipping hazard checks.

## Change

- Reuse a cached escape direction while the six-tick planning budget is valid and the immediate step remains safe.
- Replan immediately when escape mode changes, the tick rewinds, the six-tick budget expires, or the cached next step becomes blocked/unsafe.
- Keep the live threat context and canStep validation evaluated every tick; do not alter fuse, blast, movement, placement or Symbian-parity claims.
- Expose the last planning tick only within the core module for deterministic regression tests.

## Acceptance criteria

1. A stable escape decision is reused at ticks 61 and 65 after a plan at tick 60.
2. A fresh plan is produced at tick 66 (six 60 Hz ticks later).
3. A newly blocked cached next step forces a replan immediately at tick 67.
4. Existing chain-reaction, active-flame, 1/2/4-spawn and all-41-arena tests remain green.
5. Run gradle --project-dir android :core:test --stacktrace --no-daemon on the exact source/test SHA and Mainline JS/Python checks on that SHA; re-check the documentation-updated PR head.
6. No APK packaging/signing/upload, new assets/dependencies, merge, main write or auto-merge.

## Open-source and safety review

This is a bounded optimization of the project's existing Kotlin controller, not a copied AI implementation. The official libGDX documentation was reviewed for the existing application framework; no third-party code or asset is introduced. All AI/fuse/hitbox rules remain provisional until matched against Symbian EKA2L1.

## Verification ledger

- Implementation committed; source/test and docs-head CI results pending.
- Xiaomi Pad 5 and Symbian/EKA2L1 parity: **NOT TESTED**.
- Artifacts expected: **0**; no APK task is allowed.
- Google Drive LIVE sync: **SYNC FAILED** if the Drive/Docs connector is unavailable in the current runtime; do not claim sync without a verified read-after-write.
