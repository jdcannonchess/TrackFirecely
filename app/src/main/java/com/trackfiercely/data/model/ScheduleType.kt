package com.trackfiercely.data.model

/**
 * Defines how a task is scheduled/repeated.
 */
enum class ScheduleType {
    WEEKLY,      // Repeat on specific days of week (uses assignedDays bitmask)
    MONTHLY,     // Repeat monthly (day X or Xth weekday)
    YEARLY,      // Repeat yearly (month + day or month + Xth weekday)
    ONE_TIME     // Single occurrence (uses scheduledDate)
}

