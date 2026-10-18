// Regression coverage for optional two-player local practice (not Symbian-equivalence tests).
const assert=require('node:assert/strict'),fs=require('fs'),path=require('path');
const C=require('../web/core.js'),W=require('../web/world.js');
const stages=JSON.parse(fs.readFileSync(path.join(__dirname,'../web/assets/stages.js'),'utf8').replace(/^window.EXPLODE_STAGES=/,'').replace(/;\s*$/,''));
assert.equal(stages.length,41);
assert.throws(()=>W.rulesFor({speed:-1}),/Invalid/);
assert.throws(()=>W.rulesFor({bombLimit:1.4}),/Invalid/);
assert.throws(()=>W.rulesFor({nonsense:3}),/Unknown/);
assert.throws(()=>W.create(stages[0],{bot:true,duel:true}),/either/);
let steps=0,bombs=0;
for(const stage of stages){
 const a=W.create(stage,{duel:true,seed:123,rules:{bombLimit:2}});
 const b=W.create(stage,{duel:true,seed:123,rules:{bombLimit:2}});
 assert.equal(a.health2,3);assert(a.player2&&a.player2.x>=0);
 assert(Math.hypot(a.player.x-a.player2.x,a.player.y-a.player2.y)>=12,`independent spawns stage ${stage.id}`);
 for(let i=0;i<150;i++){
  const controls={up:i%80<10,left:i%80>30&&i%80<50,bomb:i===2,
   p2:{left:i%50<10,right:i%50>25&&i%50<40,bomb:i===3}};
  W.step(a,controls,C.STEP);W.step(b,controls,C.STEP);steps++;
  assert.deepEqual(W.snapshot(a),W.snapshot(b),`deterministic local game stage ${stage.id}`);
  assert(Number.isFinite(a.player.x)&&Number.isFinite(a.player2.x));
  assert(a.player2.x>=0&&a.player2.x<stage.w&&a.player2.y>=0&&a.player2.y<stage.h);
  assert(a.bombs.filter(x=>x.owner==='player').length<=2);
  assert(a.bombs.filter(x=>x.owner==='player2').length<=2);
 }
 bombs+=a.nextBomb-1;
}
function emptyStage(){return {id:50,w:128,h:128,spawns:[{x:24,y:24},{x:104,y:104}],
 obstacle:{x:0,y:0,w:16,h:16,grid:Array.from({length:16},()=>Array(16).fill(0))}};}
{
 const w=W.create(emptyStage(),{duel:true});
 const a={...w.player},b={...w.player2};
 W.step(w,{right:true,p2:{left:true}},C.STEP);
 assert(w.player.x>a.x&&w.player2.x<b.x,'two actors move independently');
 assert(W.placeBomb(w,'player'));assert(W.placeBomb(w,'player2'));
 assert.equal(w.bombs.length,2,'each owner has own bomb allocation');
 assert(!W.placeBomb(w,'player'));assert(!W.placeBomb(w,'player2'));
 assert.equal(w.player.passBomb,1);assert.equal(w.player2.passBomb,2);
}
{
 const w=W.create(emptyStage(),{duel:true});w.player.invincible=0;w.player2.invincible=0;
 w.flames=[{cells:[{x:w.player.x,y:w.player.y}],life:100}];
 for(let i=0;i<3;i++){w.player.invincible=0;W.step(w);}
 assert.equal(w.health,0);assert.equal(w.status,'defeat');assert.equal(w.winner,'P2');
}
{
 const w=W.create(emptyStage(),{duel:true});
 for(let i=0;i<3;i++){
   w.player.invincible=0;w.player2.invincible=0;
   w.flames=[{cells:[{x:w.player.x,y:w.player.y},{x:w.player2.x,y:w.player2.y}],life:100}];W.step(w);
 }
 assert.equal(w.health,0);assert.equal(w.health2,0);assert.equal(w.status,'draw');assert.equal(w.winner,null);
}
console.log(`PASS optional local duel on 41 arenas, ${steps} deterministic steps, ${bombs} bomb placements, isolated bomb ownership, duel/draw outcomes`);
