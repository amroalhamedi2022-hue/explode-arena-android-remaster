from pathlib import Path
from xml.etree import ElementTree as ET
r=Path(__file__).resolve().parents[1]
src=r/'web';dst=r/'android/app/src/main/assets'
sources={p.relative_to(src):p for p in src.rglob('*') if p.is_file()}
android={p.relative_to(dst):p for p in dst.rglob('*') if p.is_file()}
assert sources.keys()==android.keys(),'Android must package every game resource (and no stale ones)'
for relative,p in sources.items():
    assert p.read_bytes()==android[relative].read_bytes(),f'Android source out of sync: {relative}'
manifest=ET.parse(r/'android/app/src/main/AndroidManifest.xml').getroot()
ns='{http://schemas.android.com/apk/res/android}'
assert not any(p.attrib.get(ns+'name')=='android.permission.INTERNET' for p in manifest.findall('uses-permission'))
activity=(r/'android/app/src/main/java/com/explodearena/remaster/MainActivity.java').read_text()
for required in ('WebViewAssetLoader','appassets.androidplatform.net','shouldInterceptRequest','shouldOverrideUrlLoading','Asset not found'):
    assert required in activity
assert (r/'.github/workflows/android-debug.yml').is_file()
print('PASS Android packaging integrity:',len(sources),'source resources exactly synchronized; no INTERNET permission; origin restricted (source inspection only, APK not compiled)')
