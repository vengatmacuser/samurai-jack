package com.thigazhini_labs.samuraijack

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager

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
import com.thigazhini_labs.samuraijack.ui.GameUI
import com.google.androidgamesdk.GameActivity
import kotlinx.coroutines.*
import kotlin.math.abs

enum class GameState {
    SPLASH,
    MAIN_MENU,
    GAMEPLAY
}

class MainActivity : GameActivity() {

    companion object {
        init {
            System.loadLibrary("samuraijack")
        }
    }

    // Core Game States
    var gameState by mutableStateOf(GameState.SPLASH)
    var currentStageIndex by mutableIntStateOf(0)
    var isStage1InsideMine by mutableStateOf(false)
    
    // Player statistics
    var playerHealth by mutableFloatStateOf(100f)
    var playerShield by mutableFloatStateOf(50f)
    var kills by mutableIntStateOf(0)
    var score by mutableIntStateOf(0)
    
    // OpenGL elements
    private lateinit var glSurfaceView: GLSurfaceView
    private val renderer by lazy { GLRenderer(this) }

    // Physics thread variables
    private var gameLoopJob: Job? = null
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Game Entities
    private lateinit var jackMesh: Mesh
    private lateinit var skyMesh: Mesh
    private val itemMeshes = mutableListOf<Mesh>()

    // Camera controls
    private var cameraOrbitYawDeg = 180f
    private var cameraOrbitPitchDeg = -45f
    private var targetJoystickMoveVec = Vector3(0f, 0f, 0f)
    private var joystickMoveVec = Vector3(0f, 0f, 0f)
    private var animTime = 0f

