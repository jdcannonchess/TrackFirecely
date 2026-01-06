package com.trackfiercely.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackfiercely.ui.theme.*
import com.trackfiercely.viewmodel.WeightEntry
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalTextApi::class)
@Composable
fun WeightGraph(
    entries: List<WeightEntry>,
    goalWeight: Float? = null,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        EmptyGraphState(modifier)
        return
    }
    
    val sortedEntries = remember(entries) { entries.sortedBy { it.date } }
    
    // Calculate bounds
    val weights = sortedEntries.map { it.weight }
    val minWeight = (weights.minOrNull() ?: 0f).let { min ->
        goalWeight?.let { goal -> minOf(min, goal) } ?: min
    } - 2f
    val maxWeight = (weights.maxOrNull() ?: 100f).let { max ->
        goalWeight?.let { goal -> maxOf(max, goal) } ?: max
    } + 2f
    
    val weightRange = maxWeight - minWeight
    
    // Calculate 7-day moving average for trend line
    val trendPoints = remember(sortedEntries) {
        calculateMovingAverage(sortedEntries, windowSize = 7)
    }
    
    val textMeasurer = rememberTextMeasurer()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val leftPadding = 50f
            val rightPadding = 16f
            val topPadding = 16f
            val bottomPadding = 30f
            
            val graphWidth = canvasWidth - leftPadding - rightPadding
            val graphHeight = canvasHeight - topPadding - bottomPadding
            
            // Draw Y-axis labels
            val yLabels = 5
            for (i in 0..yLabels) {
                val y = topPadding + (graphHeight * i / yLabels)
                val weight = maxWeight - (weightRange * i / yLabels)
                
                // Grid line
                drawLine(
                    color = StrokeDark.copy(alpha = 0.3f),
                    start = Offset(leftPadding, y),
                    end = Offset(canvasWidth - rightPadding, y),
                    strokeWidth = 1f
                )
                
                // Label
                val labelText = String.format("%.0f", weight)
                val textResult = textMeasurer.measure(
                    text = labelText,
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = TextSecondaryLight
                    )
                )
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(
                        x = leftPadding - textResult.size.width - 8f,
                        y = y - textResult.size.height / 2
                    )
                )
            }
            
            // Draw goal line if set
            if (goalWeight != null && goalWeight in minWeight..maxWeight) {
                val goalY = topPadding + graphHeight * (maxWeight - goalWeight) / weightRange
                
                // Dashed line for goal
                val dashLength = 10f
                val gapLength = 5f
                var x = leftPadding
                while (x < canvasWidth - rightPadding) {
                    drawLine(
                        color = SageGreen,
                        start = Offset(x, goalY),
                        end = Offset(minOf(x + dashLength, canvasWidth - rightPadding), goalY),
                        strokeWidth = 2f
                    )
                    x += dashLength + gapLength
                }
                
                // Goal label
                val goalLabel = textMeasurer.measure(
                    text = "Goal",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = SageGreen,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(
                    textLayoutResult = goalLabel,
                    topLeft = Offset(
                        x = canvasWidth - rightPadding - goalLabel.size.width,
                        y = goalY - goalLabel.size.height - 4f
                    )
                )
            }
            
            if (sortedEntries.size >= 2) {
                // Calculate points
                val points = sortedEntries.mapIndexed { index, entry ->
                    val x = leftPadding + (graphWidth * index / (sortedEntries.size - 1))
                    val y = topPadding + graphHeight * (maxWeight - entry.weight) / weightRange
                    Offset(x, y)
                }
                
                // Draw trend line (7-day moving average)
                if (trendPoints.size >= 2) {
                    val trendPath = Path()
                    trendPoints.forEachIndexed { index, (entryIndex, avgWeight) ->
                        val x = leftPadding + (graphWidth * entryIndex / (sortedEntries.size - 1))
                        val y = topPadding + graphHeight * (maxWeight - avgWeight) / weightRange
                        
                        if (index == 0) {
                            trendPath.moveTo(x, y)
                        } else {
                            trendPath.lineTo(x, y)
                        }
                    }
                    
                    drawPath(
                        path = trendPath,
                        color = ForestGreen.copy(alpha = 0.5f),
                        style = Stroke(
                            width = 3f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                
                // Draw main line
                val linePath = Path()
                points.forEachIndexed { index, point ->
                    if (index == 0) {
                        linePath.moveTo(point.x, point.y)
                    } else {
                        linePath.lineTo(point.x, point.y)
                    }
                }
                
                drawPath(
                    path = linePath,
                    color = FallPrimary,
                    style = Stroke(
                        width = 3f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                
                // Draw dots
                points.forEach { point ->
                    drawCircle(
                        color = FallPrimary,
                        radius = 5f,
                        center = point
                    )
                    drawCircle(
                        color = DarkSurface,
                        radius = 3f,
                        center = point
                    )
                }
            } else if (sortedEntries.size == 1) {
                // Single point
                val entry = sortedEntries.first()
                val x = leftPadding + graphWidth / 2
                val y = topPadding + graphHeight * (maxWeight - entry.weight) / weightRange
                
                drawCircle(
                    color = FallPrimary,
                    radius = 8f,
                    center = Offset(x, y)
                )
            }
            
            // Draw X-axis labels (first and last date)
            if (sortedEntries.isNotEmpty()) {
                val dateFormatter = DateTimeFormatter.ofPattern("M/d")
                
                // First date
                val firstLabel = textMeasurer.measure(
                    text = sortedEntries.first().date.format(dateFormatter),
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = TextSecondaryLight
                    )
                )
                drawText(
                    textLayoutResult = firstLabel,
                    topLeft = Offset(
                        x = leftPadding,
                        y = canvasHeight - firstLabel.size.height
                    )
                )
                
                // Last date
                if (sortedEntries.size > 1) {
                    val lastLabel = textMeasurer.measure(
                        text = sortedEntries.last().date.format(dateFormatter),
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = TextSecondaryLight
                        )
                    )
                    drawText(
                        textLayoutResult = lastLabel,
                        topLeft = Offset(
                            x = canvasWidth - rightPadding - lastLabel.size.width,
                            y = canvasHeight - lastLabel.size.height
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGraphState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Log your weight to see the graph",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/**
 * Calculate 7-day moving average
 * Returns list of pairs: (index in original list, average weight)
 */
private fun calculateMovingAverage(
    entries: List<WeightEntry>,
    windowSize: Int = 7
): List<Pair<Int, Float>> {
    if (entries.size < windowSize) return emptyList()
    
    val result = mutableListOf<Pair<Int, Float>>()
    
    for (i in (windowSize - 1) until entries.size) {
        val windowEntries = entries.subList(i - windowSize + 1, i + 1)
        val avg = windowEntries.map { it.weight }.average().toFloat()
        result.add(i to avg)
    }
    
    return result
}

