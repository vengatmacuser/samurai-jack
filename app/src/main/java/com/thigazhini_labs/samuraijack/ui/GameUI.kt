package com.thigazhini_labs.samuraijack.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thigazhini_labs.samuraijack.GameState
import com.thigazhini_labs.samuraijack.R
import com.thigazhini_labs.samuraijack.stages.Stages
import kotlinx.coroutines.delay

@Composable
fun GameUI(
    gameState: GameState,
    currentStageIndex: Int,
    unlockedStageCount: Int,
    playerHealth: Float,
    playerSwordEnergy: Float,
    enemiesRemaining: Int,
    typedText: String,
    isTextFullyTyped: Boolean,
    jackState: String,
    playerCoins: Int,
    playerCrystals: Int,
    onStartGame: () -> Unit,
    onSelectStage: (Int) -> Unit,
    onNextStage: () -> Unit,
    onResumeGame: () -> Unit,
    onRetryStage: () -> Unit,
    onBackToMenu: () -> Unit,
    onAdvanceText: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onMeleeAttack: () -> Unit,
    onJump: () -> Unit,
    onBlock: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (gameState) {
            GameState.SPLASH -> SplashScreen(onStartGame)
            GameState.MAIN_MENU -> MainMenuScreen(onStartGame, onSelectStage = { onSelectStage(-1) })
            GameState.STAGE_SELECT -> StageSelectScreen(unlockedStageCount, onSelectStage, onBackToMenu)
            GameState.TRAVEL_TRANSITION -> TravelTransitionScreen()
            GameState.INTRO_CUTSCENE, GameState.OUTRO_CUTSCENE -> CutsceneOverlay(typedText, onAdvanceText)
            GameState.GAMEPLAY -> GameplayHUD(
                playerHealth = playerHealth,
                playerSwordEnergy = playerSwordEnergy,
                enemiesRemaining = enemiesRemaining,
                playerCoins = playerCoins,
                playerCrystals = playerCrystals,
                onBack = onBackToMenu,
                onMove = onMove,
                onMeleeAttack = onMeleeAttack,
                onJump = onJump,
                onBlock = onBlock
            )
            GameState.GAME_OVER -> GameOverScreen(onRetryStage, onBackToMenu)
            GameState.GAME_COMPLETE -> GameCompleteScreen(onBackToMenu)
        }

        // Gameplay combat overlays (Slash, flash, speed lines) based on jackState
        if (gameState == GameState.GAMEPLAY) {
            if (jackState == "Attack") {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, size.height * 0.2f)
                            lineTo(size.width, size.height * 0.7f)
                            lineTo(size.width, size.height * 0.8f)
                            lineTo(0f, size.height * 0.3f)
                            close()
                        }
                        drawPath(path, color = Color.White.copy(alpha = 0.85f))
                        
                        val path2 = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, size.height * 0.23f)
                            lineTo(size.width, size.height * 0.73f)
                            lineTo(size.width, size.height * 0.76f)
                            lineTo(0f, size.height * 0.26f)
                            close()
                        }
                        drawPath(path2, color = Color(0xFFD41414))
                    }
                }
            }

            if (jackState == "Hurt") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x77D41414))
                )
            }

            if (jackState == "Special") {
                val transition = rememberInfiniteTransition(label = "specialLines")
                val linesOffset by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(200, easing = LinearEasing)
                    ),
                    label = "rot"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().rotate(linesOffset)) {
                        val center = center
                        val radius = size.minDimension
                        val numLines = 30
                        for (i in 0 until numLines) {
                            val angle = (2 * Math.PI / numLines) * i
                            val startDist = radius * 0.3f
                            val endDist = radius * 1.5f
                            val startX = (center.x + startDist * Math.cos(angle)).toFloat()
                            val startY = (center.y + startDist * Math.sin(angle)).toFloat()
                            val endX = (center.x + endDist * Math.cos(angle)).toFloat()
                            val endY = (center.y + endDist * Math.sin(angle)).toFloat()
                            
                            drawLine(
                                color = Color.White.copy(alpha = 0.7f),
                                start = androidx.compose.ui.geometry.Offset(startX, startY),
                                end = androidx.compose.ui.geometry.Offset(endX, endY),
                                strokeWidth = 4f
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onStart: () -> Unit) {
    var animationStep by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        delay(1000)
        animationStep = 1 // Show company presents (Powered by Thigazhini Labs)
        delay(2000)
        animationStep = 2 // Slash flash logo title
    }

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (animationStep >= 2) 1f else 0f,
        animationSpec = tween(1500),
        label = "bgAlpha"
    )

    var flashAlpha by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animationStep) {
        if (animationStep == 2) {
            flashAlpha = 1.0f
            animate(
                initialValue = 1.0f,
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1000)
            ) { value, _ ->
                flashAlpha = value
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070709)),
        contentAlignment = Alignment.Center
    ) {
        if (backgroundAlpha > 0f) {
            Image(
                painter = painterResource(id = R.drawable.main_background),
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(backgroundAlpha)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(backgroundAlpha)
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }

        // Step 1: Powered by Thigazhini Labs
        if (animationStep == 1) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "POWERED BY",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.alpha(0.8f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "THIGAZHINI LABS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 8.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // Step 2: Main Slash and Logo
        if (animationStep >= 2) {
            val scale = remember { Animatable(0.4f) }
            LaunchedEffect(Unit) {
                scale.animateTo(1.0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(scale.value)
            ) {
                Spacer(modifier = Modifier.height(130.dp))
                Box(
                    modifier = Modifier
                        .padding(bottom = 36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x55D41414)) // ~33% alpha red
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.main_logo),
                        contentDescription = "Samurai Jack Logo",
                        modifier = Modifier
                            .width(320.dp)
                            .height(100.dp)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(Color(0xFFD41414), RoundedCornerShape(4.dp))
                        .clickable { onStart() }
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "START ADVENTURE",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                }
            }
        }

        if (flashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(flashAlpha)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun MainMenuScreen(onStart: () -> Unit, onSelectStage: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.main_background),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Attached Samurai Jack logo image added to the home page inside a red transparent background container
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x55D41414)) // ~33% alpha red
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.main_logo),
                    contentDescription = "Samurai Jack Logo",
                    modifier = Modifier
                        .width(320.dp)
                        .height(100.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "EXILED IN TIME",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 10.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            MenuButton("CONTINUE ADVENTURE", onStart)
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton("CHRONICLES MAP", onSelectStage)
        }
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
            .background(Color(0xFF16161B), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.LightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun StageSelectScreen(unlockedStageCount: Int, onSelectStage: (Int) -> Unit, onBack: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "portal")
    val portalRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.main_background),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // Rotating Space-Time Portal representation background
        Box(
            modifier = Modifier
                .size(450.dp)
                .rotate(portalRotation)
                .alpha(0.15f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1B0B33), Color(0xFF0F041A), Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BACK TO MENU",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(8.dp)
                )
                Text(
                    text = "THE 13 CHRONICLES PORTALS",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.width(100.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Horizon layout for 13 Stage Nodes
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                itemsIndexed(Stages.stagesList) { idx, stage ->
                    val isUnlocked = idx < unlockedStageCount
                    StageNode(idx + 1, stage.title, stage.objective, isUnlocked) {
                        if (isUnlocked) onSelectStage(idx)
                    }
                }
            }
        }
    }
}

