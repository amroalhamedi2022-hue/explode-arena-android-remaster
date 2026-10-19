# Explode Arena — SIS NPS visual audit v32 (2026-10-19)

**Status: research-only, isolated branch.** Preserve `main`, Work Mainline, maps, and existing gameplay. This is not an implementation of new actors or rules.

## New, tested facts (not re-extraction)
- Existing v6 original data already contains **74 gameplay NPS groups (884 frames)** and **17 menu groups (130 frames)**. Total: **91 groups / 1,014 decoded frames**.
- Full original pixel-index SHA-256 comparison finds **999 distinct pixel frames** (15 repetitions among the 1,014 frames).
- Exact **cross-group** pixel overlaps: gameplay **004 ↔ 034** share 5 frames (indices 0–4); gameplay **046 ↔ 051** share 2 frames (indices 12–13). Use pixel hash de-duplication when producing later HD artwork, but do not merge group identities in the animation engine.
- Original Mainline 0.21 archive includes **91 PNG files from 6 gameplay groups**: 000(32), 025(16), 042(1), 047(14), 048(14), 051(14).
- Read-only inspection of archived `web/game.js` shows actual sprite references for groups **000, 042, 047, 048, 051** (**75 PNG frames**). Group **025** is packaged but not referenced in the game renderer. Thus **86 of the 91 group identities are not wired into the archived renderer**, of which 85 are not packaged. This is NOT a claim about uninspected later Work changes.

## High-value visible art families, NOT proven gameplay roles
- Human-shaped animation families: 000, 001, 005–018, 021, 053, 057. Some may be NPCs, heroes, or overlays; exact identity requires original runtime evidence.
- Green creature animation: **022, 023, 024**.
- UI/bonus-style icon bank **038–045**, including black bomb symbol 042, a green symbol 044, and L mark 045.
- Explosion family **046–052** (only 047,048,051 wired in archived game). Their in-engine semantics/timing remain unverified.
- Original gameplay-side UI art **054–067**: square frame, blue bars, key/cup/trophy, character portrait, waiting-for-player screens.
- Original menu NPS has **17 groups**: `menu/010` contains the visible **WAITING FOREVER** logo, `menu/001` a bomb, `menu/003–006` large character art, and `menu/014–015` animated background candidates.

## Important mismatch to investigate, NOT a confirmed bug
`web/game.js` (Mainline 0.21) draws bomb world entities using `g042_000.png`, visually a bomb symbol on an orange square. Whether the **original world bomb** used this image or a different animation (perhaps 025/026) is **not proven**. Test against EKA2L1 before changing the sprite.

## Implementation gate and next safe step
1. Read latest Work Mainline HEAD and compare before applying anything; this branch does not touch game runtime.
2. Use the v32 research catalog/report artifacts for semantic review, but treat palette-bank 0 colors as provisional. **Do not infer original palettes, tick timing, or actors from thumbnails alone.**
3. Link confirmed UI art after original Symbian screenshots/video align; add regression tests for every render reference and world-layer composition.
4. Keep original 41 maps and all 98 unpacked resources intact. Do not re-extract or overwrite core/world/stages with research copies.
5. When confidently verified, merge selectively via review, never wholesale replace Mainline.

## Authority
Source: existing research v6 manifest and index PNGs; archived Mainline 0.21 ZIP and inspected `web/game.js`; prior v19 collision and v30/v31 video handoffs. The latter Windows Mobile video remains supporting evidence only, not Symbian authority.

**All observations are static and reproducible; no Android APK/device test or HD remaster was performed here.**
