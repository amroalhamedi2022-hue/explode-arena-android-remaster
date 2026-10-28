# N49 — Bound native bot escape replanning without weakening per-tick safety

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft/open, no merge.

## Engineering question

The controller's escape branch replanned on every 60 Hz simulation tick even though the normal navigation cadence is six ticks (10 Hz). NativeBotEscapeRoute.firstDirection builds timed-blast indexes and performs bounded grid search; doing that for every living bot every frame creates avoidable CPU work, especially in four-spawn arenas. A safe cadence must still replan immediately when the timed-threat set changes or the cached next step becomes blocked.

## Change

- Reuse a cached escape direction while the six-tick planning budget is valid and the immediate step remains safe.
- Replan immediately when escape mode changes, the tick rewinds, the six-tick budget expires, the cached next step becomes blocked, or threat identities/geometry change.
- Compare threat identity and geometry without countdown values, because fuse countdowns naturally change each tick; new/disappearing chain threats still invalidate the plan immediately.
- Keep the live threat context and canStep validation evaluated every tick; do not alter fuse, blast, movement, placement or Symbian-parity claims.
- Expose the last planning tick only within the core module for deterministic regression tests.

## Acceptance criteria

1. A stable escape decision is reused at ticks 61 and 65 after a plan at tick 60.
2. A fresh plan is produced at tick 66 (six 60 Hz ticks later).
3. A newly blocked cached next step forces a replan immediately at tick 67.
4. A newly added timed threat invalidates a cached route on the next decision.
5. Existing chain-reaction, active-flame, 1/2/4-spawn and all-41-arena tests remain green.
6. Run gradle --project-dir android :core:test --stacktrace --no-daemon on the exact source/test SHA and Mainline JS/Python checks on that SHA; re-check the documentation-updated PR head.
7. No APK packaging/signing/upload, new assets/dependencies, merge, main write or auto-merge.

## Open-source and safety review

This is a bounded optimization of the project's existing Kotlin controller, not a copied AI implementation. The official libGDX documentation was reviewed for the existing application framework; no third-party code or asset is introduced. All AI/fuse/hitbox rules remain provisional until matched against Symbian EKA2L1.

## Verification ledger

- Follow-up threat-set invalidation added after reviewing the first implementation; final source/test and docs-head CI pending.
- Xiaomi Pad 5 and Symbian/EKA2L1 parity: **NOT TESTED**.
- Artifacts expected: **0**; no APK task is allowed.
- Google Drive LIVE sync: **SYNC FAILED** if the Drive/Docs connector is unavailable in the current runtime; do not claim sync without a verified read-after-write.


## Verification addendum — 2026-10-27

- **Final source/test SHA:** b68c8588ff1b1de044656bb3f504f3344345b219.
- **PASS — Native Kotlin/JUnit:** [CI #40526396371](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40526396371), native-source-check completed successfully.
- **PASS — Mainline JS/Python:** [CI #40526396471](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40526396471), exact same SHA.
- A follow-up regression also proves a newly appearing timed-chain threat invalidates the cached plan immediately; the stable route remains throttled to six ticks while next-step safety is checked every decision.
- **Artifacts: 0** in both runs; no APK packaging/signing/upload.
- Xiaomi Pad 5 and Symbian/EKA2L1 parity: **NOT TESTED**. No new game assets, dependency, merge, main write or auto-merge.
