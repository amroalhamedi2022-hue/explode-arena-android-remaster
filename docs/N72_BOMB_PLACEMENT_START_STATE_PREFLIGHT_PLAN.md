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

## Evidence — 2026-10-28

- Initial source/test SHA `96f3ec4c6c5c9555e3ea24bea497509efb5d72f9`: Mainline PR CI [#40677000922](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40677000922) PASS; Native Kotlin/JUnit [#40677000893](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40677000893) FAIL because two older fixtures placed actors inside terrain under the new preflight.
- Corrected test fixture SHA: `4b2c7f0c8677ab184b6c8f467c1ad5effea37c4d`; production source remains the same N72 guard. The fixtures now keep the actor hitbox clear while preserving the target bomb center/blast path.
- **PASS — Native Kotlin/JUnit:** [CI #40677471792](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40677471792), `:core:test`, BUILD SUCCESSFUL in 49s.
- **PASS — Mainline PR:** [#40677471695](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40677471695). **PASS — Mainline push:** [#40677460732](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40677460732).
- Native artifacts: 0. No APK build/package/upload, no new assets/dependencies, no merge/main write.
- Android device gameplay and Symbian/EKA2L1 parity: **NOT TESTED**. PR #1 remains separate.
- Google Drive LIVE sync: **SYNC FAILED** — no Drive/Docs connector is exposed in this runtime.


CI is pending for the source/test commit. Do not treat source checks as device or EKA2L1 parity evidence.
