package com.thigazhini_labs.samuraijack

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import com.thigazhini_labs.samuraijack.audio.SoundManager
import com.thigazhini_labs.samuraijack.engine.GLRenderer
import com.thigazhini_labs.samuraijack.engine.Vector3
import com.thigazhini_labs.samuraijack.models.Mesh
import com.thigazhini_labs.samuraijack.models.Models3D
import com.thigazhini_labs.samuraijack.stages.Stages
import com.thigazhini_labs.samuraijack.ui.GameUI
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.sin

enum class GameState {
    SPLASH,
    MAIN_MENU,
    STAGE_SELECT,
    TRAVEL_TRANSITION,
    INTRO_CUTSCENE,
    GAMEPLAY,
    OUTRO_CUTSCENE,
    GAME_OVER,
    GAME_COMPLETE
}

class MainActivity : ComponentActivity(), SensorEventListener {

    // Sensors for motion controls
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var sensorTiltX = 0f
    private var smoothedCameraTiltOffset = 0f

    // Core Game States
    var gameState by mutableStateOf(GameState.SPLASH)
    var currentStageIndex by mutableIntStateOf(0)
    var unlockedStageCount by mutableIntStateOf(13)
    
    // Player statistics
    var playerHealth by mutableFloatStateOf(100f)
    var playerSwordEnergy by mutableFloatStateOf(0f)
    var enemiesRemaining by mutableIntStateOf(3)
    var playerCoins by mutableIntStateOf(245)
    var playerCrystals by mutableIntStateOf(12)
    
    // Cinematic subtitles typing
    var typedText by mutableStateOf("")
    var isTextFullyTyped by mutableStateOf(false)
    private var currentDialogIndex = 0
    private var typingJob: Job? = null

    // OpenGL elements
    private lateinit var glSurfaceView: GLSurfaceView
    private val renderer by lazy { GLRenderer(this) }

    // Physics thread variables
    private var gameLoopJob: Job? = null
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Game Entities
    private lateinit var jackMesh: Mesh
    private lateinit var jackMeshNormal: Mesh
    private lateinit var jackMeshAttacking: Mesh
    private lateinit var skyMesh: Mesh
    private val enemyMeshes = mutableListOf<Mesh>()
    private val laserMeshes = mutableListOf<Mesh>()
    private val environmentMeshes = mutableListOf<Mesh>()
    private var groundMesh: Mesh? = null
    private var bossMesh: Mesh? = null

    // Controls
    private var targetJoystickMoveVec = Vector3(0f, 0f, 0f)
    private var joystickMoveVec = Vector3(0f, 0f, 0f)
    private var animTime = 0f

    // Gameplay parameters
    private var jackPos = Vector3(0f, 0f, 0f)
    private var jackVelY = 0f
    private var isJumping = false
    var jackState by mutableStateOf("Idle") // Idle, Run, Jump, Attack, Block, Hurt
    private var jackActionTime = 0f
    private var hitFlashTimer = 0f
    private var specialFlashActive = false
    private var specialFlashTime = 0f
    private var bossHitsRemaining = 5

