package com.explodearena.remaster.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport

/**
 * Native libGDX vertical slice of the EXISTING Mainline.
 * Displays original arena art and runs ported provisional movement/collision.
 * Bombs, original Symbian rules, HUD and all other gameplay remain unported.
 */
class ExplodeArenaNativeGame : ApplicationAdapter() {
    private lateinit var stage: StageData
    private lateinit var actor: Actor
    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var batch: SpriteBatch
    private lateinit var shapes: ShapeRenderer
    private lateinit var arena: Texture
    private val clock = FixedStepClock()

    override fun create() {
        val stages = StageCatalog.parse(Gdx.files.internal("assets/stages.js").readString("UTF-8"))
        stage = stages.first()
        val spawn = stage.spawns.firstOrNull() ?: Spawn(stage.width / 2.0, stage.height / 2.0)
        val safe = StageCollision.nearestFree(stage, spawn.x, spawn.y)
            ?: error("Original arena ${stage.id} has no free player spawn")
        actor = Actor(safe.x, safe.y)
        arena = Texture(Gdx.files.internal(stage.image))
        camera = OrthographicCamera()
        viewport = FitViewport(stage.width.toFloat(), stage.height.toFloat(), camera)
        batch = SpriteBatch()
        shapes = ShapeRenderer()
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    override fun resize(width: Int, height: Int) {
        if (::viewport.isInitialized) viewport.update(width, height, true)
    }

    override fun render() {
        clock.advance(Gdx.graphics.deltaTime.toDouble()) {
            val dx = (if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) 1 else 0) -
                (if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) 1 else 0)
            val dy = (if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) 1 else 0) -
                (if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) 1 else 0)
            StageCollision.move(stage, actor, dx.toDouble(), dy.toDouble(), 77.0, FixedStepClock.STEP_SECONDS)
        }
        ScreenUtils.clear(0f, 0f, 0f, 1f)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.draw(arena, 0f, 0f, stage.width.toFloat(), stage.height.toFloat())
        batch.end()
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.12f, 0.92f, 0.78f, 1f)
        shapes.circle(actor.x.toFloat(), (stage.height - actor.y).toFloat(), 5f, 16)
        shapes.end()
    }

    override fun pause() { /* No simulation ticks are advanced while paused. */ }

    override fun dispose() {
        if (::arena.isInitialized) arena.dispose()
        if (::batch.isInitialized) batch.dispose()
        if (::shapes.isInitialized) shapes.dispose()
    }
}
