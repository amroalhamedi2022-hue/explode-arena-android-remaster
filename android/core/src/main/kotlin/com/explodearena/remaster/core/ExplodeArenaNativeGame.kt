package com.explodearena.remaster.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport

/**
 * Native libGDX vertical slice of the EXISTING Mainline.
 * Displays original arena art and runs ported provisional movement/collision.
 * Native provisional bombs, fuses and blast visualization are now integrated.
 * Original Symbian rules, HUD, sound and most game modes remain unported.
 */
class ExplodeArenaNativeGame : ApplicationAdapter() {
    private lateinit var stages: List<StageData>
    private var stageIndex = 0
    private lateinit var stage: StageData
    private lateinit var actor: Actor
    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var batch: SpriteBatch
    private lateinit var shapes: ShapeRenderer
    private lateinit var font: BitmapFont
    private val touchControls = NativeTouchControls()
    private lateinit var arena: Texture
    private val clock = FixedStepClock()
    private lateinit var bombSimulation: NativeBombSimulation

    override fun create() {
        stages = StageCatalog.parse(Gdx.files.internal("assets/stages.js").readString("UTF-8"))
        stage = stages.first()
        val spawn = stage.spawns.firstOrNull() ?: Spawn(stage.width / 2.0, stage.height / 2.0)
        val safe = StageCollision.nearestFree(stage, spawn.x, spawn.y)
            ?: error("Original arena ${stage.id} has no free player spawn")
        actor = Actor(safe.x, safe.y)
        bombSimulation = NativeBombSimulation(stage)
        arena = Texture(Gdx.files.internal(stage.image))
        camera = OrthographicCamera()
        viewport = FitViewport(stage.width.toFloat(), stage.height.toFloat(), camera)
        batch = SpriteBatch()
        shapes = ShapeRenderer()
        font = BitmapFont()
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    override fun resize(width: Int, height: Int) {
        if (::viewport.isInitialized) viewport.update(width, height, true)
    }

    /**
     * Stage navigation is a native data/texture swap, not a WebView navigation.
     * Original Symbian stage-selection rules and stage progression are unverified.
     */
    private fun selectStage(index: Int) {
        val target = (index + stages.size) % stages.size
        if (target == stageIndex) return
        val next = stages[target]
        val spawn = next.spawns.firstOrNull() ?: Spawn(next.width / 2.0, next.height / 2.0)
        val safe = StageCollision.nearestFree(next, spawn.x, spawn.y)
            ?: error("Original arena ${next.id} has no free player spawn")
        val texture = Texture(Gdx.files.internal(next.image))
        arena.dispose()
        arena = texture
        stageIndex = target
        stage = next
        actor = Actor(safe.x, safe.y)
        bombSimulation = NativeBombSimulation(next)
        viewport = FitViewport(next.width.toFloat(), next.height.toFloat(), camera)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    override fun render() {
        // Sample input ONCE per render, preserving one-shot multi-touch actions
        // even if the fixed-step clock performs several catch-up ticks.
        val width = Gdx.graphics.width
        val height = Gdx.graphics.height
        val contacts = (0 until 10).mapNotNull { pointer ->
            if (Gdx.input.isTouched(pointer)) {
                NativeTouchControls.Contact(pointer, Gdx.input.getX(pointer).toFloat(), Gdx.input.getY(pointer).toFloat())
            } else null
        }
        val touch = touchControls.update(contacts, width, height)
        if (touch.nextStage || Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) selectStage(stageIndex + 1)
        else if (touch.previousStage || Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) selectStage(stageIndex - 1)
        if (touch.placeBomb || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) bombSimulation.place(actor)
        val keyX = (if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) 1 else 0) -
            (if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) 1 else 0)
        val keyY = (if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) 1 else 0) -
            (if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) 1 else 0)
        val dx = (keyX + touch.dx).coerceIn(-1f, 1f).toDouble()
        val dy = (keyY + touch.dy).coerceIn(-1f, 1f).toDouble()
        clock.advance(Gdx.graphics.deltaTime.toDouble()) {
            StageCollision.move(
                stage, actor, dx, dy, 77.0,
                FixedStepClock.STEP_SECONDS, bombSimulation.bombs,
            )
            bombSimulation.tick()
        }
        ScreenUtils.clear(0f, 0f, 0f, 1f)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.draw(arena, 0f, 0f, stage.width.toFloat(), stage.height.toFloat())
        batch.end()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.12f, 0.92f, 0.78f, 1f)
        shapes.circle(actor.x.toFloat(), (stage.height - actor.y).toFloat(), 5f, 16)
        // Native provisional bomb/flame overlays. Real original NPS sprites
        // and their timing will replace these placeholders after parity review.
        shapes.setColor(0.16f, 0.18f, 0.24f, 1f)
        for (bomb in bombSimulation.bombs) {
            shapes.circle(bomb.x.toFloat(), (stage.height - bomb.y).toFloat(), 7f, 16)
        }
        shapes.setColor(1f, 0.64f, 0.22f, 0.75f)
        for (flame in bombSimulation.flames) for (cell in flame.cells) {
            shapes.rect((cell.x - 8).toFloat(), (stage.height - cell.y - 8).toFloat(), 16f, 16f)
        }
        shapes.end()

        // Overlay uses screen pixels, not arena pixels; stays reachable on
        // tablets/phones with different aspect ratios and letterboxing.
        val controls = touchControls.layout(width, height)
        val overlay = Matrix4().setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
        shapes.projectionMatrix = overlay
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.10f, 0.19f, 0.26f, 0.48f)
        shapes.circle(controls.padX, height - controls.padY, controls.radius * 1.5f, 32)
        shapes.circle(controls.bombX, height - controls.bombY, controls.radius * 1.2f, 32)
        shapes.circle(controls.previousX, height - controls.stageY, controls.radius * 0.70f, 24)
        shapes.circle(controls.nextX, height - controls.stageY, controls.radius * 0.70f, 24)
        shapes.setColor(0.16f, 0.90f, 0.82f, 0.82f)
        shapes.circle(
            controls.padX + touch.dx * controls.radius,
            height - (controls.padY + touch.dy * controls.radius),
            controls.radius * 0.50f, 24,
        )
        shapes.end()
        batch.projectionMatrix = overlay
        batch.begin()
        font.draw(batch, "ARENA ${stageIndex + 1}/${stages.size}", width * 0.43f, height - controls.stageY + 6f)
        font.draw(batch, "<", controls.previousX - 4f, height - controls.stageY + 6f)
        font.draw(batch, ">", controls.nextX - 4f, height - controls.stageY + 6f)
        font.draw(batch, "B", controls.bombX - 5f, height - controls.bombY + 6f)
        batch.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    override fun pause() { touchControls.reset() }

    override fun dispose() {
        if (::arena.isInitialized) arena.dispose()
        if (::batch.isInitialized) batch.dispose()
        if (::shapes.isInitialized) shapes.dispose()
        if (::font.isInitialized) font.dispose()
    }
}
