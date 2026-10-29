package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    audioRms: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 15
    val infiniteTransition = rememberInfiniteTransition(label = "WaveBars")

    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BarAnim1"
    )

    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BarAnim2"
    )

    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BarAnim3"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val baseColor = MaterialTheme.colorScheme.primary

        for (i in 0 until barCount) {
            val distFromCenter = kotlin.math.abs(i - barCount / 2).toFloat() / (barCount / 2).toFloat()
            val centerWeight = 1f - (distFromCenter * 0.45f)

            val baseAnim = when (i % 3) {
                0 -> anim1
                1 -> anim2
                else -> anim3
            }

            val dynamicHeight = if (isListening) {
                val rmsBoost = (audioRms * 1.5f).coerceIn(0f, 1f)
                (0.15f + (baseAnim * 0.35f) + (rmsBoost * 0.5f)) * centerWeight
            } else if (isSpeaking) {
                (0.2f + (baseAnim * 0.65f)) * centerWeight
            } else {
                0.15f * centerWeight
            }.coerceIn(0.1f, 1f)

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(fraction = dynamicHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isListening || isSpeaking) baseColor else baseColor.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