    // Gameplay parameters
    private var jackPos = Vector3(0f, 0f, 0f)
    var jackState by mutableStateOf("Idle")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable immersive full-screen mode to cover the background image fully
        @Suppress("DEPRECATION")
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
                    playerShield = playerShield,
                    kills = kills,
                    score = score,
                    onStartGame = { startGame() },
                    onBackToMenu = {
                        keepScreenOn(false)
                        gameState = GameState.MAIN_MENU
                        SoundManager.playMusic(this@MainActivity)
                    },
                    onMove = { dx, dz -> targetJoystickMoveVec = Vector3(dx, 0f, dz) },
                    onPickup = { triggerPickup() }
                )
            }
        }

        // Initialize and start Sound Synthesizer
        SoundManager.start(this)
        buildMainMenuScene()
    }

    private fun keepScreenOn(enable: Boolean) {
        if (enable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
        SoundManager.start(this)
        startGameLoop()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
        SoundManager.stop()
        stopGameLoop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            @Suppress("DEPRECATION")
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
        keepScreenOn(false)
        SoundManager.stop()
        mainScope.cancel()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (gameState == GameState.GAMEPLAY && event.actionMasked == MotionEvent.ACTION_CANCEL) {
            targetJoystickMoveVec = Vector3(0f, 0f, 0f)
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
        targetJoystickMoveVec = Vector3(0f, 0f, 0f)
        joystickMoveVec = Vector3(0f, 0f, 0f)
        keepScreenOn(true)
        gameState = GameState.GAMEPLAY

        mainScope.launch {
            setupStageEntities(index)
        }
    }

    private suspend fun setupStageEntities(index: Int) {
        playerHealth = 100f
        playerShield = 50f
        isStage1InsideMine = false
        kills = 0
        score = 0
        jackPos = Vector3(0f, 0.4f, 0f)
        jackState = "Idle"
        cameraOrbitPitchDeg = -20f

        itemMeshes.clear()

        val list = withContext(Dispatchers.IO) {
            val subList = mutableListOf<Mesh>()

            val gMesh = Models3D.createGround(50f, 200f, 0.2f, 0.25f, 0.3f)
            subList.add(gMesh)

            val jack = Models3D.loadObj(this@MainActivity, "samurai_model.obj", "samurai_model.mtl", scaleMultiplier = 2.0f, yOffset = 0.715f)
            jack.isHero = true
            jackMesh = jack
            jackMesh.position = jackPos
            subList.add(jackMesh)

            val sky = Models3D.createSkySphere()
            sky.position = Vector3(0f, 0f, 0f)
            sky.isVisible = true
            skyMesh = sky
            subList.add(sky)

            subList
        }

        cameraOrbitYawDeg = 180f
        renderer.cameraPos = Vector3(0f, 4f, -6f)
        renderer.cameraTarget = jackPos

        synchronized(renderer.renderMeshes) {
            renderer.renderMeshes.clear()
            renderer.renderMeshes.addAll(list)
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
        if (!::jackMesh.isInitialized) return
        val nowMs = System.currentTimeMillis()

        // Smooth joystick interpolation (higher = snappier)
        joystickMoveVec.x += (targetJoystickMoveVec.x - joystickMoveVec.x) * 0.35f
        joystickMoveVec.z += (targetJoystickMoveVec.z - joystickMoveVec.z) * 0.35f
        if (abs(joystickMoveVec.x) < 0.02f && targetJoystickMoveVec.x == 0f) joystickMoveVec.x = 0f
        if (abs(joystickMoveVec.z) < 0.02f && targetJoystickMoveVec.z == 0f) joystickMoveVec.z = 0f

        // Update animation timer
        if (jackState == "Run") {
            animTime += 0.15f
        } else {
            animTime = 0f
        }
        renderer.animTime = animTime

        // Health regeneration
        if (gameState == GameState.GAMEPLAY && playerHealth in 0.1f..99.9f) {
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

        val prevJackX = jackPos.x
        val prevJackZ = jackPos.z

        // Joystick-only movement (camera-relative)
        val yawRad = Math.toRadians(cameraOrbitYawDeg.toDouble())
        val sinCamYaw = kotlin.math.sin(yawRad).toFloat()
        val cosCamYaw = kotlin.math.cos(yawRad).toFloat()
        val joySpeed = 0.14f

        var moveX = joystickMoveVec.x * cosCamYaw * joySpeed - joystickMoveVec.z * sinCamYaw * joySpeed
        var moveZ = joystickMoveVec.x * sinCamYaw * joySpeed + joystickMoveVec.z * cosCamYaw * joySpeed

        val targetX = (jackPos.x + moveX).coerceIn(-24f, 24f)
        val targetZ = (jackPos.z + moveZ).coerceIn(-99f, 99f)
        jackPos.x = targetX
        jackPos.z = targetZ

        val dx = jackPos.x - prevJackX
        val dz = jackPos.z - prevJackZ
        if (dx != 0f || dz != 0f) {
            val angleRad = kotlin.math.atan2(dx.toDouble(), dz.toDouble())
            val angleDeg = Math.toDegrees(angleRad).toFloat()
            jackMesh.rotation.y = angleDeg
            if (jackState == "Idle" || jackState == "Run") {
                jackState = "Run"
            }
        } else {
            if (jackState == "Run") {
                jackState = "Idle"
            }
        }

        // 2. Update Jack Mesh Position
        jackMesh.position = jackPos

        if (::skyMesh.isInitialized) {
            skyMesh.position.x = jackPos.x
            skyMesh.position.z = jackPos.z
        }

        // 3. Isometric camera — 45-degree top-down behind Jack
        val camYawRad = Math.toRadians(cameraOrbitYawDeg.toDouble())
        val camPitchRad = Math.toRadians(cameraOrbitPitchDeg.toDouble())
        val camDist = 6.0f
        val camHeight = 4.0f
        val targetCamX = jackPos.x - kotlin.math.sin(camYawRad).toFloat() * camDist
        val targetCamY = (jackPos.y + camHeight + kotlin.math.sin(camPitchRad).toFloat() * camDist * 0.4f).coerceAtLeast(0.5f)
        val targetCamZ = jackPos.z - kotlin.math.cos(camYawRad).toFloat() * camDist
        if (targetCamX.isFinite() && targetCamY.isFinite() && targetCamZ.isFinite()) {
            renderer.cameraPos.x += (targetCamX - renderer.cameraPos.x) * 0.15f
            renderer.cameraPos.y += (targetCamY - renderer.cameraPos.y) * 0.15f
            renderer.cameraPos.z += (targetCamZ - renderer.cameraPos.z) * 0.15f
        }
        renderer.cameraTarget = Vector3(jackPos.x, jackPos.y + 1.5f, jackPos.z)

        // Update current rendering stage details
        renderer.currentStageIndex = currentStageIndex

        // Low health heartbeat triggers
        SoundManager.isLowHealthHeartbeatActive = playerHealth < 35f

        // 6. Check Win conditions
        val winZ = 80f
        if (jackPos.z >= winZ) {
            keepScreenOn(false)
            gameState = GameState.MAIN_MENU
            SoundManager.playMusic(this@MainActivity)
        }


    }

    private fun triggerPickup() {
        val iter = itemMeshes.iterator()
        while (iter.hasNext()) {
            val item = iter.next()
            if (item.isVisible && jackPos.dist(item.position) < 2.0f) {
                item.isVisible = false
                synchronized(renderer.renderMeshes) { renderer.renderMeshes.remove(item) }
                iter.remove()
                if (item.scale.x > 0.15f) { // Health pack
                    playerHealth = (playerHealth + 30f).coerceAtMost(100f)
                } else { // Shield
                    playerShield = (playerShield + 25f).coerceAtMost(100f)
                }
                SoundManager.triggerSlash()
                score += 20
                break
            }
        }
    }

}
