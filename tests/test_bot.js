// Deterministic pathfinding and attack checks for OPTIONAL practice bot.
// This is not a claim of matching the Symbian opponent logic.
const assert=require('node:assert/strict');
const W=require('../web/world.js'),C=require('../web/core.js');
const clear={id:900,w:192,h:128,spawns:[{x:24,y:24},{x:144,y:24}],
 obstacle:{x:0,y:0,w:24,h:16,grid:Array.from({length:16},()=>Array(24).fill(0))}};
const first=W.nextPathCell(clear,144,24,24,24);
assert(first.x<144 && first.y===28,'bot route should head toward target on an empty arena');
const noPath={...clear,obstacle:{...clear.obstacle,grid:clear.obstacle.grid.map(row=>[...row])}};
for(let y=0;y<16;y++)noPath.obstacle.grid[y][12]=1;
const wallRoute=W.nextPathCell(noPath,144,24,24,24);
assert(wallRoute.x>=100,'pathfinding must not tunnel across a solid wall');
const run=()=>{
 const w=W.create(clear,{bot:true,seed:42});let bombs=0,walk=0;
 const start=Math.hypot(w.enemy.x-w.player.x,w.enemy.y-w.player.y);
 for(let i=0;i<500;i++){
  W.step(w,{},C.STEP);walk++;
  bombs+=w.events.filter(e=>e.type==='place'&&e.owner==='bot').length;
  assert(w.bombs.filter(b=>b.owner==='bot').length<=1);
  assert(Number.isFinite(w.enemy.x)&&Number.isFinite(w.enemy.y));
  if(w.status!=='playing')break;
 }
 assert(bombs>0,'bot should actually use a bomb when reaching the player');
 return {bombs,start,walk,winner:w.winner,position:[w.enemy.x,w.enemy.y],status:w.status};
};
assert.deepEqual(run(),run(),'practice bot must be deterministic from same seed/input');
console.log('PASS practice bot: grid navigation, walls, autonomous bomb, limit and determinism');
