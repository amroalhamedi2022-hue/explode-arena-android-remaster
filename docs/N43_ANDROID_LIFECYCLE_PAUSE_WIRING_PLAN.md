# N43 — Wire Android background lifecycle into the native pause gate

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX migration; keep Draft, no merge.

## Engineering question

The native core already had a tested `NativePauseGate`, but `ExplodeArenaNativeGame` did not override libGDX's `ApplicationListener.pause()` / `resume()`. Thus the lifecycle safety contract was implemented in isolation rather than wired to Android's actual libGDX callbacks. Could an app background/resume cycle leave a bomb fuse or bot simulation running without an explicit player resume?

## Source review

- Reviewed the official libGDX lifecycle guide: https://libgdx.com/wiki/app/the-life-cycle
- The guide states that Android invokes `pause()` on backgrounding and `resume()` when the app returns; `ApplicationAdapter` provides default implementations.
- No third-party code copied; this connects the project's existing tested pause gate to the standard libGDX callbacks.

## Acceptance criteria

1. `ExplodeArenaNativeGame.pause()` routes to `NativePauseGate.onBackground(clock)`.
2. `resume()` only clears the background flag and accumulator; it does not auto-unpause gameplay.
3. Bomb fuse and fixed-step count remain unchanged while backgrounded and after foregrounding until the explicit in-game resume action.
4. Lifecycle transitions are idempotent and discard partial-frame time to avoid catch-up ticks.
5. Run the existing Native Kotlin/JUnit workflow on the exact source/test SHA; no APK packaging or artifact upload.
6. Recheck the current PR head and its workflow result after documenting evidence.

## Implementation

- Added `NativeLifecycleCallbacks` as a small adapter over the existing pause gate and fixed-step clock.
- Wired the game's libGDX `pause()` / `resume()` overrides to that adapter.
- Added a JVM integration test that exercises the callback adapter with a live bomb fuse and confirms an explicit resume is required.

## Evidence boundaries

- Source/JUnit CI can prove the core lifecycle state transition and compile the libGDX game source.
- Physical Android Home/app-switch behavior and Xiaomi Pad 5 / HyperOS remain **NOT TESTED**.
- No APK build, signing, artifact upload, release, merge, main write, or copyrighted asset upload.
