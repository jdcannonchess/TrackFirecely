package com.trackfiercely.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.trackfiercely.ui.components.AnimatedProgressBar
import com.trackfiercely.ui.components.TaskItem
import com.trackfiercely.ui.theme.EmeraldPrimary
import com.trackfiercely.util.DateUtils
import com.trackfiercely.viewmodel.HomeUiState
import com.trackfiercely.viewmodel.HomeViewModel
import com.trackfiercely.viewmodel.TaskWithStatus
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onViewTasks: () -> Unit,
    onViewWeeklyReport: () -> Unit,
    onViewHistory: () -> Unit,
    onViewProgressGallery: () -> Unit = {},
    onViewWeightTracker: () -> Unit = {},
    onViewBloodPressureTracker: () -> Unit = {},
    onCapturePhoto: (Long, LocalDate) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Pager state for day-by-day swiping
    // We use a large range (1 year back to 1 year forward = ~730 pages)
    val totalPages = DateUtils.daysSinceMinDate(LocalDate.now()) + 365 // 1 year back + 1 year forward
    val initialPage = uiState.currentPageIndex
    
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalPages }
    )
    
    // Sync pager with ViewModel when page changes
    LaunchedEffect(pagerState.currentPage) {
        val newDate = DateUtils.dateFromPageIndex(pagerState.currentPage)
        if (newDate != uiState.selectedDate) {
            viewModel.selectDate(newDate)
        }
    }
    
    // Sync pager when ViewModel date changes (e.g., "Go to today" button)
    LaunchedEffect(uiState.selectedDate) {
        val targetPage = uiState.currentPageIndex
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                onViewTasks = {
                    scope.launch { drawerState.close() }
                    onViewTasks()
                },
                onViewProgressGallery = {
                    scope.launch { drawerState.close() }
                    onViewProgressGallery()
                },
                onViewWeightTracker = {
                    scope.launch { drawerState.close() }
                    onViewWeightTracker()
                },
                onViewBloodPressureTracker = {
                    scope.launch { drawerState.close() }
                    onViewBloodPressureTracker()
                },
                onViewWeeklyReport = {
                    scope.launch { drawerState.close() }
                    onViewWeeklyReport()
                },
                onViewHistory = {
                    scope.launch { drawerState.close() }
                    onViewHistory()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                HomeTopBar(
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddTask,
                    containerColor = EmeraldPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task"
                    )
                }
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) { page ->
                val pageDate = DateUtils.dateFromPageIndex(page)
                
                // Only render content for the current page
                if (page == pagerState.currentPage) {
                    DayContent(
                        uiState = uiState,
                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                        onEditTask = onEditTask,
                        onSetNumericValue = { taskId, value -> viewModel.setTaskNumericValue(taskId, value) },
                        onSetStarValue = { taskId, stars -> viewModel.setTaskStarValue(taskId, stars) },
                        onCapturePhoto = { taskId -> onCapturePhoto(taskId, viewModel.getSelectedDate()) },
                        onSetBpReading = { taskId, sys, dia, hr -> viewModel.addBpReading(taskId, sys, dia, hr) },
                        onGoToToday = { viewModel.goToToday() }
                    )
                } else {
                    // Placeholder for non-current pages (shows date header only)
                    DayPlaceholder(
                        date = pageDate,
                        isToday = pageDate == LocalDate.now()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onMenuClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "TRACK",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "FIERCELY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = EmeraldPrimary,
                    letterSpacing = 4.sp
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun NavigationDrawerContent(
    onViewTasks: () -> Unit,
    onViewProgressGallery: () -> Unit,
    onViewWeightTracker: () -> Unit,
    onViewBloodPressureTracker: () -> Unit,
    onViewWeeklyReport: () -> Unit,
    onViewHistory: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // App header in drawer
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "TRACK",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "FIERCELY",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = EmeraldPrimary,
                letterSpacing = 4.sp
            )
        }
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        
        // Navigation items
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.List, contentDescription = null) },
            label = { Text("Tasks List") },
            selected = false,
            onClick = onViewTasks,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
            label = { Text("Progress Photos") },
            selected = false,
            onClick = onViewProgressGallery,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.MonitorWeight, contentDescription = null) },
            label = { Text("Weight Tracker") },
            selected = false,
            onClick = onViewWeightTracker,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
            label = { Text("Blood Pressure") },
            selected = false,
            onClick = onViewBloodPressureTracker,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
            label = { Text("Weekly Report") },
            selected = false,
            onClick = onViewWeeklyReport,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text("One-Off Tasks") },
            selected = false,
            onClick = onViewHistory,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun DayContent(
    uiState: HomeUiState,
    onToggleTask: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onSetNumericValue: (Long, Float) -> Unit,
    onSetStarValue: (Long, Int) -> Unit,
    onCapturePhoto: (Long) -> Unit,
    onSetBpReading: (Long, Int, Int, Int) -> Unit,
    onGoToToday: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Sticky header - stays fixed at top
        DayHeader(
            formattedDate = uiState.formattedDate,
            isToday = uiState.isToday,
            completionProgress = uiState.completionProgress,
            taskCount = uiState.tasksForDay.size,
            completedCount = uiState.tasksForDay.count { it.isCompleted },
            onGoToToday = onGoToToday,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Scrollable task list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Loading state
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                }
            }
            
            // Empty state
            if (!uiState.isLoading && uiState.tasksForDay.isEmpty()) {
                item {
                    EmptyDayState()
                }
            }
            
            // All tasks in a flat list (ordered by scheduledHour from DB)
            if (!uiState.isLoading && uiState.tasksForDay.isNotEmpty()) {
                items(uiState.tasksForDay, key = { it.task.id }) { taskWithStatus ->
                    TaskItem(
                        taskWithStatus = taskWithStatus,
                        onToggleCompletion = onToggleTask,
                        onEditTask = onEditTask,
                        onSetNumericValue = onSetNumericValue,
                        onSetStarValue = onSetStarValue,
                        onCapturePhoto = onCapturePhoto,
                        onSetBpReading = onSetBpReading
                    )
                }
            }
            
            // Bottom spacing for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun DayPlaceholder(
    date: LocalDate,
    isToday: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isToday) {
                    EmeraldPrimary.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = DateUtils.formatFull(date),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DayHeader(
    formattedDate: String,
    isToday: Boolean,
    completionProgress: Float,
    taskCount: Int,
    completedCount: Int,
    onGoToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) {
                EmeraldPrimary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isToday) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = EmeraldPrimary
                            ) {
                                Text(
                                    text = "TODAY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "$completedCount of $taskCount tasks completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!isToday) {
                    TextButton(onClick = onGoToToday) {
                        Text("Go to today", color = EmeraldPrimary)
                    }
                }
            }
            
            if (taskCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                
                AnimatedProgressBar(
                    progress = completionProgress,
                    modifier = Modifier.fillMaxWidth(),
                    height = 12.dp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${(completionProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyDayState() {
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
                imageVector = Icons.Default.EventAvailable,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No tasks for this day",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Add a task to get started!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
