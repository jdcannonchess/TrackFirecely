package com.trackfiercely.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trackfiercely.TrackFiercelyApp
import com.trackfiercely.data.repository.TaskRepository
import com.trackfiercely.data.repository.WeeklySnapshotRepository
import com.trackfiercely.ui.screens.*
import com.trackfiercely.util.DateUtils
import com.trackfiercely.viewmodel.*
import com.trackfiercely.viewmodel.ProgressGalleryViewModel
import com.trackfiercely.viewmodel.WeightTrackerViewModel

@Composable
fun TrackFiercelyNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val app = context.applicationContext as TrackFiercelyApp
    val database = app.database
    
    // Create repositories
    val taskRepository = remember {
        TaskRepository(database.taskDao(), database.taskCompletionDao())
    }
    val weeklySnapshotRepository = remember {
        WeeklySnapshotRepository(
            database.weeklySnapshotDao(),
            taskRepository
        )
    }
    
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route
    ) {
        // Home Screen
        composable(NavRoutes.Home.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(taskRepository)
            )
            
            HomeScreen(
                viewModel = viewModel,
                onAddTask = { navController.navigate(NavRoutes.CreateTask.route) },
                onEditTask = { taskId ->
                    navController.navigate(NavRoutes.EditTask.createRoute(taskId))
                },
                onViewTasks = { navController.navigate(NavRoutes.TaskList.route) },
                onViewWeeklyReport = { navController.navigate(NavRoutes.WeeklyReport.route) },
                onViewHistory = { navController.navigate(NavRoutes.History.route) },
                onViewProgressGallery = { navController.navigate(NavRoutes.ProgressGallery.route) },
                onViewWeightTracker = { navController.navigate(NavRoutes.WeightTracker.route) },
                onViewBloodPressureTracker = { navController.navigate(NavRoutes.BloodPressureTracker.route) },
                onCapturePhoto = { taskId, date ->
                    navController.navigate(NavRoutes.PhotoCapture.createRoute(taskId, DateUtils.toEpochMillis(date)))
                }
            )
        }
        
        // Task List Screen
        composable(NavRoutes.TaskList.route) {
            val viewModel: TaskListViewModel = viewModel(
                factory = TaskListViewModel.Factory(taskRepository)
            )
            
            TaskListScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onAddTask = { navController.navigate(NavRoutes.CreateTask.route) },
                onEditTask = { taskId ->
                    navController.navigate(NavRoutes.EditTask.createRoute(taskId))
                }
            )
        }
        
        // Create Task Screen
        composable(NavRoutes.CreateTask.route) {
            val viewModel: CreateTaskViewModel = viewModel(
                factory = CreateTaskViewModel.Factory(taskRepository)
            )
            
            CreateTaskScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        
        // Edit Task Screen
        composable(
            route = NavRoutes.EditTask.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: return@composable
            
            val viewModel: CreateTaskViewModel = viewModel(
                factory = CreateTaskViewModel.Factory(taskRepository, taskId)
            )
            
            CreateTaskScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        
        // Weekly Report Screen (current week)
        composable(NavRoutes.WeeklyReport.route) {
            val viewModel: WeeklyReportViewModel = viewModel(
                factory = WeeklyReportViewModel.Factory(
                    weeklySnapshotRepository,
                    taskRepository
                )
            )
            
            WeeklyReportScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Weekly Report Screen (specific week)
        composable(
            route = NavRoutes.WeeklyReportForDate.route,
            arguments = listOf(
                navArgument("weekStartMillis") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val weekStartMillis = backStackEntry.arguments?.getLong("weekStartMillis") ?: return@composable
            val weekStart = DateUtils.fromEpochMillis(weekStartMillis)
            
            val viewModel: WeeklyReportViewModel = viewModel(
                factory = WeeklyReportViewModel.Factory(
                    weeklySnapshotRepository,
                    taskRepository,
                    weekStart
                )
            )
            
            WeeklyReportScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        // History Screen (One-Off Tasks)
        composable(NavRoutes.History.route) {
            val viewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.Factory(taskRepository)
            )
            
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Photo Capture Screen
        composable(
            route = NavRoutes.PhotoCapture.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType },
                navArgument("dateMillis") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: return@composable
            val dateMillis = backStackEntry.arguments?.getLong("dateMillis") ?: return@composable
            val date = DateUtils.fromEpochMillis(dateMillis)
            
            PhotoCaptureScreen(
                taskId = taskId,
                date = date,
                taskRepository = taskRepository,
                onBack = { navController.popBackStack() },
                onPhotoSaved = { navController.popBackStack() }
            )
        }
        
        // Progress Gallery Screen
        composable(NavRoutes.ProgressGallery.route) {
            val viewModel: ProgressGalleryViewModel = viewModel(
                factory = ProgressGalleryViewModel.Factory(taskRepository)
            )
            
            ProgressGalleryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Weight Tracker Screen
        composable(NavRoutes.WeightTracker.route) {
            val viewModel: WeightTrackerViewModel = viewModel(
                factory = WeightTrackerViewModel.Factory(taskRepository, context)
            )
            
            WeightTrackerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Blood Pressure Tracker Screen
        composable(NavRoutes.BloodPressureTracker.route) {
            val viewModel: BloodPressureViewModel = viewModel(
                factory = BloodPressureViewModel.Factory(taskRepository)
            )
            
            BloodPressureTrackerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
