// Provisional collision consistency checks, NOT proof of Symbian timings/rules.
const assert = require('node:assert/strict');
const C = require('../web/core.js');
const W = require('../web/world.js');

const stage = {id: 991, w: 128, h: 128, spawns: [{x:40,y:40},{x:104,y:104}],
    obstacle: {x:0,y:0,w:16,h:16,grid:Array.from({length:16},()=>Array(16).fill(0))}};
const flame = () => [{cells:[{x:40,y:40}],life:10}];

function playerHealthAt(x) {
    const w=W.create(stage);
    w.player.x=x;w.player.y=40;w.player.invincible=0;
    w.flames=flame();
    W.step(w,{},C.STEP);
    return w.health;
}
assert.equal(playerHealthAt(51),2,'player radius must overlap flame edge');
assert.equal(playerHealthAt(53),3,'non-overlapping actor must not be hit');
{
    const w=W.create(stage,{duel:true});
    w.player2.x=51;w.player2.y=40;w.player2.invincible=0;
    w.flames=flame();
    W.step(w,{},C.STEP);
    assert.equal(w.health2,2,'second player uses the same collision radius');
}
{
    const w=W.create(stage,{bot:true,rules:{botSpeed:0}});
    w.enemy.x=51;w.enemy.y=40;
    w.flames=flame();
    W.step(w,{},C.STEP);
    assert.equal(w.enemy.alive,false,'practice bot receives edge-of-flame collision');
    assert.equal(w.status,'victory');
}
console.log('PASS provisional blast collision radius: P1, P2, bot, non-overlap');
