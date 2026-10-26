# N19 — Connected active-flame escape region

Date: 2026-10-26. PR #2 remains Draft; collision PR #1 remains separate.

## Engineering question
When a bot starts inside an active flame, `NativeBotEscapeRoute` currently uses `allowBurningTransit=true` as a blanket exemption for every burning BFS cell. Can it therefore route through a second, spatially disconnected active flame while escaping the first one? This can cause avoidable damage or defeat even though the planner only intended to tolerate transit through the fire already underfoot.

## Acceptance criteria
1. Compute the connected component of active flame cells whose actor-hit footprints overlap the bot's starting flame region.
2. During flame-only escape, allow burning transit only inside that connected component; all other active-fire cells remain blocked.
3. Keep normal escape behavior unchanged when no flame is underfoot, and preserve destination-refuge validation.
4. Add JUnit regressions with two simultaneous, spatially separated explosions; prove the first flame component includes its connected ray but excludes the second explosion.
5. Existing active-flame escape, timed-threat, bot-chain, and all-41-arena deterministic integration tests remain in the same core suite.
6. Run native Kotlin/JUnit and Mainline source checks on the exact source+test SHA. No APK packaging/build, artifact upload, release, merge, or main write.

## Licensed-source review
Inspected the official `libgdx/gdx-ai` repository LICENSE; it is Apache-2.0. Its general pathfinding utilities do not implement this simulation's flame-footprint connectivity or bomb deadlines, so no external code is copied and no new dependency is justified. Existing bounded BFS and NativeBombSimulation remain the implementation base.

## Baseline
- Current PR #2 head before N19: `68ffcd99b3ffd7cc56f1bbb484ab02d89e6b386a`.
- N18 source/test SHA: `336d1ffcd8b6a3bc791b0fb89f2c8b9c0ccbd4d1`; Native Kotlin/JUnit run 40246895575 PASS; Mainline run 40246895576 PASS.
- Latest PR-head checks: Native run 40247120422 PASS; Mainline run 40247120383 PASS.
- Device/Xiaomi Pad 5 and Symbian/EKA2L1 parity remain NOT TESTED.
- Google Drive LIVE sync is unavailable in this runtime; no Drive sync will be claimed without read-back verification.

## Result — N19 SOURCE ACCEPTANCE PASS (2026-10-26)
- Production fix: `NativeBotEscapeRoute.kt` now computes the active flame component containing the bot's starting position with a bounded 8-neighbor flood fill over the 16px flame-cell lattice. During an active-flame escape, only cells within that connected component may be traversed while burning; disconnected active flames remain blocked. Normal escape and unlit-refuge checks remain in force.
- Production commits: `603da2db187d553950ea9734f12c5e4558e1fc07` (component and route gate), `454505f8126dd0683ec680312ac2b1f5adc244c4` (clarified invariant).
- Regression commit / exact source+test SHA: `9ebc27a34cedab8e671923181084ba2bddb7d2fc`. Test uses two simultaneous blasts centered at x=40 and x=168, proving the first component includes its connected ray but excludes the separated second explosion, while a refuge remains reachable.
- **PASS:** actual Gradle step `gradle --project-dir android :core:test --stacktrace --no-daemon`, with `:core:compileKotlin`, `:core:compileTestKotlin`, and `:core:test` all successful in [Native Kotlin CI run 40251474221](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40251474221), on the exact source+test SHA.
- **PASS:** Mainline JS/Python source checks on the same SHA in [run 40251474309](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40251474309).
- Native workflow artifacts: [] (zero). No APK packaging/upload, no release, merge, or main write.
- **NOT TESTED:** Xiaomi Pad 5 runtime, full gameplay, and Symbian/EKA2L1 fire/AI parity. The change only corrects the provisional native route's handling of disconnected active flames.
- **Drive SYNC FAILED:** Google Drive/Docs connector tools are unavailable in this runtime; LIVE HANDOFF/INDEX were not read or written. GitHub plan and acceptance ledger are the recovery records; no Drive sync is claimed.
- PR #2 remains open Draft; collision PR #1 remains separate.

## Next
Alternate to App Commander for its next source-only functional milestone; preserve real-device and EKA2L1 verification as separate acceptance gates.
