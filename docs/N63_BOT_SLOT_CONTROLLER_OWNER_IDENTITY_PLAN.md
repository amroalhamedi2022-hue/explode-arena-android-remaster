# N63 — Roster/controller owner identity invariant

**Date:** 2026-10-28  
**PR:** #2 — Native Kotlin/libGDX; keep Draft/open, no merge.

## Engineering question

Can a roster slot and its AI controller disagree about which owner owns a timed bomb, causing the controller to fail to recognize its own bomb and choose the wrong escape behavior?

## Finding and production change

- Confirmed a gap: BotSlot.owner was used when committing a bomb, while NativeBotController used its own private owner ID to recognize owned timed bombs.
- A caller could construct BotSlot("bot", ..., controller = NativeBotController("bot2")); unique roster IDs and distinct object references did not reject this inconsistent pairing.
- Expose a read-only internal controller owner identity and reject a roster before gameplay unless each slot owner exactly matches its controller identity.
- No game assets, dependencies, original Symbian claims, or public binary artifacts added.

## Acceptance criteria

1. A mismatched slot/controller owner pair throws IllegalArgumentException during roster construction.
2. Canonical one/two/four-spawn rosters continue to construct with independent actors, vitality and controllers.
3. Existing bomb ownership, threat, replay and 41-arena tests remain green.
4. Free PR CI runs :core:test on the source/test commit; no APK packaging.
5. Android device and Symbian/EKA2L1 parity remain separately reported as NOT TESTED.

## Final evidence — PASS

- Production+test SHA: 7bf33170fab67ea9b3f56bfad038ed35bba39e5a.
- Native Kotlin/JUnit CI #40623127680: PASS on the exact source/test SHA.
- Mainline JS/Python CI #40623127776: PASS on the exact source/test SHA.
- APK artifacts: 0; native workflow ran core tests only.
- Android device / EKA2L1: NOT TESTED.
- Google Drive LIVE: SYNC FAILED; no Drive/Docs connector was available for read/write/read-back verification.
