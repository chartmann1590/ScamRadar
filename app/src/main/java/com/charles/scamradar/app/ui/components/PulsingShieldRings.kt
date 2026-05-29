package com.charles.scamradar.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Brand-level pulsing radar animation. Three concentric rings pulse at staggered intervals
 * while a rotating sweep line traces the circumference. Reused across Scanning, QuickVerdict,
 * UrlScanning, and the home-screen widget so the visual language stays consistent.
 */
@Composable
fun PulsingShieldRings(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    diameter: Dp = 280.dp,
    showSweep: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    val transition = rememberInfiniteTransition(label = "rings")

    val pulse1 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )
    val pulse2 by transition.animateFloat(
        initialValue = 1.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )
    val pulse3 by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse3"
    )
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radiusPx = size.width / 2

            drawCircle(
                color = color.copy(alpha = 0.1f),
                radius = (radiusPx * pulse1),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = (radiusPx * 0.714f * pulse2),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = (radiusPx * 0.428f * pulse3),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = 0.08f),
                radius = 4.dp.toPx(),
                center = center
            )

            if (showSweep) {
                val sweepRad = Math.toRadians(sweepAngle.toDouble()).toFloat()
                val lineEnd = Offset(
                    center.x + radiusPx * kotlin.math.cos(sweepRad),
                    center.y + radiusPx * kotlin.math.sin(sweepRad)
                )
                drawLine(
                    color = color.copy(alpha = 0.5f),
                    start = center,
                    end = lineEnd,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        content()
    }
}
