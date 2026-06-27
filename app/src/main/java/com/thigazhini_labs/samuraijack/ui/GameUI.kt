package com.thigazhini_labs.samuraijack.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.delay

@Composable
fun GameUI(
    gameState: GameState,
    currentStageIndex: Int,
    isStage1InsideMine: Boolean,
    playerHealth: Float,
    playerSwordEnergy: Float,
    isHealthRegenActive: Boolean,
    enemiesRemaining: Int,
    typedText: String,
    isTextFullyTyped: Boolean,
    jackState: String,
    playerCoins: Int,
    playerCrystals: Int,
    onStartGame: () -> Unit,
    onSelectStage: (Int) -> Unit,

    onRetryStage: () -> Unit,
    onBackToMenu: () -> Unit,
    onAdvanceText: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onLook: (Float, Float) -> Unit,
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
            GameState.INTRO_CUTSCENE, GameState.OUTRO_CUTSCENE -> CutsceneOverlay(typedText, onAdvanceText)
            GameState.GAMEPLAY -> GameplayHUD(
                playerHealth = playerHealth,
                playerSwordEnergy = playerSwordEnergy,
                isHealthRegenActive = isHealthRegenActive,
                enemiesRemaining = enemiesRemaining,
                playerCoins = playerCoins,
                playerCrystals = playerCrystals,
                onBack = onBackToMenu,
                onMove = onMove,
                onLook = onLook,
                onMeleeAttack = onMeleeAttack,
                onJump = onJump,
                onBlock = onBlock
            )
            GameState.GAME_OVER -> GameOverScreen(onRetryStage, onBackToMenu)
        }

        // Gameplay combat overlays (Slash, flash, speed lines) based on jackState
        if (gameState == GameState.GAMEPLAY) {

            if (currentStageIndex == 0) {
                // Pass the boolean state directly to the overlay
                StormOverlay(isInside = isStage1InsideMine) 
            }
            
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
        delay(600)
        animationStep = 2
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
    label: String? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var tapFlash by remember { mutableFloatStateOf(0f) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "btnScale"
    )
    
    LaunchedEffect(tapFlash) {
        if (tapFlash > 0f) {
            animate(
                initialValue = tapFlash,
                targetValue = 0f,
                animationSpec = tween(durationMillis = 280, easing = LinearEasing)
            ) { value, _ ->
                tapFlash = value
            }
        }
    }

    val glowColor = when {
        tapFlash > 0f -> Color(0xFF7ED3FF).copy(alpha = 0.8f + tapFlash * 0.2f)
        isPressed -> Color.White.copy(alpha = 0.95f)
        else -> Color.White.copy(alpha = 0.6f)
    }
    val buttonBg = if (isPressed) {
        Brush.radialGradient(listOf(Color(0x8848515B), Color(0xAA101318)))
    } else {
        Brush.radialGradient(listOf(Color(0x664A515E), Color(0xAA0B0D12)))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .size(sizeDp)
            .background(buttonBg, CircleShape)
            .border(2.dp, glowColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable default ripple to use our custom scale/glow feedback
                onClick = {
                    tapFlash = 1f
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (tapFlash > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(2.6.dp, Color(0xFF8CE8FF).copy(alpha = tapFlash), CircleShape)
            )
        }
        Text(
            text = icon,
            fontSize = if (sizeDp > 50.dp) 26.sp else 18.sp,
            modifier = Modifier.alpha(if (isPressed) 1.0f else 0.9f)
        )
        if (label != null) {
            Text(
                text = label,
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
            )
        }
    }
}

@Composable
fun GameplayHUD(
    playerHealth: Float,
    playerSwordEnergy: Float,
    isHealthRegenActive: Boolean,
    enemiesRemaining: Int,
    playerCoins: Int,
    playerCrystals: Int,
    onBack: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onLook: (Float, Float) -> Unit,
    onMeleeAttack: () -> Unit,
    onJump: () -> Unit,
    onBlock: () -> Unit
) {
    var animatedHealth by remember { mutableStateOf(playerHealth) }
    var previousHealth by remember { mutableStateOf(playerHealth) }
    var damageFlashAlpha by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playerHealth) {
        if (playerHealth < previousHealth) {
            damageFlashAlpha = 0.7f
            animate(
                initialValue = 0.7f,
                targetValue = 0f,
                animationSpec = tween(durationMillis = 360, easing = LinearEasing)
            ) { value, _ ->
                damageFlashAlpha = value
            }
        }
        previousHealth = playerHealth
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
        val healthPercentage = (playerHealth / 100f).coerceIn(0f, 1f)
        val lowHp = healthPercentage < 0.2f
        val infiniteTransition = rememberInfiniteTransition(label = "compactHudPulse")
        val lowHpPulse by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "lowHpPulse"
        )
        val regenRingAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "regenRingAlpha"
        )
        val regenFlow by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "regenFlow"
        )

        // 1. Compact Top-Left HUD
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 2.dp, top = 4.dp)
                .widthIn(max = 220.dp)
                .background(
                    Color(0xCC0D0F14),
                    RoundedCornerShape(12.dp)
                )
                .border(1.dp, Color(0xFF5A3A3A).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .border(
                        1.5.dp,
                        if (damageFlashAlpha > 0f) Color(0xFFFF4444) else Color(0x88AEDFFF),
                        CircleShape
                    )
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x33223344), Color(0xCC0D0F14))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isHealthRegenActive) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .border(
                                2.dp,
                                Color(0xFF64B5F6).copy(alpha = regenRingAlpha),
                                CircleShape
                            )
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.black_eyes),
                    contentDescription = "Samurai Jack Portrait",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
                if (damageFlashAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = damageFlashAlpha * 0.25f), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(7.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(Color(0x80201010), RoundedCornerShape(7.dp))
                        .border(
                            1.dp,
                            if (lowHp) Color(0xFFFF5252).copy(alpha = lowHpPulse) else Color(0xAA6A2A2A),
                            RoundedCornerShape(7.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(healthPercentage)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        if (lowHp) Color(0xFFC62828) else Color(0xFF8E1A1A),
                                        if (lowHp) Color(0xFFFF5252) else Color(0xFFE53935)
                                    )
                                ),
                                RoundedCornerShape(7.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((animatedHealth / 100f).coerceIn(0f, 1f))
                            .background(
                                Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(7.dp)
                            )
                    )
                    if (damageFlashAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.White.copy(alpha = damageFlashAlpha * 0.35f), RoundedCornerShape(7.dp))
                        )
                    }
                    if (isHealthRegenActive) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val x1 = size.width * regenFlow
                            val x2 = (size.width * ((regenFlow + 0.35f) % 1f))
                            val x3 = (size.width * ((regenFlow + 0.7f) % 1f))
                            drawCircle(Color(0xFF7ED3FF).copy(alpha = 0.8f), radius = 1.6f, center = Offset(x1, size.height * 0.45f))
                            drawCircle(Color(0xFF7ED3FF).copy(alpha = 0.65f), radius = 1.2f, center = Offset(x2, size.height * 0.62f))
                            drawCircle(Color(0xFF7ED3FF).copy(alpha = 0.75f), radius = 1.4f, center = Offset(x3, size.height * 0.35f))
                        }
                    }
                    Text(
                        text = "${playerHealth.toInt()}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(0xAA0D1D26), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((playerSwordEnergy / 100f).coerceIn(0f, 1f))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF006C8D), Color(0xFF00D2FF))),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0x8A0B0E13), RoundedCornerShape(8.dp))
                            .border(0.8.dp, Color(0x99FFD54F), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🪙", fontSize = 8.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "$playerCoins", color = Color(0xFFFFD54F), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier
                            .background(Color(0x8A0B0E13), RoundedCornerShape(8.dp))
                            .border(0.8.dp, Color(0x9939D5FF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💎", fontSize = 8.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "$playerCrystals", color = Color(0xFF39D5FF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isHealthRegenActive) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(Color(0xFF5CC8FF), CircleShape)
                                .border(1.dp, Color(0xFFD6F4FF), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "REGEN",
                            color = Color(0xFF7ED3FF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }

        // 2. Enemies Remaining Middle
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
                .background(Color(0xC0000000), RoundedCornerShape(6.dp))
                .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text(
                text = "ENEMIES: $enemiesRemaining",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.6.sp
            )
        }

        // 3. Map & Collapsible Jump Cluster (Top-Right)
        var isMapExpanded by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Map Icon (Always Visible)
            GameplayActionButton(
                icon = "🗺️",
                onClick = { 
                    isMapExpanded = !isMapExpanded 
                },
                sizeDp = 52.dp,
                label = "MAP"
            )
            
            // Jump Icon (Collapses in/out when Map is tapped)
            AnimatedVisibility(
                visible = isMapExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    GameplayActionButton(
                        icon = "⬆",
                        onClick = onJump,
                        sizeDp = 48.dp,
                        label = "JUMP"
                    )
                }
            }
        }

        // 4. Floating Virtual Joystick (Active anywhere on the left panel)
        var joystickCenter by remember { mutableStateOf(Offset.Zero) }
        var knobOffset by remember { mutableStateOf(Offset.Zero) }
        var isJoystickPressed by remember { mutableStateOf(false) }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }
        
        val density = LocalDensity.current
        val joystickRadius = 66.dp
        val joystickRadiusPx = with(density) { joystickRadius.toPx() }
        val joystickBaseSizePx = with(density) { 132.dp.toPx() }
        
        val defaultCenter = Offset(
            x = with(density) { 100.dp.toPx() },
            y = if (containerSize.height > 0) {
                containerSize.height - with(density) { 118.dp.toPx() }
            } else {
                0f
            }
        )
        val activeCenter = if (isJoystickPressed) joystickCenter else defaultCenter

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.6f)
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
                        .size(132.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (isJoystickPressed) {
                                    listOf(Color(0x4D9CA3AF), Color(0x220E1014))
                                } else {
                                    listOf(Color(0x339CA3AF), Color(0x220E1014))
                                }
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.4.dp,
                            color = Color.White.copy(alpha = if (isJoystickPressed) 0.8f else 0.55f),
                            shape = CircleShape
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
                            .size(52.dp)
                            .background(
                                color = if (isJoystickPressed) Color(0xCC2F343C) else Color(0xBBD2D5DA),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.5.dp,
                                color = Color.White.copy(alpha = if (isJoystickPressed) 0.9f else 0.78f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }

        // 5. Camera look joystick centered on right side with clear cardinal arrows
        var lookCenter by remember { mutableStateOf(Offset.Zero) }
        var lookKnobOffset by remember { mutableStateOf(Offset.Zero) }
        var isLookPressed by remember { mutableStateOf(false) }
        val lookRadius = 58.dp
        val lookRadiusPx = with(density) { lookRadius.toPx() }
        val lookBaseSizePx = with(density) { 120.dp.toPx() }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .size(128.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent()
                            val firstChange = down.changes.firstOrNull() ?: continue
                            if (firstChange.pressed) {
                                firstChange.consume()
                                isLookPressed = true
                                lookCenter = Offset(size.width / 2f, size.height / 2f)
                                lookKnobOffset = Offset.Zero

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pointerChange = event.changes.firstOrNull() ?: break
                                    if (pointerChange.pressed) {
                                        pointerChange.consume()
                                        val dragOffset = pointerChange.position - lookCenter
                                        val dist = kotlin.math.sqrt(dragOffset.x * dragOffset.x + dragOffset.y * dragOffset.y)
                                        lookKnobOffset = if (dist > lookRadiusPx) {
                                            Offset(
                                                dragOffset.x * lookRadiusPx / dist,
                                                dragOffset.y * lookRadiusPx / dist
                                            )
                                        } else {
                                            dragOffset
                                        }
                                        onLook(-lookKnobOffset.x / lookRadiusPx, -lookKnobOffset.y / lookRadiusPx)
                                    } else {
                                        break
                                    }
                                }
                                isLookPressed = false
                                lookKnobOffset = Offset.Zero
                                onLook(0f, 0f)
                            }
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x664E6B87), Color(0x220B0E13))
                        ),
                        CircleShape
                    )
                    .border(
                        width = 2.4.dp,
                        color = Color(0xFFAEDFFF).copy(alpha = if (isLookPressed) 0.95f else 0.72f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(size.width / 2f, 12f),
                        end = Offset(size.width / 2f, size.height - 12f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(12f, size.height / 2f),
                        end = Offset(size.width - 12f, size.height / 2f),
                        strokeWidth = 2f
                    )
                }
                Text("↑ N", color = Color.White.copy(alpha = 0.95f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
                Text("↓ S", color = Color.White.copy(alpha = 0.95f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
                Text("← W", color = Color.White.copy(alpha = 0.95f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
                Text("E →", color = Color.White.copy(alpha = 0.95f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp))
                Text("✥", color = Color(0xFFB7E7FF).copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.align(Alignment.Center))

                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { lookKnobOffset.x.toDp() },
                            y = with(density) { lookKnobOffset.y.toDp() }
                        )
                        .size(42.dp)
                        .background(
                            color = if (isLookPressed) Color(0xCC2D3A4A) else Color(0xAA9BB2C8),
                            shape = CircleShape
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.88f), CircleShape)
                )
            }
        }

        // 6. Action Button Cluster (Bottom-Right) - Jump Removed[cite: 5]
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
                .size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Large Sword Attack (Bottom-Right)
            GameplayActionButton(
                icon = "⚔",
                onClick = onMeleeAttack,
                sizeDp = 72.dp,
                label = "ATK",
                modifier = Modifier.align(Alignment.BottomEnd)
            )

            // Defense / Block Button (Bottom-Left)
            GameplayActionButton(
                icon = "🛡️",
                onClick = onBlock,
                sizeDp = 56.dp,
                label = "GUARD",
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 14.dp)
            )
        }



    }
}

