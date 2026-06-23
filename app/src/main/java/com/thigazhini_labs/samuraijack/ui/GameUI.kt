package com.thigazhini_labs.samuraijack.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
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
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp)
                    .scale(scale.value)
            ) {
                // Unified premium glassmorphic card container
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFFD41414).copy(alpha = 0.8f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.75f),
                                    Color(0xFF1E0505).copy(alpha = 0.85f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Tint logo white for crisp contrast and premium look
                        Image(
                            painter = painterResource(id = R.drawable.main_logo),
                            contentDescription = "Samurai Jack Logo",
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier = Modifier
                                .width(280.dp)
                                .height(85.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Fading gradient divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFFD41414).copy(alpha = 0.8f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Start Adventure button (perfectly aligned with logo width inside card)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF8F0F0F),
                                            Color(0xFFD41414),
                                            Color(0xFF8F0F0F)
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                .clickable { onStart() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "START ADVENTURE",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp
                            )
                        }
                    }
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
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp)
        ) {
            // Unified premium glassmorphic card container for consistency
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFD41414).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color(0xFF1E0505).copy(alpha = 0.85f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Logo tinted White/Silver for consistency and contrast
                    Image(
                        painter = painterResource(id = R.drawable.main_logo),
                        contentDescription = "Samurai Jack Logo",
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier
                            .width(280.dp)
                            .height(85.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFFD41414).copy(alpha = 0.8f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    MenuButton("CONTINUE ADVENTURE", onStart, isPrimary = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    MenuButton("CHRONICLES MAP", onSelectStage, isPrimary = false)
                }
            }
        }
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit, isPrimary: Boolean = false) {
    val backgroundBrush = if (isPrimary) {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF8F0F0F), Color(0xFFD41414), Color(0xFF8F0F0F))
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF16161B), Color(0xFF1A1A24), Color(0xFF16161B))
        )
    }

    val borderModifier = if (isPrimary) {
        Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
    } else {
        Modifier.border(1.dp, Color(0xFFD41414).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundBrush)
            .then(borderModifier)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isPrimary) Color.White else Color.LightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

data class MapNode(
    val index: Int,
    val x: Float, // fraction of width
    val y: Float  // fraction of height
)

fun getMapIconResId(index: Int): Int {
    return when (index) {
        0 -> R.drawable.fantasy_map_icons_object_01 // 1: burning castle
        1 -> R.drawable.fantasy_map_icons_object_02 // 2: dunes training
        2 -> R.drawable.fantasy_map_icons_object_03 // 3: bridge crossing
        3 -> R.drawable.fantasy_map_icons_object_04 // 4: sword in stone
        4 -> R.drawable.fantasy_map_icons_object_06 // 5: dark red castle (formerly 06)
        5 -> R.drawable.fantasy_map_icons_object_07 // 6: portal (formerly 07)
        6 -> R.drawable.fantasy_map_icons_object_05 // 7: city of machines (formerly 05)
        7 -> R.drawable.fantasy_map_icons_object_08 // 8: archer village
        8 -> R.drawable.fantasy_map_icons_object_11 // 9: highlands floating island (formerly 11)
        9 -> R.drawable.fantasy_map_icons_object_09 // 10: escape from fortress (formerly 09)
        10 -> R.drawable.fantasy_map_icons_object_10 // 11: three-eyed beast
        11 -> R.drawable.fantasy_map_icons_object_12 // 12: lava guardian
        12 -> R.drawable.fantasy_map_icons_object_13 // 13: path of the ronin torii
        else -> R.drawable.fantasy_map_icons_object_01
    }
}

