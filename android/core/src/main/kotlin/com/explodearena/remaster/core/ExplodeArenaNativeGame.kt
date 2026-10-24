package com.explodearena.remaster.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Sound
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
    private lateinit var placeSound: Sound
    private lateinit var explosionSound: Sound
    private val touchControls = NativeTouchControls()
    private lateinit var arena: Texture
    // Original Mainline sprites (not yet an HD reconstruction). Frames are
    // mapped from the existing web renderer, not guessed Symbian animation.
    private lateinit var playerFrames: Array<Texture>
    private lateinit var bombSprite: Texture
    private lateinit var blastCenter: Array<Texture>
    private lateinit var blastVertical: Array<Texture>
    private lateinit var blastHorizontal: Array<Texture>
    private var facing = 2 // front/down, matching Mainline's frame groups
    private var walking = false
    private var animationSeconds = 0.0
    private val clock = FixedStepClock()
    private lateinit var bombSimulation: NativeBombSimulation
    private lateinit var vitality: NativeVitality
    private var roundFlow = NativeRoundFlow()
    private val pauseGate = NativePauseGate()
    // First provisional opponent on arenas with at least two spawn slots.
    // Original Symbian bot AI, character palette and match rules are unverified.
    private var botActor: Actor? = null
    private var botVitality: NativeVitality? = null
    private var botController = NativeBotController()
    private var botFacing = 2
    private var botWalking = false

    override fun create() {
        stages = StageCatalog.parse(Gdx.files.internal("assets/stages.js").readString("UTF-8"))
        stage = stages.first()
        val spawn = stage.spawns.firstOrNull() ?: Spawn(stage.width / 2.0, stage.height / 2.0)
        val safe = StageCollision.nearestFree(stage, spawn.x, spawn.y)
            ?: error("Original arena ${stage.id} has no free player spawn")
        actor = Actor(safe.x, safe.y)
        bombSimulation = NativeBombSimulation(stage)
        vitality = NativeVitality()
        resetBot(stage)
        roundFlow = NativeRoundFlow()
        // Keep Android Back inside the native game to open/close pause.
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        arena = Texture(Gdx.files.internal(stage.image))
        fun loadFrames(group: String, count: Int): Array<Texture> =
            Array(count) { index ->
                Texture(Gdx.files.internal("assets/sprites/${group}_${index.toString().padStart(3, '0')}.png"))
            }
        playerFrames = loadFrames("g000", NativeSpriteFrames.PLAYER_FRAME_COUNT)
        bombSprite = Texture(Gdx.files.internal("assets/sprites/g042_000.png"))
        blastCenter = loadFrames("g047", NativeSpriteFrames.BLAST_FRAME_COUNT)
        blastVertical = loadFrames("g048", NativeSpriteFrames.BLAST_FRAME_COUNT)
        blastHorizontal = loadFrames("g051", NativeSpriteFrames.BLAST_FRAME_COUNT)
        camera = OrthographicCamera()
        viewport = FitViewport(stage.width.toFloat(), stage.height.toFloat(), camera)
        batch = SpriteBatch()
        shapes = ShapeRenderer()
        font = BitmapFont()
        // Original bundled WAVs, owned by this native screen and disposed with it.
        placeSound = Gdx.audio.newSound(Gdx.files.internal("assets/sfx/bomb_place.wav"))
        explosionSound = Gdx.audio.newSound(Gdx.files.internal("assets/sfx/bomb_explode1.wav"))
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
        vitality = NativeVitality()
        resetBot(next)
        roundFlow = NativeRoundFlow()
        facing = 2
        walking = false
        animationSeconds = 0.0
        clock.clearPending()
        viewport = FitViewport(next.width.toFloat(), next.height.toFloat(), camera)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    private fun retryStage() {
        val spawn = stage.spawns.firstOrNull() ?: Spawn(stage.width / 2.0, stage.height / 2.0)
        val safe = StageCollision.nearestFree(stage, spawn.x, spawn.y)
            ?: error("Original arena ${stage.id} has no free player spawn")
        actor = Actor(safe.x, safe.y)
        bombSimulation = NativeBombSimulation(stage)
        vitality = NativeVitality()
        resetBot(stage)
        roundFlow = NativeRoundFlow()
        facing = 2
        walking = false
        animationSeconds = 0.0
        clock.clearPending()
    }

    private fun resetBot(current: StageData) {
        val spawn = current.spawns.getOrNull(1)
        val safe = spawn?.let { StageCollision.nearestFree(current, it.x, it.y) }
        botActor = safe?.let { Actor(it.x, it.y) }
        botVitality = if (safe == null) null else NativeVitality()
        botController = NativeBotController()
        botFacing = 2
        botWalking = false
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
        val pauseToggled = touch.pauseToggle ||
            Gdx.input.isKeyJustPressed(Input.Keys.BACK) ||
            Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.P)
        if (pauseToggled) pauseGate.toggle(clock)
        if (!pauseGate.isPaused && !pauseToggled) {
            if (touch.nextStage || Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) selectStage(stageIndex + 1)
            else if (touch.previousStage || Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) selectStage(stageIndex - 1)
        }
        // Original Symbian first-hand input evidence: keypad 5 plants and 8
        // requests remote detonation. Remote power remains unavailable until
        // its actual pickup mapping is established; Space/touch are modern aids.
        if (!pauseGate.isPaused && !pauseToggled &&
            (touch.placeBomb || Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
                Gdx.input.isKeyJustPressed(Input.Keys.NUM_5))) {
            if (roundFlow.canRetry) retryStage()
            else if (roundFlow.isPlaying && bombSimulation.place(actor)) placeSound.play(0.65f)
        }
        if (!pauseGate.isPaused && !pauseToggled &&
            Gdx.input.isKeyJustPressed(Input.Keys.NUM_8) && roundFlow.isPlaying) {
            if (bombSimulation.detonateRemote()) explosionSound.play(0.75f)
        }
        val keyX = (if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) 1 else 0) -
            (if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) 1 else 0)
        val keyY = (if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) 1 else 0) -
            (if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) 1 else 0)
        val dx = (keyX + touch.dx).coerceIn(-1f, 1f).toDouble()
        val dy = (keyY + touch.dy).coerceIn(-1f, 1f).toDouble()
        if (dx != 0.0 || dy != 0.0) {
            facing = if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                if (dx > 0.0) 3 else 1
            } else if (dy > 0.0) 2 else 0
        }
        var advanceAfterTick = false
        pauseGate.advance(clock, Gdx.graphics.deltaTime.toDouble()) {
            animationSeconds += FixedStepClock.STEP_SECONDS
            if (roundFlow.isPlaying) {
                val oldX = actor.x
                val oldY = actor.y
                StageCollision.move(
                    stage, actor, dx, dy, 77.0,
                    FixedStepClock.STEP_SECONDS, bombSimulation.bombs,
                )
                walking = actor.x != oldX || actor.y != oldY
                if (bombSimulation.tick() > 0) explosionSound.play(0.75f)
                // Use the actor's actual provisional 5px collision radius, not a
                // point sample that misses flame hits along the sprite edge.
                vitality.tick(bombSimulation.isBurning(actor.x, actor.y, radius = StageCollision.ACTOR_RADIUS))
                val bot = botActor
                val botLife = botVitality
                if (bot != null && botLife != null && !botLife.defeated) {
                    val turn = NativeBotTurn.advance(
                        stage, bot, actor, bombSimulation, botController, clock.ticks,
                    )
                    botWalking = turn.moved
                    if (botWalking) {
                        botFacing = when {
                            turn.decision.dx > 0 -> 3
                            turn.decision.dx < 0 -> 1
                            turn.decision.dy > 0 -> 2
                            else -> 0
                        }
                    }
                    if (turn.bombPlaced) placeSound.play(0.45f)
                    botLife.tick(bombSimulation.isBurning(bot.x, bot.y, StageCollision.ACTOR_RADIUS))
                } else botWalking = false
            } else {
                walking = false
                botWalking = false
            }
            if (roundFlow.tick(
                    vitality.defeated, botVitality?.defeated == true,
                    botActor != null, stageIndex < stages.lastIndex,
                )) advanceAfterTick = true
        }
        // Swap textures/stage only after all catch-up ticks, never inside the
        // FixedStepClock callback (which owns the current accumulator).
        if (advanceAfterTick) selectStage(stageIndex + 1)
        ScreenUtils.clear(0f, 0f, 0f, 1f)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.draw(arena, 0f, 0f, stage.width.toFloat(), stage.height.toFloat())
        // Match the original Mainline sprite families and orientation; native
        // renderer uses y-up coordinates, while simulation stays y-down.
        for (bomb in bombSimulation.bombs) {
            batch.draw(bombSprite, (bomb.x - 8).toFloat(),
                (stage.height - bomb.y - 8).toFloat(), 16f, 16f)
        }
        for (flame in bombSimulation.flames) {
            val frame = NativeSpriteFrames.flame(flame.remaining, 0.45)
            val alpha = (flame.remaining / 0.12).coerceIn(0.0, 1.0).toFloat()
            batch.setColor(1f, 1f, 1f, alpha)
            val center = flame.cells.firstOrNull()
            for (cell in flame.cells) {
                val sprite = when {
                    cell == center -> blastCenter[frame]
                    cell.x == center?.x -> blastVertical[frame]
                    else -> blastHorizontal[frame]
                }
                batch.draw(sprite, (cell.x - 8).toFloat(),
                    (stage.height - cell.y - 8).toFloat(), 16f, 16f)
            }
        }
        batch.setColor(1f, 1f, 1f, 1f)
        val sprite = playerFrames[NativeSpriteFrames.player(facing, walking, animationSeconds)]
        if (!vitality.defeated &&
            (vitality.invulnerableSeconds <= 0.0 ||
                kotlin.math.floor(animationSeconds * 13).toInt() % 2 != 0)) {
            batch.draw(sprite, (actor.x - sprite.width / 2.0).toFloat(),
                (stage.height - actor.y - 7).toFloat())
        }
        val bot = botActor
        val botLife = botVitality
        if (bot != null && botLife != null && !botLife.defeated &&
            (botLife.invulnerableSeconds <= 0.0 ||
                kotlin.math.floor(animationSeconds * 13).toInt() % 2 != 0)) {
            // Temporary tinted Mainline character until verified opponent art.
            batch.setColor(1f, 0.55f, 0.55f, 1f)
            val botSprite = playerFrames[NativeSpriteFrames.player(botFacing, botWalking, animationSeconds)]
            batch.draw(botSprite, (bot.x - botSprite.width / 2.0).toFloat(),
                (stage.height - bot.y - 7).toFloat())
            batch.setColor(1f, 1f, 1f, 1f)
        }
        batch.end()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

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
        shapes.circle(controls.pauseX, height - controls.stageY, controls.radius * 0.70f, 24)
        shapes.setColor(0.16f, 0.90f, 0.82f, 0.82f)
        shapes.circle(
            controls.padX + touch.dx * controls.radius,
            height - (controls.padY + touch.dy * controls.radius),
            controls.radius * 0.50f, 24,
        )
        shapes.end()
        batch.projectionMatrix = overlay
        batch.begin()
        val botHp = botVitality?.let { "  BOT HP ${it.health}" }.orEmpty()
        font.draw(batch, "ARENA ${stageIndex + 1}/${stages.size}  HP ${vitality.health}" + botHp, width * 0.30f, height - controls.stageY + 6f)
        val roundMessage = when (roundFlow.state) {
            NativeRoundFlow.State.WON, NativeRoundFlow.State.ADVANCING -> "VICTORY - NEXT ARENA"
            NativeRoundFlow.State.COMPLETE -> "FINAL ARENA WON"
            NativeRoundFlow.State.LOST -> "DEFEAT - PRESS B TO RETRY"
            NativeRoundFlow.State.DRAW -> "DRAW - PRESS B TO RETRY"
            NativeRoundFlow.State.PLAYING -> null
        }
        if (roundMessage != null) font.draw(batch, roundMessage, width * 0.32f, height * 0.53f)
        font.draw(batch, "<", controls.previousX - 4f, height - controls.stageY + 6f)
        font.draw(batch, ">", controls.nextX - 4f, height - controls.stageY + 6f)
        font.draw(batch, if (pauseGate.isPaused) ">" else "II", controls.pauseX - 6f, height - controls.stageY + 6f)
        if (pauseGate.isPaused) font.draw(batch, "PAUSED - TAP > TO RESUME", width * 0.33f, height * 0.57f)
        font.draw(batch, if (roundFlow.canRetry) "R" else "B", controls.bombX - 5f, height - controls.bombY + 6f)
        batch.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    override fun pause() {
        touchControls.reset()
        clock.clearPending()
        walking = false
        if (::placeSound.isInitialized) placeSound.stop()
        if (::explosionSound.isInitialized) explosionSound.stop()
    }

    override fun dispose() {
        Gdx.input.setCatchKey(Input.Keys.BACK, false)
        if (::arena.isInitialized) arena.dispose()
        if (::playerFrames.isInitialized) playerFrames.forEach(Texture::dispose)
        if (::bombSprite.isInitialized) bombSprite.dispose()
        if (::blastCenter.isInitialized) blastCenter.forEach(Texture::dispose)
        if (::blastVertical.isInitialized) blastVertical.forEach(Texture::dispose)
        if (::blastHorizontal.isInitialized) blastHorizontal.forEach(Texture::dispose)
        if (::batch.isInitialized) batch.dispose()
        if (::shapes.isInitialized) shapes.dispose()
        if (::font.isInitialized) font.dispose()
        if (::placeSound.isInitialized) placeSound.dispose()
        if (::explosionSound.isInitialized) explosionSound.dispose()
    }
}
