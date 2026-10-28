# N71 — Actor/bomb start-state collision preflight

Date: 2026-10-28

## Engineering question (before CI)

Can the production canonical tick reject an actor that starts inside a bomb it does not own, without consuming the tick, advancing a fuse, moving any actor, changing vitality, or latching a round outcome?

Source inspection alone is insufficient: the existing preflight intentionally skipped bomb occupancy to permit the owner to exit a freshly placed bomb. That broad exemption also allowed a malformed player/opponent state to start inside a foreign bomb and advance the simulation.

## Implementation

- Keep terrain and actor-pair preflight non-mutating.
- Validate bomb overlap for the player and each living opponent before canonical tick reservation.
- Refuse player/bot bomb placement when the 16px bomb footprint would overlap another living actor; reject before allocating a bomb ID, and wire the guard into native gameplay and replay paths.
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


## Evidence — 2026-10-28

- First source/test attempt: `f050bbf931bbff8779ef86c4c70ba8012f7ad4e4`. Mainline JS/Python **PASS** [#40669350356](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40669350356); native Kotlin/JUnit **FAIL** [#40669350248](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40669350248). Four regressions exposed a real integration gap: bomb placement could put a bomb cell over a neighboring actor, so the next canonical tick correctly rejected the malformed start state.
- Corrective production + integration-test SHA: `5a6b21dfc9571e67164457520df5a3a5ce36e5bb`.
- `NativeBombSimulation.place` now checks other actor hitboxes before incrementing the bomb ID; the roster, replay harness, and libGDX input path pass their live actor blockers. Regression coverage verifies refused placement consumes neither an ID nor owner grace, foreign-bomb starts fail before mutation, the same tick can be retried after repair, and valid owner exit grace still works.
- **PASS — Native Kotlin/JUnit:** [CI #40670102041](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40670102041), `:core:test`, BUILD SUCCESSFUL in 57s.
- **PASS — Mainline JS/Python PR:** [CI #40670102080](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40670102080). **PASS — Mainline push:** [CI #40670091795](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40670091795).
- Native artifact query: **0 artifacts**. No APK build/package/upload.
- Android device gameplay and Symbian/EKA2L1 parity: **NOT TESTED**. No original assets or dependencies added; PR #1 remains separate; no merge/main write.
- Google Drive LIVE sync: **SYNC FAILED** — no Google Drive/Docs connector is exposed in this runtime; no Drive read/write/read-back is claimed.
