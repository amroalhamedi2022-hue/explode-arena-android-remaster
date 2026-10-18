#!/usr/bin/env python3
from pathlib import Path
import hashlib
root=Path(__file__).resolve().parents[1]
files=[p for p in root.rglob('*') if p.is_file() and p.name!='SHA256SUMS.txt' and '__pycache__' not in p.parts]
text='\n'.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+str(p.relative_to(root)) for p in sorted(files))+'\n'
(root/'SHA256SUMS.txt').write_text(text)
print('Prepared SHA256 checks for',len(files),'files')
