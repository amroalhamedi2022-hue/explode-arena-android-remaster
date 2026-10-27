# N43 — Wire Android background lifecycle into the native pause gate

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX migration; keep Draft, no merge.

## Engineering question

The game already overrode libGDX `pause()` / `resume()` and called the pause gate. Review found that the callback contract lacked a focused combined regression covering the fixed-step clock, bomb fuse, and held-touch quarantine together. Can this existing lifecycle path be extracted behind a pure test seam so those linked safety guarantees remain protected?

## Source review

- Reviewed the official libGDX lifecycle guide: https://libgdx.com/wiki/app/the-life-cycle
- The guide states that Android invokes `pause()` on backgrounding and `resume()` when the app returns; `ApplicationAdapter` provides default implementations.
- No third-party code copied. The lifecycle hooks already existed; this milestone makes their combined behavior independently testable without an Android device.

## Acceptance criteria

1. `ExplodeArenaNativeGame.pause()` routes to `NativePauseGate.onBackground(clock)`.
2. `resume()` only clears the background flag and accumulator; it does not auto-unpause gameplay.
3. Bomb fuse and fixed-step count remain unchanged while backgrounded and after foregrounding until the explicit in-game resume action.
4. Lifecycle transitions are idempotent and discard partial-frame time to avoid catch-up ticks.
5. Run the existing Native Kotlin/JUnit workflow on the exact source/test SHA; no APK packaging or artifact upload.
6. Recheck the current PR head and its workflow result after documenting evidence.

## Implementation

- Added `NativeLifecycleCallbacks` as a pure adapter over the existing pause gate, fixed-step clock, and touch quarantine.
- The game's existing libGDX `pause()` / `resume()` overrides now delegate to that adapter while preserving walking-animation and sound cleanup.
- Added a JVM integration test that exercises the linked callback path with a live bomb fuse and a held bomb touch; it verifies no catch-up and requires a full release frame plus explicit resume.

## Evidence boundaries

- Source/JUnit CI can prove the callback adapter's fixed-step, fuse, and held-touch behavior and compile the libGDX game source.
- Physical Android Home/app-switch behavior and Xiaomi Pad 5 / HyperOS remain **NOT TESTED**.
- No APK build, signing, artifact upload, release, merge, main write, or copyrighted asset upload.
