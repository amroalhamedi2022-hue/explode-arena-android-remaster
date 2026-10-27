# N50 — Share canonical threat keys across native bots

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft/open, no merge.

## Engineering question

N49 reduced expensive escape-path replanning to a six-tick cadence, but NativeBotController still mapped and sorted the same timed-threat identity/geometry list independently for every bot decision at 60 Hz. The roster already creates one immutable NativeBotThreatContext per living simulation tick, so per-controller reconstruction is redundant allocation and sorting.

## Implementation

- Build the sorted, exact ThreatKey list once alongside the shared per-tick threat snapshot.
- Make every bot compare its previous key list with the same immutable context list. Fuse countdown values remain intentionally excluded; threat identity/geometry changes still invalidate cached routes immediately.
- Hoist movement direction vectors and grid-neighbor deltas to controller companion tables to avoid repeated array allocation in decision/path planning.
- Preserve collision, fuse, route cadence, bomb placement, and provisional AI semantics.

## Acceptance criteria

1. Threat keys are canonical and ordered by bomb ID.
2. A countdown-only tick preserves the key list values and does not falsely signal a new threat.
3. A new timed threat entering the prediction horizon changes the key list.
4. Existing chain, active-flame, cached-route, and all-41-arena/1-2-4-spawn integration tests remain green.
5. Native Kotlin/JUnit and Mainline JS/Python checks pass on the exact source/test SHA; the docs-updated PR head is rechecked.
6. No APK packaging/signing/upload, new dependencies/assets, merge, main write, or auto-merge.

## Source and licensing review

This optimizes project-owned Kotlin logic. No third-party AI code, game assets, or dependency is introduced. The bot remains explicitly provisional until compared against Symbian/EKA2L1.

## Verification ledger

- Source/test SHA and CI: **PENDING**.
- Xiaomi Pad 5 and Symbian/EKA2L1 parity: **NOT TESTED**.
- Artifacts: **0 expected**.
- Google Drive LIVE sync: **SYNC FAILED** if no Drive/Docs connector is exposed; do not claim sync without verified read-after-write.
