#!/usr/bin/env python3
from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[1];src=root/'web';dst=root/'android/app/src/main/assets'
if dst.exists():shutil.rmtree(dst)
shutil.copytree(src,dst,ignore=shutil.ignore_patterns('__pycache__','.DS_Store'))
print('Synced',sum(p.is_file() for p in dst.rglob('*')),'web files to Android assets')
