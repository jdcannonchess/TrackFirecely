package com.trackfiercely.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trackfiercely.ui.theme.*
import com.trackfiercely.viewmodel.HistoryFilter
import com.trackfiercely.viewmodel.HistoryViewModel
import com.trackfiercely.viewmodel.OneOffTaskEntry
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("One-Off Tasks", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FallPrimary)
                }
            } else {
                // Stats summary
                StatsSummary(
                    total = uiState.totalTasks,
                    completed = uiState.completedCount,
                    incomplete = uiState.incompleteCount,
                    modifier = Modifier.padding(16.dp)
                )
                
                // Filter chips
                FilterChips(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = { viewModel.setFilter(it) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (uiState.entries.isEmpty()) {
                    EmptyHistoryState(filter = uiState.selectedFilter)
                } else {
                    // Task list grouped by date
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.entries, key = { it.task.id }) { entry ->
                            OneOffTaskCard(entry = entry)
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSummary(
    total: Int,
    completed: Int,
    incomplete: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FallPrimary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = total.toString(),
                label = "Total",
                color = MaterialTheme.colorScheme.onSurface
            )
            StatItem(
                value = completed.toString(),
                label = "Completed",
                color = SageGreen
            )
            StatItem(
                value = incomplete.toString(),
                label = "Pending",
                color = FallPrimary
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilterChips(
    selectedFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FallPrimary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun OneOffTaskCard(entry: OneOffTaskEntry) {
    val task = entry.task
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Completion indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (entry.isCompleted) SageGreen 
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (entry.isCompleted) Icons.Default.Check else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (entry.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Scheduled date
                entry.scheduledDate?.let { date ->
                    Text(
                        text = "Scheduled: ${date.format(dateFormatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Completed date (if different from scheduled)
                if (entry.isCompleted && entry.completedDate != null) {
                    val completedDateStr = entry.completedDate.format(dateFormatter)
                    Text(
                        text = "Completed: $completedDateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = SageGreen
                    )
                }
            }
            
            // Category badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = getCategoryColor(task.category).copy(alpha = 0.2f)
            ) {
                Text(
                    text = task.category.name.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = getCategoryColor(task.category),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(filter: HistoryFilter) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = when (filter) {
                    HistoryFilter.ALL -> "No one-off tasks"
                    HistoryFilter.COMPLETED -> "No completed tasks"
                    HistoryFilter.INCOMPLETE -> "No pending tasks"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Create a one-time task to see it here!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getCategoryColor(category: com.trackfiercely.data.model.Category): Color {
    return when (category) {
        com.trackfiercely.data.model.Category.HEALTH_FITNESS -> HealthFitnessColor
        com.trackfiercely.data.model.Category.FAMILY -> FamilyColor
        com.trackfiercely.data.model.Category.HOME_CHORES -> HomeChoresColor
        com.trackfiercely.data.model.Category.HOBBIES -> HobbiesColor
        com.trackfiercely.data.model.Category.PERSONAL_GROWTH -> PersonalGrowthColor
        com.trackfiercely.data.model.Category.WORK -> WorkColor
    }
}
