package com.thigazhini_labs.samuraijack

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import com.google.androidgamesdk.GameActivity
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.sin

enum class GameState {
    SPLASH,
    MAIN_MENU,
    INTRO_CUTSCENE,
    GAMEPLAY,
    OUTRO_CUTSCENE,
    GAME_OVER
}

class MainActivity : GameActivity(), SensorEventListener {

    companion object {
        init {
            System.loadLibrary("samuraijack")
        }
    }

    // Sensors for motion controls
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var sensorTiltX = 0f
    private var smoothedCameraTiltOffset = 0f

    // Core Game States
    var gameState by mutableStateOf(GameState.SPLASH)
    var currentStageIndex by mutableIntStateOf(0)
    var isStage1InsideMine by mutableStateOf(false)
    
    // Player statistics
    var playerHealth by mutableFloatStateOf(100f)
    var playerSwordEnergy by mutableFloatStateOf(0f)
    var isHealthRegenActive by mutableStateOf(false)
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
    private var targetCameraLookVec = Vector3(0f, 0f, 0f)
    private var cameraLookVec = Vector3(0f, 0f, 0f)
    private var cameraOrbitYawDeg = 180f
    private var cameraOrbitPitchDeg = 10f

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
    private var lastDamageTimestampMs = 0L

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
                else -> R.drawable.bg_stage1_outside
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
                    isStage1InsideMine = isStage1InsideMine,
                    playerHealth = playerHealth,
                    playerSwordEnergy = playerSwordEnergy,
                    isHealthRegenActive = isHealthRegenActive,
                    enemiesRemaining = enemiesRemaining,
                    typedText = typedText,
                    isTextFullyTyped = isTextFullyTyped,
                    jackState = jackState,
                    playerCoins = playerCoins,
                    playerCrystals = playerCrystals,
                    onStartGame = { startGame() },
                    onSelectStage = { enterStage(it) },
                    onRetryStage = { enterStage(currentStageIndex) },
                    onBackToMenu = {
                        gameState = GameState.MAIN_MENU
                        SoundManager.playMusic(this@MainActivity)
                    },
                    onAdvanceText = { advanceDialogue() },
                    onMove = { dx, dz -> targetJoystickMoveVec = Vector3(dx, 0f, dz) },
                    onLook = { dx, dz -> targetCameraLookVec = Vector3(dx, 0f, dz) },
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
                    val inLookZone = event.x > screenWidth * 0.78f && event.y < screenHeight * 0.30f
                    touchStartedInMaskedZone = inJoystickZone || inButtonsZone || inLookZone
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
                jack.isHero = true
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
        enterStage(0)
    }

    private fun enterStage(index: Int) {
        currentStageIndex = index
        SoundManager.stopMusic()
        gameState = GameState.INTRO_CUTSCENE

        mainScope.launch {
            setupStageEntities(index)
            startCutscene(index, true)
        }
    }

    private suspend fun setupStageEntities(index: Int) {
        playerHealth = 100f
        playerSwordEnergy = 0f
        isHealthRegenActive = false
        lastDamageTimestampMs = System.currentTimeMillis()
        isStage1InsideMine = false
        enemiesRemaining = 0
        jackPos = Vector3(0f, 0.4f, 0f)
        jackVelY = 0f
        isJumping = false
        jackState = "Idle"
        targetCameraLookVec = Vector3(0f, 0f, 0f)
        cameraLookVec = Vector3(0f, 0f, 0f)
        cameraOrbitPitchDeg = 10f


        enemyMeshes.clear()
        laserMeshes.clear()
        environmentMeshes.clear()

        val list = withContext(Dispatchers.IO) {
            val subList = mutableListOf<Mesh>()

            val gMesh = Models3D.createGround(12f, 80f, 0.2f, 0.25f, 0.3f)
            groundMesh = gMesh
            subList.add(gMesh)

            val normal = Models3D.loadObj(this@MainActivity, "samurai_model.obj", "samurai_model.mtl", scaleMultiplier = 2.0f, yOffset = 0.715f)
            val attacking = Models3D.loadObj(this@MainActivity, "samurai_model.obj", "samurai_model.mtl", scaleMultiplier = 2.0f, yOffset = 0.715f)
            normal.isHero = true
            attacking.isHero = true
            jackMeshNormal = normal
            jackMeshAttacking = attacking
            jackMesh = normal
            jackMesh.position = jackPos
            subList.add(jackMesh)

            val sky = Models3D.createSkySphere()
            sky.position = Vector3(0f, 0f, 0f)
            sky.isVisible = true
            skyMesh = sky
            subList.add(sky)

            bossMesh = null
            subList
        }

        bossHitsRemaining = 0
        enemiesRemaining = 0

        cameraOrbitYawDeg = 180f
        renderer.cameraPos = Vector3(0.9f, 1.8f, -5.5f)
        renderer.cameraTarget = Vector3(-0.2f, 1.0f, 1.8f)

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
                gameState = GameState.MAIN_MENU
            }
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
        val nowMs = System.currentTimeMillis()
        // Smoothly interpolate joystick vectors
        joystickMoveVec.x += (targetJoystickMoveVec.x - joystickMoveVec.x) * 0.18f
        joystickMoveVec.z += (targetJoystickMoveVec.z - joystickMoveVec.z) * 0.18f
        cameraLookVec.x += (targetCameraLookVec.x - cameraLookVec.x) * 0.16f
        cameraLookVec.z += (targetCameraLookVec.z - cameraLookVec.z) * 0.16f
        if (abs(joystickMoveVec.x) < 0.01f && targetJoystickMoveVec.x == 0f) joystickMoveVec.x = 0f
        if (abs(joystickMoveVec.z) < 0.01f && targetJoystickMoveVec.z == 0f) joystickMoveVec.z = 0f
        if (abs(cameraLookVec.x) < 0.01f && targetCameraLookVec.x == 0f) cameraLookVec.x = 0f
        if (abs(cameraLookVec.z) < 0.01f && targetCameraLookVec.z == 0f) cameraLookVec.z = 0f

        // Update animation timer
        if (jackState == "Run") {
            animTime += 0.15f
        } else {
            animTime = 0f
        }
        renderer.animTime = animTime

        // Health regeneration after 10 seconds without damage.
        val regenAllowed = gameState == GameState.GAMEPLAY &&
            playerHealth > 0f &&
            playerHealth < 100f &&
            (nowMs - lastDamageTimestampMs) >= 10_000L
        isHealthRegenActive = regenAllowed
        if (regenAllowed) {
            playerHealth = (playerHealth + 0.09f).coerceAtMost(100f)
        }
        if (currentStageIndex == 0) {
            val insideMine = jackPos.z >= 0f
            if (insideMine != isStage1InsideMine) {
                isStage1InsideMine = insideMine
            }
        } else if (isStage1InsideMine) {
            isStage1InsideMine = false
        }

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
            val moveSpeed = 0.14f
            val limitZ = floatArrayOf(-35f, 35f)
            val limitX = floatArrayOf(-4.5f, 4.5f)
            val targetX = (jackPos.x + joystickMoveVec.x * moveSpeed).coerceIn(limitX[0], limitX[1])
            val targetZ = (jackPos.z + joystickMoveVec.z * moveSpeed).coerceIn(limitZ[0], limitZ[1])
            
            val resolvedPos = checkCollision(targetX, targetZ, jackPos.x, jackPos.z)
            jackPos.x = resolvedPos.x
            jackPos.z = resolvedPos.z

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

        // 3. Camera orbit with auto-follow and full 360 look control.
        val lookActive = kotlin.math.abs(cameraLookVec.x) > 0.03f || kotlin.math.abs(cameraLookVec.z) > 0.03f
        val movementYaw = if (jackState == "Run") {
            jackMesh.rotation.y
        } else {
            jackMesh.rotation.y
        }
        if (lookActive) {
            cameraOrbitYawDeg = normalizeAngleDeg(cameraOrbitYawDeg + cameraLookVec.x * 4.6f)
            val minPitch = -20f
            val maxPitch = 28f
            cameraOrbitPitchDeg = (cameraOrbitPitchDeg + cameraLookVec.z * 2.6f).coerceIn(minPitch, maxPitch)
        } else {
            val desiredYaw = normalizeAngleDeg(movementYaw + 180f)
            val alignSpeed = if (currentStageIndex == 0) 0.26f else 0.18f
            cameraOrbitYawDeg = lerpAngleDeg(cameraOrbitYawDeg, desiredYaw, alignSpeed)
            cameraOrbitPitchDeg += (11f - cameraOrbitPitchDeg) * 0.16f
        }
        val camDistance = if (currentStageIndex == 0) 4.2f else 5.6f
        val yawRad = Math.toRadians(cameraOrbitYawDeg.toDouble())
        val pitchRad = Math.toRadians(cameraOrbitPitchDeg.toDouble())
        val cosPitch = kotlin.math.cos(pitchRad).toFloat()
        var targetCamX = jackPos.x + (kotlin.math.sin(yawRad).toFloat() * cosPitch * camDistance)
        val targetCamY = jackPos.y + 1.0f + (kotlin.math.sin(pitchRad).toFloat() * camDistance)
        val targetCamZ = jackPos.z + (kotlin.math.cos(yawRad).toFloat() * cosPitch * camDistance)
        val cameraLerp = 0.14f
        renderer.cameraPos.x += (targetCamX - renderer.cameraPos.x) * cameraLerp
        renderer.cameraPos.y += (targetCamY - renderer.cameraPos.y) * cameraLerp
        renderer.cameraPos.z += (targetCamZ - renderer.cameraPos.z) * cameraLerp

        // Apply device tilt/motion to shift camera focus (target looking direction) left/right
        val tiltStrength = 0.3f
        val tiltRange = 2.5f
        val targetTiltOffset = (-sensorTiltX * tiltStrength).coerceIn(-tiltRange, tiltRange)
        smoothedCameraTiltOffset += (targetTiltOffset - smoothedCameraTiltOffset) * 0.1f
        
        val lookYawDeg = if (lookActive) normalizeAngleDeg(cameraOrbitYawDeg + 180f) else movementYaw
        val lookYawRad = Math.toRadians(lookYawDeg.toDouble())
        val targetTargetX = jackPos.x + kotlin.math.sin(lookYawRad).toFloat() * 1.8f + smoothedCameraTiltOffset
        val targetTargetZ = jackPos.z + kotlin.math.cos(lookYawRad).toFloat() * 1.8f
        val flashOffset = if (specialFlashActive) (sin(specialFlashTime * 50f) * 0.2f) else 0f
        val adjustedTargetX = targetTargetX + flashOffset
        renderer.cameraTarget = Vector3(adjustedTargetX, jackPos.y + 0.68f, targetTargetZ)

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
        val winZ = 14.2f
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
        lastDamageTimestampMs = System.currentTimeMillis()
        isHealthRegenActive = false
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

    private fun checkCollision(newX: Float, newZ: Float, oldX: Float, oldZ: Float): Vector3 {
        return Vector3(newX, 0.4f, newZ)
    }

    private fun refocusCameraOnCharacter() {
        // Reset tilt offset
        sensorTiltX = 0f
        smoothedCameraTiltOffset = 0f
        targetCameraLookVec = Vector3(0f, 0f, 0f)
        cameraLookVec = Vector3(0f, 0f, 0f)
        cameraOrbitPitchDeg = 10f
        
        // Reset camera positions OTS behind Jack
        val targetCamX = jackPos.x + 0.9f
        val targetCamY = jackPos.y + 1.4f
        val targetCamZ = jackPos.z - 5.5f
        cameraOrbitYawDeg = normalizeAngleDeg(180f)
        renderer.cameraPos = Vector3(targetCamX, targetCamY, targetCamZ)
        renderer.cameraTarget = Vector3(jackPos.x, jackPos.y + 0.6f, jackPos.z + 1.8f)
        
        SoundManager.triggerSpecialCharge()
    }

    private fun normalizeAngleDeg(angle: Float): Float {
        var a = angle % 360f
        if (a < 0f) a += 360f
        return a
    }

    private fun lerpAngleDeg(current: Float, target: Float, t: Float): Float {
        var delta = (target - current + 540f) % 360f - 180f
        if (delta > 180f) delta -= 360f
        return normalizeAngleDeg(current + delta * t.coerceIn(0f, 1f))
    }
}
