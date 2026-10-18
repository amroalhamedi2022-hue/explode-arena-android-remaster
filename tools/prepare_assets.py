#!/usr/bin/env python3
"""Rebuilds bundled playable reference assets using earlier verified v6/v9/v10 resources.
No AI imagery is created. Read-only inputs; writes to web/."""
import json, zipfile, shutil, io
from pathlib import Path
BASE=Path(__file__).resolve().parents[1]
ROOT=Path('/mnt/data')
out=BASE/'web'
assert (ROOT/'Explode_Arena_Research_v10/stage_object_evidence_v10.json').is_file()
stages=json.loads((ROOT/'Explode_Arena_Research_v10/stage_object_evidence_v10.json').read_text())['stages']
assert len(stages)==41
meta=[]
for s in stages:
 i=s['stage']; d=s['layer2']; world=s['world_bounds']
 src=ROOT/'Explode_Arena_Research_v9/scene_previews'/f'explodearena.lvl{i:03}.all_sections.file.candidate.png'
 assert src.is_file(),src
 shutil.copyfile(src,out/'assets/maps'/f'map_{i:03}.png')
 points=[{'x':a['x'],'y':a['y'],'type':a['type_id']} for a in s['objects']]
 meta.append({'id':i,'image':f'assets/maps/map_{i:03}.png','w':world['w'],'h':world['h'],'obstacle':{'x':d['x'],'y':d['y'],'w':d['width'],'h':d['height'],'grid':d['grid']},'objects':points,'spawns':[{'x':p['x'],'y':p['y']} for p in points if p['type']==63]})
(out/'assets/stages.js').write_text('window.EXPLODE_STAGES='+json.dumps(meta,separators=(',',':'))+';\n',encoding='utf8')
with zipfile.ZipFile(ROOT/'Explode_Arena_Research_Pack_v6_GRAPHICS_COMPLETE.zip') as z:
 for group,frames in [(0,32),(25,16),(42,1),(47,14),(48,14),(51,14)]:
  for idx in range(frames):
   name=f'Explode_Arena_v6/graphics/nps_frames/explode/group_{group:03}/frame_{idx:03}_preview_bank0.png'
   if name in z.namelist():
    (out/'assets/sprites'/f'g{group:03}_{idx:03}.png').write_bytes(z.read(name))
with zipfile.ZipFile(ROOT/'Explode_Arena_Original_98_Unpacked_Resources.zip') as z:
 for n in ['bomb_place.wav','bomb_explode1.wav','bomb_explode2.wav','bonus.wav','hero_respawn.wav']:
  (out/'assets/sfx'/n).write_bytes(z.read('Explode_Arena_Original_Resources/'+n))
print('Prepared',len(meta),'stages, 41 original map images, original NPS frame previews, original WAV audio')