@Composable
fun StageSelectScreen(unlockedStageCount: Int, onSelectStage: (Int) -> Unit, onBack: () -> Unit) {
    var selectedStageIndex by remember { mutableIntStateOf(0) }
    val currentStage = Stages.stagesList.getOrNull(selectedStageIndex) ?: Stages.stagesList[0]
    val isSelectedUnlocked = selectedStageIndex < unlockedStageCount

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E13)) // solid background under map
    ) {
        // Main map background image with reduced opacity to enhance contrast of icons and text
        Image(
            painter = painterResource(id = R.drawable.bg_map),
            contentDescription = "Campaign Map",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.9f
        )

        // Subtle dark overlay to ground the UI elements
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.05f)) // extremely light so map art stays fully clear
        )

        // Atmospheric "fear background" vignette overlay (dark red/black glow at screen borders)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color(0xFF1E0505).copy(alpha = 0.8f) // Deep blood red fear glow
                        ),
                        radius = 1600f
                    )
                )
        )

        // Map Node Hotspots overlay
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val containerWidth = maxWidth
            val containerHeight = maxHeight

            val mapNodes = listOf(
                // Sequence matches physical progression trail: 1 -> 2 -> 3 -> 4 -> 8 -> 5 -> 6 -> 7 -> 9 -> 10 -> 11 -> 12 -> 13
                MapNode(1, 0.15f, 0.20f),
                MapNode(2, 0.39f, 0.22f),
                MapNode(3, 0.67f, 0.18f),
                MapNode(4, 0.87f, 0.25f),
                MapNode(8, 0.85f, 0.53f),
                MapNode(5, 0.67f, 0.46f),
                MapNode(6, 0.45f, 0.45f),
                MapNode(7, 0.13f, 0.47f),
                MapNode(9, 0.12f, 0.70f),
                MapNode(10, 0.35f, 0.72f),
                MapNode(11, 0.59f, 0.69f),
                MapNode(12, 0.85f, 0.78f),
                MapNode(13, 0.48f, 0.87f)
            )

            // Draw connecting path dashed line
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = androidx.compose.ui.graphics.Path()
                mapNodes.forEachIndexed { i, node ->
                    val x = size.width * node.x
                    val y = size.height * node.y
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(0xFFD41414), // Red path line
                    style = Stroke(
                        width = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )
                )
            }

            mapNodes.forEach { node ->
                val idx = node.index - 1
                val isUnlocked = idx < unlockedStageCount
                val isSelected = selectedStageIndex == idx

                // Pulsing animation for selected/active nodes
                val infiniteTransition = rememberInfiniteTransition(label = "pulse_${node.index}")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 0.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                // Dynamically size key milestones (Aku's Castle Node 5, Time Portal Node 6, Highlands Node 9, Ronin Torii Node 13)
                val isBigNode = node.index == 5 || node.index == 6 || node.index == 9 || node.index == 13
                val widthDp = if (isBigNode) 160.dp else 130.dp
                val heightDp = if (isBigNode) 100.dp else 80.dp
                val halfWidth = if (isBigNode) 80.dp else 65.dp
                val halfHeight = if (isBigNode) 50.dp else 40.dp

                // Position node container box centered around path coordinate
                Box(
                    modifier = Modifier
                        .size(width = widthDp, height = heightDp)
                        .absoluteOffset(
                            x = containerWidth * node.x - halfWidth,
                            y = containerHeight * node.y - halfHeight
                        )
                        .alpha(if (isUnlocked) 1.0f else 0.45f)
                        .clickable { selectedStageIndex = idx },
                    contentAlignment = Alignment.Center
                ) {
                    // Glowing red aura behind selected node
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(width = widthDp - 15.dp, height = heightDp - 15.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                    alpha = pulseAlpha
                                }
                                .background(
                                    color = Color(0xFFD41414),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        )
                    }

                    // Main custom illustrated fantasy icon (with built-in text label)
                    Image(
                        painter = painterResource(id = getMapIconResId(idx)),
                        contentDescription = "Stage ${node.index} Icon",
                        modifier = Modifier.size(width = widthDp, height = heightDp),
                        contentScale = ContentScale.Fit
                    )

                    // Small gold circular badge displaying the stage number
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                            .absoluteOffset(
                                x = if (isBigNode) (-24).dp else (-12).dp,
                                y = if (isBigNode) 10.dp else 6.dp
                            )
                            .background(
                                color = Color(0xFFFFD700), // Gold
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = Color.Black,
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${node.index}",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Unified Glassmorphic Top Header Bar (acting as both Header and selected details card)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.90f),
                            Color(0xFF1E0505).copy(alpha = 0.80f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Back to Menu button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color(0xFFD41414).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BACK TO MENU",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                // Center: Current Selected Stage Details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PORTAL ${selectedStageIndex + 1}: ${currentStage.title}",
                        color = Color(0xFFFFCC00),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = currentStage.subtitle.uppercase(),
                        color = Color(0xFFD41414), // Red fear subtitle
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentStage.objective,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }

                // Right side: Enter Portal action button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelectedUnlocked) {
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF8F0F0F), Color(0xFFD41414), Color(0xFF8F0F0F))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(Color.DarkGray, Color.Gray, Color.DarkGray)
                                )
                            }
                        )
                        .border(
                            1.dp,
                            if (isSelectedUnlocked) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable(enabled = isSelectedUnlocked) {
                            onSelectStage(selectedStageIndex)
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSelectedUnlocked) "ENTER PORTAL" else "LOCKED",
                        color = if (isSelectedUnlocked) Color.White else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
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
fun SlantedProgressBar(
    progress: Float,
    laggingProgress: Float = progress,
    color: Brush,
    laggingColor: Color = Color.Transparent,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val skew = height * 0.4f // Slant angle proportion
        
        // Background path
        val bgPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(skew, 0f)
            lineTo(width, 0f)
            lineTo(width - skew, height)
            lineTo(0f, height)
            close()
        }
        drawPath(bgPath, color = backgroundColor)
        
        // Lagging progress path (if any)
        if (laggingProgress > progress && laggingColor != Color.Transparent) {
            val lagWidth = skew + (width - skew) * laggingProgress
            val lagPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(skew, 0f)
                lineTo(lagWidth, 0f)
                lineTo(lagWidth - skew, height)
                lineTo(0f, height)
                close()
            }
            drawPath(lagPath, color = laggingColor)
        }
        
        // Main progress path
        if (progress > 0f) {
            val progWidth = skew + (width - skew) * progress
            val progPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(skew, 0f)
                lineTo(progWidth, 0f)
                lineTo(progWidth - skew, height)
                lineTo(0f, height)
                close()
            }
            drawPath(progPath, brush = color)
        }
    }
}

