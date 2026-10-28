# Explode Arena — Native Kotlin + libGDX Migration Plan
**Approved user direction:** 2026-10-23. **Status:** N00 STRUCTURAL AUDIT PARTIAL; N01 KOTLIN/LIBGDX SOURCE ADDED; N03 PROVISIONAL NATIVE BOMBS/CHAIN ADDED; NATIVE CI BUILD VERIFIED; DEVICE AND SYMBIAN PARITY NOT VERIFIED (2026-10-23).
**Target:** Android first (Xiaomi Pad 5 + Android phones), same single **Explode Arena Mainline**, not a separate game or throwaway prototype.
**Repository:** `amroalhamedi2022-hue/explode-arena-android-remaster`; public as observed 2026-10-23, user changed visibility.

## Decision: a real engine conversion, not a WebView wrapper
- **Game/simulation code:** Kotlin on JVM/Android, with fixed-rate deterministic simulation. **Rendering/input/audio:** libGDX, native Android backend.
- **Final Android gameplay must not depend on HTML, JavaScript or WebView.** Existing `web/` and the current `android/` WebView app remain read-only behavioral references / transitional comparison fixtures until parity is proven.
- Preserve the existing **package/application identity** `com.explodearena.remaster` where practical and preserve saves via an explicit compatibility migration (DOM storage is not directly accessible from native Kotlin).
- Do not independently rewrite game design. Preserve the exact intended Symbian behavior and HD remaster goals. Treat current JS 60Hz rules as **provisional baselines**, not proof of original bomb timing, blast/collision, AI, HUD or game modes.
- libGDX is an Apache-2.0-licensed framework (not a license grant for original game art/audio). Official Kotlin guide and project generator: https://libgdx.com/wiki/jvm-langs/using-libgdx-with-kotlin and https://github.com/libgdx/gdx-liftoff. Start from the official generator's Kotlin configuration rather than an unrelated unmaintained clone; pin versions only after compatibility/build verification.

## Grounded repository audit (2026-10-23)
- `android/app/build.gradle` currently only applies `com.android.application`, has AndroidX WebKit and does not apply Kotlin or depend on libGDX.
- `web/world.js` contains the 60Hz seeded engine and **provisional** physics; the Android app displays synced `web/` assets via WebView.
- There are 41 original stage previews; 42 NPS blast sprite frames; 15 WAV effects and original MOD, from existing asset research, with HD layering not yet finished.
- The isolated `feature/symbian-remote-v33` branch contains evidence-backed keypad-5 bomb/8 remote detonation research; keep independent and selectively port **verified** behavior, never auto-merge it.
- PR #1 on `automation/blast-hitbox-safety-20261023` corrects a provisional JS blast hitbox; review it separately and use it only as a reference for the port. None of the migration steps below is marked complete merely because WebView APK builds or JS tests pass.
- Existing `README_AR.md` and `BUILD_STATUS.md` contain historical claims from when the repository was private. Public visibility was changed by the user; historical CI evidence cannot be used as current native-engine acceptance.

## Phases and mandatory evidence
| ID | Concrete work (same Mainline) | Required test/evidence | Status |
|---|---|---|---|
| N00 | Snapshot existing JS/WebView Mainline, original SIS/EKA2L1 observations, 41 arenas and SHA-256 asset inventory; identify known **provisional** mechanics and user-approved visuals | Reproducible baseline and itemized parity manifest; keep separate Symbian vs Windows Mobile evidence | IN PROGRESS: 41-stage structural inventory checked; SHA-256/EKA2L1 not tested |
| N01 | Add official-style Gradle `core` Kotlin libGDX simulation module and Android libGDX launcher within this repo, preserving app ID. Use a simple native boot/screen, no WebView as gameplay backend | `core` Kotlin unit tests and native Android debug APK built from exact SHA | SOURCE ADDED; Gradle :core:test and :app:assembleDebug PASS on SHA 5ce6814d; device startup NOT TESTED |
| N02 | Port all 41 arena formats/data, object/spawn/wall mappings, deterministic seeded RNG and fixed 60Hz tick/step accounting to Kotlin **without changing values silently** | 41-stage schema/positions/invariants + fixed-seed replay and data SHA comparison to checked-in baseline | NOT STARTED |
| N03 | Port movement, tile/pixel collisions, bomb placement ownership/counting/fuses, blast spread/chain reactions, invulnerability and damage; port documented verified keypad remote behavior if supported | Property/negative tests and JS-versus-Kotlin golden replay **for existing provisional behavior**; separate direct Symbian EKA2L1 matching tests | IN PROGRESS: provisional native bombs, fuses, chain, walls and actor bomb-solid/exit-only collision implemented; Kotlin JUnit and Android compile PASS; actor damage and Symbian parity NOT TESTED |
| N04 | Port native SpriteBatch rendering (player frames, 41 stages, NPS explosions), depth/layers, camera/scaling and touch/gamepad/key input; original audio with libGDX asset lifecycle | Golden-frame / asset presence checks; portrait/landscape controls and performance on supported Android | NOT STARTED |
| N05 | Recreate original HUD, menus, options, game modes and AI based on **actual Symbian evidence**; current local duel/practice bot are explicitly non-original until independently proven | Replay and mode-by-mode EKA2L1 comparisons and regression scenarios with evidence tags | NOT STARTED |
| N06 | HD asset-by-asset faithful recreation preserving original geometry/poses/colors/animation counts; original game audio/HUD parity | Asset atlas audit and side-by-side screenshot comparison against approved original at deterministic captures | NOT STARTED |
| N07 | Replace WebView runtime fully, migrate compatible settings/saves and prevent data loss; remove AndroidX WebKit only after complete parity | Clean install, upgrade, offline play, pause/background/resume, input/audio, asset integrity, no WebView gameplay classes used | NOT STARTED |
| N08 | Full free CI and device acceptance (no paid API), publish only a reviewable PR and internal test artifact; no public release without approval | Current-SHA APK signature, Android device and game test, stable performance, memory, sound, crash resistance, complete acceptance matrix | NOT STARTED |
| N09 | Independent **double recheck**: re-execute requirements/tests after all above show PASS and compare real Android capture to Symbian reference | Second-pass audit marks VERIFIED only if all mandatory functionality, visual fidelity and real device steps are evidenced | NOT STARTED |

