package com.trackfiercely.ui.navigation

sealed class NavRoutes(val route: String) {
    object Home : NavRoutes("home")
    object TaskList : NavRoutes("task_list")
    object CreateTask : NavRoutes("create_task")
    object EditTask : NavRoutes("edit_task/{taskId}") {
        fun createRoute(taskId: Long) = "edit_task/$taskId"
    }
    object WeeklyReport : NavRoutes("weekly_report")
    object WeeklyReportForDate : NavRoutes("weekly_report/{weekStartMillis}") {
        fun createRoute(weekStartMillis: Long) = "weekly_report/$weekStartMillis"
    }
    object History : NavRoutes("history")
    object PhotoCapture : NavRoutes("photo_capture/{taskId}/{dateMillis}") {
        fun createRoute(taskId: Long, dateMillis: Long) = "photo_capture/$taskId/$dateMillis"
    }
    object ProgressGallery : NavRoutes("progress_gallery")
    object WeightTracker : NavRoutes("weight_tracker")
    object BloodPressureTracker : NavRoutes("blood_pressure_tracker")
}
