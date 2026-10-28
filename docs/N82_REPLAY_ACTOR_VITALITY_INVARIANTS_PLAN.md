# N82 — Replay actor vitality invariants

Date: 2026-10-28  
PR: #2 Native Kotlin/libGDX; keep Draft/open.

## Engineering question

Can the strict replay comparator accept two equally malformed traces whose actor fields cannot be produced by the current native vitality model?

## Why source inspection alone is insufficient

The failure occurs at the serialized trace boundary, not during normal gameplay. Adversarial trace fixtures must mutate health, invulnerability and defeat fields independently, then verify that validation rejects them before byte-for-byte equality can be reported.

## Production change

- Restrict serialized actor health to the native model's supported range, 0..99 (zero is reserved for defeated actors).
- Bound invulnerability seconds to the constructor/model range, 0..10.
- Require `defeated == (health == 0)`, matching `NativeVitality.defeated`.
- Keep the replay schema and gameplay rules unchanged; this validates trace integrity and does not establish Symbian parity.

## Acceptance criteria

1. A valid native actor frame remains accepted.
2. Health above 99, invulnerability above 10, zero health with `defeated=false`, and positive health with `defeated=true` are rejected.
3. Invalid expected traces return an `expected-player` divergence at frame zero even when compared with a valid trace.
4. Existing frozen-terminal, retry-baseline, 41-stage and 1/2/4-spawn replay tests remain green.
5. Native Kotlin/JUnit and Mainline JS/Python checks pass on the exact source/test commit.
6. No APK build/package/upload, new dependencies, external assets, merge, or main write.

## Verification — 2026-10-28

- Source/test commit: `c64ba5e4a9604e89c25f215a41f5168aa4f1ce42`.
- Production blob: `3f17d8671c788ef182848c3faaad01627b67f118`.
- Test blob: `e52e11762836f2b3c4b27b310e9541b49cc1c0a8`.
- **PASS:** Native Kotlin/JUnit [CI #40749023210](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40749023210); log reports `BUILD SUCCESSFUL in 55s`.
- **PASS:** Mainline JS/Python [CI #40749023219](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40749023219).
- Native workflow artifacts: 0. No APK build/package/upload.
- Android device gameplay and Symbian/EKA2L1 parity: **NOT TESTED**.
- PR #2 remains Draft/open; collision PR #1 remains separate. No merge, main write or unlicensed assets.
- Google Drive LIVE sync: **SYNC FAILED** — this runtime exposes no Google Drive/Docs connector; no Drive read/write/read-back is claimed.
