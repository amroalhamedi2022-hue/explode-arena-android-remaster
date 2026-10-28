# N40 — Retry-only input isolation for terminal rounds

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft, no merge.

## Engineering question

Can a LOST/DRAW round be escaped by stage-navigation input, especially when navigation, remote detonation and retry are pressed in the same render frame? Source inspection found navigation was arbitrated before the terminal-round gate, violating the retry-only contract.

## Acceptance criteria

1. For both LOST and DRAW, navigation-only, remote-only and idle frames return an empty command.
2. An explicit retry wins over simultaneous navigation and remote input, returning only `Decision(retry = true)`.
3. Terminal navigation attempts do not advance the fixed-step clock, age a live fuse, or change the terminal result.
4. Normal in-round navigation priority and WON/victory-hold progression remain unchanged.
5. Run native Kotlin/JUnit `:core:test` and Mainline JS/Python source checks on the exact source/test SHA, then recheck the docs-updated PR head. Zero APK artifacts.
6. No `main` write, merge, release, original asset upload or auto-merge.

## Scope

This is native input arbitration and timer safety, not a claim about original Symbian menu/keypad behavior. Movement, bomb rules, AI, collision, fuse duration, damage and stage data are unchanged. Xiaomi Pad 5 and EKA2L1 parity remain **NOT TESTED**.


## Validation results

- Exact source/test SHA: `9cca4668966f18d1a300ffb336df4d56fb93f6db`.
- **PASS:** Native Kotlin/JUnit [run #40465366401](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40465366401), `:core:test`, BUILD SUCCESSFUL in 58s.
- **PASS:** Mainline JS/Python [run #40465366403](https://github.com/amroalhamedi2022-hue/explode-arena-android-remaster/actions/runs/40465366403).
- Artifacts: 0 confirmed; no APK packaging. Device and EKA2L1 remain NOT TESTED.
- The docs-updated PR-head recheck is pending and will be recorded in the PR discussion.
