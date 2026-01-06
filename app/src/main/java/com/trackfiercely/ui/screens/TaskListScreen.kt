package com.trackfiercely.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import com.trackfiercely.data.model.Category
import com.trackfiercely.data.model.Task
import com.trackfiercely.data.model.TaskInputType
import com.trackfiercely.ui.theme.DangerRed
import com.trackfiercely.ui.theme.EmeraldPrimary
import com.trackfiercely.ui.theme.getCategoryColor
import com.trackfiercely.viewmodel.TaskFilter
import com.trackfiercely.viewmodel.TaskListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    onBack: () -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var searchExpanded by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Tasks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (searchExpanded || uiState.searchQuery.isNotBlank()) 
                                EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onAddTask) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = EmeraldPrimary
                        )
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
            // Search bar (expandable)
            AnimatedVisibility(
                visible = searchExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search tasks...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        cursorColor = EmeraldPrimary
                    )
                )
            }
            
            // Filter tabs
            FilterTabs(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) },
                allCount = uiState.allCount,
                assignedCount = uiState.assignedCount,
                unassignedCount = uiState.unassignedCount,
                hiddenCount = uiState.hiddenCount
            )
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                }
            } else if (uiState.displayedTasks.isEmpty()) {
                EmptyTasksState(
                    filter = uiState.selectedFilter,
                    onAddTask = onAddTask
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.tasksByCategory.forEach { (category, tasks) ->
                        item {
                            CategoryHeader(category = category, count = tasks.size)
                        }
                        
                        items(tasks, key = { it.id }) { task ->
                            TaskListItem(
                                task = task,
                                isHiddenView = uiState.selectedFilter == TaskFilter.HIDDEN,
                                onEdit = { onEditTask(task.id) },
                                onDelete = { taskToDelete = task },
                                onToggleHidden = { viewModel.toggleTaskHidden(task.id) }
                            )
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task?") },
            text = { 
                Text("Are you sure you want to delete \"${task.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(task.id)
                        taskToDelete = null
                    }
                ) {
                    Text("Delete", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FilterTabs(
    selectedFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit,
    allCount: Int,
    assignedCount: Int,
    unassignedCount: Int,
    hiddenCount: Int
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedFilter == TaskFilter.ALL,
                onClick = { onFilterSelected(TaskFilter.ALL) },
                label = { Text("All ($allCount)") },
                leadingIcon = if (selectedFilter == TaskFilter.ALL) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = EmeraldPrimary,
                    selectedLeadingIconColor = EmeraldPrimary
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == TaskFilter.ASSIGNED,
                onClick = { onFilterSelected(TaskFilter.ASSIGNED) },
                label = { Text("Assigned ($assignedCount)") },
                leadingIcon = if (selectedFilter == TaskFilter.ASSIGNED) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = EmeraldPrimary,
                    selectedLeadingIconColor = EmeraldPrimary
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == TaskFilter.UNASSIGNED,
                onClick = { onFilterSelected(TaskFilter.UNASSIGNED) },
                label = { Text("Unassigned ($unassignedCount)") },
                leadingIcon = if (selectedFilter == TaskFilter.UNASSIGNED) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = EmeraldPrimary,
                    selectedLeadingIconColor = EmeraldPrimary
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == TaskFilter.HIDDEN,
                onClick = { onFilterSelected(TaskFilter.HIDDEN) },
                label = { Text("Hidden ($hiddenCount)") },
                leadingIcon = if (selectedFilter == TaskFilter.HIDDEN) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.outline,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
private fun CategoryHeader(category: Category, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(getCategoryColor(category), RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListItem(
    task: Task,
    isHiddenView: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleHidden: () -> Unit
) {
    val categoryColor = getCategoryColor(task.category)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false // Don't dismiss, wait for dialog
            } else if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onToggleHidden()
                false // Don't dismiss, let state update naturally
            } else {
                false
            }
        }
    )
    
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val isSwipingToDelete = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val isSwipingToHide = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when {
                            isSwipingToDelete -> DangerRed
                            isSwipingToHide -> if (isHiddenView) EmeraldPrimary else MaterialTheme.colorScheme.outline
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = when {
                    isSwipingToDelete -> Alignment.CenterEnd
                    isSwipingToHide -> Alignment.CenterStart
                    else -> Alignment.Center
                }
            ) {
                when {
                    isSwipingToDelete -> Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                    isSwipingToHide -> Icon(
                        imageVector = if (isHiddenView) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isHiddenView) "Show" else "Hide",
                        tint = Color.White
                    )
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isHiddenView) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category indicator
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(categoryColor)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = task.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (isHiddenView) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        // Input type indicator
                        if (task.inputType != TaskInputType.CHECKBOX) {
                            InputTypeIcon(inputType = task.inputType)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Schedule info row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Schedule badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = task.scheduleType.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        
                        // Schedule description
                        Text(
                            text = task.getScheduleDescription(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // System task indicator
                    if (task.isSystemTask) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "System task",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InputTypeIcon(inputType: TaskInputType) {
    val (icon, description) = when (inputType) {
        TaskInputType.CHECKBOX -> Icons.Default.CheckBox to "Checkbox"
        TaskInputType.SLIDER -> Icons.Default.LinearScale to "Slider"
        TaskInputType.STARS -> Icons.Default.Star to "Star rating"
        TaskInputType.NUMBER -> Icons.Default.Numbers to "Number"
        TaskInputType.PHOTO -> Icons.Default.CameraAlt to "Photo"
        TaskInputType.BLOOD_PRESSURE -> Icons.Default.Favorite to "Blood Pressure"
    }
    
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.size(20.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .padding(3.dp)
                .size(14.dp)
        )
    }
}

@Composable
private fun EmptyTasksState(
    filter: TaskFilter,
    onAddTask: () -> Unit
) {
    val (icon, message, subMessage) = when (filter) {
        TaskFilter.ALL -> Triple(
            Icons.Default.CheckCircleOutline,
            "No tasks yet",
            "Create your first habit to start tracking!"
        )
        TaskFilter.ASSIGNED -> Triple(
            Icons.Default.EventAvailable,
            "No assigned tasks",
            "Assign tasks to specific days to see them here"
        )
        TaskFilter.UNASSIGNED -> Triple(
            Icons.Default.EventBusy,
            "No unassigned tasks",
            "All your tasks have been assigned to days"
        )
        TaskFilter.HIDDEN -> Triple(
            Icons.Default.VisibilityOff,
            "No hidden tasks",
            "Swipe left on tasks to hide them"
        )
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = subMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            if (filter == TaskFilter.ALL) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onAddTask,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Task")
                }
            }
        }
    }
}
