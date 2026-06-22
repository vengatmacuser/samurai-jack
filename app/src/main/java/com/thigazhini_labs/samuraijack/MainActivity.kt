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
    var unlockedStageCount by mutableIntStateOf(1)
    
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
    private var joystickMoveVec = Vector3(0f, 0f, 0f)

    // Gameplay parameters
    private var jackPos = Vector3(0f, 0f, 0f)
    private var jackVelY = 0f
    private var isJumping = false
    var jackState by mutableStateOf("Idle") // Idle, Run, Jump, Attack, Block, Hurt
    private var jackActionTime = 0f
    private var hitFlashTimer = 0f
    private var specialFlashActive = false
    private var specialFlashTime = 0f

    // Touch gesture tracking variables
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchStartTime = 0L
    private val swipeThreshold = 80f
    private val tapThresholdTime = 220L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                GameState.SPLASH, GameState.MAIN_MENU, GameState.STAGE_SELECT -> R.drawable.red_eyes
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
                    },
                    onAdvanceText = { advanceDialogue() },
                    onMove = { dx, dz -> joystickMoveVec = Vector3(dx, 0f, dz) },
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

    // Touch gesture parser mapping gestures directly to actions
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
                    jackPos.x = (diffX / 100f).coerceIn(-4.5f, 4.5f)
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

    private fun buildMainMenuScene() {
        val list = mutableListOf<Mesh>()
        // Torii gate at the center
        val torii = Models3D.createToriiGate()
        torii.position = Vector3(0f, 0f, 2f)
        torii.scale = Vector3(0.8f, 0.8f, 0.8f)
        list.add(torii)

        // Add ground
        val ground = Models3D.createGround(20f, 20f, 0.2f, 0.15f, 0.15f)
        ground.position = Vector3(0f, 0f, 0f)
        list.add(ground)

        // Static Jack
        val jack = Models3D.createJack()
        jack.position = Vector3(0f, 0.4f, 0f)
        list.add(jack)

        // Set camera
        renderer.cameraPos = Vector3(0f, 1.8f, -4.5f)
        renderer.cameraTarget = Vector3(0f, 0.6f, 0.5f)

        renderer.renderMeshes.clear()
        renderer.renderMeshes.addAll(list)
    }

    private fun startGame() {
        gameState = GameState.STAGE_SELECT
    }

    private fun enterStage(index: Int) {
        currentStageIndex = index
        SoundManager.currentStage = index + 1
        gameState = GameState.TRAVEL_TRANSITION

        mainScope.launch {
            delay(2000)
            setupStageEntities(index)
            startCutscene(index, true)
        }
    }

    private fun setupStageEntities(index: Int) {
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

        val list = mutableListOf<Mesh>()

        // 1. Create Ground
        val gc = stage.groundColor
        groundMesh = Models3D.createGround(12f, 80f, gc[0], gc[1], gc[2])
        groundMesh?.let { list.add(it) }

        // 2. Create Jack
        jackMeshNormal = Models3D.createJack(isAttacking = false)
        jackMeshAttacking = Models3D.createJack(isAttacking = true)
        jackMesh = jackMeshNormal
        jackMesh.position = jackPos
        list.add(jackMesh)

        // Textured 2D Sky Backdrop mapping stage image
        skyMesh = Models3D.createTexturedSkyBackdrop()
        skyMesh.position = Vector3(0f, 0f, 0f)
        skyMesh.isVisible = true
        list.add(skyMesh)

        // 3. Environment structures based on theme
        if (stage.stageNumber in listOf(1, 2, 4, 13)) {
            // Ancient Pagodas
            val gate = Models3D.createToriiGate()
            gate.position = Vector3(0f, 0f, 15f)
            list.add(gate)
        }

        // 4. Create enemies (DISABLED - EXPLORATION MODE)
        bossMesh = null
        enemiesRemaining = 1

        renderer.renderMeshes.clear()
        renderer.renderMeshes.addAll(list)
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
                if (dist < attackRange) {
                    triggerHitResponse(boss)
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
        jackPos.x = (jackPos.x + dir * 1.5f).coerceIn(-4.5f, 4.5f)
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
            // Halve boss health
            playerHealth = (playerHealth + 30f).coerceAtMost(100f)
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

        // Apply joystick movement vectors
        if (joystickMoveVec.x != 0f || joystickMoveVec.z != 0f) {
            val moveSpeed = 0.06f
            jackPos.x = (jackPos.x + joystickMoveVec.x * moveSpeed).coerceIn(-4.5f, 4.5f)
            jackPos.z = (jackPos.z + joystickMoveVec.z * moveSpeed).coerceIn(-35f, 35f)

            // Dynamic rotation Y so Jack faces movement direction
            val angleRad = kotlin.math.atan2(joystickMoveVec.x.toDouble(), joystickMoveVec.z.toDouble())
            val angleDeg = Math.toDegrees(angleRad).toFloat()
            jackMeshNormal.rotation.y = angleDeg
            jackMeshAttacking.rotation.y = angleDeg

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
            // Rapidly flicker camera target
            renderer.cameraTarget.x = (sin(specialFlashTime * 50f) * 0.2f)
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
        jackMesh.silhouetteMode = if (specialFlashActive) 1 else 0

        skyMesh.position.x = jackPos.x
        skyMesh.position.z = jackPos.z

        // 3. Move Camera smoothly behind Jack (Third-Person OTS orbit)
        val targetCamX = jackPos.x + 0.9f
        val targetCamY = jackPos.y + 1.4f
        val targetCamZ = jackPos.z - 5.5f
        renderer.cameraPos.x += (targetCamX - renderer.cameraPos.x) * 0.1f
        renderer.cameraPos.y += (targetCamY - renderer.cameraPos.y) * 0.1f
        renderer.cameraPos.z += (targetCamZ - renderer.cameraPos.z) * 0.1f

        // Apply device tilt/motion to shift camera focus (target looking direction) left/right
        val targetTiltOffset = (-sensorTiltX * 0.3f).coerceIn(-2.5f, 2.5f)
        smoothedCameraTiltOffset += (targetTiltOffset - smoothedCameraTiltOffset) * 0.1f
        renderer.cameraTarget = Vector3(jackPos.x - 0.2f + smoothedCameraTiltOffset, jackPos.y + 0.6f, jackPos.z + 1.8f)

        // Update current rendering stage details
        renderer.currentStageIndex = currentStageIndex
        renderer.hitFlashAmount = hitFlashTimer
        if (hitFlashTimer > 0f) {
            hitFlashTimer -= 0.05f
        }

        // Low health heartbeat triggers
        SoundManager.isLowHealthHeartbeatActive = playerHealth < 35f

        // 4. Update Enemy Behaviors (DISABLED - EXPLORATION MODE)

        // 5. Update Projectiles (DISABLED - EXPLORATION MODE)

        // 6. Check Win conditions (EXPLORATION GATE EXIT TRIGGER)
        if (jackPos.z >= 14.2f && enemiesRemaining > 0) {
            enemiesRemaining = 0
            gameState = GameState.OUTRO_CUTSCENE
            startCutscene(currentStageIndex, false)
            enemiesRemaining = -1 // Stop duplicate triggers
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
}