## Source boundary and architecture target
```text
android/                     # native Android launcher and lifecycle (existing app identity)
core/                        # Kotlin platform-neutral libGDX game code and deterministic simulation
  model/                     # typed Stage, Tile, Bomb, Actor, Blast and GameState
  simulation/                # fixed 1/60s tick, RNG, collisions, inputs, events and rules
  screens/                   # libGDX game/menu/pause/options
  assets/                    # loaders/atlases/audio and documented SHA inventory
  input/                     # touch + controller/key mapping
tests/                       # JS historical compatibility tests + new native contract tests
web/                         # TRANSITIONAL REFERENCE, no shipping WebView gameplay dependency
research/                    # EKA2L1/SIS authority, keep separate from Windows Mobile evidence
docs/                        # checkpoints, acceptance evidence, migration reports
```
Android-only release goal; optional headless/desktop libGDX test harness is allowed **for CI only**, not a Windows application product. Do not rename repo, package ID, or discard current source/asset evidence.

## Autonomous development directions
1. **Prioritize N00 then N01 before adding more HTML/JS-only gameplay features.** JS bug fixes that protect source-data fidelity can still be accepted on their own merits, but new game functionality should be Kotlin/libGDX native.
2. One project branch or reviewable PR per migration slice. Keep original JS + native side by side during migration; measure parity before removing WebView files or workflows.
3. Use maintained Apache-2.0 libGDX and appropriate Kotlin Gradle support, verify actual licenses and dependencies; no paid AI API or paid runner.
4. Do not confuse rendering parity with mechanics parity. Never claim original rules without Symbian/EKA2L1 authority. All unsupported assumptions must be labeled PROVISIONAL.
5. Track every user requirement in `docs/AUTONOMOUS_ACCEPTANCE.md` with PASS / FAIL / NOT TESTED / BLOCKED, tested SHA and actual command/run link. A successful JS test or old WebView APK is **not** native completion.
6. No public **release/artifact promotion** of original copyrighted resources without separately checking rights. The user has already changed repository visibility to public; report that original assets in public source may be accessible and may carry copyright restrictions. Never silently change visibility.
7. No auto-merge, no force push, no replacing Mainline with an unrelated project. Final independent double recheck must include physical-device verification.

## First implementation checkpoint (2026-10-23)
The first native source slice now exists on this PR branch: `android/core` (Kotlin simulation, 41-stage JSON data parser, fixed 60Hz clock, deterministic LCG, JUnit tests), and `NativeGameActivity`/libGDX Android dependencies. The internal native activity renders the original map texture and a provisional actor. It is intentionally **not** the launcher yet, to avoid destroying the existing playable WebView path before native parity. No original game mode is claimed restored by this slice. The new public-PR native validation workflow runs real Kotlin JUnit :core:test and Android :app:assembleDebug without uploading any APK or original game assets (PASS on 5ce6814d, run 39973107989). The old private-only artifact workflow remains guarded; device launch and original Symbian parity are still unverified. See `docs/AUTONOMOUS_ACCEPTANCE.md` for evidence and blockers.
