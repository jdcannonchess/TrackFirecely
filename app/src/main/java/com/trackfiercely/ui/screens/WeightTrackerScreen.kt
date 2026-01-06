package com.trackfiercely.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trackfiercely.ui.components.WeightGraph
import com.trackfiercely.ui.theme.*
import com.trackfiercely.viewmodel.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrackerScreen(
    viewModel: WeightTrackerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Weight Tracker",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showGoalEditor() }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Set Goal"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FallPrimary)
                }
            } else {
                WeightTrackerContent(
                    uiState = uiState,
                    onPeriodSelected = { viewModel.selectPeriod(it) }
                )
            }
            
            // Goal editor dialog
            if (uiState.showGoalEditor) {
                GoalEditorDialog(
                    currentGoal = uiState.goalWeight,
                    onDismiss = { viewModel.hideGoalEditor() },
                    onSave = { viewModel.setGoalWeight(it) },
                    onClear = { viewModel.clearGoal() }
                )
            }
            
            // Milestone celebration
            uiState.newMilestone?.let { milestone ->
                MilestoneCelebration(
                    milestone = milestone,
                    onDismiss = { viewModel.dismissMilestone() }
                )
            }
        }
    }
}

@Composable
private fun WeightTrackerContent(
    uiState: WeightTrackerUiState,
    onPeriodSelected: (WeightTimePeriod) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current weight and goal card
        item {
            CurrentWeightCard(
                currentWeight = uiState.currentWeight,
                goalWeight = uiState.goalWeight,
                weightToGoal = uiState.weightToGoal,
                progressToGoal = uiState.progressToGoal
            )
        }
        
        // Period selector
        item {
            PeriodSelector(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = onPeriodSelected
            )
        }
        
        // Graph
        item {
            WeightGraph(
                entries = uiState.stats.entries,
                goalWeight = uiState.goalWeight,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Statistics
        item {
            StatisticsCard(stats = uiState.stats)
        }
        
        // History header
        if (uiState.hasEntries) {
            item {
                Text(
                    text = "Recent Entries",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // History entries (show last 20)
        items(uiState.allEntries.take(20)) { entry ->
            WeightEntryRow(entry = entry)
        }
        
        // Empty state
        if (!uiState.hasEntries) {
            item {
                EmptyWeightState()
            }
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CurrentWeightCard(
    currentWeight: Float?,
    goalWeight: Float?,
    weightToGoal: Float?,
    progressToGoal: Float?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FallPrimary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Current weight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentWeight?.let { "${String.format("%.1f", it)} lbs" } ?: "-- lbs",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (goalWeight != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Goal: ${String.format("%.1f", goalWeight)} lbs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        weightToGoal?.let { toGo ->
                            Text(
                                text = if (toGo > 0) {
                                    "${String.format("%.1f", toGo)} lbs to go"
                                } else {
                                    "Goal reached!"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (toGo <= 0) SageGreen else FallPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Progress bar
            if (progressToGoal != null && goalWeight != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = { progressToGoal },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = SageGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${(progressToGoal * 100).toInt()}% to goal",
                    style = MaterialTheme.typography.labelSmall,
                    color = SageGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: WeightTimePeriod,
    onPeriodSelected: (WeightTimePeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeightTimePeriod.entries.forEach { period ->
            val isSelected = period == selectedPeriod
            
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPeriodSelected(period) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) FallPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun StatisticsCard(stats: WeightStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Stats for period",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Avg",
                    value = stats.average?.let { "${String.format("%.1f", it)}" } ?: "--"
                )
                StatItem(
                    label = "Min",
                    value = stats.min?.let { "${String.format("%.1f", it)}" } ?: "--"
                )
                StatItem(
                    label = "Max",
                    value = stats.max?.let { "${String.format("%.1f", it)}" } ?: "--"
                )
            }
            
            // Trend
            stats.trendPerWeek?.let { trend ->
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (trend < 0) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (trend < 0) SageGreen else HealthFitnessColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trend: ${if (trend > 0) "+" else ""}${String.format("%.1f", trend)} lbs/week",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (trend < 0) SageGreen else HealthFitnessColor
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeightEntryRow(entry: WeightEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${String.format("%.1f", entry.weight)} lbs",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = FallPrimary
            )
        }
    }
}

@Composable
private fun EmptyWeightState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MonitorWeight,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No weight entries yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Log your weight using the \"Log Weight\" task",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GoalEditorDialog(
    currentGoal: Float?,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit,
    onClear: () -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal?.toString() ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Goal Weight",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { text ->
                        if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d*$"))) {
                            goalText = text
                        }
                    },
                    label = { Text("Goal weight (lbs)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (currentGoal != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            onClear()
                            onDismiss()
                        }
                    ) {
                        Text("Clear Goal", color = HealthFitnessColor)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    goalText.toFloatOrNull()?.let { onSave(it) }
                },
                enabled = goalText.toFloatOrNull() != null
            ) {
                Text("Save", color = FallPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MilestoneCelebration(
    milestone: WeightMilestone,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = GoldenYellow
            )
        },
        title = {
            Text(
                text = "Milestone Achieved!",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = milestone.label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = FallPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Awesome!", color = FallPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

