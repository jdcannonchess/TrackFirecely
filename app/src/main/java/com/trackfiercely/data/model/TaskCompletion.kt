package com.trackfiercely.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Records when a task was completed on a specific date.
 * Now supports different value types based on TaskInputType.
 * 
 * @property id Unique identifier
 * @property taskId Reference to the parent Task
 * @property date The date (as epoch millis at midnight) when task was assigned
 * @property completedAt Timestamp when the task was marked complete (null if not completed)
 * @property numericValue Value for SLIDER or NUMBER input types
 * @property starValue Value for STARS input type (1-5 typically)
 * @property photoUri URI to the captured photo for PHOTO input type
 */
@Entity(
    tableName = "task_completions",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["date"]),
        Index(value = ["taskId", "date"], unique = true)
    ]
)
data class TaskCompletion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val date: Long, // Epoch millis at start of day
    val completedAt: Long? = null,
    val numericValue: Float? = null, // For SLIDER and NUMBER types
    val starValue: Int? = null, // For STARS type
    val photoUri: String? = null, // For PHOTO type
    val bpData: String? = null // For BLOOD_PRESSURE type (JSON string)
) {
    val isCompleted: Boolean 
        get() = completedAt != null
    
    /**
     * Check if this completion has a value recorded (for non-checkbox types)
     */
    val hasValue: Boolean
        get() = numericValue != null || starValue != null || photoUri != null || bpData != null
    
    /**
     * Get a display string for the recorded value
     */
    fun getValueDisplay(inputType: TaskInputType, suffix: String = ""): String {
        return when (inputType) {
            TaskInputType.CHECKBOX -> if (isCompleted) "✓" else ""
            TaskInputType.SLIDER -> numericValue?.toInt()?.toString() ?: "-"
            TaskInputType.STARS -> "★".repeat(starValue ?: 0)
            TaskInputType.NUMBER -> {
                numericValue?.let { value ->
                    if (value == value.toLong().toFloat()) {
                        "${value.toLong()}$suffix"
                    } else {
                        "$value$suffix"
                    }
                } ?: "-"
            }
            TaskInputType.PHOTO -> if (photoUri != null) "📷" else "-"
            TaskInputType.BLOOD_PRESSURE -> {
                bpData?.let { json ->
                    try {
                        val data = BloodPressureData.fromJson(json)
                        data.latestReading?.displayString ?: "-"
                    } catch (e: Exception) { "-" }
                } ?: "-"
            }
        }
    }
}
