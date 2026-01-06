package com.trackfiercely.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated gradient progress bar with celebration effects.
 * - Animated gradient that shifts colors (teal to emerald to gold)
 * - Smooth width animation as progress changes
 * - At 100%: confetti/sparkle particle animation
 * - At milestones (25%, 50%, 75%): brief glow pulse effect
 */
@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    cornerRadius: Dp = 6.dp
) {
    // Smooth progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress"
    )

    // Infinite gradient shift animation
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientOffset"
    )

    // Milestone glow pulse
    val milestones = listOf(0.25f, 0.5f, 0.75f, 1f)
    var lastMilestone by remember { mutableFloatStateOf(0f) }
    var showGlow by remember { mutableStateOf(false) }

    LaunchedEffect(animatedProgress) {
        val currentMilestone = milestones.lastOrNull { animatedProgress >= it } ?: 0f
        if (currentMilestone > lastMilestone && currentMilestone > 0f) {
            showGlow = true
            lastMilestone = currentMilestone
        }
    }

    val glowAlpha by animateFloatAsState(
        targetValue = if (showGlow) 1f else 0f,
        animationSpec = if (showGlow) {
            tween(200, easing = FastOutSlowInEasing)
        } else {
            tween(600, easing = LinearOutSlowInEasing)
        },
        finishedListener = { if (showGlow) showGlow = false },
        label = "glow"
    )

    // Confetti particles for 100%
    val particles = remember { List(20) { ConfettiParticle() } }
    val confettiProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti"
    )

    // Colors for the gradient - Fall forest theme
    val forestGreen = Color(0xFF4A6741)    // Deep forest green
    val sageGreen = Color(0xFF7D9B76)      // Sage green
    val goldenYellow = Color(0xFFC9A227)   // Golden yellow
    val burntOrange = Color(0xFFD4843E)    // Burnt orange
    val trackColor = Color(0xFF3A3A34).copy(alpha = 0.4f)  // Mossy brown

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val barHeight = size.height
        val barWidth = size.width
        val radius = cornerRadius.toPx()

        // Draw track (background)
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(radius, radius)
        )

        // Calculate progress width
        val progressWidth = barWidth * animatedProgress

        if (progressWidth > 0) {
            // Create animated gradient brush - forest to autumn leaves
            val gradientColors = listOf(forestGreen, sageGreen, goldenYellow, burntOrange, forestGreen)
            val offsetColors = gradientColors.indices.map { index ->
                val adjustedStop = (index.toFloat() / (gradientColors.size - 1) + gradientOffset) % 1f
                adjustedStop to gradientColors[index]
            }.sortedBy { it.first }

            val brush = Brush.horizontalGradient(
                colorStops = offsetColors.toTypedArray(),
                startX = 0f,
                endX = progressWidth * 1.5f
            )

            // Draw progress bar
            drawRoundRect(
                brush = brush,
                topLeft = Offset.Zero,
                size = Size(progressWidth, barHeight),
                cornerRadius = CornerRadius(radius, radius)
            )

            // Draw glow effect at milestones
            if (glowAlpha > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = glowAlpha * 0.6f),
                    topLeft = Offset.Zero,
                    size = Size(progressWidth, barHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }

            // Draw sparkle/shine effect
            val shineOffset = (gradientOffset * progressWidth * 2) % (progressWidth + barHeight * 4) - barHeight * 2
            if (shineOffset > 0 && shineOffset < progressWidth) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        start = Offset(shineOffset - barHeight, 0f),
                        end = Offset(shineOffset + barHeight, barHeight)
                    ),
                    start = Offset(shineOffset, 0f),
                    end = Offset(shineOffset - barHeight / 2, barHeight),
                    strokeWidth = barHeight / 2
                )
            }

            // Draw confetti at 100%
            if (animatedProgress >= 0.99f) {
                drawConfetti(
                    particles = particles,
                    progress = confettiProgress,
                    barWidth = progressWidth,
                    barHeight = barHeight
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val startX: Float = Random.nextFloat(),
    val speed: Float = 0.5f + Random.nextFloat() * 0.5f,
    val angle: Float = Random.nextFloat() * 360f,
    val size: Float = 3f + Random.nextFloat() * 4f,
    val color: Color = listOf(
        Color(0xFFC9A227), // Golden yellow
        Color(0xFFD4843E), // Burnt orange
        Color(0xFF7D9B76), // Sage green
        Color(0xFFB54A3C), // Rusty red (maple leaf)
        Color(0xFF4A6741), // Forest green
        Color(0xFF8B6F47)  // Warm brown (bark)
    ).random()
)

private fun DrawScope.drawConfetti(
    particles: List<ConfettiParticle>,
    progress: Float,
    barWidth: Float,
    barHeight: Float
) {
    particles.forEach { particle ->
        val particleProgress = ((progress * particle.speed) + particle.startX) % 1f

        // Particles rise up from the bar
        val baseY = barHeight / 2 - particleProgress * barHeight * 3
        val wobble = sin(particleProgress * 6f * Math.PI.toFloat()) * 8f
        val x = particle.startX * barWidth + wobble
        val y = baseY

        // Only draw if above the bar
        if (y < barHeight) {
            val alpha = (1f - particleProgress).coerceIn(0f, 1f) * 0.8f

            // Draw sparkle
            val rotation = particle.angle + progress * 360f
            val size = particle.size * (1f - particleProgress * 0.5f)

            // Star shape
            drawStar(
                center = Offset(x, y),
                radius = size,
                rotation = rotation,
                color = particle.color.copy(alpha = alpha)
            )
        }
    }
}

private fun DrawScope.drawStar(
    center: Offset,
    radius: Float,
    rotation: Float,
    color: Color
) {
    val points = 4
    val innerRadius = radius * 0.4f

    val path = Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = (rotation + i * 180f / points) * Math.PI.toFloat() / 180f
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    drawPath(path, color)
}

