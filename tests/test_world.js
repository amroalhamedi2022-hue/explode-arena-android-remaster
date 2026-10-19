const assert=require('node:assert/strict');
const C=require('../web/core.js'),W=require('../web/world.js');
assert.equal(C.spriteFrame(3,false,20),0);
assert.equal(C.spriteFrame(2,false,20),8);
assert.equal(C.spriteFrame(1,false,20),16);
assert.equal(C.spriteFrame(0,false,20),24);
for(let dir=0;dir<4;dir++){assert(C.spriteFrame(dir,true,.3)>=dir*0); assert.equal(Math.floor(C.spriteFrame(dir,true,.3)/8),Math.floor(C.spriteFrame(dir,false,0)/8));}
const makeStage=(id=0,blocked=[])=>( {id,w:128,h:128,spawns:[{x:40,y:40},{x:104,y:104}],obstacle:{x:0,y:0,w:16,h:16,grid:Array.from({length:16},(_,y)=>Array.from({length:16},(_,x)=>blocked.some(p=>p[0]===x&&p[1]===y)?1:0))}} );
{
 const stage=makeStage();const w=W.create(stage,{seed:1234});
 const original={x:w.player.x,y:w.player.y};
 for(let i=0;i<60;i++)W.step(w,{right:true},1/60);
 assert(w.player.x>original.x+50,'player should move with repeated 60Hz updates');
 assert.equal(w.ticks,60);assert.equal(w.health,3);
 w.player.x=40;w.player.y=40;
 assert(W.placeBomb(w));assert.equal(w.bombs.length,1);assert.equal(W.placeBomb(w),false,'bomb limit');
 assert.equal(w.player.passBomb,1,'newly placed bomb should not trap owner');
 const b=w.bombs[0];for(let i=0;i<18;i++)W.step(w,{right:true},1/60);
 assert.equal(w.player.passBomb,null,'leaving own bomb removes pass-through');
 assert(C.blocked(stage,b.x,b.y,5,w.bombs),'bomb blocks reentry');
}
{
 const stage=makeStage();const w=W.create(stage);
 const x=C.cellCenter(w.player.x),y=C.cellCenter(w.player.y);
 w.bombs=[{id:10,x,y,fuse:0,owner:'player'},{id:11,x:x+16,y,fuse:99,owner:'player'}];
 w.nextBomb=12;W.step(w,{},C.STEP);
 assert.equal(w.bombs.length,0,'blast triggers chain reaction');
 assert.equal(w.events.filter(e=>e.type==='explode').length,2);
 assert.equal(w.flames.length,2);
}
{
 const stage=makeStage(),w=W.create(stage);w.player.invincible=0;
 w.flames=[{cells:[{x:w.player.x,y:w.player.y}],life:100}];
 W.step(w);assert.equal(w.health,2);
 for(let i=0;i<20;i++)W.step(w);assert.equal(w.health,2,'invulnerability prevents repeated damage');
 for(let i=0;i<120;i++)W.step(w);assert.equal(w.health,1);
 for(let i=0;i<120;i++)W.step(w);assert.equal(w.status,'defeat');
 const old=w.t;W.step(w,{right:true},1);assert.equal(w.t,old,'defeated rounds must not advance');
}
{
 const stage=makeStage(),a=W.create(stage,{bot:true,seed:5}),b=W.create(stage,{bot:true,seed:5});
 for(let i=0;i<150;i++){W.step(a,{right:i%16<4,down:i%18<3},C.STEP);W.step(b,{right:i%16<4,down:i%18<3},C.STEP);}
 assert.deepEqual(W.snapshot(a),W.snapshot(b),'fixed-seed simulation should reproduce');
 assert.deepEqual(a.enemy,b.enemy);
}
{
 const stage=makeStage(0,[[6,5],[6,6],[6,7]]),w=W.create(stage);
 w.player.x=43;w.player.y=52;
 const startX=w.player.x;
 W.move(w,w.player,1,0,120,.25);assert(w.player.x<startX+10,'collision must block high speed tunnelling');
 assert.equal(C.nearestFree(stage,48,48)?.x!==undefined,true,'nearest-free identifies a valid cell');
}
{
 const w=W.create(makeStage());assert(!W.placeBomb({...w,status:'paused'}),'cannot plant when paused');
 const a=W.snapshot(w);assert.equal(a.status,'playing');assert.equal(a.stage,0);
}
console.log('PASS world engine: movement, bomb limit, pass-through, chain reactions, damage, victory/defeat states, reproducibility, obstacles');
// Symbian-observed remote rules, without guessing multi-bomb targeting.
 {
 const stage=makeStage(),w=W.create(stage);
 assert.equal(W.detonateRemote(w),false,'no remote bomb: no detonation');
 assert.equal(W.setRemoteDetonator(w,'player'),true);
 assert.equal(w.player.remoteDetonator,true);
 assert.equal(W.placeBomb(w),true);
 const b=w.bombs[0];assert.equal(b.remote,true);assert.equal(b.fuse,null);
 for(let i=0;i<900;i++)W.step(w,{},C.STEP);
 assert.equal(w.bombs.length,1,'remote bombs must never auto-detonate');
 assert.equal(w.bombs[0].fuse,null);
 W.step(w,{remote:true},C.STEP);
 assert.equal(w.bombs.length,0);assert.equal(w.events.filter(e=>e.type==='explode').length,1);
 assert.equal(W.detonateRemote(w),false,'repeated remote action must not explode twice');
}
{
 const stage=makeStage(),w=W.create(stage,{rules:{bombLimit:2}});
 assert.equal(W.setRemoteDetonator(w,'player2'),false,'P2 does not exist without duel');
 assert.equal(W.setRemoteDetonator(w,'bot'),false,'bot remote power is not verified');
 assert.equal(W.setRemoteDetonator(w,'player',false),true);
 assert.equal(W.placeBomb(w),true);assert.equal(w.bombs[0].remote,false);
 assert.equal(W.detonateRemote(w),false,'ordinary bombs must not be remote-triggered');
 for(let i=0;i<110;i++)W.step(w,{},C.STEP);
 assert.equal(w.bombs.length,0,'ordinary timer stays intact');
}
{
 const stage=makeStage(),w=W.create(stage,{rules:{bombLimit:3}});
 W.setRemoteDetonator(w);
 assert(W.placeBomb(w));w.player.x=72;w.player.y=72;assert(W.placeBomb(w));
 assert.equal(W.detonateRemote(w),false,'multiple remote bomb selection is unverified');
 assert.equal(w.bombs.length,2);
 w.status='paused';assert.equal(W.detonateRemote(w),false,'paused game cannot detonate');
}
{
 const w=W.create(makeStage(),{duel:true});
 assert(W.setRemoteDetonator(w,'player2'));
 assert(W.placeBomb(w,'player2'));
 assert.equal(w.bombs[0].owner,'player2');
 assert.equal(W.detonateRemote(w,'player'),false,'remote triggers only the owner');
 assert.equal(W.detonateRemote(w,'player2'),true);
}
console.log('PASS remote bomb regression: no countdown, keypad trigger, normal bombs, owner isolation, pause, unresolved multi-bomb policy');
