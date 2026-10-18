import json,hashlib,re
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
s=(root/'web/assets/stages.js').read_text()
stages=json.loads(s[len('window.EXPLODE_STAGES='):-2]);assert len(stages)==41
assert [a['id'] for a in stages]==list(range(41))
assert sum(len(a['objects']) for a in stages)==464
assert sum(len(a['spawns']) for a in stages)==106
for stage in stages:
    p=root/'web'/stage['image'];assert p.is_file(),p
    with Image.open(p) as im:assert im.size==(stage['w'],stage['h']),(im.size,stage['id'])
    layer=stage['obstacle'];assert len(layer['grid'])==layer['h']
    assert all(len(row)==layer['w'] for row in layer['grid'])
    for point in stage['spawns']:assert 0<=point['x']<stage['w'] and 0<=point['y']<stage['h']
assert (root/'android/app/src/main/assets/index.html').read_bytes()==(root/'web/index.html').read_bytes()
print('PASS 41 authentic map previews, 464 records, 106 candidate markers, dimensions, Android web sync')

from pathlib import Path
audio=root/'web/assets/sfx'
assert len(list(audio.glob('*.wav')))==15, 'expected all original WAV audio assets'
assert all(p.read_bytes().startswith(b'RIFF') and p.read_bytes()[8:12]==b'WAVE' for p in audio.glob('*.wav'))
assert (root/'web/assets/music/soundtrack.mod').read_bytes()[1080:1084]==b'6CHN'
assert (root/'web/assets/music/explode_arena_original_mod_preview.mp3').stat().st_size>1000000
print('PASS 15 RIFF WAVE effects, original soundtrack module and local soundtrack preview')
