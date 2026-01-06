package com.trackfiercely.data.model

/**
 * Defines the type of input required to complete a task.
 */
enum class TaskInputType {
    CHECKBOX,       // Simple completion toggle (default)
    SLIDER,         // Numeric slider (e.g., mood 1-10)
    STARS,          // Star rating (e.g., sleep quality 1-5)
    NUMBER,         // Numeric input (e.g., weight, steps)
    PHOTO,          // Photo capture with timer
    BLOOD_PRESSURE  // Blood pressure with systolic/diastolic/HR
}

