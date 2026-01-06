package com.trackfiercely

import android.app.Application
import com.trackfiercely.data.DatabaseSeeder
import com.trackfiercely.data.WeeklySetupManager
import com.trackfiercely.data.database.TrackFiercelyDatabase
import com.trackfiercely.data.repository.TaskRepository
import com.trackfiercely.data.repository.WeeklySnapshotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TrackFiercelyApp : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    val database: TrackFiercelyDatabase by lazy {
        TrackFiercelyDatabase.getDatabase(this)
    }
    
    private val taskRepository by lazy {
        TaskRepository(database.taskDao(), database.taskCompletionDao())
    }
    
    private val weeklySnapshotRepository by lazy {
        WeeklySnapshotRepository(
            database.weeklySnapshotDao(),
            taskRepository
        )
    }
    
    private val weeklySetupManager by lazy {
        WeeklySetupManager(this, taskRepository, weeklySnapshotRepository)
    }
    
    private val databaseSeeder by lazy {
        DatabaseSeeder(taskRepository)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Perform startup tasks (seed database, weekly setup + daily rollover)
        applicationScope.launch(Dispatchers.IO) {
            // Seed pre-defined tasks on first launch
            databaseSeeder.seedIfNeeded()
            
            // Weekly setup and rollover
            weeklySetupManager.performStartupTasks()
        }
    }
}
