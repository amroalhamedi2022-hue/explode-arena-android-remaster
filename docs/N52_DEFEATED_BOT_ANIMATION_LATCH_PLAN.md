# N52 — Stop defeated bots on the same damage sample

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft/open, no merge.

## Engineering question

When a bot loses its final health point during the post-simulation damage sample—including a flame expiring at the fixed-tick boundary—does its `walking` animation flag remain true until the next tick? The renderer can observe that stale state during the victory hold even though the bot has already been defeated.

## Implementation

- Route both inline damage (`advance(..., applyDamage = true)`) and canonical post-tick damage (`applyDamageForLastTick`) through one `tickDamage` helper.
- Keep the existing health, invulnerability and flame-occupancy timing unchanged.
- Clear `BotSlot.walking` immediately when that same damage sample makes `NativeVitality.defeated` true.
- Do not change movement, damage amount, round-flow decisions, actor blocking, or assets.

## Acceptance criteria

1. A flame that expires on the damage-sample boundary still defeats a 1-HP bot as before.
2. The same sample clears its walking flag; no next-tick delay is required.
3. Non-defeated bots retain existing movement/animation behavior.
4. Native Kotlin/JUnit and Mainline JS/Python checks pass on the exact source/test SHA and the docs-updated PR head.
5. No APK packaging/signing/upload, new assets/dependencies, merge, main write, or auto-merge.

## Verification ledger

- **Source + regression-test SHA:** `8fc53afe1ffd4c0c87945a55a2cca2f6305bc3d7`.
- **PASS — Native Kotlin/JUnit:** [CI #40555838259](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40555838259), `:core:test`; no APK packaging.
- **PASS — Mainline JS/Python:** [CI #40555838370](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40555838370).
- **PASS — documentation-updated PR head before this evidence commit:** `ef3df9c83b39ab012f81baf15255496cb093c04a`; Native [CI #40556342390](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40556342390) and Mainline [CI #40556342373](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40556342373).
- Device / Android rendering / Symbian EKA2L1 parity: **NOT TESTED**.
- APK artifacts: **0 expected**.
- Google Drive LIVE sync: **SYNC FAILED** — no Drive/Docs connector is exposed in this runtime; no LIVE read/write or folder metadata verification is claimed.
