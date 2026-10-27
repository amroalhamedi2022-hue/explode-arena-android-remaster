# N42 — Freeze presentation clock after terminal outcomes

**Date:** 2026-10-27  
**PR:** #2 — Native Kotlin/libGDX; keep Draft, no merge.

## Engineering question

N41 freezes simulation state after LOST/DRAW/ADVANCING/COMPLETE, but the render loop increments `animationSeconds` before every fixed tick. On the final arena, COMPLETE can remain visible while animation/blink timing continues. Can the same terminal latch stop presentation-time advancement without stopping the WON victory-hold timer?

## Planned production change

- Expose a pure `NativeRoundFlow` decision for whether presentation animation time may advance.
- Permit animation time in PLAYING and WON only; freeze it in LOST, DRAW, ADVANCING and COMPLETE.
- Gate the libGDX animation clock with that decision; do not alter simulation tick, stage transition timing, assets, input or gameplay rules.

## Acceptance criteria

1. PLAYING and WON allow presentation time to advance; WON still reaches ADVANCING/COMPLETE after its configured hold.
2. LOST, DRAW, ADVANCING and COMPLETE do not advance presentation time.
3. Existing 41-arena terminal freeze, retry, fuse, bot, collision and stage tests remain unchanged and pass.
4. Run the native Kotlin/JUnit `:core:test` workflow and Mainline JS/Python workflow on the exact source/test SHA; recheck the docs-updated PR head. Confirm zero artifacts.
5. No APK packaging/signing/upload, release, merge, main write, auto-merge or copyrighted asset upload.

## Evidence boundary

This tests the native source clock contract only. Android rendering, player-visible behavior on Xiaomi Pad 5, and Symbian/EKA2L1 parity remain NOT TESTED.
