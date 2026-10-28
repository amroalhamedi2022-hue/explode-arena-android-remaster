# N72 — Validate the bomb placer’s actual actor state

Date: 2026-10-28  
PR: #2 Native Kotlin/libGDX; keep Draft/open.

## Engineering question

Can a direct bomb-placement call create a bomb when the actor’s real hitbox overlaps a wall, when its `passBomb` token belongs to another owner, or when the actor overlaps a blocker—even if the snapped bomb center itself is clear?

## Why source inspection is not enough

The placement routine previously checked only the snapped bomb center and whether that new bomb would overlap another actor. The canonical tick has a broader preflight, but `NativeBombSimulation.place` is also called by AI/replay/game paths and should fail closed independently. A crafted grid/position fixture is needed to prove the real actor hitbox, owner grace and ID counter remain unchanged.

## Production change

- Validate the current actor hitbox against terrain and existing bombs before quota/ID mutation.
- Permit `passBomb` only when the referenced active bomb belongs to the placement owner.
- Reject actor-to-actor overlap before placing a bomb, even when the snapped bomb center is outside the other actor’s hitbox.
- Preserve legitimate same-owner exit grace and all existing bomb placement behavior for valid actors.

## Acceptance criteria

1. Invalid actor position, foreign-owner grace, or overlapping blockers returns `false` before incrementing the bomb ID, adding a bomb or changing the actor’s `passBomb`.
2. A real regression reproduces the wall-adjacent snap case: actor x=31 overlaps a wall at x=32..39 while its snapped bomb center x=24 is clear.
3. Existing fuse, chain reaction, remote bomb, collision, bot and 41-arena integration tests remain green.
4. Source/JUnit CI only; no APK build/package/upload or external assets.
5. Android device gameplay and original Symbian/EKA2L1 parity remain NOT TESTED.

## Evidence

CI is pending for the source/test commit. Do not treat source checks as device or EKA2L1 parity evidence.
