# Explode Arena — Remote Detonator v33 (2026-10-19)

**Scope:** one isolated feature branch; `main` is unchanged. Starts from the real private GitHub Mainline 0.21, not a rebuilt prototype.

## Implemented
- The original user's first-hand Symbian observation is encoded: power-equipped placed bombs have `remote: true` and `fuse: null`, and **never auto-detonate from the usual countdown**.
- `setRemoteDetonator(w, owner, enabled)`: explicit internal power grant for future item/pickup integration. Default remains off. The normal gameplay does not award the pickup yet.
- `detonateRemote(w, owner)`: detonation via one-owner, one-remote-bomb selection. With more than one remote bomb, safely returns false until original Symbian targeting policy is established.
- Original Symbian keypad **5** plants bomb, **8** requests remote detonation; existing Space/WASD and optional duel remain available. A touch detonate button is hidden until power is granted.
- Existing ordinary bombs still use normal fuse, blast, hit and chain systems (which remain provisional pending original parity).
- Android WebView source assets for all changed web files were updated identically.

## Verified locally (not Android device)
- `node tests/test_world.js` including source-confirmed remote regressions: PASS.
- `node tests/test_core.js`, `test_duel.js`, `test_bot.js`, `test_all_arenas.js`: PASS.
- `python tests/test_data.py`, `test_android_config.py`: PASS.
- `python tests/test_browser.py` desktop + mobile viewport incl. keypad 5/8 and touch remote button: PASS.
- `node --check web/game.js` and `web/world.js`: PASS.
- `python tools/build_standalone.py`: PASS locally; updated `Explode_Arena_Offline.html` is packaged in local handoff, **but the large generated standalone file was not committed to this branch**. Regenerate with this command after checking out feature branch.

## Not done / don't infer
- Remote pickup object type and map spawn mapping, icon, sound, multi-bomb targeting, interaction with external explosions, status persistence after death/levels: unverified original behavior.
- Original stone regeneration timings and collision occupancy: not implemented.
- End-to-end APK and on-device Android test for v33: not done.
- Browser and Android tests demonstrate no obvious regressions, **not** Symbian-perfect equivalence.

## Handoff
- Compare this branch to latest Work mainline before selective merge.
- Do not overwrite Work changes; merge or cherry-pick by review after CI and a fresh Android build.
- Do not connect power to an arbitrary letter pickup until a direct Symbian check or stronger source evidence confirms item-to-effect relation.
