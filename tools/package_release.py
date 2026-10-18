#!/usr/bin/env python3
"""Build, verify, and package the single continuously updated mainline."""
import hashlib,subprocess,zipfile
from pathlib import Path
root=Path(__file__).resolve().parents[1]
for cmd in [
    ['node','--check','web/core.js'],['node','--check','web/world.js'],['node','--check','web/game.js'],
    ['node','tests/test_core.js'],['node','tests/test_world.js'],['node','tests/test_duel.js'],['node','tests/test_bot.js'],['node','tests/test_all_arenas.js'],
    ['python','tools/build_standalone.py'],['python','tools/sync_android.py'],
    ['python','tests/test_data.py'],['python','tests/test_android_config.py'],['python','tests/test_browser.py'],['python','tests/test_standalone.py']
]:
    subprocess.run(cmd,cwd=root,check=True)
sha=[]
for p in sorted(root.rglob('*')):
    if p.is_file() and '.git' not in p.parts and p.name!='SHA256SUMS.txt':
        sha.append(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.relative_to(root).as_posix()}')
(root/'SHA256SUMS.txt').write_text('\n'.join(sha)+'\n')
out=root.parent/'Explode_Arena_Mainline.zip'
with zipfile.ZipFile(out,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for p in sorted(root.rglob('*')):
        if p.is_file() and '.git' not in p.parts and '__pycache__' not in p.parts:
            z.write(p,'Explode_Arena_Mainline/'+p.relative_to(root).as_posix())
with zipfile.ZipFile(out) as z:
    assert z.testzip() is None
    assert 'Explode_Arena_Mainline/Explode_Arena_Offline.html' in z.namelist()
    print('Packaged',len(z.namelist()),'files at',out,'size',out.stat().st_size)
