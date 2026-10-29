package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcCyanGlow
import com.example.ui.theme.ArcCyanPrimary
import com.example.ui.theme.ArcCyanSecondary
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonPurple
import com.example.ui.viewmodel.AiProcessingStage
import com.example.ui.viewmodel.RobbyViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Futuristic Semi-Transparent HUD Overlay.
 * Visualizes AI processing states (Analyzing, Executing, Speaking, Listening)
 * with animated holographic reticles, cybernetic telemetry, and rhythmic haptic feedback.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FuturisticHudOverlay(
    viewModel: RobbyViewModel,
    modifier: Modifier = Modifier
) {
    val isVisible by viewModel.isHudOverlayVisible.collectAsState()
    val stage by viewModel.aiProcessingStage.collectAsState()
    val directive by viewModel.currentDirective.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val audioRms by viewModel.voiceManager.audioRms.collectAsState()

    val context = LocalContext.current
    val composeHaptic = LocalHapticFeedback.current
    val hapticController = remember { HudHapticFeedback(context, composeHaptic) }
    val hapticsEnabled = userProfile.hapticsEnabled
    val isUrdu = userProfile.language == "ur"

    // Trigger haptic pulses upon AI state transitions
    LaunchedEffect(stage) {
        hapticController.onStageTransition(stage, hapticsEnabled)
    }

    // Rhythmic neural pulse ticks during deep AI analysis
    LaunchedEffect(stage, hapticsEnabled) {
        if (stage == AiProcessingStage.ANALYZING && hapticsEnabled) {
            while (true) {
                delay(850L)
                hapticController.onNeuralPulseTick(true)
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(250)) + scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 0.94f),
        exit = fadeOut(tween(200)) + scaleOut(tween(250, easing = FastOutSlowInEasing), targetScale = 0.94f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("futuristic_hud_overlay")
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xC8061224), // Center semi-transparent deep space
                            Color(0xF0030712)  // Dark vignette perimeter
                        )
                    )
                )
                .statusBarsPadding()
        ) {
            // 1. Futuristic Cybernetic Canvas Layer (Scanlines, Reticles, Corner brackets)
            HudCyberneticCanvas(
                stage = stage,
                audioRms = audioRms,
                modifier = Modifier.fillMaxSize()
            )

            // 2. HUD Content Layer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Telemetry Header
                HudTopTelemetryBar(
                    stage = stage,
                    batteryLevel = telemetry.batteryLevel,
                    isCharging = telemetry.isCharging,
                    hapticsEnabled = hapticsEnabled,
                    onHapticsToggle = { viewModel.toggleHaptics(!hapticsEnabled) },
                    onDismiss = {
                        hapticController.onAbort(hapticsEnabled)
                        viewModel.dismissHudOverlay()
                    }
                )

                // Central Dynamic Hologram Core Display
                HudCentralHologram(
                    stage = stage,
                    audioRms = audioRms,
                    isUrdu = isUrdu,
                    directive = directive,
                    modifier = Modifier.weight(1f)
                )

                // Bottom Tactical Terminal & Control Console
                HudBottomControlPanel(
                    stage = stage,
                    statusBanner = statusBanner,
                    isUrdu = isUrdu,
                    onAbort = {
                        hapticController.onAbort(hapticsEnabled)
                        viewModel.abortAiProcessing()
                    },
                    onTestCycle = {
                        hapticController.onStageTransition(AiProcessingStage.ANALYZING, hapticsEnabled)
                        viewModel.simulateHudPulseDemo()
                    },
                    onDismiss = {
                        hapticController.onDirectiveComplete(hapticsEnabled)
                        viewModel.dismissHudOverlay()
                    }
                )
            }
        }
    }
}

/**
 * Canvas drawing animated HUD elements:
 * - Sweeping laser radar arc
 * - Rotating degree-calibrated reticles
 * - Corner targeting brackets
 * - Moving horizontal scanline
 */
