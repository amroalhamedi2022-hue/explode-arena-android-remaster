/* Source assets and stage geometry are original extractions.
 * Gameplay rules in this module remain provisional pending EKA2L1 comparison. */
(function(root){'use strict';
 const CELL=8,BOMB_CELL=16,STEP=1/60;
 const clamp=(n,min,max)=>Math.max(min,Math.min(max,n));
 function tile(stage,x,y){
   const l=stage.obstacle,c=Math.floor((x-l.x)/CELL),r=Math.floor((y-l.y)/CELL);
   if(c<0||r<0||r>=l.h||c>=l.w)return -1;
   return l.grid[r][c];
 }
 function touchesBomb(b,x,y,radius=5){
   const size=7;return Math.abs(b.x-x)<size+radius&&Math.abs(b.y-y)<size+radius;
 }
 function blocked(stage,x,y,radius=5,bombs=[],ignoreBombId=null){
   if(x-radius<0||y-radius<0||x+radius>=stage.w||y+radius>=stage.h)return true;
   for(const dx of [-radius,radius])for(const dy of [-radius,radius]){
     if(tile(stage,x+dx,y+dy)!==0)return true;
   }
   for(const b of bombs)if(b.id!==ignoreBombId&&touchesBomb(b,x,y,radius))return true;
   return false;
 }
 function nearestFree(stage,x,y,radius=4){
   const bx=clamp(x,radius+1,stage.w-radius-2),by=clamp(y,radius+1,stage.h-radius-2);
   if(!blocked(stage,bx,by,radius))return {x:bx,y:by};
   // Deterministic expanding grid search; never return a known blocked cell.
   const ox=Math.floor(bx/CELL),oy=Math.floor(by/CELL),lim=Math.ceil(Math.max(stage.w,stage.h)/CELL);
   for(let r=1;r<=lim;r++)for(let dy=-r;dy<=r;dy++)for(let dx=-r;dx<=r;dx++){
     if(Math.max(Math.abs(dx),Math.abs(dy))!==r)continue;
     const px=(ox+dx)*CELL+CELL/2,py=(oy+dy)*CELL+CELL/2;
     if(!blocked(stage,px,py,radius))return {x:px,y:py};
   }
   return null;
 }
 function cellCenter(x){return Math.floor(x/BOMB_CELL)*BOMB_CELL+BOMB_CELL/2}
 function explosionCells(stage,x,y,range=3){
   const cx=Math.floor(x/BOMB_CELL),cy=Math.floor(y/BOMB_CELL),hit=[{x:cx*BOMB_CELL+8,y:cy*BOMB_CELL+8}];
   for(const [dx,dy] of [[1,0],[-1,0],[0,1],[0,-1]])for(let n=1;n<=range;n++){
     const px=(cx+dx*n)*BOMB_CELL+8,py=(cy+dy*n)*BOMB_CELL+8;
     if(px<0||py<0||px>=stage.w||py>=stage.h)break;
     if(blocked(stage,px,py,3))break;
     hit.push({x:px,y:py});
   }
   return hit;
 }
 function blastHits(cell,x,y,radius=7){return Math.abs(cell.x-x)<8+radius&&Math.abs(cell.y-y)<8+radius}
 function spriteFrame(dir,moving,animationSeconds){
   const bases=[24,16,8,0];const base=bases[dir]??8;
   return base+(moving?Math.floor(Math.max(0,animationSeconds)*12)%8:0);
 }
 function seededNext(n){return (Math.imul(n,1664525)+1013904223)>>>0}
 const api={CELL,BOMB_CELL,STEP,clamp,tile,touchesBomb,blocked,nearestFree,cellCenter,explosionCells,blastHits,spriteFrame,seededNext};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 root.EACore=api;
 })(typeof window!=='undefined'?window:globalThis);
