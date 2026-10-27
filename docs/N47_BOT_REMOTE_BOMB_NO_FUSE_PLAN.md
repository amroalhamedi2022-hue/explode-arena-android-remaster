# N47 — Remote bombs are not timed escape targets unless a timed chain can ignite them

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; remain Draft, no merge.

## Engineering question

Can the provisional bot AI distinguish an isolated remote bomb from an automatic timed threat, while still escaping a remote bomb that is predicted to be ignited by a timed bomb or active flame?

**Scope correction:** the current production API only grants remote detonators to `player` and `player2`; it rejects `bot` owners. Therefore a bot-owned remote bomb is not reachable through the public simulation API today. The selector is still made defensive so a future ownership change cannot accidentally treat a no-fuse bomb as a timed escape target.

## Implementation

- `selectOwnedTimedBomb` selects only the bot's owned bombs that are non-remote and have a fuse.
- Timed-chain predictions remain authoritative for escape decisions: if a timed bomb's ray or an active flame schedules a remote bomb, that remote bomb remains in the shared threat footprints and can trigger escape.
- No remote fuse is invented and no remote bomb is auto-detonated by this change.

## Acceptance criteria

1. A synthetic owned remote bomb is not selected as an automatic-fuse escape target.
2. An isolated player-owned remote bomb does not divert a bot from a clear pursuit route.
3. A remote bomb reached by a timed chain remains a threat and causes the bot to choose an escape direction.
4. Existing timed-own-bomb escape and chain-reaction tests continue to pass.
5. Run `gradle --project-dir android :core:test --stacktrace --no-daemon` on the exact source/test SHA and Mainline JS/Python source checks; confirm zero artifacts.
6. No APK packaging/signing/upload, copyrighted asset additions, merge, main write or auto-merge.

## Source and license review

This is a correction to project-owned Kotlin AI logic. No third-party code or game assets are copied or added. The existing libGDX dependency remains a dependency, not vendored source. AI/fuse behavior remains provisional until independently compared with Symbian/EKA2L1.

## Verification ledger

Pending source/test CI and independent PR-head recheck. Xiaomi Pad 5, original Symbian/EKA2L1 behavior and visual AI behavior remain **NOT TESTED**.
