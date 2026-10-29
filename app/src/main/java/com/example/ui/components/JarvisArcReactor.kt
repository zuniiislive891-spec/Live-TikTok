package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisArcReactor(
    isListening: Boolean,
    isSpeaking: Boolean,
    audioRms: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorRotation")

    // Outer slow rotation
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 4000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OuterRing"
    )

    // Inner reverse fast rotation
    val innerAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 2500 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "InnerRing"
    )

    // Breathing pulse
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breathing"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val glowColor = MaterialTheme.colorScheme.primaryContainer

    val effectiveRms = if (isListening) audioRms.coerceIn(0.1f, 1.0f) else if (isSpeaking) 0.5f else 0.05f
    val dynamicCoreScale = breathScale + (effectiveRms * 0.25f)

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("jarvis_arc_reactor")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 120.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.88f

            // 1. Background Ambient Radial Glow
            val ambientBrush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = if (isListening) 0.45f else 0.25f),
                    secondaryColor.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = center,
                radius = baseRadius * 1.1f
            )
            drawCircle(brush = ambientBrush, radius = baseRadius * 1.1f, center = center)

            // 2. Outer Tick Ring (HUD Telemetry marks)
            val tickCount = 48
            for (i in 0 until tickCount) {
                val angleRad = Math.toRadians((i * (360f / tickCount)).toDouble())
                val isMajor = i % 4 == 0
                val tickLen = if (isMajor) 10f else 5f
                val startDist = baseRadius - tickLen
                val endDist = baseRadius

                val startX = (center.x + startDist * cos(angleRad)).toFloat()
                val startY = (center.y + startDist * sin(angleRad)).toFloat()
                val endX = (center.x + endDist * cos(angleRad)).toFloat()
                val endY = (center.y + endDist * sin(angleRad)).toFloat()

                drawLine(
                    color = if (isMajor) primaryColor.copy(alpha = 0.8f) else primaryColor.copy(alpha = 0.35f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 2.5f else 1.2f
                )
            }

            // 3. Rotating Outer Segmented Arcs
            rotate(outerAngle, pivot = center) {
                drawArc(
                    color = primaryColor,
                    startAngle = 0f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.85f, center.y - baseRadius * 0.85f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 1.7f, baseRadius * 1.7f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = secondaryColor,
                    startAngle = 120f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.85f, center.y - baseRadius * 0.85f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 1.7f, baseRadius * 1.7f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryColor,
                    startAngle = 220f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.85f, center.y - baseRadius * 0.85f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 1.7f, baseRadius * 1.7f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }

            // 4. Middle Concentric Circuit Ring
            val midRadius = baseRadius * 0.65f
            drawCircle(
                color = primaryColor.copy(alpha = 0.35f),
                radius = midRadius,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Inner Reverse Arcs with Nodes
            rotate(innerAngle, pivot = center) {
                drawArc(
                    color = primaryColor.copy(alpha = 0.9f),
                    startAngle = 30f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(center.x - midRadius, center.y - midRadius),
                    size = androidx.compose.ui.geometry.Size(midRadius * 2f, midRadius * 2f),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Square)
                )
                drawArc(
                    color = secondaryColor.copy(alpha = 0.9f),
                    startAngle = 190f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(center.x - midRadius, center.y - midRadius),
                    size = androidx.compose.ui.geometry.Size(midRadius * 2f, midRadius * 2f),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Square)
                )

                // Orbiting Node Dots
                for (angle in listOf(30.0, 130.0, 190.0, 300.0)) {
                    val rad = Math.toRadians(angle)
                    val nx = (center.x + midRadius * cos(rad)).toFloat()
                    val ny = (center.y + midRadius * sin(rad)).toFloat()
                    drawCircle(color = primaryColor, radius = 3.5f, center = Offset(nx, ny))
                }
            }

            // 5. Pulsing AI Core (Reactive Arc Sphere)
            val coreRadius = baseRadius * 0.42f * dynamicCoreScale
            val coreBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    primaryColor,
                    secondaryColor.copy(alpha = 0.7f),
                    glowColor.copy(alpha = 0.2f)
                ),
                center = center,
                radius = coreRadius
            )
            drawCircle(brush = coreBrush, radius = coreRadius, center = center)

            // Core Outline
            drawCircle(
                color = primaryColor.copy(alpha = 0.9f),
                radius = coreRadius,
                center = center,
                style = Stroke(width = 2.5f)
            )
        }

        // Center Icon Indicator
        val icon = when {
            isSpeaking -> Icons.Default.VolumeUp
            else -> Icons.Default.Mic
        }
        val iconTint = if (isListening) primaryColor else Color.Black

        Icon(
            imageVector = icon,
            contentDescription = if (isListening) "Listening" else "Activate Voice",
            tint = iconTint,
            modifier = Modifier.size(36.dp)
        )
    }
}