@Composable
fun GameplayActionButton(
    icon: String,
    onClick: () -> Unit,
    sizeDp: Dp,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "btnScale"
    )
    
    val glowColor = if (isPressed) Color(0xFFD41414) else Color.White.copy(alpha = 0.6f)
    val buttonBg = if (isPressed) {
        Brush.radialGradient(listOf(Color(0xFF6B0A0A), Color(0xFF1E0505)))
    } else {
        Brush.radialGradient(listOf(Color(0x44FFFFFF), Color(0x1AFFFFFF)))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .size(sizeDp)
            .background(buttonBg, RoundedCornerShape(sizeDp / 2))
            .border(2.dp, glowColor, RoundedCornerShape(sizeDp / 2))
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable default ripple to use our custom scale/glow feedback
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = if (sizeDp > 50.dp) 26.sp else 18.sp,
            modifier = Modifier.alpha(if (isPressed) 1.0f else 0.85f)
        )
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
    var animatedHealth by remember { mutableStateOf(playerHealth) }
    LaunchedEffect(playerHealth) {
        delay(300) // Small delay before lagging health catches up
        animate(
            initialValue = animatedHealth,
            targetValue = playerHealth,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animatedHealth = value
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. Top-Left Player Profile HUD
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color(0xFF1E0505).copy(alpha = 0.8f))
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(1.5.dp, Color(0xFFD41414).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar (Jack's eyes crop)
                val healthPercentage = playerHealth / 100f
                val avatarRingColor = when {
                    healthPercentage > 0.5f -> Color(0xFF4CAF50) // Green
                    healthPercentage > 0.25f -> Color(0xFFFF9800) // Orange
                    else -> Color(0xFFF44336) // Red
                }
                
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .border(2.dp, avatarRingColor, RoundedCornerShape(27.dp))
                        .background(Color.Black, RoundedCornerShape(27.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.black_eyes),
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .graphicsLayer {
                                clip = true
                                shape = RoundedCornerShape(25.dp)
                            }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    // Health Bar (HP label)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "HP",
                            color = Color(0xFFE57373),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.width(22.dp)
                        )
                        SlantedProgressBar(
                            progress = (playerHealth / 100f).coerceIn(0f, 1f),
                            laggingProgress = (animatedHealth / 100f).coerceIn(0f, 1f),
                            color = Brush.horizontalGradient(listOf(Color(0xFFB71C1C), Color(0xFFE53935))),
                            laggingColor = Color(0xFFFFCDD2).copy(alpha = 0.6f),
                            backgroundColor = Color(0xFF2E0C0C),
                            modifier = Modifier
                                .size(170.dp, 13.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${playerHealth.toInt()}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Sword Energy
                    val isEnergyFull = playerSwordEnergy >= 100f
                    val infiniteTransition = rememberInfiniteTransition(label = "energyPulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NRG",
                            color = if (isEnergyFull) Color(0xFFFFD54F) else Color(0xFF4DD0E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.width(22.dp)
                        )
                        
                        val energyBrush = if (isEnergyFull) {
                            Brush.horizontalGradient(listOf(Color(0xFFFF8F00), Color(0xFFFFC107)))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFF00838F), Color(0xFF00E5FF)))
                        }
                        
                        SlantedProgressBar(
                            progress = (playerSwordEnergy / 100f).coerceIn(0f, 1f),
                            color = energyBrush,
                            backgroundColor = Color(0xFF0A222B),
                            modifier = Modifier
                                .size(170.dp, 8.dp)
                                .graphicsLayer {
                                    if (isEnergyFull) {
                                        alpha = pulseAlpha
                                    }
                                }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnergyFull) "MAX!" else "${playerSwordEnergy.toInt()}%",
                            color = if (isEnergyFull) Color(0xFFFFD54F) else Color(0xFF80DEEA),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stats: Coins & Crystals
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🪙", fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$playerCoins",
                                color = Color(0xFFFFD700),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Row(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💎", fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$playerCrystals",
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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

        // 4. Floating Virtual Joystick (Active anywhere on the left panel)
        var joystickCenter by remember { mutableStateOf(Offset.Zero) }
        var knobOffset by remember { mutableStateOf(Offset.Zero) }
        var isJoystickPressed by remember { mutableStateOf(false) }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }
        
        val density = LocalDensity.current
        val joystickRadius = 50.dp
        val joystickRadiusPx = with(density) { joystickRadius.toPx() }
        val joystickBaseSizePx = with(density) { 100.dp.toPx() }
        
        val defaultCenter = Offset(
            x = with(density) { 100.dp.toPx() },
            y = if (containerSize.height > 0) {
                containerSize.height - with(density) { 100.dp.toPx() }
            } else {
                0f
            }
        )
        val activeCenter = if (isJoystickPressed) joystickCenter else defaultCenter

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.48f)
                .align(Alignment.BottomStart)
                .onGloballyPositioned { coordinates ->
                    containerSize = coordinates.size
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent()
                            val firstChange = down.changes.firstOrNull() ?: continue
                            if (firstChange.pressed) {
                                firstChange.consume()
                                isJoystickPressed = true
                                joystickCenter = firstChange.position
                                knobOffset = Offset.Zero
                                
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pointerChange = event.changes.firstOrNull() ?: break
                                    if (pointerChange.pressed) {
                                        pointerChange.consume()
                                        val dragOffset = pointerChange.position - joystickCenter
                                        val dist = kotlin.math.sqrt(dragOffset.x * dragOffset.x + dragOffset.y * dragOffset.y)
                                        if (dist > joystickRadiusPx) {
                                            knobOffset = Offset(
                                                dragOffset.x * joystickRadiusPx / dist,
                                                dragOffset.y * joystickRadiusPx / dist
                                            )
                                        } else {
                                            knobOffset = dragOffset
                                        }
                                        onMove(-knobOffset.x / joystickRadiusPx, -knobOffset.y / joystickRadiusPx)
                                    } else {
                                        break
                                    }
                                }
                                isJoystickPressed = false
                                knobOffset = Offset.Zero
                                onMove(0f, 0f)
                            }
                        }
                    }
                }
        ) {
            // Draw joystick relative to activeCenter
            if (activeCenter.y > 0) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { (activeCenter.x - joystickBaseSizePx / 2f).toDp() },
                            y = with(density) { (activeCenter.y - joystickBaseSizePx / 2f).toDp() }
                        )
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (isJoystickPressed) {
                                    listOf(Color(0x33D41414), Color(0x11D41414))
                                } else {
                                    listOf(Color(0x22FFFFFF), Color(0x05FFFFFF))
                                }
                            ),
                            shape = RoundedCornerShape(50.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = if (isJoystickPressed) Color(0xCCD41414) else Color(0x44FFFFFF),
                            shape = RoundedCornerShape(50.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isJoystickPressed) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text("▲", color = Color(0x33D41414), fontSize = 10.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
                            Text("▼", color = Color(0x33D41414), fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
                            Text("◀", color = Color(0x33D41414), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp))
                            Text("▶", color = Color(0x33D41414), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
                        }
                    }
                    
                    // Knob
                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(density) { knobOffset.x.toDp() },
                                y = with(density) { knobOffset.y.toDp() }
                            )
                            .size(46.dp)
                            .background(
                                color = if (isJoystickPressed) Color(0xCC1E0505) else Color(0x99FFFFFF),
                                shape = RoundedCornerShape(23.dp)
                            )
                            .border(
                                width = 2.5.dp,
                                color = if (isJoystickPressed) Color(0xFFD41414) else Color.White,
                                shape = RoundedCornerShape(23.dp)
                            )
                    )
                }
            }
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
            GameplayActionButton(
                icon = "🗡️",
                onClick = onMeleeAttack,
                sizeDp = 72.dp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )

            // Jump Button (Top-Right)
            GameplayActionButton(
                icon = "⬆️",
                onClick = onJump,
                sizeDp = 48.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp)
            )

            // Defense / Block Button (Bottom-Left)
            GameplayActionButton(
                icon = "🛡️",
                onClick = onBlock,
                sizeDp = 48.dp,
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 12.dp)
            )
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

            MenuButton("RETRY PORTAL", onRetry, isPrimary = true)
            Spacer(modifier = Modifier.height(12.dp))
            MenuButton("RETURN TO MAP", onMenu, isPrimary = false)
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

            MenuButton("RETURN TO MENU", onMenu, isPrimary = true)
        }
    }
}