private val TWO_PI = kotlin.math.PI.toFloat() * 2f

@Composable
fun StormOverlay(isInside: Boolean) {
    val targetAlpha by animateFloatAsState(targetValue = if (isInside) 0f else 1f, animationSpec = tween(1000))
    if (targetAlpha <= 0f) return

    val transition = rememberInfiniteTransition(label = "storm")

    val rainT by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing)),
        label = "rainT"
    )

    val rainT2 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(animation = tween(1300, easing = LinearEasing)),
        label = "rainT2"
    )

    val windPhase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(5000, easing = LinearEasing)),
        label = "windPhase"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(targetAlpha)) {
        val w = size.width
        val h = size.height
        val baseColor = Color(0xFFB0C8E8)

        val windRad = windPhase * TWO_PI
        val windAngle = kotlin.math.sin(windRad) * 10f + 5f

        val gustX = floatArrayOf(0.06f, 0.18f, 0.30f, 0.38f, 0.50f, 0.60f, 0.72f, 0.84f, 0.94f)
        val gustSpread = floatArrayOf(0.05f, 0.07f, 0.04f, 0.09f, 0.06f, 0.08f, 0.05f, 0.07f, 0.04f)

        for (g in gustX.indices) {
            val gSeed = g * 577L
            val count = 6 + ((gSeed * 13) % 5).toInt()
            for (d in 0 until count) {
                val dSeed = gSeed + d * 233L
                val offsetX = (((dSeed * 41) % 1000) / 1000f - 0.5f) * gustSpread[g] * 2f
                val phase = ((dSeed * 17) % 1000) / 1000f
                val speed = 0.7f + ((dSeed * 11) % 100) / 400f
                val yPos = ((rainT * speed) + phase) % 1f
                val x = (gustX[g] + offsetX) * w
                val len = 12f + ((dSeed * 7) % 8)
                val a = windAngle + ((dSeed * 3) % 7).toFloat() - 3f
                val alpha = 0.06f + ((dSeed * 19) % 1000) / 8000f

                drawLine(
                    color = baseColor.copy(alpha = alpha),
                    start = Offset(x, yPos * h),
                    end = Offset(x + a, yPos * h + len),
                    strokeWidth = 0.7f,
                    cap = StrokeCap.Round
                )
            }
        }

        for (g in gustX.indices) {
            val gSeed = g * 733L + 100
            val count = 8 + ((gSeed * 11) % 7).toInt()
            for (d in 0 until count) {
                val dSeed = gSeed + d * 199L
                val offsetX = (((dSeed * 37) % 1000) / 1000f - 0.5f) * gustSpread[g] * 2f
                val phase = ((dSeed * 13) % 1000) / 1000f
                val speed = 1.0f + ((dSeed * 7) % 100) / 300f
                val yPos = ((rainT * speed) + phase) % 1f
                val x = (gustX[g] + offsetX) * w
                val len = 22f + ((dSeed * 5) % 14)
                val a = windAngle + ((dSeed * 5) % 9).toFloat() - 4f
                val alpha = 0.10f + ((dSeed * 17) % 1000) / 5000f

                drawLine(
                    color = baseColor.copy(alpha = alpha),
                    start = Offset(x, yPos * h),
                    end = Offset(x + a, yPos * h + len),
                    strokeWidth = 1.1f + ((dSeed * 3) % 5) / 10f,
                    cap = StrokeCap.Round
                )
            }
        }

        for (i in 0 until 55) {
            val seed = i * 839L + 200
            val randX = ((seed * 31) % 1000) / 1000f
            val phase = ((seed * 11) % 1000) / 1000f
            val speed = 1.4f + ((seed * 3) % 100) / 180f
            val yPos = ((rainT2 * speed) + phase) % 1f
            val x = randX * w
            val len = 32f + ((seed * 7) % 18)
            val gustOffset = kotlin.math.sin(randX * TWO_PI * 3f + windRad) * 6f
            val a = windAngle + gustOffset + ((seed * 5) % 7).toFloat() - 3f
            val alpha = 0.20f + ((seed * 13) % 1000) / 2500f
            val topAlpha = (alpha * (1f - (yPos * 0.3f)).coerceAtLeast(0.35f))
            val baseW = 2.0f + ((seed * 5) % 7) / 10f
            val tipW = baseW * 0.35f

            val endX = x + a
            val endY = yPos * h + len
            val sx = x
            val sy = yPos * h

            drawLine(
                color = baseColor.copy(alpha = topAlpha * 0.5f),
                start = Offset(sx - tipW / 2f, sy),
                end = Offset(endX - baseW / 2f, endY),
                strokeWidth = tipW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = baseColor.copy(alpha = topAlpha),
                start = Offset(sx, sy),
                end = Offset(endX, endY),
                strokeWidth = baseW * 0.6f,
                cap = StrokeCap.Round
            )
        }

        val groundY = h - 10f
        for (i in 0 until 28) {
            val seed = i * 641L + 300
            val randX = ((seed * 23) % 1000) / 1000f
            val splashT = (rainT * 3.5f + ((seed * 11) % 1000) / 1000f) % 1f
            val splashX = randX * w

            val rippleR = splashT * 28f
            drawCircle(
                color = baseColor.copy(alpha = (1f - splashT) * 0.18f),
                radius = rippleR,
                center = Offset(splashX, groundY),
                style = Stroke(width = 1.2f * (1f - splashT))
            )

            val crownCount = 3 + ((seed * 7) % 4).toInt()
            for (d in 0 until crownCount) {
                val dSeed = seed + d * 31L
                val cx = ((dSeed * 13) % 1000) / 1000f * 24f - 12f
                val cy = ((dSeed * 7) % 1000) / 1000f * 12f
                val t = (splashT * 1.8f) % 1f
                val alpha = (1f - t) * 0.45f
                if (alpha > 0f) {
                    val dy = cy * (1f - t) * (1f - t)
                    val dx = cx * (1f - t)
                    drawLine(
                        color = baseColor.copy(alpha = alpha),
                        start = Offset(splashX + dx - 1f, groundY - dy),
                        end = Offset(splashX + dx + 1f, groundY - dy + 4f),
                        strokeWidth = 1.2f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        for (i in 0 until 18) {
            val seed = i * 271L + 400
            val randX = ((seed * 17) % 1000) / 1000f
            val mistT = (rainT * 2.2f + ((seed * 5) % 1000) / 1000f) % 1f
            val mx = randX * w
            val spread = mistT * 22f
            val mAlpha = (1f - mistT) * 0.10f
            drawCircle(
                color = baseColor.copy(alpha = mAlpha),
                radius = spread,
                center = Offset(mx, groundY - 3f),
                style = Stroke(width = 1f)
            )
        }

        for (i in 0 until 7) {
            val seed = i * 919L + 500
            val randX = ((seed * 43) % 1000) / 1000f
            val phase = ((seed * 23) % 1000) / 1000f
            val speed = 0.15f + ((seed * 7) % 100) / 600f
            val yPos = ((rainT * speed) + phase) % 1f
            val x = randX * w
            val drift = kotlin.math.sin(randX * TWO_PI * 0.7f + windRad * 0.5f) * 8f
            val r = 4f + ((seed * 11) % 6)
            val alpha = 0.04f + ((seed * 13) % 1000) / 6000f
            drawCircle(
                color = baseColor.copy(alpha = alpha),
                radius = r,
                center = Offset(x + drift, yPos * h),
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.08f),
                radius = r * 0.25f,
                center = Offset(x + drift - r * 0.3f, yPos * h - r * 0.3f)
            )
        }
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

