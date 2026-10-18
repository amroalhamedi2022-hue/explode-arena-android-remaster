from pathlib import Path
from playwright.sync_api import sync_playwright
from base64 import b64encode
import json,re
root=Path(__file__).resolve().parents[1]
web=root/'web'
html=(web/'index.html').read_text()
html=html.replace('<link rel="stylesheet" href="style.css">','<style>'+(web/'style.css').read_text()+'</style>')
s=(web/'assets/stages.js').read_text()
stages=json.loads(s[len('window.EXPLODE_STAGES='):-2])
# The browser sandbox disallows local/file navigation and localhost. Use the same
# production scripts in a set_content harness, embedding only referenced art.
for i in (0,18):
    p=web/stages[i]['image']
    stages[i]['image']='data:image/png;base64,'+b64encode(p.read_bytes()).decode()
js_data='window.EXPLODE_STAGES='+json.dumps(stages,separators=(',',':'))+';\n'
refs=['assets/sprites/g000_'+str(i).zfill(3)+'.png' for i in range(32)]+['assets/sprites/g042_000.png']+['assets/sprites/g'+g+'_'+str(i).zfill(3)+'.png' for g in ('047','048','051') for i in range(14)]
assets={p:'data:image/png;base64,'+b64encode((web/p).read_bytes()).decode() for p in refs}
assets['assets/music/explode_arena_original_mod_preview.mp3']='data:audio/mpeg;base64,'+b64encode((web/'assets/music/explode_arena_original_mod_preview.mp3').read_bytes()).decode()
js_data+='window.EA_ASSET_OVERRIDES='+json.dumps(assets)+';\n'
for path,code in [('assets/stages.js',js_data),('core.js',(web/'core.js').read_text()),('world.js',(web/'world.js').read_text()),('game.js',(web/'game.js').read_text())]:
    html=html.replace('<script src="'+path+'"></script>','<script>'+code+'</script>')
with sync_playwright() as p:
 browser=p.chromium.launch(headless=True,executable_path='/usr/bin/chromium',args=['--no-sandbox','--disable-dev-shm-usage','--disable-gpu'])
 for kind,w,h in [('desktop',1130,900),('mobile',390,844)]:
    page=browser.new_page(viewport={'width':w,'height':h})
    errors=[]
    page.on('pageerror',lambda err:errors.append(str(err)))
    page.set_content(html,wait_until='load')
    page.wait_for_timeout(450)
    assert page.locator('#stage option').count()==41
    page.locator('#music').click()
    assert page.locator('#music').inner_text()=='♫ Music On'
    page.locator('#sound').click()
    assert page.locator('#sound').inner_text()=='♫ Muted'
    page.locator('#sound').click()
    page.locator('#music').click()
    assert page.locator('#music').inner_text()=='♫ Music Off'
    start=page.evaluate('window.EADebug.getState()');assert start['stage']==0,start
    page.keyboard.down('ArrowRight');page.wait_for_timeout(350);page.keyboard.up('ArrowRight')
    moved=page.evaluate('window.EADebug.getState()')
    assert moved['x']>start['x'] or moved['y']!=start['y'],(start,moved)
    page.keyboard.press('Space');page.wait_for_timeout(100)
    assert page.evaluate('window.EADebug.getState().bombs')==1
    page.locator('#stage').select_option('18');page.wait_for_timeout(200)
    assert page.evaluate('window.EADebug.getState().stage')==18
    page.locator('#pause').click();assert page.evaluate('window.EADebug.getState().paused')
    page.locator('#pause').click();assert not page.evaluate('window.EADebug.getState().paused')
    page.locator('#bot').check();page.wait_for_timeout(200);assert page.locator('#bot').is_checked()
    page.locator('#duel').check();page.wait_for_timeout(100)
    assert page.evaluate('window.EADebug.getState().duel')
    assert not page.locator('#bot').is_checked(),'2P mode disables demo bot'
    assert page.locator('#p2Hud').is_visible()
    p2_start=page.evaluate('window.EADebug.getState()')
    # Compare immediately after a single direction; four opposing inputs cancel.
    page.keyboard.down('KeyD');page.wait_for_timeout(180);page.keyboard.up('KeyD')
    p2_end=page.evaluate('window.EADebug.getState()')
    assert (p2_end['x2'],p2_end['y2']) != (p2_start['x2'],p2_start['y2']), (p2_start,p2_end)
    page.keyboard.press('KeyF');page.wait_for_timeout(80)
    assert page.evaluate('window.EADebug.getState().bombs')>=1,'player 2 plants a bomb with F'
    # UI touch bomb for player 2 (same event path as mobile pointer input).
    page.locator('#restart').click();page.wait_for_timeout(70)
    assert page.locator('#duelTouch').is_visible()
    page.locator('#bombButton2').click();page.wait_for_timeout(100)
    assert page.evaluate('window.EADebug.getState().bombs')>=1,'player 2 touch button plants bomb'
    page.screenshot(path=str(root/'previews'/f'mainline_local_duel_{kind}.png'),full_page=True)
    page.locator('#duel').uncheck();page.wait_for_timeout(100)
    assert not page.evaluate('window.EADebug.getState().duel'),'leaving 2P restores single-player'
    page.screenshot(path=str(root/'previews'/f'mainline_{kind}.png'),full_page=True)
    assert not errors,errors
    assert page.evaluate('document.documentElement.scrollWidth <= window.innerWidth+2'),'horizontal overflow'
    print('PASS',kind,'key movement, bomb, arena switch, pause, bot, no JS errors, no overflow')
    page.close()
 browser.close()
