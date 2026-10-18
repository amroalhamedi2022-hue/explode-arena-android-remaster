/* UI and original-art renderer for the continuously developed Explode Arena mainline.
 * Simulated rules remain experimental until cross-checked with Symbian gameplay. */
(function(){'use strict';
const STAGES=window.EXPLODE_STAGES,Core=window.EACore,World=window.EAWorld;
const $=id=>document.getElementById(id),canvas=$('arena'),ctx=canvas.getContext('2d',{alpha:false});
ctx.imageSmoothingEnabled=false;
const select=$('stage');for(const stage of STAGES){const option=document.createElement('option');option.value=stage.id;option.textContent=`Arena ${String(stage.id).padStart(2,'0')} · ${stage.w} × ${stage.h}`;select.appendChild(option)}
const cache=new Map(),keys=new Set(),touch=new Set(),touch2=new Set();let muted=false,musicEnabled=false,musicTrack=null,w=null,accumulator=0,lastTs=0,visualTime=0,pendingBomb=new Set(),seq=0,lastGamepadBomb=[false,false];
const PREFS_KEY='ea.mainline.prefs.v1';
function readPrefs(){try{return JSON.parse(localStorage.getItem(PREFS_KEY)||'{}')}catch(_){return {}}}
function savePrefs(){try{localStorage.setItem(PREFS_KEY,JSON.stringify({stage:w?.stage?.id??0,muted,musicEnabled,bot:$('bot').checked,duel:$('duel').checked}))}catch(_){}}
const prefs=readPrefs();muted=!!prefs.muted;musicEnabled=!!prefs.musicEnabled; $('sound').textContent=muted?'♫ Muted':'♫ Sound';$('music').textContent=musicEnabled?'♫ Music On':'♫ Music Off';if(prefs.duel===true)$('duel').checked=true;else if(prefs.bot===true)$('bot').checked=true;
const FILES={player:Array.from({length:32},(_,i)=>`assets/sprites/g000_${String(i).padStart(3,'0')}.png`),bomb:'assets/sprites/g042_000.png',explosionCenter:Array.from({length:14},(_,i)=>`assets/sprites/g047_${String(i).padStart(3,'0')}.png`),explosionVertical:Array.from({length:14},(_,i)=>`assets/sprites/g048_${String(i).padStart(3,'0')}.png`),explosionHorizontal:Array.from({length:14},(_,i)=>`assets/sprites/g051_${String(i).padStart(3,'0')}.png`)};
function resource(src){if(cache.has(src))return cache.get(src);const img=new Image();img.src=(window.EA_ASSET_OVERRIDES&&window.EA_ASSET_OVERRIDES[src])||src;cache.set(src,img);return img}
function getImage(src){const img=resource(src);return img.complete&&img.naturalWidth?img:null}
for(const file of [...FILES.player,FILES.bomb,...FILES.explosionCenter,...FILES.explosionVertical,...FILES.explosionHorizontal])resource(file);
function audio(name){if(muted)return;try{const path=`assets/sfx/${name}.wav`;const snd=new Audio((window.EA_ASSET_OVERRIDES&&window.EA_ASSET_OVERRIDES[path])||path);snd.volume=.4;snd.play().catch(()=>{});}catch(_){/* Audio may be unavailable until user interaction. */}}
// The bundled track is an audio preview rendered from the extracted original
// FastTracker MOD. Playback is opt-in; a user gesture is required on mobile.
const MUSIC_PATH='assets/music/explode_arena_original_mod_preview.mp3';
function musicPlayer(){
 if(!musicTrack){musicTrack=new Audio((window.EA_ASSET_OVERRIDES&&window.EA_ASSET_OVERRIDES[MUSIC_PATH])||MUSIC_PATH);
  musicTrack.loop=true;musicTrack.preload='none';musicTrack.volume=.30;}
 return musicTrack;
}
function syncMusic(userGesture=false){
 if(!musicEnabled||muted||document.hidden||w?.status!=='playing'){
  if(musicTrack&&!musicTrack.paused)musicTrack.pause();return;
 }
 // Only a user-initiated enable/resume attempts playback. Browsers may block
 // automatic starts, in which case the Music button will explicitly retry.
 if(userGesture)musicPlayer().play().catch(()=>{});
}
function showCover(title,subtitle){const c=$('pausedCover');c.classList.remove('hidden');c.querySelector('div').textContent=title;c.querySelector('small').textContent=subtitle}
function hideCover(){$('pausedCover').classList.add('hidden')}
function stageTitle(){return `Arena ${String(w.stage.id).padStart(2,'0')} / 40`}
function setup(id){const s=STAGES[Number(id)];if(!s)return;
 try{w=World.create(s,{bot:$('bot').checked,duel:$('duel').checked,seed:((s.id+1)*2654435761)>>>0})}catch(err){showCover('ARENA ERROR',err.message);throw err}
 accumulator=0;pendingBomb.clear();visualTime=0;keys.clear();touch.clear();touch2.clear();lastGamepadBomb=[false,false];seq++;
 canvas.width=s.w;canvas.height=s.h;ctx.imageSmoothingEnabled=false;resource(s.image);$('stageName').textContent=stageTitle();select.value=String(s.id);$('hint').textContent=w.duel?'Local two-player practice · Not original Bluetooth':'Original assets · Rules under verification';$('controlsLegend').innerHTML=w.duel?'P1: ARROWS + SPACE<br/>P2: WASD + F<br/>2 GAMEPADS SUPPORTED':'ARROWS / WASD · MOVE<br/>SPACE · BOMB<br/>GAMEPAD SUPPORTED';hideCover();syncHud();syncPause();syncMusic(false);savePrefs();
}
function syncHud(){if(!w)return;$('health').textContent=w.health?'♥ '.repeat(w.health).trim():'—';$('p2Hud').classList.toggle('hidden',!w.duel);$('duelTouch').classList.toggle('hidden',!w.duel);$('health2').textContent=w.health2?'♥ '.repeat(w.health2).trim():'—';$('bombs').textContent=String(Math.max(0,w.rules.bombLimit-w.bombs.filter(b=>b.owner==='player').length));$('score').textContent=String(w.score);const seconds=Math.floor(w.elapsed);$('timer').textContent=`${String(Math.floor(seconds/60)).padStart(2,'0')}:${String(seconds%60).padStart(2,'0')}`;
 const stat=$('matchStatus');if(stat)stat.textContent=w.status==='draw'?'DRAW':w.duel&&w.winner?'WINNER '+w.winner:w.status==='defeat'?'ROUND LOST':w.status==='victory'?'ROUND WON':w.status==='paused'?'PAUSED':w.duel?'LOCAL DUEL':'PLAYING';}
function syncPause(){$('pause').textContent=w?.status==='paused'?'▶ Resume':'Ⅱ Pause';}
function pause(v=true){if(!w||w.status==='defeat'||w.status==='victory'||w.status==='draw')return;
 w.status=v?'paused':'playing';if(v){showCover('PAUSED','Press Resume to continue');pendingBomb.clear();keys.clear();touch.clear();touch2.clear();document.querySelectorAll('.pad.is-down').forEach(el=>el.classList.remove('is-down'))}else{hideCover();accumulator=0;lastTs=performance.now()}
 syncPause();syncHud();syncMusic(!v);}
function gamepadInput(){
 let pads=[];try{pads=Array.from(navigator.getGamepads?.()||[]).filter(p=>p&&p.connected)}catch(_){}
 const read=(pad,index)=>{
   if(!pad){lastGamepadBomb[index]=false;return {up:false,down:false,left:false,right:false}}
   const button=i=>Boolean(pad.buttons?.[i]?.pressed),axis=i=>Number(pad.axes?.[i]||0);
   const bomb=button(0)||button(1);
   if(bomb&&!lastGamepadBomb[index])requestBomb(index===1?'player2':'player');
   lastGamepadBomb[index]=bomb;
   return {up:axis(1)<-.35||button(12),down:axis(1)>.35||button(13),left:axis(0)<-.35||button(14),right:axis(0)>.35||button(15)};
 };
 return {p1:read(pads[0],0),p2:read(w?.duel?pads[1]:null,1)};
}
function input(){
 const pads=gamepadInput(),duel=Boolean(w?.duel);
 return {up:keys.has('ArrowUp')||(!duel&&keys.has('KeyW'))||touch.has('up')||pads.p1.up,
 down:keys.has('ArrowDown')||(!duel&&keys.has('KeyS'))||touch.has('down')||pads.p1.down,
 left:keys.has('ArrowLeft')||(!duel&&keys.has('KeyA'))||touch.has('left')||pads.p1.left,
 right:keys.has('ArrowRight')||(!duel&&keys.has('KeyD'))||touch.has('right')||pads.p1.right,
 bomb:pendingBomb.has('player'),
 p2:duel?{up:keys.has('KeyW')||touch2.has('up')||pads.p2.up,down:keys.has('KeyS')||touch2.has('down')||pads.p2.down,
 left:keys.has('KeyA')||touch2.has('left')||pads.p2.left,right:keys.has('KeyD')||touch2.has('right')||pads.p2.right,bomb:pendingBomb.has('player2')}:undefined};
}
function requestBomb(owner='player'){if(!w||w.status!=='playing')return;pendingBomb.add(owner)}
function advance(dt){if(!w||w.status!=='playing'){pendingBomb.clear();return}
 const controls=input();World.step(w,controls,dt);pendingBomb.clear();
 for(const e of w.events){if(e.type==='place')audio('bomb_place');if(e.type==='explode')audio('bomb_explode1');if(e.type==='playerHit')audio('hero_respawn');if(e.type==='enemyHit')audio('bonus');}
 syncMusic(false);
 if(w.status==='draw')showCover('DRAW','Both players eliminated · Restart to play');
 if(w.status==='defeat')showCover(w.duel?'PLAYER 2 WINS':'ROUND OVER','Press Restart to play again');
 if(w.status==='victory')showCover(w.duel?'PLAYER 1 WINS':'ROUND WON',w.duel?'Restart to play again':'Experimental opponent defeated · Restart to play');
 syncHud();}
function drawCharacter(actor,isPlayer){// g000 frames: right 00-07, front 08-15, left 16-23, back 24-31.
 const directionBases=[24,16,8,0];
 let frame=directionBases[actor.dir??2]??8;
 if(actor.moving)frame+=Math.floor(visualTime*12)%8;
 const sprite=getImage(FILES.player[frame])||getImage(FILES.player[0]);
 if(isPlayer&&actor.invincible>0&&Math.floor(visualTime*13)%2===0)return;
 if(sprite)ctx.drawImage(sprite,Math.round(actor.x-sprite.width/2),Math.round(actor.y-sprite.height+7));
 else{ctx.fillStyle=isPlayer?'#d8eaf2':'#fb956a';ctx.fillRect(actor.x-5,actor.y-12,10,14)}
 if(!isPlayer){ctx.fillStyle='#ff9c79';ctx.fillRect(actor.x-6,actor.y+5,12,2);if(w.duel){ctx.fillStyle='#ffcf7b';ctx.font='bold 9px monospace';ctx.textAlign='center';ctx.fillText('P2',actor.x,actor.y-21);ctx.textAlign='start'}}
}
function render(){if(!w)return;const s=w.stage;ctx.clearRect(0,0,s.w,s.h);
 const bg=getImage(s.image);if(bg)ctx.drawImage(bg,0,0);else{ctx.fillStyle='#1e3035';ctx.fillRect(0,0,s.w,s.h);ctx.fillStyle='#e8e8e8';ctx.fillText('Loading map…',10,20)}
 for(const b of w.bombs){const spr=getImage(FILES.bomb),scale=1+Math.sin(visualTime*16)*.07;if(spr)ctx.drawImage(spr,b.x-8*scale,b.y-8*scale,16*scale,16*scale);else{ctx.fillStyle='#181513';ctx.beginPath();ctx.arc(b.x,b.y,7,0,2*Math.PI);ctx.fill()}}
 for(const flame of w.flames){
  // Use original 14-frame NPS animation for blast segments instead of the
  // prototype rectangles. Timing is provisional until EKA2L1 verification.
  const progress=1-flame.life/(flame.duration||w.rules.blastLifetime);
  const frame=Core.clamp(Math.floor(progress*14),0,13);
  ctx.globalAlpha=Math.min(1,flame.life/.12);
  const center=flame.cells[0];
  for(const c of flame.cells){
    const files=c===center?FILES.explosionCenter:
      c.x===center.x?FILES.explosionVertical:FILES.explosionHorizontal;
    const sprite=getImage(files[frame]);
    if(sprite)ctx.drawImage(sprite,Math.round(c.x-8),Math.round(c.y-8));
    else{ctx.fillStyle='#ffa62e';ctx.fillRect(c.x-7,c.y-7,14,14)}
  }
  ctx.globalAlpha=1;
 }
 if(w.botEnabled&&w.enemy.alive)drawCharacter(w.enemy,false);if(w.duel&&w.player2&&w.health2>0)drawCharacter(w.player2,false);if(w.health>0)drawCharacter(w.player,true);
}
function loop(ts){if(!lastTs)lastTs=ts;const elapsed=Math.min(.12,Math.max(0,(ts-lastTs)/1000));lastTs=ts;
 if(w?.status==='playing'){
   accumulator+=elapsed;let steps=0;
   while(accumulator>=Core.STEP&&steps<8){advance(Core.STEP);accumulator-=Core.STEP;steps++;if(w.status!=='playing'){accumulator=0;break}}
   visualTime+=elapsed;
 }else accumulator=0;
 render();requestAnimationFrame(loop);
}
const handled=new Set(['ArrowUp','ArrowDown','ArrowLeft','ArrowRight','Space','KeyW','KeyA','KeyS','KeyD','KeyF']);
window.addEventListener('keydown',e=>{if(handled.has(e.code)){
 // Do not consume keys while a select or other form field is focused.
 if(e.target instanceof HTMLSelectElement||(e.target instanceof HTMLInputElement&&e.target.type!=='checkbox'))return;
 e.preventDefault();keys.add(e.code);if(e.code==='Space'&&!e.repeat)requestBomb();if(e.code==='KeyF'&&!e.repeat&&w?.duel)requestBomb('player2');}
 if(e.code==='Escape'&&w){e.preventDefault();pause(w.status!=='paused')}});
window.addEventListener('keyup',e=>keys.delete(e.code));
window.addEventListener('blur',()=>{keys.clear();touch.clear();touch2.clear();pause(true)});
document.addEventListener('visibilitychange',()=>{if(document.hidden){pause(true);syncMusic(false)}});
for(const el of document.querySelectorAll('[data-dir]')){const direction=el.dataset.dir;
 el.addEventListener('pointerdown',e=>{e.preventDefault();el.setPointerCapture(e.pointerId);touch.add(direction);el.classList.add('is-down')});
 const release=()=>{touch.delete(direction);el.classList.remove('is-down')};
 el.addEventListener('pointerup',release);el.addEventListener('pointercancel',release);el.addEventListener('lostpointercapture',release);
}
for(const el of document.querySelectorAll('[data-dir2]')){const direction=el.dataset.dir2;
 el.addEventListener('pointerdown',e=>{e.preventDefault();el.setPointerCapture(e.pointerId);touch2.add(direction);el.classList.add('is-down')});
 const release=()=>{touch2.delete(direction);el.classList.remove('is-down')};
 el.addEventListener('pointerup',release);el.addEventListener('pointercancel',release);el.addEventListener('lostpointercapture',release);
}
$('bombButton2').addEventListener('pointerdown',e=>{e.preventDefault();requestBomb('player2')});
$('bombButton2').addEventListener('click',e=>{if(e.detail===0)requestBomb('player2')});
$('bombButton').addEventListener('pointerdown',e=>{e.preventDefault();requestBomb()});
// Keyboard-triggered clicks have detail=0; pointerdown already handled touch/mouse.
$('bombButton').addEventListener('click',e=>{if(e.detail===0)requestBomb()});
$('restart').addEventListener('click',()=>{audio('menu_select');setup(w.stage.id)});
$('pause').addEventListener('click',()=>{audio('menu_abort');pause(w.status!=='paused')});
$('stage').addEventListener('change',e=>{audio('menu_move');setup(e.target.value)});
$('bot').addEventListener('change',()=>{if($('bot').checked)$('duel').checked=false;setup(w.stage.id);canvas.focus({preventScroll:true})});
$('duel').addEventListener('change',()=>{if($('duel').checked)$('bot').checked=false;setup(w.stage.id);canvas.focus({preventScroll:true})});
$('sound').addEventListener('click',()=>{muted=!muted;$('sound').textContent=muted?'♫ Muted':'♫ Sound';syncMusic(true);savePrefs()});
$('music').addEventListener('click',()=>{musicEnabled=!musicEnabled;$('music').textContent=musicEnabled?'♫ Music On':'♫ Music Off';syncMusic(true);savePrefs()});
window.EAApp={pause:()=>pause(true),resume:()=>pause(false)};
window.EADebug={getState:()=>World.snapshot(w),setStage:setup,placeBomb:()=>World.placeBomb(w),tick:dt=>{World.step(w,input(),dt);syncHud()},render,getWorld:()=>w,requestBomb};
setup(Number.isInteger(prefs.stage)&&prefs.stage>=0&&prefs.stage<STAGES.length?prefs.stage:0);requestAnimationFrame(loop);
})();
