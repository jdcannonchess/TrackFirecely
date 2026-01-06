package com.trackfiercely.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Weekly summary snapshot containing aggregated stats for a week.
 * Generated at end of each week (Sunday) or when viewing history.
 * 
 * @property id Unique identifier
 * @property weekStartDate Monday's date (epoch millis at midnight)
 * @property tasksCompleted Total number of tasks completed during the week
 * @property totalTasks Total number of tasks that were scheduled
 * @property avgMood Average mood rating for the week
 * @property avgSleepHours Average hours of sleep
 * @property avgSleepQuality Average sleep quality rating
 * @property avgWeight Average weight recorded
 * @property totalSteps Total steps for the week
 * @property perfectDays Number of days with 100% task completion
 * @property longestStreak Longest streak of consecutive completed tasks
 * @property highlights JSON string of notable achievements/patterns
 * @property createdAt When this snapshot was generated
 */
@Entity(
    tableName = "weekly_snapshots",
    indices = [
        Index(value = ["weekStartDate"], unique = true)
    ]
)
data class WeeklySnapshot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weekStartDate: Long, // Monday's date at midnight
    val tasksCompleted: Int = 0,
    val totalTasks: Int = 0,
    val avgMood: Float? = null,
    val avgSleepHours: Float? = null,
    val avgSleepQuality: Float? = null,
    val avgWeight: Float? = null,
    val totalSteps: Int = 0,
    val perfectDays: Int = 0,
    val longestStreak: Int = 0,
    val highlights: String? = null, // JSON array of highlight strings
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculate task completion percentage
     */
    val completionPercentage: Float
        get() = if (totalTasks > 0) tasksCompleted.toFloat() / totalTasks else 0f
    
    /**
     * Get letter grade based on completion percentage
     */
    val grade: String
        get() = when {
            completionPercentage >= 0.95f -> "A+"
            completionPercentage >= 0.90f -> "A"
            completionPercentage >= 0.85f -> "A-"
            completionPercentage >= 0.80f -> "B+"
            completionPercentage >= 0.75f -> "B"
            completionPercentage >= 0.70f -> "B-"
            completionPercentage >= 0.65f -> "C+"
            completionPercentage >= 0.60f -> "C"
            completionPercentage >= 0.55f -> "C-"
            completionPercentage >= 0.50f -> "D"
            else -> "F"
        }
}

