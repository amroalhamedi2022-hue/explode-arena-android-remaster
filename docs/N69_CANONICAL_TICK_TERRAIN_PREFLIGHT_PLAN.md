# N69 — Canonical tick terrain-position preflight

Date: 2026-10-28

## Finding
NativeArenaTick validated that player/opponent coordinates were finite, but did not validate that actors began a playing tick inside the arena's traversable terrain. A zero-input or low-speed tick could therefore advance fuses, bots, and round state from an actor position already overlapping a solid cell or outside the legal arena bounds.

## Implementation
- Before canonical tick reservation or gameplay mutation, validate the player's and every living opponent's start position against terrain/bounds using the existing StageCollision geometry.
- Deliberately pass no bomb list to this terrain check. Bomb owner-exit grace remains a separate collision rule and must not be mistaken for invalid terrain.
- Keep the validation inside the PLAYING preflight; terminal presentation ticks remain governed by the round latch and finite-input contract.
- Add regressions for both a player and an opponent beginning inside a solid cell, including unchanged state and successful retry with the same canonical tick after repairing the position.

## Acceptance criteria
1. Player terrain-invalid input is rejected before player position, vitality, bombs/flames, roster tick, or round tick is mutated.
2. A living opponent inside a solid cell is rejected before the player moves or bomb fuses advance.
3. After repairing the invalid actor position, the same canonical tick succeeds.
4. Existing 41-arena, collision, bot, bomb, round, and replay tests remain green.
5. No APK packaging/build/upload. Android gameplay and Symbian/EKA2L1 parity remain separate NOT TESTED gates.

## Evidence
- Source/test commit: CI pending.
- Android device gameplay: NOT TESTED.
- Symbian/EKA2L1 parity: NOT TESTED.
- No external assets added; PR #1 collision work remains separate; no merge or main write.
