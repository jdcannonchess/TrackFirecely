package com.trackfiercely.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackfiercely.ui.theme.*
import com.trackfiercely.util.DateUtils
import com.trackfiercely.viewmodel.WeeklyReportUiState
import com.trackfiercely.viewmodel.WeeklyReportViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReportScreen(
    viewModel: WeeklyReportViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isCurrentWeek) {
                        TextButton(onClick = { viewModel.goToCurrentWeek() }) {
                            Text("This Week", color = EmeraldPrimary)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Week navigation
                item {
                    WeekNavigationHeader(
                        weekRange = uiState.weekRangeFormatted,
                        onPrevious = { viewModel.goToPreviousWeek() },
                        onNext = { viewModel.goToNextWeek() }
                    )
                }
                
                // Grade card
                item {
                    GradeCard(
                        grade = uiState.grade,
                        completionPercentage = uiState.completionPercentage,
                        tasksCompleted = uiState.tasksCompleted,
                        totalTasks = uiState.totalTasks
                    )
                }
                
                // Highlights
                if (uiState.highlights.isNotEmpty()) {
                    item {
                        HighlightsCard(highlights = uiState.highlights)
                    }
                }
                
                // Perfect days
                if (uiState.perfectDays.isNotEmpty()) {
                    item {
                        PerfectDaysCard(
                            perfectDays = uiState.perfectDays,
                            weekStart = uiState.weekStart
                        )
                    }
                }
                
                // Wellness summary
                item {
                    WellnessSummaryCard(
                        avgMood = uiState.avgMood,
                        avgSleepHours = uiState.avgSleepHours,
                        avgSleepQuality = uiState.avgSleepQuality,
                        totalSteps = uiState.totalSteps
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun WeekNavigationHeader(
    weekRange: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week")
        }
        
        Text(
            text = weekRange,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next week")
        }
    }
}

@Composable
private fun GradeCard(
    grade: String,
    completionPercentage: Float,
    tasksCompleted: Int,
    totalTasks: Int
) {
    val gradeColor = when {
        grade.startsWith("A") -> EmeraldPrimary
        grade.startsWith("B") -> FamilyColor
        grade.startsWith("C") -> HomeChoresColor
        else -> DangerRed
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = gradeColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Grade circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(gradeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = grade,
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "${(completionPercentage * 100).toInt()}% Complete",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = gradeColor
            )
            
            Text(
                text = "$tasksCompleted of $totalTasks tasks completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { completionPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = gradeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun HighlightsCard(highlights: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = HomeChoresColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Highlights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            highlights.forEach { highlight ->
                Text(
                    text = highlight,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PerfectDaysCard(
    perfectDays: List<LocalDate>,
    weekStart: LocalDate
) {
    val weekDates = DateUtils.getWeekDates(weekStart)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = HomeChoresColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Perfect Days",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${perfectDays.size}/7",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weekDates.forEach { date ->
                    val isPerfect = perfectDays.contains(date)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = DateUtils.getDayLetter(date.dayOfWeek),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPerfect) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPerfect) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Perfect",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WellnessSummaryCard(
    avgMood: Float?,
    avgSleepHours: Float?,
    avgSleepQuality: Float?,
    totalSteps: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Wellness Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Mood
                WellnessMetric(
                    label = "Avg Mood",
                    value = avgMood?.let { String.format("%.1f", it) } ?: "-",
                    subValue = "/10",
                    color = avgMood?.let { getMoodColor(it.toInt()) } ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Sleep
                WellnessMetric(
                    label = "Avg Sleep",
                    value = avgSleepHours?.let { String.format("%.1f", it) } ?: "-",
                    subValue = "hrs",
                    color = FamilyColor
                )
                
                // Sleep Quality
                WellnessMetric(
                    label = "Sleep Quality",
                    value = avgSleepQuality?.let { String.format("%.1f", it) } ?: "-",
                    subValue = "/5",
                    color = HobbiesColor
                )
                
                // Steps
                WellnessMetric(
                    label = "Total Steps",
                    value = if (totalSteps > 0) "${totalSteps / 1000}k" else "-",
                    subValue = "",
                    color = WorkColor
                )
            }
        }
    }
}

@Composable
private fun WellnessMetric(
    label: String,
    value: String,
    subValue: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (subValue.isNotEmpty()) {
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
