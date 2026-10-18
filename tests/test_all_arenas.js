// Integration smoke test: exercise gameplay systems on every original arena.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const W=require('../web/world.js'),C=require('../web/core.js');
const text=fs.readFileSync(path.join(__dirname,'../web/assets/stages.js'),'utf8');
const stages=JSON.parse(text.slice('window.EXPLODE_STAGES='.length).trim().replace(/;$/,''));
let ticks=0,bombEvents=0,detonations=0;
for(const s of stages){
 const w=W.create(s,{bot:s.id%2===0,seed:777+s.id});
 for(let i=0;i<720;i++){
  const controls={right:i%160<40,left:i%160>=80&&i%160<120,down:i%160>=40&&i%160<80,up:i%160>=120,bomb:i%110===0};
  W.step(w,controls,C.STEP);ticks++;
  for(const e of w.events){if(e.type==='place')bombEvents++;if(e.type==='explode')detonations++;}
  assert(Number.isFinite(w.player.x)&&Number.isFinite(w.player.y));
  assert(w.player.x>=0&&w.player.x<s.w&&w.player.y>=0&&w.player.y<s.h);
  assert(w.health>=0&&w.health<=3);
  assert(w.bombs.filter(b=>b.owner==='player').length<=W.CFG.bombLimit);
  assert(w.bombs.filter(b=>b.owner==='bot').length<=W.CFG.bombLimit);
  if(w.status!=='playing')break;
 }
}
assert.equal(stages.length,41);assert(ticks>1000);assert(bombEvents>0);assert(detonations>0);
console.log(`PASS 41 arena integrations, ${ticks} simulation ticks, ${bombEvents} planted bombs, ${detonations} explosions; no invalid player coordinates`);
