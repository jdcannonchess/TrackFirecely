package com.trackfiercely.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trackfiercely.data.repository.TaskRepository
import com.trackfiercely.data.repository.WeeklySnapshotRepository
import com.trackfiercely.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

// Extension for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "track_fiercely_prefs")

/**
 * Manages weekly task setup, rollover of incomplete tasks, and snapshot generation.
 * Called when the app starts to ensure proper initialization.
 */
class WeeklySetupManager(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val weeklySnapshotRepository: WeeklySnapshotRepository
) {
    
    companion object {
        private val LAST_WEEK_SETUP_KEY = longPreferencesKey("last_week_setup")
        private val LAST_ROLLOVER_DATE_KEY = longPreferencesKey("last_rollover_date")
    }
    
    /**
     * Perform all startup tasks:
     * 1. Weekly setup (if new week)
     * 2. Rollover incomplete one-time tasks (daily)
     */
    suspend fun performStartupTasks() {
        performWeeklySetupIfNeeded()
        performDailyRolloverIfNeeded()
    }
    
    /**
     * Check and perform weekly setup if needed.
     * Called when we enter a new week.
     */
    suspend fun performWeeklySetupIfNeeded() {
        val today = LocalDate.now()
        val currentWeekStart = DateUtils.getWeekStart(today)
        val currentWeekStartMillis = DateUtils.toEpochMillis(currentWeekStart)
        
        // Get the last setup week
        val lastSetupWeekMillis = context.dataStore.data
            .map { prefs -> prefs[LAST_WEEK_SETUP_KEY] ?: 0L }
            .first()
        
        // If we're in a new week (or first time)
        if (lastSetupWeekMillis < currentWeekStartMillis) {
            // Generate snapshot for previous week if it doesn't exist
            if (lastSetupWeekMillis > 0) {
                val previousWeekStart = DateUtils.fromEpochMillis(lastSetupWeekMillis)
                weeklySnapshotRepository.generateSnapshotIfNeeded(previousWeekStart)
            }
            
            // Setup task completions for the new week
            taskRepository.setupWeekCompletions(currentWeekStart)
            
            // Update the last setup timestamp
            context.dataStore.edit { prefs ->
                prefs[LAST_WEEK_SETUP_KEY] = currentWeekStartMillis
            }
        }
    }
    
    /**
     * Roll over incomplete one-time tasks to today.
     * Called once per day when the app starts.
     */
    suspend fun performDailyRolloverIfNeeded() {
        val todayMillis = DateUtils.toEpochMillis(LocalDate.now())
        
        // Get the last rollover date
        val lastRolloverDateMillis = context.dataStore.data
            .map { prefs -> prefs[LAST_ROLLOVER_DATE_KEY] ?: 0L }
            .first()
        
        // If we haven't done rollover today
        if (lastRolloverDateMillis < todayMillis) {
            // Roll over incomplete tasks with autoRollover enabled
            taskRepository.rolloverIncompleteTasks()
            
            // Update the last rollover timestamp
            context.dataStore.edit { prefs ->
                prefs[LAST_ROLLOVER_DATE_KEY] = todayMillis
            }
        }
    }
    
    /**
     * Force rollover now (useful for testing)
     */
    suspend fun forceRollover() {
        taskRepository.rolloverIncompleteTasks()
    }
    
    /**
     * Force setup for current week (useful for testing or manual refresh)
     */
    suspend fun forceWeekSetup() {
        val currentWeekStart = DateUtils.getWeekStart(LocalDate.now())
        taskRepository.setupWeekCompletions(currentWeekStart)
    }
    
    /**
     * Generate snapshot for a specific week
     */
    suspend fun generateSnapshotForWeek(weekStart: LocalDate) {
        weeklySnapshotRepository.generateAndSaveSnapshot(weekStart)
    }
}

