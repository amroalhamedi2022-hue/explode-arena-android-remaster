/* Deterministic 60Hz mainline simulation. Movement, bombs, health and AI remain
 * provisional until compared against original Symbian gameplay in EKA2L1.
 * Local duel is an optional modern practice mode, not an original mode claim. */
(function(root){'use strict';
 const C=(typeof module!=='undefined'&&module.exports)?require('./core.js'):root.EACore;
 const CFG=Object.freeze({speed:77,botSpeed:34,bombFuse:1.6,blastLifetime:.45,invulnerability:1.7,bombRange:3,bombLimit:1});
 const OPTIONS={speed:[10,250],botSpeed:[0,250],bombFuse:[.1,20],blastLifetime:[.05,5],invulnerability:[0,10],bombRange:[1,12],bombLimit:[1,8]};
 function rulesFor(overrides={}){
   const result={...CFG};
   for(const [key,value] of Object.entries(overrides)){
     if(!Object.prototype.hasOwnProperty.call(OPTIONS,key))throw new Error('Unknown gameplay rule: '+key);
     const [min,max]=OPTIONS[key];
     if(!Number.isFinite(value)||value<min||value>max||(key==='bombLimit'&&!Number.isInteger(value)))throw new Error('Invalid gameplay rule '+key);
     result[key]=value;
   }
   return Object.freeze(result);
 }
 function actorAt(point){return {x:point.x,y:point.y,dir:2,moving:false,invincible:1,passBomb:null};}
 function alternateSpawn(stage,p){
   const candidate=stage.spawns[1]||{x:stage.w-p.x-14,y:stage.h-p.y-14};
   let best=C.nearestFree(stage,candidate.x,candidate.y),dist=best?Math.hypot(best.x-p.x,best.y-p.y):-1;
   // Some original stages only contain one spawn marker. Choose a distant free
   // area for optional practice mode, never manufacture an "original" marker.
   if(dist<24)for(const [x,y] of [[8,8],[stage.w-9,8],[8,stage.h-9],[stage.w-9,stage.h-9],
       [stage.w/2,8],[stage.w/2,stage.h-9],[8,stage.h/2],[stage.w-9,stage.h/2]]){
     const free=C.nearestFree(stage,x,y);if(!free)continue;
     const d=Math.hypot(free.x-p.x,free.y-p.y);if(d>dist){best=free;dist=d;}
   }
   return best;
 }
 function create(stage,{bot=false,duel=false,seed=12345,rules={}}={}){
   if(bot&&duel)throw new Error('Choose either demo bot or local duel');
   const first=stage.spawns[0]||{x:stage.w/2,y:stage.h/2},p=C.nearestFree(stage,first.x,first.y);
   if(!p)throw new Error('Arena has no valid player spawn: '+stage.id);
   const e=alternateSpawn(stage,p);
   if(duel&&(!e||Math.hypot(p.x-e.x,p.y-e.y)<12))throw new Error('Arena has no valid secondary spawn');
   return {stage,t:0,elapsed:0,health:3,health2:duel?3:null,score:0,status:'playing',winner:null,ticks:0,seed:seed>>>0,
     rules:rulesFor(rules),player:actorAt(p),player2:duel?actorAt(e):null,
     enemy:{x:e?.x??p.x,y:e?.y??p.y,alive:bot,dx:1,dy:0,nextMove:0,nextBombAt:1.2,goalX:e?.x??p.x,goalY:e?.y??p.y,passBomb:null,moving:false,dir:2},
     bombs:[],flames:[],events:[],nextBomb:1,botEnabled:bot,duel};
 }
 function emit(w,type,extra={}){w.events.push({type,...extra})}
 function move(w,actor,dx,dy,speed,dt){
   const length=Math.hypot(dx,dy);actor.moving=length>0;
   if(!length||dt<=0)return;
   dx/=length;dy/=length;
   const dist=speed*dt,n=Math.max(1,Math.ceil(dist/3)),sx=dx*dist/n,sy=dy*dist/n;
   for(let i=0;i<n;i++){
     const ignore=actor.passBomb;
     if(!C.blocked(w.stage,actor.x+sx,actor.y,5,w.bombs,ignore))actor.x+=sx;
     if(!C.blocked(w.stage,actor.x,actor.y+sy,5,w.bombs,ignore))actor.y+=sy;
     if(actor.passBomb!==null&&!w.bombs.some(b=>b.id===actor.passBomb&&C.touchesBomb(b,actor.x,actor.y,5)))actor.passBomb=null;
   }
   actor.dir=Math.abs(dx)>=Math.abs(dy)?(dx<0?1:3):(dy<0?0:2);
 }
 function placeBomb(w,owner='player'){
   if(w.status!=='playing'||!['player','player2','bot'].includes(owner))return false;
   const actor=owner==='player'?w.player:owner==='player2'?w.player2:w.botEnabled&&w.enemy.alive?w.enemy:null;
   if(!actor||w.bombs.filter(b=>b.owner===owner).length>=w.rules.bombLimit)return false;
   const x=C.cellCenter(actor.x),y=C.cellCenter(actor.y);
   if(C.blocked(w.stage,x,y,3)||w.bombs.some(b=>b.x===x&&b.y===y))return false;
   const b={id:w.nextBomb++,x,y,fuse:w.rules.bombFuse,owner};
   w.bombs.push(b);
   if(C.touchesBomb(b,actor.x,actor.y,5))actor.passBomb=b.id;
   emit(w,'place',{owner});return true;
 }
 function detonate(w,ids){
   const queue=[...ids],exploded=new Set();
   while(queue.length){
     const id=queue.shift();if(exploded.has(id))continue;
     const bomb=w.bombs.find(b=>b.id===id);if(!bomb)continue;
     exploded.add(id);
     const cells=C.explosionCells(w.stage,bomb.x,bomb.y,w.rules.bombRange);
     w.flames.push({cells,life:w.rules.blastLifetime,duration:w.rules.blastLifetime});emit(w,'explode',{id,owner:bomb.owner});
     for(const other of w.bombs){if(!exploded.has(other.id)&&cells.some(c=>C.blastHits(c,other.x,other.y,0)))queue.push(other.id)}
   }
   w.bombs=w.bombs.filter(b=>!exploded.has(b.id));
   for(const actor of [w.player,w.player2,w.enemy])if(actor&&actor.passBomb!==null&&exploded.has(actor.passBomb))actor.passBomb=null;
   return exploded.size;
 }
 // Apply the same provisional 5px actor hitbox used for movement collisions.
 // This fixes missed edge-of-flame hits; original Symbian geometry is not yet proven.
 function unsafe(w,x,y,radius=0){return w.flames.some(f=>f.cells.some(c=>C.blastHits(c,x,y,radius)))}
 function hitPlayer(w,actor,field,identity){
   if(actor.invincible!==0||!unsafe(w,actor.x,actor.y,5))return;
   w[field]=Math.max(0,w[field]-1);actor.invincible=w.rules.invulnerability;
   emit(w,'playerHit',{player:identity});
 }
 // A deterministic breadth-first route through the provisional static
 // collision grid. This is practice-AI behavior, NOT extracted Symbian AI.
 function nextPathCell(stage,x,y,tx,ty,bombs=[],ignoreBombId=null){
   const width=Math.ceil(stage.w/C.CELL),height=Math.ceil(stage.h/C.CELL),length=width*height;
   const sx=C.clamp(Math.floor(x/C.CELL),0,width-1),sy=C.clamp(Math.floor(y/C.CELL),0,height-1),start=sy*width+sx;
   const q=new Int32Array(length),seen=new Uint8Array(length),first=new Int32Array(length);
   let head=0,tail=1,best=start,bestDistance=(x-tx)**2+(y-ty)**2;
   q[0]=start;seen[start]=1;first[start]=-1;
   const offsets=[[0,-1],[-1,0],[1,0],[0,1]];
   while(head<tail){
     const at=q[head++],gx=at%width,gy=(at-gx)/width;
     for(const [dx,dy] of offsets){
       const nx=gx+dx,ny=gy+dy;if(nx<0||ny<0||nx>=width||ny>=height)continue;
       const index=ny*width+nx;if(seen[index])continue;
       const px=nx*C.CELL+C.CELL/2,py=ny*C.CELL+C.CELL/2;
       if(C.blocked(stage,px,py,4,bombs,ignoreBombId))continue;
       seen[index]=1;q[tail++]=index;first[index]=at===start?index:first[at];
       const dist=(px-tx)**2+(py-ty)**2;
       if(dist<bestDistance){bestDistance=dist;best=index}
     }
   }
   if(best===start)return {x,y};
   const index=first[best];return {x:(index%width)*C.CELL+C.CELL/2,y:Math.floor(index/width)*C.CELL+C.CELL/2};
 }
 function step(w,input={},dt=C.STEP){
   w.events=[];
   if(w.status!=='playing')return w;
   dt=C.clamp(dt,0,.05);w.t+=dt;w.elapsed+=dt;w.ticks++;
   const p=w.player;p.invincible=Math.max(0,p.invincible-dt);
   move(w,p,(input.right?1:0)-(input.left?1:0),(input.down?1:0)-(input.up?1:0),w.rules.speed,dt);
   if(input.bomb)placeBomb(w);
   if(w.duel){
     const p2=w.player2,d=input.p2||{};
     p2.invincible=Math.max(0,p2.invincible-dt);
     move(w,p2,(d.right?1:0)-(d.left?1:0),(d.down?1:0)-(d.up?1:0),w.rules.speed,dt);
     if(d.bomb)placeBomb(w,'player2');
   }
   for(const f of w.flames)f.life-=dt;
   w.flames=w.flames.filter(f=>f.life>0);
   const due=[];for(const b of w.bombs){b.fuse-=dt;if(b.fuse<=0)due.push(b.id)}
   if(due.length)detonate(w,due);
   hitPlayer(w,p,'health','P1');
   if(w.duel){
     hitPlayer(w,w.player2,'health2','P2');
     if(w.health===0||w.health2===0){
       w.status=w.health===0&&w.health2===0?'draw':w.health===0?'defeat':'victory';
       w.winner=w.health===0&&w.health2===0?null:w.health===0?'P2':'P1';emit(w,'roundOver',{winner:w.winner});
     }
   }else if(w.health===0){w.status='defeat';emit(w,'roundOver')}
   const e=w.enemy;
   if(w.status==='playing'&&w.botEnabled&&e.alive){
     if(w.t>=e.nextMove){
       const own=w.bombs.find(b=>b.owner==='bot');
       // Retreat after placing a bomb; otherwise approach the player.
       const retreat=own?{x:C.clamp(e.x+(e.x>=own.x?56:-56),8,w.stage.w-9),
         y:C.clamp(e.y+(e.y>=own.y?56:-56),8,w.stage.h-9)}:null;
       const target=retreat||w.player;
       const waypoint=nextPathCell(w.stage,e.x,e.y,target.x,target.y,w.bombs,e.passBomb);
       e.goalX=waypoint.x;e.goalY=waypoint.y;e.nextMove=w.t+.12;
     }
     move(w,e,e.goalX-e.x,e.goalY-e.y,w.rules.botSpeed,dt);
     if(w.t>=e.nextBombAt&&e.passBomb===null&&Math.hypot(e.x-p.x,e.y-p.y)<48){
       if(placeBomb(w,'bot'))e.nextBombAt=w.t+2.8;else e.nextBombAt=w.t+.45;
     }
     if(unsafe(w,e.x,e.y,5)){e.alive=false;w.score+=100;emit(w,'enemyHit');emit(w,'roundWon');w.status='victory';w.winner='P1'}
   }
   return w;
 }
 function snapshot(w){return {stage:w.stage.id,x:w.player.x,y:w.player.y,x2:w.player2?.x??null,y2:w.player2?.y??null,
   health:w.health,health2:w.health2,bombs:w.bombs.length,flames:w.flames.length,score:w.score,
   paused:w.status==='paused',status:w.status,winner:w.winner,duel:w.duel,gameTime:w.t,ticks:w.ticks,enemyAlive:w.enemy.alive};}
 const api={CFG,rulesFor,create,move,placeBomb,detonate,nextPathCell,step,snapshot,unsafe};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 root.EAWorld=api;
})(typeof window!=='undefined'?window:globalThis);
