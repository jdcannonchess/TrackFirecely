package com.trackfiercely.data.repository

import com.trackfiercely.data.database.WeeklySnapshotDao
import com.trackfiercely.data.model.WeeklySnapshot
import com.trackfiercely.data.model.TaskInputType
import com.trackfiercely.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class WeeklySnapshotRepository(
    private val snapshotDao: WeeklySnapshotDao,
    private val taskRepository: TaskRepository
) {
    
    // ============ SNAPSHOT QUERIES ============
    
    suspend fun getSnapshot(snapshotId: Long): WeeklySnapshot? {
        return snapshotDao.getSnapshotById(snapshotId)
    }
    
    suspend fun getSnapshotForWeek(weekStart: LocalDate): WeeklySnapshot? {
        val weekStartMillis = DateUtils.toEpochMillis(weekStart)
        return snapshotDao.getSnapshotForWeek(weekStartMillis)
    }
    
    fun observeSnapshotForWeek(weekStart: LocalDate): Flow<WeeklySnapshot?> {
        val weekStartMillis = DateUtils.toEpochMillis(weekStart)
        return snapshotDao.observeSnapshotForWeek(weekStartMillis)
    }
    
    fun observeAllSnapshots(): Flow<List<WeeklySnapshot>> {
        return snapshotDao.observeAllSnapshots()
    }
    
    fun observeRecentSnapshots(limit: Int = 12): Flow<List<WeeklySnapshot>> {
        return snapshotDao.observeRecentSnapshots(limit)
    }
    
    suspend fun snapshotExists(weekStart: LocalDate): Boolean {
        val weekStartMillis = DateUtils.toEpochMillis(weekStart)
        return snapshotDao.snapshotExists(weekStartMillis)
    }
    
    // ============ SNAPSHOT GENERATION ============
    
    /**
     * Generate a weekly snapshot for the given week.
     * Calculates all stats and highlights from task completions.
     * Wellness data (mood, sleep, weight, steps) now comes from wellness tasks.
     */
    suspend fun generateSnapshot(weekStart: LocalDate): WeeklySnapshot {
        val weekStartMillis = DateUtils.toEpochMillis(weekStart)
        
        // Get task completion stats
        val (completed, total) = taskRepository.getCompletionStatsForWeek(weekStart)
        val perfectDays = taskRepository.getPerfectDaysInWeek(weekStart)
        
        // Get wellness stats from wellness tasks
        val wellnessStats = taskRepository.getWellnessStatsForWeek(weekStart)
        
        // Generate highlights
        val highlights = generateHighlights(
            completed = completed,
            total = total,
            perfectDays = perfectDays.size,
            avgMood = wellnessStats.avgMood,
            avgSleepHours = wellnessStats.avgSleepHours
        )
        
        val snapshot = WeeklySnapshot(
            weekStartDate = weekStartMillis,
            tasksCompleted = completed,
            totalTasks = total,
            avgMood = wellnessStats.avgMood,
            avgSleepHours = wellnessStats.avgSleepHours,
            avgSleepQuality = wellnessStats.avgSleepQuality,
            avgWeight = wellnessStats.avgWeight,
            totalSteps = wellnessStats.totalSteps,
            perfectDays = perfectDays.size,
            longestStreak = calculateLongestStreak(weekStart),
            highlights = highlights
        )
        
        return snapshot
    }
    
    /**
     * Generate and save a weekly snapshot
     */
    suspend fun generateAndSaveSnapshot(weekStart: LocalDate): Long {
        val snapshot = generateSnapshot(weekStart)
        return snapshotDao.insert(snapshot)
    }
    
    /**
     * Check if we need to generate a snapshot for last week (called on Monday)
     */
    suspend fun generateSnapshotIfNeeded(previousWeekStart: LocalDate) {
        if (!snapshotExists(previousWeekStart)) {
            generateAndSaveSnapshot(previousWeekStart)
        }
    }
    
    // ============ HIGHLIGHTS GENERATION ============
    
    private fun generateHighlights(
        completed: Int,
        total: Int,
        perfectDays: Int,
        avgMood: Float?,
        avgSleepHours: Float?
    ): String {
        val highlights = mutableListOf<String>()
        
        val completionRate = if (total > 0) completed.toFloat() / total else 0f
        
        // Completion milestones
        when {
            completionRate >= 1.0f -> highlights.add("🎯 Perfect week! All tasks completed!")
            completionRate >= 0.9f -> highlights.add("🔥 Crushed it! Over 90% completion!")
            completionRate >= 0.75f -> highlights.add("💪 Strong week! 75%+ tasks done!")
            completionRate >= 0.5f -> highlights.add("👍 Good effort! Over half completed!")
            else -> { /* No highlight for low completion */ }
        }
        
        // Perfect days
        when {
            perfectDays >= 7 -> highlights.add("⭐ Perfect attendance all week!")
            perfectDays >= 5 -> highlights.add("⭐ $perfectDays perfect days this week!")
            perfectDays >= 3 -> highlights.add("✨ $perfectDays days with 100% completion!")
            perfectDays >= 1 -> highlights.add("✨ Had $perfectDays perfect day(s)!")
            else -> { /* No highlight for no perfect days */ }
        }
        
        // Sleep insights
        avgSleepHours?.let { hours ->
            when {
                hours >= 8f -> highlights.add("😴 Great sleep average: ${String.format("%.1f", hours)} hours!")
                hours >= 7f -> highlights.add("😊 Solid sleep: ${String.format("%.1f", hours)} hours average")
                hours < 6f -> highlights.add("⚠️ Consider more sleep (${String.format("%.1f", hours)} hr avg)")
                else -> { /* No highlight for average sleep */ }
            }
        }
        
        // Mood insights
        avgMood?.let { mood ->
            when {
                mood >= 8f -> highlights.add("😄 Excellent mood week! Avg: ${String.format("%.1f", mood)}")
                mood >= 6f -> highlights.add("🙂 Good vibes! Mood avg: ${String.format("%.1f", mood)}")
                mood < 4f -> highlights.add("💙 Tough week. Be kind to yourself.")
                else -> { /* No highlight for average mood */ }
            }
        }
        
        return highlights.joinToString("|")
    }
    
    private suspend fun calculateLongestStreak(weekStart: LocalDate): Int {
        // Simplified streak calculation - count consecutive days with any completion
        val perfectDays = taskRepository.getPerfectDaysInWeek(weekStart)
            .map { it.dayOfWeek.value }
            .sorted()
        
        if (perfectDays.isEmpty()) return 0
        
        var maxStreak = 1
        var currentStreak = 1
        
        for (i in 1 until perfectDays.size) {
            if (perfectDays[i] == perfectDays[i - 1] + 1) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        
        return maxStreak
    }
    
    // ============ STATISTICS ============
    
    suspend fun getAverageCompletionRate(): Float? {
        return snapshotDao.getAverageCompletionRate()
    }
    
    suspend fun getBestCompletionRate(): Float? {
        return snapshotDao.getBestCompletionRate()
    }
    
    suspend fun getTotalWeeksTracked(): Int {
        return snapshotDao.getTotalWeeksTracked()
    }
}

/**
 * Wellness statistics extracted from wellness tasks
 */
data class WellnessStats(
    val avgMood: Float? = null,
    val avgSleepHours: Float? = null,
    val avgSleepQuality: Float? = null,
    val avgWeight: Float? = null,
    val totalSteps: Int = 0
)
