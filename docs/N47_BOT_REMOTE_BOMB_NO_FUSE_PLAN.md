# N47 — Do not treat an owned remote bomb as a timed escape threat

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; remain Draft, no merge.

## Engineering question

When a bot owns a remote bomb with no countdown, does the AI incorrectly enter escape mode and abandon pursuit even though no timed blast can reach it? Source review found that `NativeBotController` documents that remote bombs are excluded from countdown threats, but `ownBomb` selected any owned bomb without filtering `remote`. Source inspection alone cannot prove the chosen movement because the result also depends on live collision/pass-bomb grace and bounded escape-route search; a deterministic production-controller regression is required.

## Acceptance criteria

1. An owned remote bomb by itself does not set the bot's timed-bomb escape target.
2. The bot continues a safe route toward the target in an open arena while retaining its owner-exit grace.
3. A remote bomb predicted to be ignited by a timed chain remains a real threat; existing chain-escape regressions must continue passing.
4. Remote bombs gain no fictional fuse and are not auto-detonated by this change.
5. Run `gradle --project-dir android :core:test --stacktrace --no-daemon` on the exact source/test SHA and Mainline JS/Python source checks; confirm zero artifacts.
6. No APK packaging/signing/upload, copyrighted asset additions, merge, main write or auto-merge.

## Source and license review

This is a correction to existing project-owned Kotlin AI logic. No third-party code or game assets are copied or added. The existing libGDX dependency remains a dependency, not vendored source. The AI/fuse behavior remains provisional until independently compared with Symbian/EKA2L1.

## Verification ledger

Pending source/test CI and independent PR-head recheck. Xiaomi Pad 5, original Symbian/EKA2L1 behavior and visual AI behavior remain **NOT TESTED**.