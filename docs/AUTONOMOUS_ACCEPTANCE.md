# Explode Arena autonomous acceptance — Native Kotlin/libGDX

AUTOMATION_COMPLETE: NOT READY
Updated: 2026-10-23 (native migration first code slice)

## What changed (production source: YES)
- Same original Mainline and Android application ID `com.explodearena.remaster`; no new repository.
- Native Kotlin module `android/core` with typed original stage model, strict parser of the existing 41-stage data file, provisional JS-compatible collision/axis movement, fixed 60Hz clock and seeded LCG.
- New real libGDX `ExplodeArenaNativeGame` rendering original stage-0 map texture with a native SpriteBatch, ShapeRenderer actor overlay and Kotlin simulation. **This is a migration slice, not the final game**: original HUD, bombs, full controls, audio and HD fidelity have not been ported.
- `NativeGameActivity` is an internal non-exported Activity, so existing WebView launcher remains the default during migration. No Android identity change or WebView removal before parity and save migration.
- Added `android/core/src/test` Kotlin/JUnit tests for clock, seeded RNG, collision, parser and original 41-stage data. Gradle modules and ARM64 native libGDX dependencies wired following the official gdx-liftoff Kotlin sample.

## Evidence and limits
| Requirement | Status | Evidence / limitation |
|---|---|---|
| N00: original 41 arena data, ordered IDs, grid dimensions and spawn counts | PASS (structural only) | Read `web/assets/stages.js` from main (Git blob `df011553a0f4f87263070d0df19f9af33e2b731e`); parsed 41 stages, IDs 0..40, zero malformed grids, 18 with 1 spawn, 2 with 2 spawns, 21 with 4 spawns. No new original asset uploads. |
| N00: full SHA-256 asset manifest and Symbian EKA2L1 parity | NOT TESTED | Only stage JS blob and structural inventory checked; no independent asset-by-asset SHA-256 or EKA2L1 replay this round. |
| N01: native Kotlin module + libGDX renderer/Android Activity | PARTIAL / NOT TESTED ON ANDROID | Actual Kotlin/libGDX source, Gradle wiring and tests committed on PR #2. No native APK compiled or launched on a real Android device. |
| Fixed-step RNG and provisional collision | PASS (local Kotlin smoke only) | Locally compiled the matching `Simulation.kt` logic with `kotlinc` and ran JVM smoke on JDK: 60 ticks from both 60 small frames and 4 quarter-second frames, LCG sequence, axis collision and nearestFree. Full repo `:core:test` **NOT TESTED**. |
| Kotlin/JUnit StageCatalog and simulation tests | NOT TESTED | Test sources committed, but no Gradle/Android SDK/dependency resolution available in local environment. |
| Existing Mainline JS source tests on migration branch | PASS | GitHub Actions `Validate Explode Arena Mainline` run [39957223878](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/39957223878) succeeded on code SHA `798d8e413b341312daa550f8a9aa85a60d4ddabd`; workflow tests JS/Python, **not Kotlin/libGDX**. |
| Native APK CI on public repository | BLOCKED | Existing `.github/workflows/android-debug.yml` has `if: github.event.repository.private == true` and only `push main`/manual triggers; it skips Android job on now-Public repo and does not run for PRs. Developer contract forbids autonomous workflow edits; needs separately authorized safe CI configuration change, no paid services. |
| Full Symbian mechanics, 41 native playable stages, bombs, AI, HUD, audio, HD graphics | NOT TESTED / NOT IMPLEMENTED | New native renderer only shows original stage-0 image and provisional actor movement. No false parity claims. |
| Xiaomi Pad 5 / phones, security, memory, performance, save migration | NOT TESTED | No physical device test, no native APK. |
| Independent double recheck | NOT READY | Must wait for N00-N08 and real-device evidence. |

## Next bounded engineering work
1. Verify Gradle Kotlin/libGDX dependencies and `gradle --project-dir android :core:test :app:assembleDebug` on a free authorized CI runner; fix compilation failures before marking N01 PASS.
2. Implement native stage switching and the full collision/bomb/timer port with golden deterministic replay against the JS provisional engine, separately compare with EKA2L1 Symbian evidence.
3. Enable native Android entry only when it can replace WebView safely; migrate settings and saves before removal.
4. Do not merge PR #1 or #2 automatically, publish an APK/Release or add copyrighted original assets.