@Composable
private fun HudCyberneticCanvas(
    stage: AiProcessingStage,
    audioRms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HudCanvasTransitions")

    // Outer reticle clockwise rotation
    val rotationOuter by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (stage == AiProcessingStage.ANALYZING) 4000 else 10000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRotate"
    )

    // Inner reticle counter-clockwise rotation
    val rotationInner by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (stage == AiProcessingStage.ANALYZING) 3000 else 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRotate"
    )

    // Vertical scanline sweep
    val scanlineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanlineProgress"
    )

    // Pulse wave expansion
    val pulseWave by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (stage == AiProcessingStage.ANALYZING) 1200 else 2400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseWave"
    )

    // Primary HUD glow color based on stage
    val activeColor = when (stage) {
        AiProcessingStage.ANALYZING -> ArcCyanPrimary
        AiProcessingStage.EXECUTING -> NeonAmber
        AiProcessingStage.SPEAKING -> NeonPurple
        AiProcessingStage.LISTENING -> ArcCyanGlow
        AiProcessingStage.IDLE -> ArcCyanPrimary
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h * 0.44f)

        // 1. Draw Corner Tactical Brackets
        val bracketLen = 36.dp.toPx()
        val bracketOffset = 20.dp.toPx()
        val bracketColor = activeColor.copy(alpha = 0.65f)
        val strokeW = 2.dp.toPx()

        // Top-Left
        drawLine(bracketColor, Offset(bracketOffset, bracketOffset), Offset(bracketOffset + bracketLen, bracketOffset), strokeW)
        drawLine(bracketColor, Offset(bracketOffset, bracketOffset), Offset(bracketOffset, bracketOffset + bracketLen), strokeW)

        // Top-Right
        drawLine(bracketColor, Offset(w - bracketOffset, bracketOffset), Offset(w - bracketOffset - bracketLen, bracketOffset), strokeW)
        drawLine(bracketColor, Offset(w - bracketOffset, bracketOffset), Offset(w - bracketOffset, bracketOffset + bracketLen), strokeW)

        // Bottom-Left
        drawLine(bracketColor, Offset(bracketOffset, h - bracketOffset), Offset(bracketOffset + bracketLen, h - bracketOffset), strokeW)
        drawLine(bracketColor, Offset(bracketOffset, h - bracketOffset), Offset(bracketOffset, h - bracketOffset - bracketLen), strokeW)

        // Bottom-Right
        drawLine(bracketColor, Offset(w - bracketOffset, h - bracketOffset), Offset(w - bracketOffset - bracketLen, h - bracketOffset), strokeW)
        drawLine(bracketColor, Offset(w - bracketOffset, h - bracketOffset), Offset(w - bracketOffset, h - bracketOffset - bracketLen), strokeW)

        // 2. Horizontal Moving Cyber Scanline
        val scanY = h * scanlineProgress
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    activeColor.copy(alpha = 0.35f),
                    activeColor.copy(alpha = 0.75f),
                    activeColor.copy(alpha = 0.35f),
                    Color.Transparent
                )
            ),
            start = Offset(0f, scanY),
            end = Offset(w, scanY),
            strokeWidth = 2.dp.toPx()
        )

        // 3. Central Radar Rings & Concentric Calibrations
        val baseRadius = (w.coerceAtMost(h) * 0.30f)

        // Subtle background radar grid
        drawCircle(
            color = activeColor.copy(alpha = 0.08f),
            radius = baseRadius * 1.25f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = activeColor.copy(alpha = 0.14f),
            radius = baseRadius * 0.95f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = activeColor.copy(alpha = 0.20f),
            radius = baseRadius * 0.65f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Crosshairs in the center
        val crossLen = baseRadius * 1.35f
        drawLine(
            color = activeColor.copy(alpha = 0.18f),
            start = Offset(center.x - crossLen, center.y),
            end = Offset(center.x + crossLen, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = activeColor.copy(alpha = 0.18f),
            start = Offset(center.x, center.y - crossLen),
            end = Offset(center.x, center.y + crossLen),
            strokeWidth = 1.dp.toPx()
        )

        // 4. Expanding Neural Pulse Wave
        val waveRadius = baseRadius * (0.4f + pulseWave * 0.85f)
        val waveAlpha = (1.0f - pulseWave).coerceIn(0f, 0.65f)
        drawCircle(
            color = activeColor.copy(alpha = waveAlpha),
            radius = waveRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // 5. Outer Segmented Reticle (Rotates Clockwise)
        rotate(degrees = rotationOuter, pivot = center) {
            val outerRadius = baseRadius * 1.15f
            for (i in 0 until 12) {
                val angleDeg = i * 30.0
                val angleRad = Math.toRadians(angleDeg)
                val isMajor = i % 3 == 0
                val tickLen = if (isMajor) 14.dp.toPx() else 7.dp.toPx()
                val start = Offset(
                    (center.x + (outerRadius - tickLen) * cos(angleRad)).toFloat(),
                    (center.y + (outerRadius - tickLen) * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    (center.x + outerRadius * cos(angleRad)).toFloat(),
                    (center.y + outerRadius * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = if (isMajor) activeColor else activeColor.copy(alpha = 0.45f),
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx()
                )
            }

            // Triple Arc Segments
            drawArc(
                color = activeColor.copy(alpha = 0.6f),
                startAngle = 10f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2f, outerRadius * 2f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = activeColor.copy(alpha = 0.6f),
                startAngle = 130f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2f, outerRadius * 2f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = activeColor.copy(alpha = 0.6f),
                startAngle = 250f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2f, outerRadius * 2f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 6. Inner Calibrated Ring (Rotates Counter-Clockwise)
        rotate(degrees = rotationInner, pivot = center) {
            val innerRadius = baseRadius * 0.78f
            for (i in 0 until 8) {
                val angleDeg = i * 45.0
                val angleRad = Math.toRadians(angleDeg)
                val dotCenter = Offset(
                    (center.x + innerRadius * cos(angleRad)).toFloat(),
                    (center.y + innerRadius * sin(angleRad)).toFloat()
                )
                drawCircle(
                    color = activeColor.copy(alpha = 0.85f),
                    radius = 2.5.dp.toPx(),
                    center = dotCenter
                )
            }

            drawArc(
                color = activeColor.copy(alpha = 0.4f),
                startAngle = 0f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                size = Size(innerRadius * 2f, innerRadius * 2f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = activeColor.copy(alpha = 0.4f),
                startAngle = 180f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                size = Size(innerRadius * 2f, innerRadius * 2f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 7. Radar Beam Sweeper
        rotate(degrees = rotationOuter * 1.5f, pivot = center) {
            val sweepRadius = baseRadius * 1.15f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        activeColor.copy(alpha = 0.02f),
                        activeColor.copy(alpha = 0.30f)
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 60f,
                useCenter = true,
                topLeft = Offset(center.x - sweepRadius, center.y - sweepRadius),
                size = Size(sweepRadius * 2f, sweepRadius * 2f)
            )
        }
    }
}

/**
 * Top Telemetry bar showing core system telemetry, haptic toggle, and dismiss button.
 */
@Composable
private fun HudTopTelemetryBar(
    stage: AiProcessingStage,
    batteryLevel: Int,
    isCharging: Boolean,
    hapticsEnabled: Boolean,
    onHapticsToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hud_top_telemetry_bar"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC0D1B2E),
        border = androidx.compose.foundation.BorderStroke(1.dp, ArcCyanPrimary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Status Indicator & Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (stage) {
                                AiProcessingStage.ANALYZING -> NeonAmber
                                AiProcessingStage.EXECUTING -> ArcCyanPrimary
                                AiProcessingStage.SPEAKING -> NeonPurple
                                AiProcessingStage.LISTENING -> ArcCyanGlow
                                AiProcessingStage.IDLE -> ArcCyanPrimary
                            }
                        )
                )

                Column {
                    Text(
                        text = "ROBBY TACTICAL HUD",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = ArcCyanPrimary
                    )
                    Text(
                        text = "CORE: GEMINI 3.8 FLASH // 60 FPS",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Right side telemetry & quick actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Battery Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x66000000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22355A))
                ) {
                    Text(
                        text = "BAT: $batteryLevel%${if (isCharging) "⚡" else ""}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ArcCyanSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Haptic Toggle Button
                IconButton(
                    onClick = onHapticsToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("hud_haptics_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Toggle Haptics",
                        tint = if (hapticsEnabled) ArcCyanGlow else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Close / Dismiss Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("hud_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss HUD",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Central Hologram Area showing current AI Processing Stage banner and directive.
 */
@Composable
private fun HudCentralHologram(
    stage: AiProcessingStage,
    audioRms: Float,
    isUrdu: Boolean,
    directive: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Stage Badge Pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = when (stage) {
                AiProcessingStage.ANALYZING -> NeonAmber.copy(alpha = 0.20f)
                AiProcessingStage.EXECUTING -> ArcCyanPrimary.copy(alpha = 0.20f)
                AiProcessingStage.SPEAKING -> NeonPurple.copy(alpha = 0.20f)
                AiProcessingStage.LISTENING -> ArcCyanGlow.copy(alpha = 0.20f)
                AiProcessingStage.IDLE -> ArcCyanPrimary.copy(alpha = 0.15f)
            },
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                when (stage) {
                    AiProcessingStage.ANALYZING -> NeonAmber
                    AiProcessingStage.EXECUTING -> ArcCyanPrimary
                    AiProcessingStage.SPEAKING -> NeonPurple
                    AiProcessingStage.LISTENING -> ArcCyanGlow
                    AiProcessingStage.IDLE -> ArcCyanPrimary.copy(alpha = 0.5f)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when (stage) {
                        AiProcessingStage.LISTENING -> if (isUrdu) "● صوتی سگنل موصول ہو رہا ہے" else "● ACOUSTIC SENSORS ACTIVE"
                        AiProcessingStage.ANALYZING -> if (isUrdu) "● نیورل پروسیسنگ جاری ہے" else "● NEURAL DIRECTIVES INFERENCE"
                        AiProcessingStage.EXECUTING -> if (isUrdu) "● کمانڈ پر عمل درآمد" else "● DIRECTIVE DISPATCH ACTIVE"
                        AiProcessingStage.SPEAKING -> if (isUrdu) "● صوتی ترسیل فعال ہے" else "● SYNTHETIC VOCALIZATION"
                        AiProcessingStage.IDLE -> if (isUrdu) "● ٹیکٹیکل ویو اسٹینڈ بائی" else "● TACTICAL STANDBY READY"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = when (stage) {
                        AiProcessingStage.ANALYZING -> NeonAmber
                        AiProcessingStage.EXECUTING -> ArcCyanPrimary
                        AiProcessingStage.SPEAKING -> NeonPurple
                        AiProcessingStage.LISTENING -> ArcCyanGlow
                        AiProcessingStage.IDLE -> ArcCyanPrimary
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Directive Text Box
        if (directive.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0x990A1728),
                border = androidx.compose.foundation.BorderStroke(1.dp, ArcCyanPrimary.copy(alpha = 0.25f)),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isUrdu) "حالیہ ہدایت" else "ACTIVE DIRECTIVE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = directive,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Real-time audio waveform / neural bars
        Spacer(modifier = Modifier.height(14.dp))
        HudEqualizerBars(
            stage = stage,
            audioRms = audioRms
        )
    }
}

/**
 * Animated equalizer frequency bars responding to audio and AI synthesis.
 */
@Composable
private fun HudEqualizerBars(
    stage: AiProcessingStage,
    audioRms: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EqualizerBounce")
    val bounceA by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "bA"
    )
    val bounceB by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(250, easing = LinearEasing), RepeatMode.Reverse),
        label = "bB"
    )
    val bounceC by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "bC"
    )

    val isActive = stage != AiProcessingStage.IDLE

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(32.dp)
    ) {
        val bars = listOf(bounceA, bounceB, bounceC, bounceB, bounceA, bounceC, bounceA, bounceB)
        bars.forEachIndexed { index, bounce ->
            val scale = if (isActive) {
                if (stage == AiProcessingStage.LISTENING) {
                    (audioRms * (index % 3 + 1) * 0.4f).coerceIn(0.15f, 1.0f)
                } else {
                    bounce
                }
            } else {
                0.15f
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((32 * scale).coerceAtLeast(4f).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when (stage) {
                            AiProcessingStage.ANALYZING -> NeonAmber
                            AiProcessingStage.EXECUTING -> ArcCyanPrimary
                            AiProcessingStage.SPEAKING -> NeonPurple
                            AiProcessingStage.LISTENING -> ArcCyanGlow
                            AiProcessingStage.IDLE -> ArcCyanPrimary.copy(alpha = 0.4f)
                        }
                    )
            )
        }
    }
}

/**
 * Bottom Tactical Control Panel with status log, Abort, and Diagnostics buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HudBottomControlPanel(
    stage: AiProcessingStage,
    statusBanner: String,
    isUrdu: Boolean,
    onAbort: () -> Unit,
    onTestCycle: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hud_bottom_control_panel"),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xE00A1629),
        border = androidx.compose.foundation.BorderStroke(1.dp, ArcCyanPrimary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Status Banner Console
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = ArcCyanPrimary,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = statusBanner.ifBlank { "NEURAL LINK OPERATIONAL" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFFE2E8F0),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Action Buttons
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Abort Directive Button
                OutlinedButton(
                    onClick = onAbort,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CrimsonPrimary,
                        containerColor = CrimsonPrimary.copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("hud_abort_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.StopCircle,
                        contentDescription = "Abort",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUrdu) "منسوخ کریں" else "ABORT DIRECTIVE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Simulate Test Pulse Button
                OutlinedButton(
                    onClick = onTestCycle,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ArcCyanPrimary,
                        containerColor = ArcCyanPrimary.copy(alpha = 0.10f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ArcCyanPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("hud_test_cycle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Test",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUrdu) "نیورل ٹیسٹ" else "TEST PULSE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dismiss / Minimize Button
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFCBD5E1),
                        containerColor = Color(0x331E293B)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier.testTag("hud_dismiss_action_button")
                ) {
                    Text(
                        text = if (isUrdu) "بند کریں" else "CLOSE HUD",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}