@Composable
fun StageNode(num: Int, title: String, objective: String, isUnlocked: Boolean, onClick: () -> Unit) {
    val scale = if (isUnlocked) 1.0f else 0.85f
    val alpha = if (isUnlocked) 1.0f else 0.4f
    
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(280.dp)
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .border(
                1.dp,
                if (isUnlocked) Color(0xFFD41414) else Color.DarkGray,
                RoundedCornerShape(8.dp)
            )
            .background(
                if (isUnlocked) Color(0xFF160A0D) else Color(0xFF0F0F12),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "PORTAL $num",
                color = if (isUnlocked) Color(0xFFFFCC00) else Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = objective,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }

            Text(
                text = if (isUnlocked) "ENTER PORTAL" else "LOCKED",
                color = if (isUnlocked) Color(0xFFD41414) else Color.DarkGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun TravelTransitionScreen() {
    val transition = rememberInfiniteTransition(label = "tartakovsky")
    
    // Panel sliding animations
    val topPanelOffset by transition.animateFloat(
        initialValue = -500f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "topPanel"
    )
    
    val bottomPanelOffset by transition.animateFloat(
        initialValue = 500f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bottomPanel"
    )
    
    val speedLinesScroll by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "speedLines"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070709))
    ) {
        // Draw speed lines in the background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val lineCount = 15
            for (i in 0 until lineCount) {
                val y = (height / lineCount) * i
                val xStart = (speedLinesScroll + (i * 123)) % width
                val xEnd = (xStart + 150f).coerceAtMost(width)
                drawLine(
                    color = Color(0xFF3F0B0B),
                    start = androidx.compose.ui.geometry.Offset(xStart, y),
                    end = androidx.compose.ui.geometry.Offset(xEnd, y),
                    strokeWidth = 3f
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Panel (slides in red/black block)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .offset(x = topPanelOffset.dp)
                    .background(Color(0xFF8F0F0F))
                    .border(2.dp, Color.Black)
            ) {
                // Diagonal speed lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (i in 0..10) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.3f),
                            start = androidx.compose.ui.geometry.Offset(i * 150f - speedLinesScroll % 150f, 0f),
                            end = androidx.compose.ui.geometry.Offset(i * 150f - speedLinesScroll % 150f - 50f, size.height),
                            strokeWidth = 8f
                        )
                    }
                }
            }

            // Middle Panel (Eye close-up, stark white/black contrast)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color.Black)
                    .border(3.dp, Color(0xFFFFCC00))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.black_eyes),
                    contentDescription = "Jack Eyes Close-up",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.85f)
                )
                
                // Overlay text
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EXILING THROUGH TIME...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Bottom Panel (slides in from right)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .offset(x = bottomPanelOffset.dp)
                    .background(Color(0xFF1B0B33))
                    .border(2.dp, Color.Black)
            ) {
                // Vertical grid lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (i in 0..10) {
                        drawLine(
                            color = Color(0xFF9900FF).copy(alpha = 0.3f),
                            start = androidx.compose.ui.geometry.Offset(i * 150f + speedLinesScroll % 150f, 0f),
                            end = androidx.compose.ui.geometry.Offset(i * 150f + speedLinesScroll % 150f, size.height),
                            strokeWidth = 4f
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CutsceneOverlay(typedText: String, onAdvance: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onAdvance() }
    ) {
        // Top Cinematic Letterbox
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .background(Color.Black)
                .align(Alignment.TopCenter)
        )

        // Bottom Cinematic Letterbox hosting subtitles
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .background(Color.Black)
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = typedText,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
            )
            
            Text(
                text = "[TAP TO ADVANCE]",
                color = Color(0xFFD41414),
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
fun GameplayHUD(
    playerHealth: Float,
    playerSwordEnergy: Float,
    enemiesRemaining: Int,
    playerCoins: Int,
    playerCrystals: Int,
    onBack: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onMeleeAttack: () -> Unit,
    onJump: () -> Unit,
    onBlock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. Top-Left Player Profile HUD
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar (Jack's eyes crop)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(2.dp, Color(0xFFD41414), RoundedCornerShape(28.dp))
                    .background(Color.Black, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.black_eyes),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            clip = true
                            shape = RoundedCornerShape(26.dp)
                        }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                // Health Bar (HP label)
                Box(
                    modifier = Modifier
                        .size(160.dp, 20.dp)
                        .background(Color(0xFF201010), RoundedCornerShape(4.dp))
                        .border(1.5.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((playerHealth / 100f).coerceIn(0f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF901010), Color(0xFFD41414))
                                ),
                                RoundedCornerShape(4.dp)
                            )
                    )
                    Text(
                        text = "${playerHealth.toInt()} / 100",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Stats: Coins & Crystals
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xBB000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🪙", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$playerCoins",
                            color = Color(0xFFFFCC00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        modifier = Modifier
                            .background(Color(0xBB000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💎", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$playerCrystals",
                            color = Color(0xFF14C5D4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Enemies Remaining Middle
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (enemiesRemaining > 0) "ENEMIES: $enemiesRemaining" else "STAGE CLEAR!",
                color = if (enemiesRemaining > 0) Color.White else Color(0xFFFFCC00),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        // 3. Pause Button (Top-Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(40.dp)
                .background(Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .border(1.5.dp, Color.White, RoundedCornerShape(20.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(3.dp, 12.dp).background(Color.White))
                Box(modifier = Modifier.size(3.dp, 12.dp).background(Color.White))
            }
        }

        // 4. Virtual Joystick (Bottom-Left)
        var joystickOffset by remember { mutableStateOf(Offset.Zero) }
        val joystickRadius = 50.dp
        val density = LocalDensity.current
        val joystickRadiusPx = with(density) { joystickRadius.toPx() }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 32.dp, start = 32.dp)
                .size(100.dp)
                .background(Color(0x33FFFFFF), RoundedCornerShape(50.dp))
                .border(2.dp, Color(0x66FFFFFF), RoundedCornerShape(50.dp))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent()
                            val firstChange = down.changes.firstOrNull() ?: continue
                            if (firstChange.pressed) {
                                firstChange.consume()
                                
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                
                                val updateJoystick = { px: Float, py: Float ->
                                    val dx = px - centerX
                                    val dy = py - centerY
                                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                    if (dist > joystickRadiusPx) {
                                        joystickOffset = Offset(dx * joystickRadiusPx / dist, dy * joystickRadiusPx / dist)
                                    } else {
                                        joystickOffset = Offset(dx, dy)
                                    }
                                    onMove(joystickOffset.x / joystickRadiusPx, -joystickOffset.y / joystickRadiusPx)
                                }
                                
                                updateJoystick(firstChange.position.x, firstChange.position.y)
                                
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pointerChange = event.changes.firstOrNull() ?: break
                                    if (pointerChange.pressed) {
                                        pointerChange.consume()
                                        updateJoystick(pointerChange.position.x, pointerChange.position.y)
                                    } else {
                                        break
                                    }
                                }
                                
                                joystickOffset = Offset.Zero
                                onMove(0f, 0f)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Knob
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { joystickOffset.x.toDp() },
                        y = with(density) { joystickOffset.y.toDp() }
                    )
                    .size(44.dp)
                    .background(Color(0xBBFFFFFF), RoundedCornerShape(22.dp))
                    .border(2.5.dp, Color.White, RoundedCornerShape(22.dp))
            )
        }

        // 5. Action Button Cluster (Bottom-Right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
                .size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Large Sword Attack (Bottom-Right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(72.dp)
                    .background(Color(0x44FFFFFF), RoundedCornerShape(36.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(36.dp))
                    .clickable { onMeleeAttack() },
                contentAlignment = Alignment.Center
            ) {
                Text("🗡️", fontSize = 28.sp)
            }

            // Jump Button (Top-Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp)
                    .size(48.dp)
                    .background(Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .border(1.5.dp, Color.LightGray, RoundedCornerShape(24.dp))
                    .clickable { onJump() },
                contentAlignment = Alignment.Center
            ) {
                Text("⬆️", fontSize = 20.sp)
            }

            // Defense / Block Button (Bottom-Left)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 12.dp)
                    .size(48.dp)
                    .background(Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .border(1.5.dp, Color.LightGray, RoundedCornerShape(24.dp))
                    .clickable { onBlock() },
                contentAlignment = Alignment.Center
            ) {
                Text("🛡️", fontSize = 20.sp)
            }
        }

        // Help Instructions Overlay
        Text(
            text = "JOYSTICK: MOVE | BUTTONS: ATTACK, DEFEND, JUMP",
            color = Color.Gray,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
fun GameOverScreen(onRetry: () -> Unit, onMenu: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.main_background),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE1A0505))
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "YOU HAVE FALLEN",
                color = Color(0xFFD41414),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Text(
                text = "Aku: You can never return to the past!",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )

            MenuButton("RETRY PORTAL", onRetry)
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton("RETURN TO MAP", onMenu)
        }
    }
}

@Composable
fun GameCompleteScreen(onMenu: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.main_background),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "THE JOURNEY IS COMPLETE",
                color = Color(0xFFFFCC00),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )
            Text(
                text = "Jack has defeated Aku, destroyed the portals, and returned to the past.",
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, bottom = 48.dp, start = 32.dp, end = 32.dp)
            )

            MenuButton("RETURN TO MENU", onMenu)
        }
    }
}
