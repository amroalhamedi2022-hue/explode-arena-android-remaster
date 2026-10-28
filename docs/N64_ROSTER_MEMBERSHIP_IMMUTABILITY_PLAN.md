# N64 — Native opponent roster membership immutability

Date: 2026-10-28

## Engineering question
Does the roster keep the exact collection whose owner and object-identity invariants it validated? Previously the constructor accepted a List property directly, so a caller retaining a MutableList could append a duplicate owner after validation and silently change the simulation roster. A Kotlin read-only List reference is not, by itself, a defensive snapshot.

## Implementation
- Copy the caller-owned list before validating it.
- Keep the internal roster snapshot private and use it for all simulation operations.
- Return a detached snapshot from the bots accessor so a JVM cast/mutation cannot alter internal membership.
- Do not change provisional AI, collision, bomb, spawn or Symbian-parity rules.

## Acceptance criteria
1. Mutating the source MutableList after construction does not change roster membership.
2. Mutating a MutableList cast of the returned snapshot does not change roster membership.
3. Owner uniqueness, controller-owner identity and actor/vitality/controller independence checks remain active.
4. Existing :core:test including all-41-arena roster/fuse/replay regressions passes.
5. No APK task, packaging, asset upload, release, merge or main write.

## Evidence boundaries
- Source/JUnit CI: pending on this commit.
- Android device/native launch and Symbian/EKA2L1 parity: NOT TESTED.
- APK artifacts: 0 (no APK build requested).
- Google Drive LIVE sync: SYNC FAILED if the Drive/Docs connector is unavailable; no sync is claimed without read-back verification.