    // Touch gesture tracking variables
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchStartTime = 0L
    private var touchStartedInMaskedZone = false
    private val swipeThreshold = 80f
    private val tapThresholdTime = 220L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable immersive full-screen mode to cover the background image fully
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        // Initialize GLSurfaceView
        glSurfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
            setRenderer(this@MainActivity.renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        setContent {
            val bgResId = when (gameState) {
                GameState.SPLASH, GameState.MAIN_MENU -> R.drawable.main_background
                GameState.STAGE_SELECT -> R.drawable.bg_map
                else -> {
                    when (currentStageIndex) {
                        0 -> R.drawable.bg_frosthollow // Stage 1: Frosthollow Mine
                        1 -> R.drawable.bg_forest      // Stage 2: The Young Prince
                        2 -> R.drawable.bg_jungle      // Stage 3: Journey Across the World
                        3 -> R.drawable.bg_village     // Stage 4: The Sacred Sword
                        4 -> R.drawable.bg_port        // Stage 5: Battle Against Evil (Aku)
                        5 -> R.drawable.bg_port        // Stage 6: Exiled Through Time
                        6 -> R.drawable.bg_port        // Stage 7: City of Machines
                        7 -> R.drawable.bg_port        // Stage 8: The Dog Archer Village
                        8 -> R.drawable.bg_forest      // Stage 9: The Warrior from the Highlands
                        9 -> R.drawable.bg_jungle      // Stage 10: Escape from the Fortress
                        10 -> R.drawable.bg_desert     // Stage 11: The Three-Eyed Beast
                        11 -> R.drawable.bg_desert     // Stage 12: The Lava Guardian
                        12 -> R.drawable.bg_village    // Stage 13: Path of the Ronin
                        else -> R.drawable.bg_village
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // Background Image behind OpenGL
                Image(
                    painter = painterResource(id = bgResId),
                    contentDescription = "Game Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark translucent overlay for atmospheric depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )

                // Background 3D View
                AndroidView(
                    factory = { glSurfaceView },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay UI Control panel
                GameUI(
                    gameState = gameState,
                    currentStageIndex = currentStageIndex,
                    unlockedStageCount = unlockedStageCount,
                    playerHealth = playerHealth,
                    playerSwordEnergy = playerSwordEnergy,
                    enemiesRemaining = enemiesRemaining,
                    typedText = typedText,
                    isTextFullyTyped = isTextFullyTyped,
                    jackState = jackState,
                    playerCoins = playerCoins,
                    playerCrystals = playerCrystals,
                    onStartGame = { startGame() },
                    onSelectStage = { idx ->
                        if (idx == -1) {
                            gameState = GameState.STAGE_SELECT
                        } else {
                            enterStage(idx)
                        }
                    },
                    onNextStage = { nextStage() },
                    onResumeGame = { gameState = GameState.GAMEPLAY },
                    onRetryStage = { enterStage(currentStageIndex) },
                    onBackToMenu = {
                        gameState = GameState.MAIN_MENU
                        SoundManager.currentStage = 1
                        SoundManager.playMusic(this@MainActivity)
                    },
                    onAdvanceText = { advanceDialogue() },
                    onMove = { dx, dz -> targetJoystickMoveVec = Vector3(dx, 0f, dz) },
                    onMeleeAttack = { triggerAttack() },
                    onJump = { triggerJump() },
                    onBlock = { triggerBlock() }
                )
            }
        }

        // Initialize Sensors for motion controls
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Initialize and start Sound Synthesizer
        SoundManager.start(this)
        buildMainMenuScene()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
        SoundManager.start(this)
        startGameLoop()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
        SoundManager.stop()
        stopGameLoop()
        sensorManager?.unregisterListener(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.stop()
        mainScope.cancel()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Y-axis represents vertical/horizontal tilt in landscape depending on rotation.
            // Check rotation orientation to adjust sensor sign.
            val rotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                display?.rotation ?: android.view.Surface.ROTATION_90
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.rotation
            }
            sensorTiltX = if (rotation == android.view.Surface.ROTATION_270) {
                -event.values[1]
            } else {
                event.values[1]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameState == GameState.GAMEPLAY) return super.onTouchEvent(event)

        // Two finger tap -> Special attack
        if (event.pointerCount == 2 && playerSwordEnergy >= 100f) {
            triggerSpecialAttack()
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                touchStartTime = System.currentTimeMillis()
                if (jackState == "Idle" || jackState == "Run") {
                    jackState = "Run"
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // Drag horizontally to move Jack
                val diffX = event.x - touchStartX
                if (abs(diffX) > 20f && !isJumping && jackState != "Attack" && jackState != "Block") {
                    jackState = "Run"
                    val limitX = if (currentStageIndex == 0) floatArrayOf(-6.0f, 6.0f) else floatArrayOf(-4.5f, 4.5f)
                    jackPos.x = (diffX / 100f).coerceIn(limitX[0], limitX[1])
                }
            }
            MotionEvent.ACTION_UP -> {
                val diffX = event.x - touchStartX
                val diffY = event.y - touchStartY
                val duration = System.currentTimeMillis() - touchStartTime

                jackState = "Idle"

                if (duration < tapThresholdTime && abs(diffX) < swipeThreshold && abs(diffY) < swipeThreshold) {
                    // Tap -> Attack
                    triggerAttack()
                } else {
                    // Swipe directions
                    if (diffY < -swipeThreshold) {
                        // Swipe Up -> Jump
                        triggerJump()
                    } else if (diffY > swipeThreshold) {
                        // Swipe Down -> Block
                        triggerBlock()
                    } else if (diffX > swipeThreshold) {
                        // Swipe Right -> Dodge roll right
                        triggerDodge(1)
                    } else if (diffX < -swipeThreshold) {
                        // Swipe Left -> Dodge roll left
                        triggerDodge(-1)
                    }
                }
            }
        }
        return true
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (gameState == GameState.GAMEPLAY) {
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels.toFloat()
            val screenHeight = metrics.heightPixels.toFloat()

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.x
                    touchStartY = event.y
                    touchStartTime = System.currentTimeMillis()

                    // Masking check: ignore swipes starting in virtual control quadrants
                    val inJoystickZone = event.x < screenWidth * 0.32f && event.y > screenHeight * 0.50f
                    val inButtonsZone = event.x > screenWidth * 0.68f && event.y > screenHeight * 0.50f
                    touchStartedInMaskedZone = inJoystickZone || inButtonsZone
                }
                MotionEvent.ACTION_UP -> {
                    if (!touchStartedInMaskedZone) {
                        val diffX = event.x - touchStartX
                        val diffY = event.y - touchStartY
                        val duration = System.currentTimeMillis() - touchStartTime

                        if (duration < 500L) {
                            if (abs(diffX) > abs(diffY)) {
                                // Horizontal swipe
                                if (diffX > swipeThreshold) {
                                    triggerDodge(1)
                                } else if (diffX < -swipeThreshold) {
                                    triggerDodge(-1)
                                    refocusCameraOnCharacter()
                                }
                            } else {
                                // Vertical swipe
                                if (diffY < -swipeThreshold) {
                                    triggerJump()
                                } else if (diffY > swipeThreshold) {
                                    triggerBlock()
                                }
                            }
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun buildMainMenuScene() {
        mainScope.launch {
            val list = withContext(Dispatchers.IO) {
                val subList = mutableListOf<Mesh>()
                // Torii gate at the center
                val torii = Models3D.createToriiGate()
                torii.position = Vector3(0f, 0f, 2f)
                torii.scale = Vector3(0.8f, 0.8f, 0.8f)
                subList.add(torii)

                // Add ground
                val ground = Models3D.createGround(20f, 20f, 0.2f, 0.15f, 0.15f)
                ground.position = Vector3(0f, 0f, 0f)
                subList.add(ground)

                // Static Jack loaded on background thread
                val jack = Models3D.loadObj(this@MainActivity, "samurai_model.obj", "samurai_model.mtl", scaleMultiplier = 2.0f, yOffset = 0.715f)
                jack.position = Vector3(0f, 0.4f, 0f)
                subList.add(jack)
                subList
            }

            // Set camera
            renderer.cameraPos = Vector3(0f, 1.8f, -4.5f)
            renderer.cameraTarget = Vector3(0f, 0.6f, 0.5f)

            synchronized(renderer.renderMeshes) {
                renderer.renderMeshes.clear()
                renderer.renderMeshes.addAll(list)
            }
        }
    }

    private fun startGame() {
        gameState = GameState.STAGE_SELECT
    }

    private fun enterStage(index: Int) {
        currentStageIndex = index
        SoundManager.currentStage = index + 1
        SoundManager.stopMusic() // Stop the background menu music during stage traversal
        gameState = GameState.TRAVEL_TRANSITION

        mainScope.launch {
            delay(2000)
            setupStageEntities(index)
            startCutscene(index, true)
        }
    }

    private suspend fun setupStageEntities(index: Int) {
        val stage = Stages.stagesList[index]
        playerHealth = 100f
        playerSwordEnergy = 0f
        enemiesRemaining = stage.enemyCount
        jackPos = Vector3(0f, 0.4f, 0f)
        jackVelY = 0f
        isJumping = false
        jackState = "Idle"

        enemyMeshes.clear()
        laserMeshes.clear()
        environmentMeshes.clear()

        val list = withContext(Dispatchers.IO) {
            val subList = mutableListOf<Mesh>()

            // 1. Create Ground (use custom 3D mesh for Stage 1 Frosthollow Mine)
            val gc = stage.groundColor
            val gMesh = if (index == 0) {
                Models3D.loadObj(
                    context = this@MainActivity,
                    objFileName = "step_1_navigation_surface_frozen_mine.obj",
                    mtlFileName = null,
                    scaleMultiplier = 1.0f,
                    yOffset = 0.0f,
                    rotationOffsetY = 0f
                )
            } else {
                Models3D.createGround(12f, 80f, gc[0], gc[1], gc[2])
            }
            groundMesh = gMesh
            subList.add(gMesh)

            // 2. Create Jack
            val normal = Models3D.loadObj(this@MainActivity, "samurai_model.obj", "samurai_model.mtl", scaleMultiplier = 2.0f, yOffset = 0.715f)
            val attacking = Models3D.loadObj(this@MainActivity, "samurai_model.obj", "samurai_model.mtl", scaleMultiplier = 2.0f, yOffset = 0.715f)
            jackMeshNormal = normal
            jackMeshAttacking = attacking
            jackMesh = normal
            jackMesh.position = jackPos
            subList.add(jackMesh)

            // Textured 2D Sky Backdrop mapping stage image
            val sky = Models3D.createTexturedSkyBackdrop()
            sky.position = Vector3(0f, 0f, 0f)
            sky.isVisible = true
            skyMesh = sky
            subList.add(sky)

            // 3. Environment structures based on theme
            if (stage.stageNumber in listOf(1, 2, 4, 13)) {
                // Ancient Pagodas
                val gate = Models3D.createToriiGate()
                gate.position = Vector3(0f, 0f, 15f)
                subList.add(gate)
            }

            // 4. Create enemies dynamically for all stages
            val count = stage.enemyCount
            val boss = stage.bossType
            
            if (boss != "None") {
                // Spawn Boss Fight
                val bMesh = if (boss == "Aku") {
                    Models3D.createAku().apply {
                        position = Vector3(0f, 0.4f, 15f)
                        scale = Vector3(1.2f, 1.2f, 1.2f)
                    }
                } else {
                    // For Lava Guardian, Shadow Ronin, Scotsman, Beast etc., use customized color/scale Samurai model
                    Models3D.loadObj(
                        context = this@MainActivity,
                        objFileName = "samurai_model.obj",
                        mtlFileName = "samurai_model.mtl",
                        scaleMultiplier = if (boss == "Beast") 3.5f else 2.5f,
                        yOffset = if (boss == "Beast") 1.0f else 0.8f,
                        rotationOffsetY = 180f
                    ).apply {
                        position = Vector3(0f, 0.4f, 14f)
                        silhouetteMode = when (boss) {
                            "Ronin" -> 1 // Silhouette black-red
                            "LavaGuardian" -> 2 // Glowing orange-red
                            else -> 0
                        }
                    }
                }
                bossMesh = bMesh
                subList.add(bMesh)
            } else if (count > 0) {
                // Spawn standard swarmer Beetle Drones spaced out along the path
                for (i in 0 until count) {
                    val enemyZ = if (index == 0) {
                        22f + i * 18f
                    } else {
                        5f + i * 4.5f
                    }
                    val enemyX = if (index == 0) {
                        getPathX(enemyZ)
                    } else {
                        if (i % 2 == 0) -1.2f else 1.2f
                    }
                    
                    val enemy = Models3D.createRoboBeetle()
                    enemy.position = Vector3(enemyX, 0.5f, enemyZ)
                    enemy.isVisible = true
                    
                    enemyMeshes.add(enemy)
                    subList.add(enemy)
                }
            }

            subList
        }

        // Setup boss and enemy states on UI thread
        if (stage.bossType != "None") {
            bossHitsRemaining = 5
            enemiesRemaining = 1
        } else if (stage.enemyCount > 0) {
            enemiesRemaining = stage.enemyCount
        } else {
            enemiesRemaining = 1
        }

        // Initialize camera position immediately to prevent starting frame jump
        if (index == 0) {
            renderer.cameraPos = Vector3(0f, 1.8f, -5.5f)
            renderer.cameraTarget = Vector3(0f, 1.0f, 1.8f)
        } else {
            renderer.cameraPos = Vector3(0.9f, 1.8f, -5.5f)
            renderer.cameraTarget = Vector3(-0.2f, 1.0f, 1.8f)
        }

        synchronized(renderer.renderMeshes) {
            renderer.renderMeshes.clear()
            renderer.renderMeshes.addAll(list)
        }
    }

    private fun startCutscene(index: Int, isIntro: Boolean) {
        val stage = Stages.stagesList[index]
        gameState = if (isIntro) GameState.INTRO_CUTSCENE else GameState.OUTRO_CUTSCENE
        currentDialogIndex = 0
        displayDialogue(stage.dialogs.getOrNull(0) ?: "...")
    }

    private fun displayDialogue(text: String) {
        typingJob?.cancel()
        isTextFullyTyped = false
        typedText = ""
        
        // synthesized dialog sweep
        SoundManager.triggerSpecialCharge()

        typingJob = mainScope.launch {
            text.forEach { char ->
                typedText += char
                delay(30)
            }
            isTextFullyTyped = true
        }
    }

    private fun advanceDialogue() {
        if (!isTextFullyTyped) {
            // Finish typing immediately
            typingJob?.cancel()
            val stage = Stages.stagesList[currentStageIndex]
            val text = stage.dialogs.getOrNull(currentDialogIndex) ?: ""
            typedText = text
            isTextFullyTyped = true
            return
        }

        currentDialogIndex++
        val stage = Stages.stagesList[currentStageIndex]
        if (currentDialogIndex < stage.dialogs.size) {
            displayDialogue(stage.dialogs[currentDialogIndex])
        } else {
            // Cutscene completed
            if (gameState == GameState.INTRO_CUTSCENE) {
                gameState = GameState.GAMEPLAY
            } else {
                // Outro complete, proceed or finish game
                if (currentStageIndex == 12) {
                    gameState = GameState.GAME_COMPLETE
                } else {
                    unlockedStageCount = (unlockedStageCount).coerceAtLeast(currentStageIndex + 2)
                    gameState = GameState.STAGE_SELECT
                }
            }
        }
    }

    private fun nextStage() {
        if (currentStageIndex < 12) {
            enterStage(currentStageIndex + 1)
        } else {
            gameState = GameState.GAME_COMPLETE
        }
    }

    // Trigger attacks and movement options
    private fun triggerAttack() {
        if (jackState == "Attack" || jackState == "Hurt") return
        jackState = "Attack"
        jackActionTime = 0.3f
        SoundManager.triggerSlash()

        // Sword collision check
        val attackRange = 2.5f
        if (bossMesh != null) {
            bossMesh?.let { boss ->
                val dist = jackPos.dist(boss.position)
                if (dist < attackRange && boss.isVisible) {
                    bossHitsRemaining--
                    SoundManager.triggerHit()
                    
                    // Visual hit flash & point light feedback
                    hitFlashTimer = 0.4f
                    renderer.pointLightPos = boss.position
                    renderer.pointLightColor = floatArrayOf(0.9f, 0.1f, 0.1f)
                    renderer.pointLightIntensity = 3.0f

                    if (bossHitsRemaining <= 0) {
                        boss.isVisible = false
                        playerSwordEnergy = (playerSwordEnergy + 40f).coerceAtMost(100f)
                        enemiesRemaining = 0
                    }
                }
            }
        } else {
            for (enemy in enemyMeshes) {
                if (enemy.isVisible) {
                    val dist = jackPos.dist(enemy.position)
                    if (dist < attackRange) {
                        triggerHitResponse(enemy)
                        enemiesRemaining = enemyMeshes.count { it.isVisible }
                    }
                }
            }
        }
    }

    private fun triggerHitResponse(entity: Mesh) {
        SoundManager.triggerHit()
        entity.isVisible = false
        playerSwordEnergy = (playerSwordEnergy + 20f).coerceAtMost(100f)

        // Animate point light at hit position
        renderer.pointLightPos = entity.position
        renderer.pointLightColor = floatArrayOf(0.9f, 0.8f, 0.1f)
        renderer.pointLightIntensity = 2.0f
    }

    private fun triggerJump() {
        if (isJumping || jackState == "Hurt") return
        isJumping = true
        jackVelY = 0.28f
        jackState = "Jump"
    }

    private fun triggerBlock() {
        if (jackState == "Hurt") return
        jackState = "Block"
        jackActionTime = 0.6f
    }

    private fun triggerDodge(dir: Int) {
        if (jackState == "Hurt") return
        jackState = "Run"
        val limitX = if (currentStageIndex == 0) floatArrayOf(-6.0f, 6.0f) else floatArrayOf(-4.5f, 4.5f)
        jackPos.x = (jackPos.x + dir * 1.5f).coerceIn(limitX[0], limitX[1])
        SoundManager.triggerSlash()
    }

    private fun triggerSpecialAttack() {
        if (playerSwordEnergy < 100f) return
        playerSwordEnergy = 0f
        specialFlashActive = true
        specialFlashTime = 1.0f
        jackState = "Special"
        jackActionTime = 1.0f

        SoundManager.triggerExplosion()

        // Screen shake + Clear all standard enemies
        if (bossMesh != null) {
            bossHitsRemaining -= 2 // Heavy damage to boss
            playerHealth = (playerHealth + 30f).coerceAtMost(100f)
            if (bossHitsRemaining <= 0) {
                bossMesh?.isVisible = false
                enemiesRemaining = 0
            }
        } else {
            for (enemy in enemyMeshes) {
                if (enemy.isVisible) {
                    enemy.isVisible = false
                }
            }
            enemiesRemaining = 0
        }
    }

    private fun startGameLoop() {
        gameLoopJob = mainScope.launch {
            while (isActive) {
                if (gameState == GameState.GAMEPLAY) {
                    updatePhysics()
                }
                delay(16) // ~60 FPS
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    // Core Frame Physics Updates
    private fun updatePhysics() {
        // Smoothly interpolate joystick vectors
        joystickMoveVec.x += (targetJoystickMoveVec.x - joystickMoveVec.x) * 0.18f
        joystickMoveVec.z += (targetJoystickMoveVec.z - joystickMoveVec.z) * 0.18f
        if (abs(joystickMoveVec.x) < 0.01f && targetJoystickMoveVec.x == 0f) joystickMoveVec.x = 0f
        if (abs(joystickMoveVec.z) < 0.01f && targetJoystickMoveVec.z == 0f) joystickMoveVec.z = 0f

        // Update animation timer
        if (jackState == "Run") {
            animTime += 0.15f
        } else {
            animTime = 0f
        }
        renderer.animTime = animTime

        // 1. Gravity and Player position updates
        if (isJumping) {
            jackPos.y += jackVelY
            jackVelY -= 0.015f // gravity acceleration
            if (jackPos.y <= 0.4f) {
                jackPos.y = 0.4f
                isJumping = false
                if (jackState == "Jump") jackState = "Idle"
            }
        }

        val prevJackX = jackPos.x
        val prevJackZ = jackPos.z

        // Apply joystick movement vectors
        if (joystickMoveVec.x != 0f || joystickMoveVec.z != 0f) {
            val moveSpeed = 0.06f
            val limitZ = if (currentStageIndex == 0) floatArrayOf(-15f, 85f) else floatArrayOf(-35f, 35f)
            
            if (currentStageIndex == 0) {
                // Winding mine path: movement is relative to the path center to prevent clipping
                val currentPathX = getPathX(jackPos.z)
                var relX = jackPos.x - currentPathX
                relX = (relX + joystickMoveVec.x * moveSpeed).coerceIn(-2.5f, 2.5f)
                
                // Move in Z
                val targetZ = (jackPos.z + joystickMoveVec.z * moveSpeed).coerceIn(limitZ[0], limitZ[1])
                val targetX = getPathX(targetZ) + relX
                
                // Run collision check on the proposed target position
                val resolvedPos = checkCollision(targetX, targetZ, jackPos.x, jackPos.z)
                jackPos.x = resolvedPos.x
                jackPos.z = resolvedPos.z
            } else {
                val limitX = floatArrayOf(-4.5f, 4.5f)
                val targetX = (jackPos.x + joystickMoveVec.x * moveSpeed).coerceIn(limitX[0], limitX[1])
                val targetZ = (jackPos.z + joystickMoveVec.z * moveSpeed).coerceIn(limitZ[0], limitZ[1])
                
                val resolvedPos = checkCollision(targetX, targetZ, jackPos.x, jackPos.z)
                jackPos.x = resolvedPos.x
                jackPos.z = resolvedPos.z
            }

            // Dynamic rotation Y so Jack faces actual 3D movement direction along winding curves
            val dx = jackPos.x - prevJackX
            val dz = jackPos.z - prevJackZ
            if (dx != 0f || dz != 0f) {
                val angleRad = kotlin.math.atan2(dx.toDouble(), dz.toDouble())
                val angleDeg = Math.toDegrees(angleRad).toFloat()
                jackMeshNormal.rotation.y = angleDeg
                jackMeshAttacking.rotation.y = angleDeg
            }

            if (jackState == "Idle") {
                jackState = "Run"
            }
        } else {
            if (jackState == "Run") {
                jackState = "Idle"
            }
        }

        // Apply visual action timer decays
        if (jackActionTime > 0) {
            jackActionTime -= 0.016f
            if (jackActionTime <= 0) {
                jackState = "Idle"
            }
        }

        // Special Screen Flash decay
        if (specialFlashActive) {
            specialFlashTime -= 0.016f
            if (specialFlashTime <= 0) {
                specialFlashActive = false
            }
        }

        // 2. Synchronize Jack Mesh Position & Handle sheathed/unsheathed swaps
        val currentJackMesh = if (jackState == "Attack") jackMeshAttacking else jackMeshNormal
        if (jackMesh != currentJackMesh) {
            synchronized(renderer.renderMeshes) {
                val idx = renderer.renderMeshes.indexOf(jackMesh)
                if (idx != -1) {
                    renderer.renderMeshes[idx] = currentJackMesh
                }
            }
            jackMesh = currentJackMesh
        }
        jackMesh.position = jackPos
        jackMesh.silhouetteMode = if (specialFlashActive) 1 else (if (jackMesh.textureName != null) 4 else 0)

        skyMesh.position.x = jackPos.x
        skyMesh.position.z = jackPos.z

        // 3. Move Camera smoothly behind Jack (Third-Person OTS orbit)
        val targetCamX = if (currentStageIndex == 0) {
            // Keep camera centered on path at its Z position, blended with character's relative offset
            val pathX = getPathX(jackPos.z - 5.5f)
            pathX + (jackPos.x - getPathX(jackPos.z)) * 0.5f
        } else {
            jackPos.x + 0.9f
        }
        val targetCamY = jackPos.y + 1.4f
        val targetCamZ = jackPos.z - 5.5f
        renderer.cameraPos.x += (targetCamX - renderer.cameraPos.x) * 0.1f
        renderer.cameraPos.y += (targetCamY - renderer.cameraPos.y) * 0.1f
        renderer.cameraPos.z += (targetCamZ - renderer.cameraPos.z) * 0.1f

        // Apply device tilt/motion to shift camera focus (target looking direction) left/right
        val targetTiltOffset = (-sensorTiltX * 0.3f).coerceIn(-2.5f, 2.5f)
        smoothedCameraTiltOffset += (targetTiltOffset - smoothedCameraTiltOffset) * 0.1f
        
        val targetTargetX = if (currentStageIndex == 0) {
            jackPos.x + smoothedCameraTiltOffset
        } else {
            jackPos.x - 0.2f + smoothedCameraTiltOffset
        }
        val flashOffset = if (specialFlashActive) (sin(specialFlashTime * 50f) * 0.2f) else 0f
        renderer.cameraTarget = Vector3(targetTargetX + flashOffset, jackPos.y + 0.6f, jackPos.z + 1.8f)

        // Update current rendering stage details
        renderer.currentStageIndex = currentStageIndex
        renderer.hitFlashAmount = hitFlashTimer
        if (hitFlashTimer > 0f) {
            hitFlashTimer -= 0.05f
        }

        // Low health heartbeat triggers
        SoundManager.isLowHealthHeartbeatActive = playerHealth < 35f

        // 4. Update Enemy Behaviors & Boss AI for all stages
        if (gameState == GameState.GAMEPLAY) {
            val currentTime = System.currentTimeMillis()
            
            // Update Boss AI
            bossMesh?.let { boss ->
                if (boss.isVisible) {
                    // Face Jack
                    val dx = jackPos.x - boss.position.x
                    val dz = jackPos.z - boss.position.z
                    val angleRad = kotlin.math.atan2(dx.toDouble(), dz.toDouble())
                    boss.rotation.y = Math.toDegrees(angleRad).toFloat()

                    // Boss attack patterns
                    val bossAttackCooldown = 2200L
                    if (currentTime % bossAttackCooldown < 25L) {
                        val stage = Stages.stagesList.getOrNull(currentStageIndex)
                        if (stage?.bossType == "Aku") {
                            // Aku fires 3 spread lasers from the future
                            fireEnemyLaser(Vector3(boss.position.x - 1f, boss.position.y + 1f, boss.position.z))
                            fireEnemyLaser(Vector3(boss.position.x, boss.position.y + 1f, boss.position.z))
                            fireEnemyLaser(Vector3(boss.position.x + 1f, boss.position.y + 1f, boss.position.z))
                        } else {
                            // Melee bosses strike Jack if in close proximity
                            val dist = jackPos.dist(boss.position)
                            if (dist < 3.2f) {
                                damageJack()
                            }
                        }
                    }
                }
            }

            // Update swarm drone behaviors
            for (enemy in enemyMeshes) {
                if (enemy.isVisible) {
                    // Hover effect
                    enemy.position.y = 0.5f + kotlin.math.sin(currentTime * 0.005f).toFloat() * 0.15f
                    
                    // Face Jack
                    val dx = jackPos.x - enemy.position.x
                    val dz = jackPos.z - enemy.position.z
                    val angleRad = kotlin.math.atan2(dx.toDouble(), dz.toDouble())
                    enemy.rotation.y = Math.toDegrees(angleRad).toFloat()
                    
                    // Periodically fire laser towards Jack
                    val enemyIndex = enemyMeshes.indexOf(enemy)
                    val shootCooldown = 2500L
                    val shootOffset = enemyIndex * 800L
                    if ((currentTime + shootOffset) % shootCooldown < 25L) {
                        fireEnemyLaser(enemy.position)
                    }
                }
            }
        }

        // 5. Update Projectiles
        synchronized(laserMeshes) {
            val laserIterator = laserMeshes.iterator()
            while (laserIterator.hasNext()) {
                val laser = laserIterator.next()
                if (laser.isVisible) {
                    // Move laser along -Z axis
                    laser.position.z -= 0.25f
                    
                    // Check collision with Jack
                    val dist = laser.position.dist(jackPos)
                    if (dist < 0.8f) {
                        laser.isVisible = false
                        synchronized(renderer.renderMeshes) {
                            renderer.renderMeshes.remove(laser)
                        }
                        damageJack()
                        laserIterator.remove()
                    } else if (laser.position.z < jackPos.z - 10f) {
                        // Out of bounds
                        laser.isVisible = false
                        synchronized(renderer.renderMeshes) {
                            renderer.renderMeshes.remove(laser)
                        }
                        laserIterator.remove()
                    }
                } else {
                    laserIterator.remove()
                }
            }
        }

        // 6. Check Win conditions (EXPLORATION GATE EXIT TRIGGER)
        val winZ = if (currentStageIndex == 0) 75.0f else 14.2f
        if (jackPos.z >= winZ && (enemiesRemaining == 0 || (enemyMeshes.isEmpty() && bossMesh == null && enemiesRemaining == 1))) {
            enemiesRemaining = -1 // Stop duplicate triggers
            gameState = GameState.OUTRO_CUTSCENE
            startCutscene(currentStageIndex, false)
        }

        // Point light decay
        if (renderer.pointLightIntensity > 0f) {
            renderer.pointLightIntensity -= 0.08f
        }
    }

    private fun fireEnemyLaser(pos: Vector3) {
        val laser = Models3D.createLaser()
        // Spawn slightly in front of enemy facing Jack
        laser.position = Vector3(pos.x, pos.y + 0.3f, pos.z - 0.8f)
        laserMeshes.add(laser)
        
        synchronized(renderer.renderMeshes) {
            renderer.renderMeshes.add(laser)
        }
        SoundManager.triggerLaser()
    }

    private fun damageJack() {
        if (jackState == "Block") {
            // Greatly reduce damage when blocking
            playerHealth = (playerHealth - 2f).coerceAtLeast(0f)
            SoundManager.triggerHit()
            return
        }

        jackState = "Hurt"
        jackActionTime = 0.2f
        playerHealth = (playerHealth - 15f).coerceAtLeast(0f)
        hitFlashTimer = 0.8f

        // Camera shakes
        renderer.cameraPos.x += (Math.random() * 0.4f - 0.2f).toFloat()

        if (playerHealth <= 0f) {
            gameState = GameState.GAME_OVER
            SoundManager.triggerExplosion()
        }
    }

    private fun getPathX(z: Float): Float {
        if (currentStageIndex == 0) {
            if (z < 0f) return 0f
            return 2.2f * sin(z * 0.08f)
        }
        return 0f
    }

    private fun checkCollision(newX: Float, newZ: Float, oldX: Float, oldZ: Float): Vector3 {
        // Check Torii gate collision (if it exists on this stage at Z = 15.0f)
        val stage = com.thigazhini_labs.samuraijack.stages.Stages.stagesList.getOrNull(currentStageIndex)
        if (stage != null && stage.stageNumber in listOf(1, 2, 4, 13)) {
            if (kotlin.math.abs(newZ - 15.0f) < 1.5f) {
                val playerRadius = 0.4f
                // Left post
                val lpX = -2.2f
                val lpDist = kotlin.math.sqrt((newX - lpX) * (newX - lpX) + (newZ - 15.0f) * (newZ - 15.0f))
                if (lpDist < (0.15f + playerRadius)) {
                    return resolveCircleCollision(newX, newZ, oldX, oldZ, lpX, 15.0f, 0.15f + playerRadius)
                }

                // Right post
                val rpX = 2.2f
                val rpDist = kotlin.math.sqrt((newX - rpX) * (newX - rpX) + (newZ - 15.0f) * (newZ - 15.0f))
                if (rpDist < (0.15f + playerRadius)) {
                    return resolveCircleCollision(newX, newZ, oldX, oldZ, rpX, 15.0f, 0.15f + playerRadius)
                }
            }
        }

        if (currentStageIndex == 0) {
            val playerRadius = 0.4f

            // 1. Check support arches (every 6 units from -12 to 84)
            for (zArch in -12..84 step 6) {
                val zArchF = zArch.toFloat()
                if (kotlin.math.abs(newZ - zArchF) > 1.5f) continue

                val pathX = getPathX(zArchF)
                // Left post
                val lpX = pathX - 2.9f
                val lpDist = kotlin.math.sqrt((newX - lpX) * (newX - lpX) + (newZ - zArchF) * (newZ - zArchF))
                if (lpDist < (0.15f + playerRadius)) {
                    return resolveCircleCollision(newX, newZ, oldX, oldZ, lpX, zArchF, 0.15f + playerRadius)
                }

                // Right post
                val rpX = pathX + 2.9f
                val rpDist = kotlin.math.sqrt((newX - rpX) * (newX - rpX) + (newZ - zArchF) * (newZ - zArchF))
                if (rpDist < (0.15f + playerRadius)) {
                    return resolveCircleCollision(newX, newZ, oldX, oldZ, rpX, zArchF, 0.15f + playerRadius)
                }
            }

            // 2. Check Static Wooden Mine Cart (Z = 24.0f)
            if (kotlin.math.abs(newZ - 24.0f) < 1.5f) {
                val cartX = getPathX(24.0f) + 1.8f
                // Cart extents: width=1.2 (X), depth=0.8 (Z)
                val minX = cartX - 0.6f - playerRadius
                val maxX = cartX + 0.6f + playerRadius
                val minZ = 24.0f - 0.4f - playerRadius
                val maxZ = 24.0f + 0.4f + playerRadius
                if (newX in minX..maxX && newZ in minZ..maxZ) {
                    return Vector3(oldX, 0.4f, oldZ)
                }
            }

            // 3. Check Exit Gate Posts (Z = 75.0f)
            if (kotlin.math.abs(newZ - 75.0f) < 1.5f) {
                val pathX = getPathX(75.0f)
                // Left post
                val lpX = pathX - 2.8f
                val lpDist = kotlin.math.sqrt((newX - lpX) * (newX - lpX) + (newZ - 75.0f) * (newZ - 75.0f))
                if (lpDist < (0.15f + playerRadius)) {
                    return resolveCircleCollision(newX, newZ, oldX, oldZ, lpX, 75.0f, 0.15f + playerRadius)
                }

                // Right post
                val rpX = pathX + 2.8f
                val rpDist = kotlin.math.sqrt((newX - rpX) * (newX - rpX) + (newZ - 75.0f) * (newZ - 75.0f))
                if (rpDist < (0.15f + playerRadius)) {
                    return resolveCircleCollision(newX, newZ, oldX, oldZ, rpX, 75.0f, 0.15f + playerRadius)
                }
            }
        }
        return Vector3(newX, 0.4f, newZ)
    }

    private fun resolveCircleCollision(
        newX: Float, newZ: Float,
        oldX: Float, oldZ: Float,
        circleX: Float, circleZ: Float,
        minDist: Float
    ): Vector3 {
        val dx = newX - circleX
        val dz = newZ - circleZ
        val dist = kotlin.math.sqrt(dx * dx + dz * dz)
        if (dist < 0.001f) {
            return Vector3(oldX, 0.4f, oldZ)
        }
        val pushX = circleX + (dx / dist) * minDist
        val pushZ = circleZ + (dz / dist) * minDist
        return Vector3(pushX, 0.4f, pushZ)
    }

    private fun refocusCameraOnCharacter() {
        // Reset tilt offset
        sensorTiltX = 0f
        smoothedCameraTiltOffset = 0f
        
        // Reset camera positions OTS behind Jack
        val targetCamX = if (currentStageIndex == 0) {
            val pathX = getPathX(jackPos.z - 5.5f)
            pathX + (jackPos.x - getPathX(jackPos.z)) * 0.5f
        } else {
            jackPos.x + 0.9f
        }
        val targetCamY = jackPos.y + 1.4f
        val targetCamZ = jackPos.z - 5.5f
        renderer.cameraPos = Vector3(targetCamX, targetCamY, targetCamZ)
        
        val targetTargetX = if (currentStageIndex == 0) {
            jackPos.x
        } else {
            jackPos.x - 0.2f
        }
        renderer.cameraTarget = Vector3(targetTargetX, jackPos.y + 0.6f, jackPos.z + 1.8f)
        
        SoundManager.triggerSpecialCharge()
    }
}
