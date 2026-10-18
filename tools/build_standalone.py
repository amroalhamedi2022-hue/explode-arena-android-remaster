#!/usr/bin/env python3
"""Packages browser playable research demo into a no-network single HTML document."""
from pathlib import Path
import base64,json
root=Path(__file__).resolve().parents[1]
web=root/'web';html=(web/'index.html').read_text()
html=html.replace('<link rel="stylesheet" href="style.css">','<style>'+(web/'style.css').read_text()+'</style>')
stages_src=(web/'assets/stages.js').read_text()
stages=json.loads(stages_src[len('window.EXPLODE_STAGES='):-2]);assert len(stages)==41
refs=set(s['image'] for s in stages)
refs.update(str(p.relative_to(web)).replace('\\','/') for p in (web/'assets/sprites').glob('*.png'))
refs.update(str(p.relative_to(web)).replace('\\','/') for p in (web/'assets/sfx').glob('*.wav'))
refs.update(str(p.relative_to(web)).replace('\\','/') for p in (web/'assets/music').glob('*.mp3'))
assets={}
for r in sorted(refs):
    ext=Path(r).suffix.lower()
    mime={'wav':'audio/wav','png':'image/png','mp3':'audio/mpeg'}[ext[1:]]
    assets[r]=f'data:{mime};base64,'+base64.b64encode((web/r).read_bytes()).decode('ascii')
# Preloaded blob references are compact stage metadata, and map images are loaded
# from the exact original rendered previews, without adding any media files.
for src in ['assets/stages.js','core.js','world.js','game.js']:
    code=(f'window.EXPLODE_STAGES='+json.dumps(stages,separators=(',',':'))+';\nwindow.EA_ASSET_OVERRIDES='+json.dumps(assets,separators=(',',':'))+';' if src=='assets/stages.js' else (web/src).read_text())
    html=html.replace('<script src="'+src+'"></script>','<script>'+code+'</script>')
assert 'src="assets' not in html and 'href="style.css"' not in html
out=root/'Explode_Arena_Offline.html';out.write_text(html,encoding='utf8')
print('Bundled standalone HTML:',out.stat().st_size,'bytes, assets',len(assets))
