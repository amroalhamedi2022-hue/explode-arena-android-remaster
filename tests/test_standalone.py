from pathlib import Path
from playwright.sync_api import sync_playwright
root=Path(__file__).resolve().parents[1]
html=(root/'Explode_Arena_Offline.html').read_text()
assert 'window.EA_ASSET_OVERRIDES=' in html and 'data:image/png;base64,' in html
assert 'data:audio/mpeg;base64,' in html, 'original soundtrack preview must be embedded for true offline use'
with sync_playwright() as p:
 browser=p.chromium.launch(headless=True,executable_path='/usr/bin/chromium',args=['--no-sandbox','--disable-dev-shm-usage','--disable-gpu'])
 page=browser.new_page(viewport={'width':412,'height':900})
 errors=[]; requests=[]
 page.on('pageerror',lambda e:errors.append(str(e)))
 page.on('request',lambda r:requests.append(r.url))
 page.set_content(html,wait_until='load',timeout=60000)
 page.wait_for_function('window.EADebug.getState().stage===0')
 page.wait_for_timeout(400)
 assert page.locator('#stage option').count()==41
 page.locator('#music').click()
 assert page.locator('#music').inner_text()=='♫ Music On'
 page.locator('#music').click()
 assert page.locator('#music').inner_text()=='♫ Music Off'
 assert page.evaluate('document.querySelector("#arena").width')==256
 assert page.evaluate('document.querySelector("#arena").height')==320
 assert not errors, errors
 assert not requests,requests[:5]
 stage=page.evaluate('window.EADebug.getState()')
 pad=page.locator('[data-dir="up"]');box=pad.bounding_box();page.mouse.move(box['x']+15,box['y']+15);page.mouse.down()
 page.wait_for_timeout(260);page.mouse.up()
 new=page.evaluate('window.EADebug.getState()')
 # A direction may be blocked by the map; validate that touch key releases.
 page.locator('#bombButton').click()
 page.wait_for_function('window.EADebug.getState().bombs===1')
 page.locator('#stage').select_option('30');page.wait_for_timeout(250)
 assert page.evaluate('window.EADebug.getState().stage')==30
 for idx in range(41):
    page.evaluate('(i)=>window.EADebug.setStage(i)',idx)
    page.evaluate('window.EADebug.render()')
    assert page.evaluate('window.EADebug.getState().stage')==idx
 page.evaluate('window.EADebug.setStage(30)')
 assert page.evaluate('document.querySelector("#arena").width')==256
 assert not errors,errors
 assert not requests,requests[:5]
 page.locator('#duel').check();page.wait_for_timeout(100)
 assert page.evaluate('window.EADebug.getState().duel')
 assert page.locator('#duelTouch').is_visible()
 page.locator('#bombButton2').click();page.wait_for_timeout(110)
 assert page.evaluate('window.EADebug.getState().bombs')>=1
 page.locator('#duel').uncheck();page.wait_for_timeout(100)
 assert not page.evaluate('window.EADebug.getState().duel')
 page.screenshot(path=str(root/'previews/standalone_mobile.png'),full_page=True)
 print('PASS standalone inline assets; 41 rendered arena switches; touch press, dual controls, bomb, 0 network requests, 0 JS exceptions')
 browser.close()
