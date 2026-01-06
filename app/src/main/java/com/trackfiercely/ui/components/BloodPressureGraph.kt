package com.trackfiercely.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackfiercely.ui.theme.*
import com.trackfiercely.viewmodel.BPDayEntry
import java.time.format.DateTimeFormatter

@Composable
fun BloodPressureGraph(
    entries: List<BPDayEntry>,
    modifier: Modifier = Modifier,
    showHeartRate: Boolean = true
) {
    if (entries.isEmpty()) return
    
    val density = LocalDensity.current
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
        }
    }
    
    val dateFormatter = DateTimeFormatter.ofPattern("M/d")
    
    // Colors for the lines
    val systolicColor = DangerRed
    val diastolicColor = FallPrimary
    val heartRateColor = SageGreen
    
    // Zone colors (semi-transparent bands)
    val normalZoneColor = SageGreen.copy(alpha = 0.1f)
    val elevatedZoneColor = GoldenYellow.copy(alpha = 0.1f)
    val highZoneColor = DangerRed.copy(alpha = 0.1f)
    
    // Calculate ranges
    val allSystolic = entries.map { it.avgSystolic }
    val allDiastolic = entries.map { it.avgDiastolic }
    val allHeartRate = if (showHeartRate) entries.map { it.avgHeartRate } else emptyList()
    
    val minBP = minOf(allDiastolic.minOrNull() ?: 60f, 60f) - 10
    val maxBP = maxOf(allSystolic.maxOrNull() ?: 140f, 140f) + 10
    
    Canvas(modifier = modifier) {
        val padding = 40.dp.toPx()
        val rightPadding = if (showHeartRate) 50.dp.toPx() else 30.dp.toPx()
        val bottomPadding = 30.dp.toPx()
        val graphWidth = size.width - padding - rightPadding
        val graphHeight = size.height - padding - bottomPadding
        
        // Draw classification zones (background bands)
        drawClassificationZones(
            padding = padding,
            graphWidth = graphWidth,
            graphHeight = graphHeight,
            minBP = minBP,
            maxBP = maxBP,
            normalZoneColor = normalZoneColor,
            elevatedZoneColor = elevatedZoneColor,
            highZoneColor = highZoneColor
        )
        
        // Draw grid lines
        drawGridLines(
            padding = padding,
            graphWidth = graphWidth,
            graphHeight = graphHeight,
            minBP = minBP,
            maxBP = maxBP,
            textPaint = textPaint
        )
        
        // Draw systolic line
        if (entries.size > 1) {
            drawDataLine(
                entries = entries,
                valueSelector = { it.avgSystolic },
                color = systolicColor,
                padding = padding,
                graphWidth = graphWidth,
                graphHeight = graphHeight,
                minValue = minBP,
                maxValue = maxBP
            )
            
            // Draw diastolic line
            drawDataLine(
                entries = entries,
                valueSelector = { it.avgDiastolic },
                color = diastolicColor,
                padding = padding,
                graphWidth = graphWidth,
                graphHeight = graphHeight,
                minValue = minBP,
                maxValue = maxBP
            )
        }
        
        // Draw data points
        entries.forEachIndexed { index, entry ->
            val x = padding + (graphWidth * index / (entries.size - 1).coerceAtLeast(1))
            
            // Systolic point
            val sysY = padding + graphHeight * (1 - (entry.avgSystolic - minBP) / (maxBP - minBP))
            drawCircle(
                color = systolicColor,
                radius = 4.dp.toPx(),
                center = Offset(x, sysY)
            )
            
            // Diastolic point
            val diaY = padding + graphHeight * (1 - (entry.avgDiastolic - minBP) / (maxBP - minBP))
            drawCircle(
                color = diastolicColor,
                radius = 4.dp.toPx(),
                center = Offset(x, diaY)
            )
        }
        
        // Draw x-axis labels (dates)
        val labelInterval = when {
            entries.size <= 7 -> 1
            entries.size <= 14 -> 2
            entries.size <= 30 -> 5
            else -> 10
        }
        
        entries.forEachIndexed { index, entry ->
            if (index % labelInterval == 0 || index == entries.size - 1) {
                val x = padding + (graphWidth * index / (entries.size - 1).coerceAtLeast(1))
                drawContext.canvas.nativeCanvas.drawText(
                    entry.date.format(dateFormatter),
                    x - 15.dp.toPx(),
                    size.height - 5.dp.toPx(),
                    textPaint
                )
            }
        }
    }
}

private fun DrawScope.drawClassificationZones(
    padding: Float,
    graphWidth: Float,
    graphHeight: Float,
    minBP: Float,
    maxBP: Float,
    normalZoneColor: Color,
    elevatedZoneColor: Color,
    highZoneColor: Color
) {
    // Normal zone: below 120/80
    val normalTop = padding + graphHeight * (1 - (120f - minBP) / (maxBP - minBP))
    val normalBottom = padding + graphHeight
    
    drawRect(
        color = normalZoneColor,
        topLeft = Offset(padding, normalTop),
        size = androidx.compose.ui.geometry.Size(graphWidth, normalBottom - normalTop)
    )
    
    // Elevated zone: 120-129 systolic
    val elevatedTop = padding + graphHeight * (1 - (129f - minBP) / (maxBP - minBP))
    
    drawRect(
        color = elevatedZoneColor,
        topLeft = Offset(padding, elevatedTop),
        size = androidx.compose.ui.geometry.Size(graphWidth, normalTop - elevatedTop)
    )
    
    // High zone: 130+
    val highTop = padding
    
    drawRect(
        color = highZoneColor,
        topLeft = Offset(padding, highTop),
        size = androidx.compose.ui.geometry.Size(graphWidth, elevatedTop - highTop)
    )
}

private fun DrawScope.drawGridLines(
    padding: Float,
    graphWidth: Float,
    graphHeight: Float,
    minBP: Float,
    maxBP: Float,
    textPaint: android.graphics.Paint
) {
    // Horizontal grid lines at key BP thresholds
    val thresholds = listOf(80f, 90f, 100f, 110f, 120f, 130f, 140f, 150f, 160f)
    
    thresholds.filter { it in minBP..maxBP }.forEach { value ->
        val y = padding + graphHeight * (1 - (value - minBP) / (maxBP - minBP))
        
        // Grid line
        drawLine(
            color = Color.Gray.copy(alpha = 0.2f),
            start = Offset(padding, y),
            end = Offset(padding + graphWidth, y),
            strokeWidth = 1.dp.toPx()
        )
        
        // Label
        drawContext.canvas.nativeCanvas.drawText(
            value.toInt().toString(),
            5.dp.toPx(),
            y + 4.dp.toPx(),
            textPaint
        )
    }
}

private fun DrawScope.drawDataLine(
    entries: List<BPDayEntry>,
    valueSelector: (BPDayEntry) -> Float,
    color: Color,
    padding: Float,
    graphWidth: Float,
    graphHeight: Float,
    minValue: Float,
    maxValue: Float
) {
    val path = Path()
    
    entries.forEachIndexed { index, entry ->
        val x = padding + (graphWidth * index / (entries.size - 1).coerceAtLeast(1))
        val value = valueSelector(entry)
        val y = padding + graphHeight * (1 - (value - minValue) / (maxValue - minValue))
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    )
}

/**
 * Legend for the blood pressure graph
 */
@Composable
fun BloodPressureGraphLegend(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(color = DangerRed, label = "Systolic")
        LegendItem(color = FallPrimary, label = "Diastolic")
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(4.dp))
        androidx.compose.material3.Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

