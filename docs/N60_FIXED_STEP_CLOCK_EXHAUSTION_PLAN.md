# N60 — Fixed-step and round-clock exhaustion safety

## Engineering question
Can the renderer's process-wide fixed-step counter and each attempt's AI counter reach their signed Long limit without wrapping, and can an unrepresentable fixed step be rejected before the gameplay callback runs?

## Why source review alone is insufficient
The guard must compile in the existing Kotlin/JUnit module and preserve the last representable tick, zero-delta behavior, and the existing 41-arena round-clock reset contract. A source-only Gradle test is the cheapest direct verification; no Android package or APK is needed.

## Implementation
- Add a fail-closed pre-callback exhaustion guard to FixedStepClock.advance.
- Keep the default behavior at tick zero; add an internal initial-tick seam so boundary conditions can be tested without billions of updates.
- Apply the same non-negative initial-state contract to NativeRoundTickClock; retain its existing guard before Long overflow.
- Add production-clock boundary regressions for the last representable tick, rejected next tick, zero delta at exhaustion, and negative injected counters.

## Acceptance criteria
- The last representable fixed-step tick executes exactly once and leaves ticks equal to Long.MAX_VALUE.
- The following positive step throws before invoking the callback and never wraps the counter.
- Zero delta at exhaustion returns zero without invoking gameplay.
- The per-round clock rejects invalid initial state and refuses the overflowing next tick.
- Existing native core/JUnit and Mainline JS/Python checks pass; no APK packaging, artifact upload, game assets, or dependency changes.

## Evidence
- Source/test commit and CI: PENDING.
- Android device lifecycle, real gameplay, and Symbian/EKA2L1 parity: NOT TESTED.
- APK artifacts: 0 expected.
