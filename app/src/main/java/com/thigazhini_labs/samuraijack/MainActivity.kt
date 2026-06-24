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
    STAGE_SELECT,
    TRAVEL_TRANSITION,
    INTRO_CUTSCENE,
    GAMEPLAY,
    OUTRO_CUTSCENE,
    GAME_OVER,
    GAME_COMPLETE
}

data class MineObstacle(val pathRelX: Float, val pathZ: Float, val radius: Float)

class MainActivity : GameActivity(), SensorEventListener {

    companion object {
        init {
            System.loadLibrary("main")
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
    var unlockedStageCount by mutableIntStateOf(13)
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
    private val mineLanternAnchors = mutableListOf<Vector3>()
    private val mineCrystalAnchors = mutableListOf<Vector3>()
    private val mineObstacleCircles = mutableListOf<MineObstacle>()
    private var groundMesh: Mesh? = null
    private var bossMesh: Mesh? = null

    // Controls
    private var targetJoystickMoveVec = Vector3(0f, 0f, 0f)
    private var joystickMoveVec = Vector3(0f, 0f, 0f)
    private var targetCameraLookVec = Vector3(0f, 0f, 0f)
    private var cameraLookVec = Vector3(0f, 0f, 0f)
    private var cameraOrbitYawDeg = 180f
    private var cameraOrbitPitchDeg = 10f
    private var thunderPulse = 0f
    private var nextThunderAtMs = 0L
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
                GameState.STAGE_SELECT -> R.drawable.bg_map
                else -> {
                    when (currentStageIndex) {
                        0 -> if (isStage1InsideMine) R.drawable.bg_frosthollow else R.drawable.bg_stage1_outside
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
                    isStage1InsideMine = isStage1InsideMine,
                    unlockedStageCount = unlockedStageCount,
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
        isHealthRegenActive = false
        lastDamageTimestampMs = System.currentTimeMillis()
        isStage1InsideMine = index != 0
        enemiesRemaining = 0
        jackPos = if (index == 0) Vector3(0f, 0.4f, -120f) else Vector3(0f, 0.4f, 0f)
        jackVelY = 0f
        isJumping = false
        jackState = "Idle"
        targetCameraLookVec = Vector3(0f, 0f, 0f)
        cameraLookVec = Vector3(0f, 0f, 0f)
        cameraOrbitPitchDeg = 10f
        thunderPulse = 0f
        nextThunderAtMs = 0L

        enemyMeshes.clear()
        laserMeshes.clear()
        environmentMeshes.clear()
        mineLanternAnchors.clear()
        mineCrystalAnchors.clear()
        mineObstacleCircles.clear()

        val list = withContext(Dispatchers.IO) {
            val subList = mutableListOf<Mesh>()

            // 1. Create Ground (use custom 3D mesh for Stage 1 Frosthollow Mine)
            val gc = stage.groundColor
            val gMesh = if (index == 0) {
                Models3D.createGround(18f, 420f, 0.06f, 0.06f, 0.07f)
            } else {
                Models3D.createGround(12f, 80f, gc[0], gc[1], gc[2])
            }
            groundMesh = gMesh
            subList.add(gMesh)

            // 2. Create Jack
            val normal = Models3D.loadObj(this@MainActivity, "samurai_model.obj", "samurai_model.mtl", scaleMultiplier = 2.0f, yOffset = 0.715f)
            val attacking = Models3D.loadObj(this@MainActivity, "samurai_model.obj", "samurai_model.mtl", scaleMultiplier = 2.0f, yOffset = 0.715f)
            normal.isHero = true
            attacking.isHero = true
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

            if (index == 0) {
                val tunnelTemplate = Models3D.createTunnelRockModule()
                val ceilingTemplate = Models3D.createCaveCeilingSection(ceilY = 4.0f)
                val exteriorRailTemplate = Models3D.createMiningRailSection(length = 8f, sleeperCount = 6)
                val caveRailTemplate = Models3D.createMiningRailSection(length = 8f, sleeperCount = 7)
                val batTemplate = Models3D.createCaveBat()
                // Long Antarctic approach path to the mine entrance
                for (zExt in -140..-20 step 10) {
                    val zExtF = zExt.toFloat()
                    val extPathX = getPathX(zExtF)

                    val leftCliff = Models3D.createIceMountainFacade()
                    leftCliff.position = Vector3(extPathX - 7.6f, 0f, zExtF)
                    leftCliff.rotation.y = 26f
                    leftCliff.scale = Vector3(1.05f, 1.15f, 1.05f)
                    environmentMeshes.add(leftCliff)
                    subList.add(leftCliff)

                    val rightCliff = Models3D.createIceMountainFacade()
                    rightCliff.position = Vector3(extPathX + 7.6f, 0f, zExtF + 1.5f)
                    rightCliff.rotation.y = -24f
                    rightCliff.scale = Vector3(0.98f, 1.08f, 0.98f)
                    environmentMeshes.add(rightCliff)
                    subList.add(rightCliff)

                    val driftA = Models3D.createSnowDriftPatch()
                    driftA.position = Vector3(extPathX - 2.7f, 0f, zExtF - 1.8f)
                    driftA.scale = Vector3(1.1f, 1.0f, 1.1f)
                    environmentMeshes.add(driftA)
                    subList.add(driftA)

                    val driftB = Models3D.createSnowDriftPatch()
                    driftB.position = Vector3(extPathX + 2.8f, 0f, zExtF + 1.6f)
                    driftB.scale = Vector3(0.95f, 0.9f, 0.95f)
                    environmentMeshes.add(driftB)
                    subList.add(driftB)

                    if (zExt % 20 == 0) {
                        val warning = Models3D.createWarningSign()
                        warning.position = Vector3(extPathX + if ((zExt / 20) % 2 == 0) 3.6f else -3.6f, 0f, zExtF + 0.8f)
                        warning.rotation.y = if ((zExt / 20) % 2 == 0) -18f else 18f
                        environmentMeshes.add(warning)
                        subList.add(warning)
                    }

                    if (zExt % 30 == 0) {
                        val abandonedCamp = Models3D.createWoodenPlatform()
                        abandonedCamp.position = Vector3(extPathX + if ((zExt / 10) % 2 == 0) 4.7f else -4.7f, 0f, zExtF - 0.6f)
                        abandonedCamp.rotation.y = if ((zExt / 10) % 2 == 0) -32f else 30f
                        environmentMeshes.add(abandonedCamp)
                        subList.add(abandonedCamp)

                        val campProps = Models3D.createMiningSupplyCluster()
                        campProps.position = Vector3(extPathX + if ((zExt / 10) % 2 == 0) 4.35f else -4.35f, 0f, zExtF + 0.5f)
                        environmentMeshes.add(campProps)
                        subList.add(campProps)
                    }

                    if (zExt % 20 == 0) {
                        val hiddenCrystal = Models3D.createIceCrystalCluster()
                        hiddenCrystal.position = Vector3(extPathX + if ((zExt / 20) % 2 == 0) 5.3f else -5.3f, 0f, zExtF - 1.4f)
                        hiddenCrystal.scale = Vector3(0.82f, 0.9f, 0.82f)
                        environmentMeshes.add(hiddenCrystal)
                        subList.add(hiddenCrystal)
                    }

                    val blizzardFog = Models3D.createMineFogBank()
                    blizzardFog.position = Vector3(extPathX + if ((zExt / 10) % 2 == 0) -0.9f else 0.9f, 0f, zExtF)
                    blizzardFog.scale = Vector3(0.9f, 0.86f, 0.9f)
                    environmentMeshes.add(blizzardFog)
                    subList.add(blizzardFog)

                    val windSnow = Models3D.createDustMoteCluster()
                    windSnow.position = Vector3(extPathX + if ((zExt / 10) % 2 == 0) 1.4f else -1.4f, 0f, zExtF + 1.0f)
                    windSnow.scale = Vector3(1.1f, 1.1f, 1.1f)
                    environmentMeshes.add(windSnow)
                    subList.add(windSnow)
                }

                for (zTrack in -124..-24 step 8) {
                    val zTrackF = zTrack.toFloat()
                    val tpX = getPathX(zTrackF)
                    val rail = exteriorRailTemplate.createInstance()
                    rail.position = Vector3(tpX, 0f, zTrackF)
                    environmentMeshes.add(rail)
                    subList.add(rail)

                    val driftOverTrack = Models3D.createSnowDriftPatch()
                    driftOverTrack.position = Vector3(tpX + if ((zTrack / 8) % 2 == 0) -0.55f else 0.55f, 0f, zTrackF + 0.6f)
                    driftOverTrack.scale = Vector3(0.75f, 0.72f, 0.75f)
                    environmentMeshes.add(driftOverTrack)
                    subList.add(driftOverTrack)
                }

                for (z in -12..1500 step 12) {
                    val zF = z.toFloat()
                    val pathX = getPathX(zF)
                    val yaw = getPathYaw(zF)

                    val tunnelModule = tunnelTemplate.createInstance()
                    tunnelModule.position = Vector3(pathX, 0f, zF)
                    tunnelModule.rotation.y = yaw
                    environmentMeshes.add(tunnelModule)
                    subList.add(tunnelModule)

                    // Ceiling detail every module
                    val ceiling = ceilingTemplate.createInstance()
                    ceiling.position = Vector3(pathX, 0f, zF)
                    ceiling.rotation.y = yaw
                    environmentMeshes.add(ceiling)
                    subList.add(ceiling)

                }

                // Continuous rail spine through the mine to avoid visible gaps.
                for (zRail in -12..1500 step 8) {
                    val zRailF = zRail.toFloat()
                    val railPathX = getPathX(zRailF)
                    val railYaw = getPathYaw(zRailF)
                    val railSection = caveRailTemplate.createInstance()
                    railSection.position = Vector3(railPathX, 0f, zRailF)
                    railSection.rotation.y = railYaw
                    environmentMeshes.add(railSection)
                    subList.add(railSection)
                }

                // Mountain base and entrance approach
                val mountainCore = Models3D.createIceMountainFacade()
                mountainCore.position = Vector3(getPathX(-18f) - 10.8f, 0f, -18f)
                mountainCore.rotation.y = 18f
                mountainCore.scale = Vector3(1.75f, 2.05f, 1.85f)
                environmentMeshes.add(mountainCore)
                subList.add(mountainCore)

                for (zExt in -36..-8 step 8) {
                    val zExtF = zExt.toFloat()
                    val px = getPathX(zExtF)
                    val leftGlacier = Models3D.createIceMountainFacade()
                    leftGlacier.position = Vector3(px - 6.0f, 0f, zExtF - 1.2f)
                    leftGlacier.rotation.y = 22f
                    leftGlacier.scale = Vector3(0.92f, 0.95f, 0.9f)
                    environmentMeshes.add(leftGlacier)
                    subList.add(leftGlacier)

                    val rightGlacier = Models3D.createIceMountainFacade()
                    rightGlacier.position = Vector3(px + 6.0f, 0f, zExtF + 1.2f)
                    rightGlacier.rotation.y = -22f
                    rightGlacier.scale = Vector3(0.88f, 0.92f, 0.88f)
                    environmentMeshes.add(rightGlacier)
                    subList.add(rightGlacier)
                }

                val mineSign = Models3D.createMineEntranceSignAku()
                mineSign.position = Vector3(getPathX(-5f) - 3.55f, 0f, -5f)
                mineSign.rotation.y = 10f
                environmentMeshes.add(mineSign)
                subList.add(mineSign)

                val entranceFrame = Models3D.createMineSupportFrame(width = 6.4f, height = 4.5f, depth = 0.82f)
                entranceFrame.position = Vector3(getPathX(-2f), 0f, -2f)
                environmentMeshes.add(entranceFrame)
                subList.add(entranceFrame)

                val frozenCart = Models3D.createMineCartDetailed(overturned = false, filled = false, damaged = true)
                frozenCart.position = Vector3(getPathX(-6f) + 2.3f, 0f, -6f)
                frozenCart.rotation.y = -18f
                environmentMeshes.add(frozenCart)
                subList.add(frozenCart)

                val entranceSupplies = Models3D.createMiningSupplyCluster()
                entranceSupplies.position = Vector3(getPathX(-4f) - 3.2f, 0f, -4f)
                entranceSupplies.rotation.y = 18f
                environmentMeshes.add(entranceSupplies)
                subList.add(entranceSupplies)

                val entranceLantern = Models3D.createLanternStand(1f, 0.72f, 0.24f)
                entranceLantern.position = Vector3(getPathX(-2f) + 3.05f, 0f, -2.4f)
                environmentMeshes.add(entranceLantern)
                subList.add(entranceLantern)
                mineLanternAnchors.add(Vector3(getPathX(-2f) + 3.05f, 1.0f, -2.4f))

                for (zEntrance in listOf(-10f, -6f, -2f)) {
                    val pX = getPathX(zEntrance)
                    val entranceIcicles = Models3D.createIcicleHazardCluster()
                    entranceIcicles.position = Vector3(pX + if (zEntrance < 0f) -2.35f else 2.35f, 0f, zEntrance - 0.15f)
                    environmentMeshes.add(entranceIcicles)
                    subList.add(entranceIcicles)

                    val drift = Models3D.createSnowDriftPatch()
                    drift.position = Vector3(pX + if (zEntrance < 0f) 2.8f else -2.8f, 0f, zEntrance + 0.5f)
                    environmentMeshes.add(drift)
                    subList.add(drift)
                }

                for (zArch in -12..1500 step 18) {
                    val zF = zArch.toFloat()
                    val pathX = getPathX(zF)

                    if (zArch % 36 == 0) {
                        val warmLanternL = Models3D.createMineLanternGlow(0.98f, 0.78f, 0.34f)
                        val leftLanternPos = Vector3(pathX - 2.62f, 0f, zF + 0.35f)
                        warmLanternL.position = leftLanternPos
                        environmentMeshes.add(warmLanternL)
                        subList.add(warmLanternL)
                        mineLanternAnchors.add(Vector3(leftLanternPos.x, 0.95f, leftLanternPos.z))

                        val warmLanternR = Models3D.createMineLanternGlow(0.96f, 0.72f, 0.3f)
                        val rightLanternPos = Vector3(pathX + 2.62f, 0f, zF - 0.35f)
                        warmLanternR.position = rightLanternPos
                        environmentMeshes.add(warmLanternR)
                        subList.add(warmLanternR)
                        mineLanternAnchors.add(Vector3(rightLanternPos.x, 0.95f, rightLanternPos.z))
                    }
                }

                for (z in -10..84 step 4) {
                    val zF = z.toFloat()
                    val pathX = getPathX(zF)
                    val coldFactor = ((22f - zF) / 22f).coerceIn(0f, 1f)
                    val corruptionFactor = ((zF - 42f) / 42f).coerceIn(0f, 1f)
                    val leftPile = Models3D.createCoalPile(radius = 0.72f, heightScale = 1.22f)
                    leftPile.position = Vector3(pathX - 3.2f, 0f, zF + 0.5f)
                    environmentMeshes.add(leftPile)
                    subList.add(leftPile)

                    val rightPile = Models3D.createCoalPile(radius = 0.64f, heightScale = 1.08f)
                    rightPile.position = Vector3(pathX + 3.2f, 0f, zF - 0.4f)
                    environmentMeshes.add(rightPile)
                    subList.add(rightPile)

                    val centerChunks = Models3D.createCoalDebrisPatch(size = 0.98f)
                    centerChunks.position = Vector3(pathX + if ((z / 4) % 2 == 0) 0.4f else -0.35f, 0f, zF - 0.2f)
                    environmentMeshes.add(centerChunks)
                    subList.add(centerChunks)

                    if (z % 8 == 0) {
                        val debris = Models3D.createCoalDebrisPatch(size = 0.92f)
                        debris.position = Vector3(pathX + if ((z / 4) % 2 == 0) 1.6f else -1.6f, 0f, zF)
                        environmentMeshes.add(debris)
                        subList.add(debris)
                    }

                    if (z % 6 == 0) {
                        val fineBallast = Models3D.createCoalDebrisPatch(size = 0.74f)
                        fineBallast.position = Vector3(pathX + if ((z / 6) % 2 == 0) 0.24f else -0.24f, 0f, zF + 0.65f)
                        environmentMeshes.add(fineBallast)
                        subList.add(fineBallast)
                    }

                    if (z % 4 == 0 && zF <= 44f) {
                        val dust = Models3D.createDustMoteCluster()
                        dust.position = Vector3(pathX + if ((z / 12) % 2 == 0) 0.9f else -0.9f, 0f, zF + 1.1f)
                        environmentMeshes.add(dust)
                        subList.add(dust)
                    }

                    if (z % 8 == 0 && zF <= 36f) {
                        val fog = Models3D.createMineFogBank()
                        fog.position = Vector3(pathX + if ((z / 8) % 2 == 0) 0.5f else -0.5f, 0f, zF + 0.6f)
                        environmentMeshes.add(fog)
                        subList.add(fog)
                    }

                    if (z % 8 == 0 && zF <= 36f) {
                        val fogLayer = Models3D.createMineFogBank()
                        fogLayer.position = Vector3(pathX + if ((z / 8) % 2 == 0) -0.95f else 0.95f, 0f, zF - 0.25f)
                        fogLayer.scale = Vector3(0.78f, 0.74f, 0.78f)
                        environmentMeshes.add(fogLayer)
                        subList.add(fogLayer)
                    }

                    if (z % 12 == 0) {
                        val smoke = Models3D.createCoalSmokePlume()
                        smoke.position = Vector3(pathX + if ((z / 12) % 2 == 0) 2.05f else -2.05f, 0f, zF - 0.5f)
                        environmentMeshes.add(smoke)
                        subList.add(smoke)
                    }

                    if (z % 8 == 0) {
                        val rubble = Models3D.createMineRubbleMound()
                        rubble.position = Vector3(pathX + if ((z / 8) % 2 == 0) 2.35f else -2.35f, 0f, zF - 0.8f)
                        environmentMeshes.add(rubble)
                        subList.add(rubble)
                    }

                    if (coldFactor > 0.1f && z % 8 == 0) {
                        val snow = Models3D.createSnowDriftPatch()
                        snow.position = Vector3(pathX + if ((z / 8) % 2 == 0) -1.8f else 1.8f, 0f, zF + 0.3f)
                        snow.scale = Vector3(0.72f + coldFactor * 0.35f, 0.7f + coldFactor * 0.3f, 0.72f + coldFactor * 0.35f)
                        environmentMeshes.add(snow)
                        subList.add(snow)

                        val icePatch = Models3D.createFrozenPuddlePatch()
                        icePatch.position = Vector3(pathX + if ((z / 8) % 2 == 0) -0.68f else 0.68f, 0f, zF - 0.45f)
                        icePatch.scale = Vector3(0.74f + coldFactor * 0.25f, 1f, 0.74f + coldFactor * 0.25f)
                        environmentMeshes.add(icePatch)
                        subList.add(icePatch)
                    }

                    if (corruptionFactor > 0.08f && z % 6 == 0) {
                        val corruptedCrystal = Models3D.createCorruptedCrystalCluster()
                        val side = if ((z / 6) % 2 == 0) 3.05f else -3.05f
                        corruptedCrystal.position = Vector3(pathX + side, 0f, zF - 0.2f)
                        corruptedCrystal.scale = Vector3(
                            0.72f + corruptionFactor * 0.85f,
                            0.75f + corruptionFactor * 0.95f,
                            0.72f + corruptionFactor * 0.85f
                        )
                        corruptedCrystal.rotation.y = if (side > 0f) -20f else 20f
                        environmentMeshes.add(corruptedCrystal)
                        subList.add(corruptedCrystal)
                        if (zF >= 46f) {
                            mineCrystalAnchors.add(Vector3(pathX + side * 0.9f, 0.95f, zF - 0.15f))
                        }
                    }
                }


                val overturnedCart = Models3D.createMineCartDetailed(overturned = true, filled = true)
                overturnedCart.position = Vector3(getPathX(52f) - 1.8f, 0.0f, 52f)
                overturnedCart.rotation.y = 32f
                overturnedCart.rotation.z = 18f
                environmentMeshes.add(overturnedCart)
                subList.add(overturnedCart)

                val parkedCart = Models3D.createMineCartDetailed(overturned = false, filled = true, damaged = true)
                parkedCart.position = Vector3(getPathX(34f) - 2.1f, 0f, 34f)
                parkedCart.rotation.y = -18f
                environmentMeshes.add(parkedCart)
                subList.add(parkedCart)

                val derailedCart = Models3D.createMineCartDetailed(overturned = true, filled = false, damaged = true)
                derailedCart.position = Vector3(getPathX(66f) + 2.15f, 0f, 66f)
                derailedCart.rotation.y = 54f
                derailedCart.rotation.z = 24f
                environmentMeshes.add(derailedCart)
                subList.add(derailedCart)

                val artifact = Models3D.createAncientMineArtifact()
                artifact.position = Vector3(getPathX(30f) - 3.35f, 0f, 30f)
                environmentMeshes.add(artifact)
                subList.add(artifact)

                val tools = Models3D.createAbandonedToolSet()
                tools.position = Vector3(getPathX(46f) + 3.15f, 0f, 46f)
                tools.rotation.y = -40f
                environmentMeshes.add(tools)
                subList.add(tools)

                val supplyClusterA = Models3D.createMiningSupplyCluster()
                supplyClusterA.position = Vector3(getPathX(18f) - 3.3f, 0f, 18f)
                supplyClusterA.rotation.y = 18f
                environmentMeshes.add(supplyClusterA)
                subList.add(supplyClusterA)

                val supplyClusterB = Models3D.createMiningSupplyCluster()
                supplyClusterB.position = Vector3(getPathX(58f) + 3.28f, 0f, 58f)
                supplyClusterB.rotation.y = -22f
                environmentMeshes.add(supplyClusterB)
                subList.add(supplyClusterB)

                // Coal carts distributed along side tracks/supports: empty, filled and broken variants
                for (zCart in listOf(6f, 14f, 22f, 36f, 48f, 62f, 74f)) {
                    val cpX = getPathX(zCart)
                    val side = if ((zCart.toInt() / 2) % 2 == 0) 2.95f else -2.95f
                    val cartV = Models3D.createMineCartDetailed(
                        overturned = zCart == 22f || zCart == 62f,
                        filled = zCart != 14f && zCart != 48f,
                        damaged = zCart >= 36f
                    )
                    cartV.position = Vector3(cpX + side, 0f, zCart)
                    cartV.rotation.y = if (side > 0f) -15f else 15f
                    environmentMeshes.add(cartV)
                    subList.add(cartV)
                }

                // Additional support-adjacent carts in the background to reinforce mine traffic
                for (zCart in listOf(10f, 28f, 44f, 58f, 80f)) {
                    val cpX = getPathX(zCart)
                    val side = if ((zCart.toInt() / 10) % 2 == 0) -3.25f else 3.25f
                    val supportCart = Models3D.createMineCartDetailed(
                        overturned = false,
                        filled = zCart % 20f != 0f,
                        damaged = zCart >= 58f
                    )
                    supportCart.position = Vector3(cpX + side, 0f, zCart - 0.4f)
                    supportCart.rotation.y = if (side > 0f) -24f else 22f
                    environmentMeshes.add(supportCart)
                    subList.add(supportCart)
                }

                // Ceiling lamps and warm route guidance through the full mine
                for (zCl in 0..1500 step 32) {
                    val clF = zCl.toFloat()
                    val clPathX = getPathX(clF)
                    val offsetX = when ((zCl / 24) % 3) { 0 -> 0f; 1 -> -1.2f; else -> 1.2f }
                    val lamp = Models3D.createCeilingLamp(0.98f, 0.74f, 0.26f)
                    lamp.position = Vector3(clPathX + offsetX, 0f, clF)
                    environmentMeshes.add(lamp)
                    subList.add(lamp)
                    mineLanternAnchors.add(Vector3(clPathX + offsetX, 2.84f, clF))
                }

                for (zBat in 24..1500 step 64) {
                    val zBatF = zBat.toFloat()
                    val batPathX = getPathX(zBatF)
                    for (i in 0..2) {
                        val bat = batTemplate.createInstance()
                        val side = if ((zBat / 32 + i) % 2 == 0) -1.7f else 1.7f
                        bat.position = Vector3(
                            batPathX + side + (i - 1) * 0.45f,
                            3.05f + (i % 2) * 0.35f,
                            zBatF + i * 0.9f
                        )
                        bat.rotation.y = if (side > 0f) -24f - i * 8f else 24f + i * 8f
                        bat.rotation.z = if (i % 2 == 0) 8f else -8f
                        bat.scale = Vector3(0.92f + i * 0.12f, 0.92f + i * 0.12f, 0.92f + i * 0.12f)
                        environmentMeshes.add(bat)
                        subList.add(bat)
                    }
                }

                for (zProp in -6..1500 step 72) {
                    val zPropF = zProp.toFloat()
                    val propPathX = getPathX(zPropF)
                    val side = if ((zProp / 6) % 2 == 0) 3.35f else -3.35f

                    val propCluster = Models3D.createMiningSupplyCluster()
                    propCluster.position = Vector3(propPathX + side * 2.8f, 0f, zPropF + 0.3f)
                    propCluster.rotation.y = if (side > 0f) -30f else 28f
                    environmentMeshes.add(propCluster)
                    subList.add(propCluster)
                }

                // Mid/deep cave dressing and progression after first handcrafted section
                for (zDeep in 96..1500 step 48) {
                    val zDeepF = zDeep.toFloat()
                    val deepPathX = getPathX(zDeepF)
                    val depthFactor = ((zDeepF - 96f) / 1404f).coerceIn(0f, 1f)

                    val debris = Models3D.createCoalDebrisPatch(size = 0.78f + depthFactor * 0.24f)
                    debris.position = Vector3(deepPathX + if ((zDeep / 48) % 2 == 0) 0.7f else -0.7f, 0f, zDeepF)
                    environmentMeshes.add(debris)
                    subList.add(debris)

                    if (zDeep % 48 == 0) {
                        val rubble = Models3D.createMineRubbleMound()
                        rubble.position = Vector3(deepPathX + if ((zDeep / 48) % 2 == 0) 2.2f else -2.2f, 0f, zDeepF - 0.8f)
                        environmentMeshes.add(rubble)
                        subList.add(rubble)
                    }

                    val crystal = Models3D.createCorruptedCrystalCluster()
                    val crystalSide = if ((zDeep / 24) % 2 == 0) 3.1f else -3.1f
                    crystal.position = Vector3(deepPathX + crystalSide, 0f, zDeepF + 0.2f)
                    crystal.scale = Vector3(
                        0.9f + depthFactor * 1.1f,
                        1.0f + depthFactor * 1.2f,
                        0.9f + depthFactor * 1.1f
                    )
                    crystal.rotation.y = if (crystalSide > 0f) -18f else 18f
                    environmentMeshes.add(crystal)
                    subList.add(crystal)
                    mineCrystalAnchors.add(Vector3(deepPathX + crystalSide * 0.9f, 1.0f, zDeepF + 0.2f))

                    if (zDeep % 144 == 0) {
                        val fogDeep = Models3D.createMineFogBank()
                        fogDeep.position = Vector3(deepPathX + if ((zDeep / 72) % 2 == 0) 0.55f else -0.55f, 0f, zDeepF + 0.5f)
                        fogDeep.scale = Vector3(0.72f, 0.68f, 0.72f)
                        environmentMeshes.add(fogDeep)
                        subList.add(fogDeep)
                    }
                }

                // Cold crystal formations near the entrance sections
                for (zCrystal in -8..36 step 8) {
                    val zCrystalF = zCrystal.toFloat()
                    val crystalPathX = getPathX(zCrystalF)
                    val side = if ((zCrystal / 8) % 2 == 0) 3.45f else -3.45f
                    val crystal = Models3D.createIceCrystalCluster()
                    crystal.position = Vector3(crystalPathX + side, 0f, zCrystalF + 0.2f)
                    crystal.scale = Vector3(
                        0.82f + (zCrystal % 3) * 0.08f,
                        0.9f + (zCrystal % 4) * 0.06f,
                        0.82f + (zCrystal % 3) * 0.08f
                    )
                    crystal.rotation.y = if (side > 0f) -18f else 18f
                    environmentMeshes.add(crystal)
                    subList.add(crystal)
                }

                // Mining equipment dressing
                for (zEquip in listOf(4f, 18f, 30f, 42f, 54f, 68f)) {
                    val eqPathX = getPathX(zEquip)
                    val side = if ((zEquip.toInt() / 6) % 2 == 0) 3.6f else -3.6f

                    val crates = Models3D.createCrateStack()
                    crates.position = Vector3(eqPathX + side, 0f, zEquip)
                    crates.rotation.y = if (side > 0f) -24f else 24f
                    environmentMeshes.add(crates)
                    subList.add(crates)

                    val rope = Models3D.createRopeBundle()
                    rope.position = Vector3(eqPathX + side * 0.88f, 0f, zEquip + 0.5f)
                    environmentMeshes.add(rope)
                    subList.add(rope)

                    val lanternStand = Models3D.createLanternStand(0.96f, 0.72f, 0.3f)
                    lanternStand.position = Vector3(eqPathX + side * 0.8f, 0f, zEquip - 0.55f)
                    environmentMeshes.add(lanternStand)
                    subList.add(lanternStand)
                }

                val addMineObstacle = { relX: Float, z: Float, radius: Float ->
                    mineObstacleCircles.add(MineObstacle(relX, z, radius))
                }

                // Environmental traversal blockers with a clear bypass path
                val lowBeamZ = 12f
                val lowBeam = Models3D.createWoodenBarricade()
                lowBeam.position = Vector3(getPathX(lowBeamZ) - 0.95f, 0f, lowBeamZ)
                lowBeam.scale = Vector3(0.9f, 0.62f, 0.9f)
                lowBeam.rotation.y = 10f
                environmentMeshes.add(lowBeam)
                subList.add(lowBeam)
                addMineObstacle(-0.95f, lowBeamZ, 0.58f)

                val slipperyIceZ = 16f
                val slipperyIce = Models3D.createFrozenPuddlePatch()
                slipperyIce.position = Vector3(getPathX(slipperyIceZ) + 0.85f, 0f, slipperyIceZ)
                slipperyIce.scale = Vector3(1.22f, 1f, 1.15f)
                environmentMeshes.add(slipperyIce)
                subList.add(slipperyIce)
                addMineObstacle(0.85f, slipperyIceZ, 0.64f)

                val fallingIcicleZ = 24f
                val icicleHazard = Models3D.createIcicleHazardCluster()
                icicleHazard.position = Vector3(getPathX(fallingIcicleZ) + 2.8f, 0f, fallingIcicleZ)
                icicleHazard.scale = Vector3(1.2f, 1.2f, 1.2f)
                icicleHazard.rotation.y = -18f
                environmentMeshes.add(icicleHazard)
                subList.add(icicleHazard)

                val fallenTimberZ = 20f
                val fallenTimber = Models3D.createCollapsedSupportDebris()
                fallenTimber.position = Vector3(getPathX(fallenTimberZ) + 1.35f, 0f, fallenTimberZ)
                fallenTimber.rotation.y = -28f
                environmentMeshes.add(fallenTimber)
                subList.add(fallenTimber)
                addMineObstacle(1.35f, fallenTimberZ, 0.72f)

                val brokenCartZ = 28f
                val brokenCart = Models3D.createMineCartDetailed(overturned = true, filled = false, damaged = true)
                brokenCart.position = Vector3(getPathX(brokenCartZ) - 1.05f, 0f, brokenCartZ)
                brokenCart.rotation.y = 32f
                brokenCart.rotation.z = 20f
                environmentMeshes.add(brokenCart)
                subList.add(brokenCart)
                addMineObstacle(-1.05f, brokenCartZ, 0.66f)

                val rockfallZ = 40f
                val rockfall = Models3D.createRockfallPile()
                rockfall.position = Vector3(getPathX(rockfallZ) + 0.95f, 0f, rockfallZ)
                environmentMeshes.add(rockfall)
                subList.add(rockfall)
                addMineObstacle(0.95f, rockfallZ, 0.8f)

                val swingTrapZ = 48f
                val swingTrap = Models3D.createFallenBeam()
                swingTrap.position = Vector3(getPathX(swingTrapZ), 1.25f, swingTrapZ)
                swingTrap.rotation.z = 34f
                swingTrap.rotation.y = 12f
                environmentMeshes.add(swingTrap)
                subList.add(swingTrap)
                val swingChain = Models3D.createHangingRope(1.8f)
                swingChain.position = Vector3(getPathX(swingTrapZ) - 0.2f, 1.7f, swingTrapZ - 0.2f)
                environmentMeshes.add(swingChain)
                subList.add(swingChain)
                addMineObstacle(0f, swingTrapZ, 0.56f)

                val collapseZ = 56f
                val collapse = Models3D.createCollapsedSupportDebris()
                collapse.position = Vector3(getPathX(collapseZ) + 1.2f, 0f, collapseZ)
                collapse.rotation.y = 36f
                environmentMeshes.add(collapse)
                subList.add(collapse)
                addMineObstacle(1.2f, collapseZ, 0.9f)

                val narrowPassageZ = 60f
                val narrowLeft = Models3D.createRockfallPile()
                narrowLeft.position = Vector3(getPathX(narrowPassageZ) - 1.7f, 0f, narrowPassageZ)
                environmentMeshes.add(narrowLeft)
                subList.add(narrowLeft)
                addMineObstacle(-1.7f, narrowPassageZ, 0.74f)

                val narrowRight = Models3D.createRockfallPile()
                narrowRight.position = Vector3(getPathX(narrowPassageZ) + 1.7f, 0f, narrowPassageZ + 0.25f)
                environmentMeshes.add(narrowRight)
                subList.add(narrowRight)
                addMineObstacle(1.7f, narrowPassageZ + 0.25f, 0.74f)

                val collapseRocks = Models3D.createRockfallPile()
                collapseRocks.position = Vector3(getPathX(collapseZ) + 1.55f, 0f, collapseZ + 0.75f)
                environmentMeshes.add(collapseRocks)
                subList.add(collapseRocks)
                addMineObstacle(1.55f, collapseZ + 0.75f, 0.7f)

                val coalChokeZ = 64f
                val unstableCoal = Models3D.createCoalPile(radius = 1.02f, heightScale = 1.5f)
                unstableCoal.position = Vector3(getPathX(coalChokeZ) - 0.85f, 0f, coalChokeZ)
                environmentMeshes.add(unstableCoal)
                subList.add(unstableCoal)
                addMineObstacle(-0.85f, coalChokeZ, 0.78f)

                val brokenRailsZ = 70f
                val brokenRails = Models3D.createBrokenPlankPile()
                brokenRails.position = Vector3(getPathX(brokenRailsZ) + 0.6f, 0f, brokenRailsZ)
                brokenRails.rotation.y = -18f
                environmentMeshes.add(brokenRails)
                subList.add(brokenRails)
                addMineObstacle(0.6f, brokenRailsZ, 0.62f)

                val chainZ = 76f
                val hangingChainA = Models3D.createHangingRope(1.55f)
                hangingChainA.position = Vector3(getPathX(chainZ) - 0.35f, 1.7f, chainZ)
                environmentMeshes.add(hangingChainA)
                subList.add(hangingChainA)
                val hangingChainB = Models3D.createHangingRope(1.45f)
                hangingChainB.position = Vector3(getPathX(chainZ) + 0.25f, 1.65f, chainZ + 0.18f)
                environmentMeshes.add(hangingChainB)
                subList.add(hangingChainB)
                addMineObstacle(-0.05f, chainZ, 0.44f)

                // Visual focal points every ~20–30m to break repetition
                val focalLargeCart = Models3D.createMineCartDetailed(overturned = false, filled = true, damaged = true)
                focalLargeCart.position = Vector3(getPathX(12f) + 3.25f, 0f, 12f)
                focalLargeCart.scale = Vector3(1.28f, 1.22f, 1.3f)
                focalLargeCart.rotation.y = -20f
                environmentMeshes.add(focalLargeCart)
                subList.add(focalLargeCart)

                val focalCoalDeposit = Models3D.createCoalPile(radius = 1.52f, heightScale = 1.95f)
                focalCoalDeposit.position = Vector3(getPathX(34f) - 3.4f, 0f, 34f)
                environmentMeshes.add(focalCoalDeposit)
                subList.add(focalCoalDeposit)
                val focalCrystalDeposit = Models3D.createIceCrystalCluster()
                focalCrystalDeposit.position = Vector3(getPathX(34f) - 2.95f, 0f, 34.75f)
                focalCrystalDeposit.scale = Vector3(1.45f, 1.65f, 1.45f)
                focalCrystalDeposit.rotation.y = 22f
                environmentMeshes.add(focalCrystalDeposit)
                subList.add(focalCrystalDeposit)

                val focalMiningStation = Models3D.createWoodenPlatform()
                focalMiningStation.position = Vector3(getPathX(56f) + 3.45f, 0f, 56f)
                focalMiningStation.rotation.y = -26f
                environmentMeshes.add(focalMiningStation)
                subList.add(focalMiningStation)
                val stationTools = Models3D.createAbandonedToolSet()
                stationTools.position = Vector3(getPathX(56f) + 3.1f, 0f, 56.55f)
                stationTools.rotation.y = 18f
                environmentMeshes.add(stationTools)
                subList.add(stationTools)

                val focalCamp = Models3D.createWarningSign()
                focalCamp.position = Vector3(getPathX(80f) - 3.2f, 0f, 80f)
                focalCamp.rotation.y = 16f
                environmentMeshes.add(focalCamp)
                subList.add(focalCamp)
                val campSupplies = Models3D.createMiningSupplyCluster()
                campSupplies.position = Vector3(getPathX(80f) - 3.55f, 0f, 80.65f)
                campSupplies.rotation.y = -14f
                environmentMeshes.add(campSupplies)
                subList.add(campSupplies)

                val focalCollapsed = Models3D.createCollapsedSupportDebris()
                focalCollapsed.position = Vector3(getPathX(24f) + 3.35f, 0f, 24f)
                focalCollapsed.scale = Vector3(1.22f, 1.18f, 1.2f)
                focalCollapsed.rotation.y = -30f
                environmentMeshes.add(focalCollapsed)
                subList.add(focalCollapsed)

                val focalElevatorShaft = Models3D.createMineSupportFrame(width = 3.2f, height = 5.2f, depth = 1.4f)
                focalElevatorShaft.position = Vector3(getPathX(72f) + 3.45f, 0f, 72f)
                focalElevatorShaft.rotation.y = -8f
                environmentMeshes.add(focalElevatorShaft)
                subList.add(focalElevatorShaft)
                val shaftRope = Models3D.createHangingRope(2.4f)
                shaftRope.position = Vector3(getPathX(72f) + 3.45f, 1.2f, 72f)
                environmentMeshes.add(shaftRope)
                subList.add(shaftRope)

                val chest = Models3D.createTreasureChest()
                chest.position = Vector3(getPathX(1484f) - 3.4f, 0f, 1484f)
                chest.rotation.y = 20f
                environmentMeshes.add(chest)
                subList.add(chest)

                val exitGate = Models3D.createMineSupportFrame(width = 6.0f, height = 4.4f, depth = 0.7f)
                exitGate.position = Vector3(getPathX(1488f), 0f, 1488f)
                environmentMeshes.add(exitGate)
                subList.add(exitGate)

                // Extend perceived depth beyond gameplay end to create vanishing tunnel scale
                for (zFar in 1512..1640 step 16) {
                    val zFarF = zFar.toFloat()
                    val farPathX = getPathX(zFarF)
                    val farYaw = getPathYaw(zFarF)

                    val farTunnel = Models3D.createTunnelRockModule(width = 8.4f, height = 5.0f, depth = 8f)
                    farTunnel.position = Vector3(farPathX, 0f, zFarF)
                    farTunnel.rotation.y = farYaw
                    environmentMeshes.add(farTunnel)
                    subList.add(farTunnel)

                    val farCeiling = Models3D.createCaveCeilingSection(ceilY = 3.9f)
                    farCeiling.position = Vector3(farPathX, 0f, zFarF)
                    farCeiling.rotation.y = farYaw
                    environmentMeshes.add(farCeiling)
                    subList.add(farCeiling)

                    if (zFar % 16 == 0) {
                        val farLamp = Models3D.createMineLanternGlow(0.92f, 0.68f, 0.24f)
                        farLamp.position = Vector3(farPathX + 2.45f, 0f, zFarF)
                        environmentMeshes.add(farLamp)
                        subList.add(farLamp)
                    }
                }
            }

            // 3. Environment structures based on theme
            if (stage.stageNumber in listOf(2, 4, 13)) {
                // Ancient Pagodas
                val gate = Models3D.createToriiGate()
                gate.position = Vector3(0f, 0f, 15f)
                subList.add(gate)
            }

            bossMesh = null

            subList
        }

        bossHitsRemaining = 0
        enemiesRemaining = 0

        // Initialize camera position immediately to prevent starting frame jump
        if (index == 0) {
            cameraOrbitYawDeg = getPathYaw(jackPos.z) + 180f
            renderer.cameraPos = Vector3(getPathX(jackPos.z - 5.8f), 2.0f, jackPos.z - 5.8f)
            renderer.cameraTarget = Vector3(getPathX(jackPos.z + 2.0f), 1.0f, jackPos.z + 2.0f)
        } else {
            cameraOrbitYawDeg = 180f
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
            val moveSpeed = 0.06f
            val limitZ = if (currentStageIndex == 0) floatArrayOf(-500f, 1500f) else floatArrayOf(-35f, 35f)
            
            if (currentStageIndex == 0) {
                // Winding mine path: movement is relative to the path center to prevent clipping
                val currentPathX = getPathX(jackPos.z)
                var relX = jackPos.x - currentPathX
                relX = (relX + joystickMoveVec.x * moveSpeed).coerceIn(-3.6f, 3.6f)
                
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

        // 3. Camera orbit with auto-follow and full 360 look control.
        val lookActive = kotlin.math.abs(cameraLookVec.x) > 0.03f || kotlin.math.abs(cameraLookVec.z) > 0.03f
        val movementYaw = if (jackState == "Run") {
            jackMesh.rotation.y
        } else if (currentStageIndex == 0) {
            getPathYaw(jackPos.z)
        } else {
            jackMesh.rotation.y
        }
        if (lookActive) {
            cameraOrbitYawDeg = normalizeAngleDeg(cameraOrbitYawDeg + cameraLookVec.x * if (currentStageIndex == 0) 3.4f else 4.6f)
            val minPitch = if (currentStageIndex == 0) -6f else -20f
            val maxPitch = if (currentStageIndex == 0) 22f else 28f
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
        if (currentStageIndex == 0) {
            targetCamX = clampToMineCameraCorridor(targetCamX, targetCamZ, 1.65f)
        }
        val cameraLerp = if (currentStageIndex == 0) 0.28f else 0.14f
        renderer.cameraPos.x += (targetCamX - renderer.cameraPos.x) * cameraLerp
        val adjustedCamY = if (currentStageIndex == 0) targetCamY.coerceAtLeast(jackPos.y + 1.95f) else targetCamY
        renderer.cameraPos.y += (adjustedCamY - renderer.cameraPos.y) * cameraLerp
        renderer.cameraPos.z += (targetCamZ - renderer.cameraPos.z) * cameraLerp
        if (currentStageIndex == 0) {
            renderer.cameraPos.x = clampToMineCameraCorridor(renderer.cameraPos.x, renderer.cameraPos.z, 1.65f)
        }

        // Apply device tilt/motion to shift camera focus (target looking direction) left/right
        val tiltStrength = if (currentStageIndex == 0) 0.12f else 0.3f
        val tiltRange = if (currentStageIndex == 0) 0.75f else 2.5f
        val targetTiltOffset = (-sensorTiltX * tiltStrength).coerceIn(-tiltRange, tiltRange)
        smoothedCameraTiltOffset += (targetTiltOffset - smoothedCameraTiltOffset) * 0.1f
        
        val lookYawDeg = if (lookActive) normalizeAngleDeg(cameraOrbitYawDeg + 180f) else movementYaw
        val lookYawRad = Math.toRadians(lookYawDeg.toDouble())
        val targetTargetX = jackPos.x + kotlin.math.sin(lookYawRad).toFloat() * 1.8f + smoothedCameraTiltOffset
        val targetTargetZ = jackPos.z + kotlin.math.cos(lookYawRad).toFloat() * 1.8f
        val flashOffset = if (specialFlashActive) (sin(specialFlashTime * 50f) * 0.2f) else 0f
        val adjustedTargetX = if (currentStageIndex == 0) {
            clampToMineCameraCorridor(targetTargetX + flashOffset, targetTargetZ, 2.0f)
        } else {
            targetTargetX + flashOffset
        }
        renderer.cameraTarget = Vector3(adjustedTargetX, jackPos.y + 0.68f, targetTargetZ)

        // Update current rendering stage details
        renderer.currentStageIndex = currentStageIndex
        renderer.hitFlashAmount = hitFlashTimer
        if (hitFlashTimer > 0f) {
            hitFlashTimer -= 0.05f
        }
        if (currentStageIndex == 0 && jackPos.z < 0f) {
            if (nextThunderAtMs == 0L) {
                nextThunderAtMs = nowMs + 1800L
            }
            if (nowMs >= nextThunderAtMs && thunderPulse <= 0f) {
                thunderPulse = 0.95f
                nextThunderAtMs = nowMs + 2800L + (Math.random() * 4200.0).toLong()
            }
            if (thunderPulse > 0f) {
                renderer.hitFlashAmount = kotlin.math.max(renderer.hitFlashAmount, thunderPulse * 0.45f)
                renderer.pointLightPos = Vector3(jackPos.x, 6.2f, jackPos.z + 10f)
                renderer.pointLightColor = floatArrayOf(0.72f, 0.84f, 1f)
                renderer.pointLightIntensity = kotlin.math.max(renderer.pointLightIntensity, 2.2f * thunderPulse)
                thunderPulse = (thunderPulse - 0.07f).coerceAtLeast(0f)
            }
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
        val winZ = if (currentStageIndex == 0) 1488.0f else 14.2f
        if (jackPos.z >= winZ && (enemiesRemaining == 0 || (enemyMeshes.isEmpty() && bossMesh == null && enemiesRemaining == 1))) {
            enemiesRemaining = -1 // Stop duplicate triggers
            gameState = GameState.OUTRO_CUTSCENE
            startCutscene(currentStageIndex, false)
        }

        // Point light decay with stage-1 lantern flicker fallback
        if (renderer.pointLightIntensity > 0f) {
            renderer.pointLightIntensity -= 0.08f
        }

        if (currentStageIndex == 0 && mineLanternAnchors.isNotEmpty()) {
            val nearestLantern = mineLanternAnchors.minByOrNull { anchor ->
                val dx = jackPos.x - anchor.x
                val dz = jackPos.z - anchor.z
                dx * dx + dz * dz
            }
            nearestLantern?.let { lanternPos ->
                val t = (System.currentTimeMillis() % 10_000L).toFloat() * 0.001f
                val flicker = (2.2f + 0.74f * sin(t * 11f + lanternPos.z * 0.2f) + 0.45f * sin(t * 23f + lanternPos.x))
                    .coerceIn(1.25f, 3.4f)
                if (renderer.pointLightIntensity < flicker) {
                    renderer.pointLightPos = lanternPos
                    renderer.pointLightColor = floatArrayOf(1.0f, 0.56f, 0.16f)
                    renderer.pointLightIntensity = flicker
                }
            }
        }

        if (currentStageIndex == 0 && jackPos.z >= 42f && mineCrystalAnchors.isNotEmpty()) {
            val nearestCrystal = mineCrystalAnchors.minByOrNull { anchor ->
                val dx = jackPos.x - anchor.x
                val dz = jackPos.z - anchor.z
                dx * dx + dz * dz
            }
            nearestCrystal?.let { crystalPos ->
                val t = (System.currentTimeMillis() % 10_000L).toFloat() * 0.001f
                val corruptionDepth = ((jackPos.z - 42f) / 36f).coerceIn(0f, 1f)
                val eeriePulse = (1.75f + 0.9f * sin(t * 7.5f + crystalPos.z * 0.25f) + 0.4f * sin(t * 15f + crystalPos.x))
                    .coerceIn(1.0f, 3.1f)
                val intensity = eeriePulse * (0.45f + corruptionDepth * 0.75f)
                if (renderer.pointLightIntensity < intensity) {
                    renderer.pointLightPos = crystalPos
                    renderer.pointLightColor = floatArrayOf(0.28f, 0.56f, 1.0f)
                    renderer.pointLightIntensity = intensity
                }
            }
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

    private fun getPathX(z: Float): Float {
        if (currentStageIndex == 0) {
            if (z < 0f) return 0f
            val segment = 42f
            val period = segment * 2f
            val local = ((z % period) + period) % period
            val phase = local / segment
            val zig = if (phase < 1f) {
                -1f + phase * 2f
            } else {
                1f - (phase - 1f) * 2f
            }
            val secondary = 0.35f * sin(z * 0.05f)
            return zig * 2.8f + secondary
        }
        return 0f
    }

    private fun getPathYaw(z: Float): Float {
        if (currentStageIndex != 0) return 0f
        val step = 0.5f
        val xAhead = getPathX(z + step)
        val xBehind = getPathX(z - step)
        val dx = xAhead - xBehind
        val dz = step * 2f
        return Math.toDegrees(kotlin.math.atan2(dx.toDouble(), dz.toDouble())).toFloat()
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
            for (zArch in -12..1500 step 18) {
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
            if (kotlin.math.abs(newZ - 1488.0f) < 1.5f) {
                val pathX = getPathX(1488.0f)
                // Left post
                val lpX = pathX - 2.8f
                val lpDist = kotlin.math.sqrt((newX - lpX) * (newX - lpX) + (newZ - 1488.0f) * (newZ - 1488.0f))
                if (lpDist < (0.15f + playerRadius)) {
                    return resolveCircleCollision(newX, newZ, oldX, oldZ, lpX, 1488.0f, 0.15f + playerRadius)
                }

                // Right post
                val rpX = pathX + 2.8f
                val rpDist = kotlin.math.sqrt((newX - rpX) * (newX - rpX) + (newZ - 1488.0f) * (newZ - 1488.0f))
                if (rpDist < (0.15f + playerRadius)) {
                    return resolveCircleCollision(newX, newZ, oldX, oldZ, rpX, 1488.0f, 0.15f + playerRadius)
                }
            }
            // 4. Check mine obstacle circles
            for (obs in mineObstacleCircles) {
                if (kotlin.math.abs(newZ - obs.pathZ) > 2.0f) continue
                val obsX = getPathX(obs.pathZ) + obs.pathRelX
                val dx = newX - obsX
                val dz = newZ - obs.pathZ
                val dist = kotlin.math.sqrt(dx * dx + dz * dz)
                if (dist < obs.radius + playerRadius) {
                    return resolveCircleCollision(newX, newZ, oldX, oldZ, obsX, obs.pathZ, obs.radius + playerRadius)
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

    private fun clampToMineCameraCorridor(x: Float, z: Float, halfWidth: Float): Float {
        if (currentStageIndex != 0) return x
        val center = getPathX(z)
        return center + (x - center).coerceIn(-halfWidth, halfWidth)
    }

    private fun refocusCameraOnCharacter() {
        // Reset tilt offset
        sensorTiltX = 0f
        smoothedCameraTiltOffset = 0f
        targetCameraLookVec = Vector3(0f, 0f, 0f)
        cameraLookVec = Vector3(0f, 0f, 0f)
        cameraOrbitPitchDeg = 10f
        
        // Reset camera positions OTS behind Jack
        val targetCamX = if (currentStageIndex == 0) {
            val pathX = getPathX(jackPos.z - 5.5f)
            pathX + (jackPos.x - getPathX(jackPos.z)) * 0.5f
        } else {
            jackPos.x + 0.9f
        }
        val targetCamY = jackPos.y + 1.4f
        val targetCamZ = jackPos.z - 5.5f
        cameraOrbitYawDeg = normalizeAngleDeg(if (currentStageIndex == 0) getPathYaw(jackPos.z) + 180f else 180f)
        val adjustedCamX = if (currentStageIndex == 0) clampToMineCameraCorridor(targetCamX, targetCamZ, 1.65f) else targetCamX
        renderer.cameraPos = Vector3(adjustedCamX, targetCamY, targetCamZ)
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
