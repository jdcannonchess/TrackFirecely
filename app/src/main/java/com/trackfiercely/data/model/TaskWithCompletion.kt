package com.trackfiercely.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Combined Task with its completions for a specific date range.
 * Used for displaying task lists with completion status.
 */
data class TaskWithCompletion(
    @Embedded
    val task: Task,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val completions: List<TaskCompletion>
) {
    /**
     * Check if task is completed for a specific date
     */
    fun isCompletedForDate(date: Long): Boolean {
        return completions.any { it.date == date && it.isCompleted }
    }
    
    /**
     * Get completion for a specific date
     */
    fun getCompletionForDate(date: Long): TaskCompletion? {
        return completions.find { it.date == date }
    }
}

