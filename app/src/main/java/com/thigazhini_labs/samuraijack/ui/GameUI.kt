package com.thigazhini_labs.samuraijack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    playerShield: Float,
    kills: Int,
    score: Int,
    onStartGame: () -> Unit,
    onBackToMenu: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onPickup: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (gameState) {
            GameState.SPLASH -> SplashScreen(onStartGame)
            GameState.MAIN_MENU -> MainMenuScreen(onStartGame)
            GameState.GAMEPLAY -> GameplayHUD(
                playerHealth = playerHealth,
                playerShield = playerShield,
                kills = kills,
                score = score,
                onMove = onMove,
                onPickup = onPickup
            )
        }

        // Block system back during gameplay
        if (gameState == GameState.GAMEPLAY) {
            BackHandler { /* consume — do nothing */ }
        }

        // Overlays (storm, etc.)
        if (gameState == GameState.GAMEPLAY) {
            if (currentStageIndex == 0) {
                StormOverlay(isInside = isStage1InsideMine)
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
fun MainMenuScreen(onStart: () -> Unit) {
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
fun GameplayHUD(
    playerHealth: Float,
    playerShield: Float,
    kills: Int,
    score: Int,
    onMove: (Float, Float) -> Unit,
    onPickup: () -> Unit
) {
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {

        // === LEFT MOVEMENT JOYSTICK (always visible) ===
        val moveRadiusPx = with(density) { 65.dp.toPx() }
        val moveBasePx = with(density) { 150.dp.toPx() }
        var moveKnob by remember { mutableStateOf(Offset.Zero) }

        // Outer ring — movement area indicator
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 36.dp)
                .background(Color(0x220E1014), CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(150.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.width / 2f - 8f
                // Directional tick marks
                for (angle in listOf(0f, 90f, 180f, 270f)) {
                    val rad = Math.toRadians(angle.toDouble()).toFloat()
                    val inner = r - 14f
                    val outer = r - 2f
                    drawLine(Color.White.copy(alpha = 0.3f),
                        Offset(cx + kotlin.math.cos(rad) * inner, cy + kotlin.math.sin(rad) * inner),
                        Offset(cx + kotlin.math.cos(rad) * outer, cy + kotlin.math.sin(rad) * outer),
                        strokeWidth = 2f)
                }
            }
            // Inner ring — dead zone boundary
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(Color(0x440E1014), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Knob
                Box(
                    modifier = Modifier
                        .offset(x = with(density) { moveKnob.x.toDp() }, y = with(density) { moveKnob.y.toDp() })
                        .size(50.dp)
                        .background(Color(0xCC2F343C), CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                )
            }
        }

        // Touch area for joystick input (invisible overlay on bottom-left)
        Box(
            modifier = Modifier
                .fillMaxHeight(0.55f)
                .fillMaxWidth(0.5f)
                .align(Alignment.BottomStart)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent()
                            val ch = down.changes.firstOrNull() ?: continue
                            if (ch.pressed) {
                                ch.consume()
                                val center = Offset(moveBasePx / 2f, moveBasePx / 2f)
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val pc = ev.changes.firstOrNull() ?: break
                                    if (pc.pressed) {
                                        pc.consume()
                                        val off = pc.position - center
                                        val dist = kotlin.math.sqrt(off.x * off.x + off.y * off.y)
                                        moveKnob = if (dist > moveRadiusPx)
                                            Offset(off.x * moveRadiusPx / dist, off.y * moveRadiusPx / dist)
                                        else off
                                        onMove(-moveKnob.x / moveRadiusPx, -moveKnob.y / moveRadiusPx)
                                    } else break
                                }
                                moveKnob = Offset.Zero
                                onMove(0f, 0f)
                            }
                        }
                    }
                }
        )

        // === RIGHT-SIDE: HP & SHIELD (vertical stack, styled) ===
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 14.dp)
                .width(56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Background card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x44000000), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(vertical = 14.dp, horizontal = 6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- Health ---
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0x33000000), CircleShape)
                            .border(1.5.dp, Color(0xCCFF4444), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♥", color = Color(0xFFFF4444), fontSize = 22.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("${playerHealth.toInt()}", color = Color.White, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    // Health bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(6.dp)
                            .background(Color(0x551A0A0A), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((playerHealth / 100f).coerceIn(0f, 1f))
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFFCC2222), Color(0xFFFF5555))),
                                    RoundedCornerShape(3.dp))
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    // --- Shield ---
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x33000000), CircleShape)
                            .border(1.5.dp, Color(0xCC4488FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♦", color = Color(0xFF4488FF), fontSize = 20.sp)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text("${playerShield.toInt()}", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(5.dp)
                            .background(Color(0x440A1A2A), RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((playerShield / 100f).coerceIn(0f, 1f))
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFF2266BB), Color(0xFF4499FF))),
                                    RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
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
        animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing)),
        label = "rainT"
    )

    val rainT2 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing)),
        label = "rainT2"
    )

    val windPhase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing)),
        label = "windPhase"
    )

    val gustPhase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing)),
        label = "gustPhase"
    )

    Box(modifier = Modifier.fillMaxSize().alpha(targetAlpha)) {
        // Dark atmospheric overlay for cold storm feeling
        val darkAlpha by animateFloatAsState(
            targetValue = if (isInside) 0f else 0.25f,
            animationSpec = tween(1500), label = "darkAlpha"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A2035).copy(alpha = darkAlpha))
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val baseColor = Color(0xFF8FA8D0)
            val deepColor = Color(0xFF6078A0)

            val windRad = windPhase * TWO_PI
            val gustRad = gustPhase * TWO_PI
            val baseGust = kotlin.math.sin(gustRad) * 0.5f + 0.5f
            val windAngle = (kotlin.math.sin(windRad) * 12f + 8f) * (0.8f + baseGust * 0.4f)

            // Fine light rain drops (dense, subtle)
            for (g in 0 until 12) {
                val gSeed = g * 577L
                val count = 8 + ((gSeed * 13) % 6).toInt()
                for (d in 0 until count) {
                    val dSeed = gSeed + d * 233L
                    val phase = ((dSeed * 17) % 1000) / 1000f
                    val speed = 0.6f + ((dSeed * 11) % 100) / 300f
                    val yPos = ((rainT * speed) + phase) % 1f
                    val x = (((dSeed * 41) % 1000) / 1000f) * w
                    val len = 8f + ((dSeed * 7) % 6)
                    val a = windAngle + ((dSeed * 3) % 7).toFloat() - 3f
                    val alpha = 0.08f + ((dSeed * 19) % 1000) / 6000f

                    drawLine(
                        color = baseColor.copy(alpha = alpha),
                        start = Offset(x, yPos * h),
                        end = Offset(x + a, yPos * h + len),
                        strokeWidth = 0.5f + ((dSeed * 5) % 3) / 10f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Medium rain streaks (more visible)
            for (g in 0 until 8) {
                val gSeed = g * 733L + 100
                val count = 6 + ((gSeed * 11) % 5).toInt()
                for (d in 0 until count) {
                    val dSeed = gSeed + d * 199L
                    val phase = ((dSeed * 13) % 1000) / 1000f
                    val speed = 1.0f + ((dSeed * 7) % 100) / 250f
                    val yPos = ((rainT * speed) + phase) % 1f
                    val x = (((dSeed * 37) % 1000) / 1000f) * w
                    val len = 20f + ((dSeed * 5) % 12)
                    val a = windAngle + ((dSeed * 5) % 9).toFloat() - 4f
                    val alpha = 0.12f + ((dSeed * 17) % 1000) / 4000f

                    drawLine(
                        color = baseColor.copy(alpha = alpha),
                        start = Offset(x, yPos * h),
                        end = Offset(x + a, yPos * h + len),
                        strokeWidth = 0.9f + ((dSeed * 3) % 5) / 8f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Heavy rain streaks (fewer, dramatic)
            for (i in 0 until 40) {
                val seed = i * 839L + 200
                val phase = ((seed * 11) % 1000) / 1000f
                val speed = 1.6f + ((seed * 3) % 100) / 150f
                val yPos = ((rainT2 * speed) + phase) % 1f
                val x = (((seed * 31) % 1000) / 1000f) * w
                val len = 35f + ((seed * 7) % 16)
                val gustOffset = kotlin.math.sin(seed.toFloat() * 0.5f + windRad) * 8f
                val a = windAngle + gustOffset + ((seed * 5) % 7).toFloat() - 3f
                val alpha = 0.18f + ((seed * 13) % 1000) / 2000f

                val baseW = 1.8f + ((seed * 5) % 7) / 12f
                val endX = x + a
                val endY = yPos * h + len

                drawLine(
                    color = baseColor.copy(alpha = alpha * 0.7f),
                    start = Offset(x - baseW * 0.3f, yPos * h),
                    end = Offset(endX - baseW * 0.3f, endY),
                    strokeWidth = baseW * 0.7f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = alpha * 0.3f),
                    start = Offset(x, yPos * h),
                    end = Offset(endX, endY),
                    strokeWidth = baseW * 0.4f,
                    cap = StrokeCap.Round
                )
            }

            // Ground splashes
            val groundY = h - 12f
            for (i in 0 until 35) {
                val seed = i * 641L + 300
                val splashT = (rainT * 3.0f + ((seed * 11) % 1000) / 1000f) % 1f
                val splashX = (((seed * 23) % 1000) / 1000f) * w

                val rippleR = splashT * 24f
                drawCircle(
                    color = baseColor.copy(alpha = (1f - splashT) * 0.15f),
                    radius = rippleR,
                    center = Offset(splashX, groundY),
                    style = Stroke(width = 1.0f * (1f - splashT))
                )

                val crownCount = 2 + ((seed * 7) % 3).toInt()
                for (d in 0 until crownCount) {
                    val dSeed = seed + d * 31L
                    val cx = ((dSeed * 13) % 1000) / 1000f * 20f - 10f
                    val cy = ((dSeed * 7) % 1000) / 1000f * 10f
                    val t = (splashT * 1.6f) % 1f
                    val sAlpha = (1f - t) * 0.4f
                    if (sAlpha > 0f) {
                        val dy = cy * (1f - t) * (1f - t)
                        val dx = cx * (1f - t)
                        drawLine(
                            color = baseColor.copy(alpha = sAlpha),
                            start = Offset(splashX + dx - 1f, groundY - dy),
                            end = Offset(splashX + dx + 1f, groundY - dy + 3f),
                            strokeWidth = 1.0f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Mist/fog layer at ground
            for (i in 0 until 20) {
                val seed = i * 271L + 400
                val randX = ((seed * 17) % 1000) / 1000f
                val mistT = (rainT * 2.0f + ((seed * 5) % 1000) / 1000f) % 1f
                val mx = randX * w
                val spread = mistT * 26f
                val mAlpha = (1f - mistT) * 0.12f
                drawCircle(
                    color = Color(0xFFC8D8F0).copy(alpha = mAlpha),
                    radius = spread,
                    center = Offset(mx, groundY + 2f),
                    style = Stroke(width = 1.2f)
                )
            }

            // Heavy fog bands
            for (i in 0 until 6) {
                val seed = i * 919L + 500
                val randX = ((seed * 43) % 1000) / 1000f
                val phase = ((seed * 23) % 1000) / 1000f
                val speed = 0.12f + ((seed * 7) % 100) / 500f
                val yPos = ((rainT * speed) + phase) % 1f
                val x = randX * w
                val drift = kotlin.math.sin(randX * TWO_PI * 0.5f + windRad * 0.3f) * 10f
                val r = 6f + ((seed * 11) % 8)
                val alpha = 0.06f + ((seed * 13) % 1000) / 4000f
                drawCircle(
                    color = deepColor.copy(alpha = alpha),
                    radius = r,
                    center = Offset(x + drift, yPos * h),
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.06f),
                    radius = r * 0.2f,
                    center = Offset(x + drift - r * 0.2f, yPos * h - r * 0.2f)
                )
            }
        }
    }
}




