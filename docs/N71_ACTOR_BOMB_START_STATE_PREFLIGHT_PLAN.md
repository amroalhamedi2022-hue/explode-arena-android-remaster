# N71 — Actor/bomb start-state collision preflight

Date: 2026-10-28

## Engineering question (before CI)

Can the production canonical tick reject an actor that starts inside a bomb it does not own, without consuming the tick, advancing a fuse, moving any actor, changing vitality, or latching a round outcome?

Source inspection alone is insufficient: the existing preflight intentionally skipped bomb occupancy to permit the owner to exit a freshly placed bomb. That broad exemption also allowed a malformed player/opponent state to start inside a foreign bomb and advance the simulation.

## Implementation

- Keep terrain and actor-pair preflight non-mutating.
- Validate bomb overlap for the player and each living opponent before canonical tick reservation.
- Permit overlap only when `passBomb` matches the overlapping bomb ID and that bomb's owner matches the actor owner (`player` or the bot slot owner).
- Preserve legitimate owner-exit grace and existing strict edge-touch semantics.
- Add canonical-tick integration regressions for player/foreign-bomb overlap, opponent/player-bomb overlap, and the valid owner-grace path.

## Acceptance criteria

1. Foreign-bomb overlap is rejected before player/bot positions, vitality, bomb fuse/flame state, round state, or either tick clock changes.
2. The rejected canonical tick can be retried after repairing the actor position.
3. The rightful owner can still leave its own bomb when matching `passBomb` grace is present.
4. Existing all-41-arena, bot, bomb, collision, and round integration tests remain green.
5. No APK build/package/upload, no new assets/dependencies, no merge/main write, and PR #1 collision work remains separate.
6. Android-device gameplay and original Symbian/EKA2L1 parity remain independent NOT TESTED gates.
