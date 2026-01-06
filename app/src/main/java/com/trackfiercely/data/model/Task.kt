package com.trackfiercely.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Represents a habit/task that can have various input types and scheduling options.
 * 
 * @property id Unique identifier for the task
 * @property name Display name of the task
 * @property category Category for grouping and color coding
 * @property inputType How the task is completed (checkbox, slider, stars, number, photo)
 * @property inputConfig JSON configuration for the input type (min/max, star count, suffix, etc.)
 * @property scheduleType How the task repeats (weekly, monthly, yearly, one-time)
 * @property assignedDays Bitmask of days (1=Monday, 2=Tuesday, etc.) - for WEEKLY schedule
 * @property scheduledDate Specific date for ONE_TIME tasks (epoch millis at midnight)
 * @property monthlyDay Day of month (1-31) for MONTHLY schedule - null if using monthlyWeek
 * @property monthlyWeek Week of month (1-5) for MONTHLY schedule - null if using monthlyDay
 * @property monthlyDayOfWeek Day of week (1=Mon-7=Sun) when using monthlyWeek
 * @property yearlyMonth Month (1-12) for YEARLY schedule
 * @property yearlyDay Day of month (1-31) for YEARLY schedule - null if using yearlyWeek
 * @property yearlyWeek Week of month (1-5) for YEARLY schedule - null if using yearlyDay
 * @property yearlyDayOfWeek Day of week (1=Mon-7=Sun) when using yearlyWeek
 * @property autoRollover If true, one-time tasks move to next day if not completed
 * @property isHidden Whether the task is hidden from the main list
 * @property isSystemTask Whether this is a pre-seeded system task
 * @property createdAt Timestamp when task was created
 * @property isActive Whether the task is currently active
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: Category,
    val inputType: TaskInputType = TaskInputType.CHECKBOX,
    val inputConfig: String = "{}", // JSON configuration
    val scheduleType: ScheduleType = ScheduleType.WEEKLY,
    val assignedDays: Int = 0, // Bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
    val scheduledDate: Long? = null, // For ONE_TIME: specific date (epoch millis at midnight)
    val monthlyDay: Int? = null, // For MONTHLY: day of month (1-31)
    val monthlyWeek: Int? = null, // For MONTHLY: week of month (1-5)
    val monthlyDayOfWeek: Int? = null, // For MONTHLY with week: day of week (1-7)
    val yearlyMonth: Int? = null, // For YEARLY: month (1-12)
    val yearlyDay: Int? = null, // For YEARLY: day of month (1-31)
    val yearlyWeek: Int? = null, // For YEARLY: week of month (1-5)
    val yearlyDayOfWeek: Int? = null, // For YEARLY with week: day of week (1-7)
    val autoRollover: Boolean = true,
    val isHidden: Boolean = false,
    val isSystemTask: Boolean = false,
    val scheduledHour: Int? = null, // 0-23 for time-based ordering, null = no specific time
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    /**
     * Get display string for scheduled hour (e.g., "9 AM", "2 PM")
     */
    fun getScheduledTimeDisplay(): String? {
        return scheduledHour?.let { hour ->
            when {
                hour == 0 -> "12 AM"
                hour < 12 -> "$hour AM"
                hour == 12 -> "12 PM"
                else -> "${hour - 12} PM"
            }
        }
    }
    
    // Legacy property for backward compatibility
    val isRecurring: Boolean 
        get() = scheduleType != ScheduleType.ONE_TIME
    
    /**
     * Check if this task is assigned to a specific day for WEEKLY schedule
     */
    fun isAssignedToDay(dayOfWeek: DayOfWeek): Boolean {
        if (scheduleType != ScheduleType.WEEKLY) return false
        val dayBit = getDayBit(dayOfWeek)
        return (assignedDays and dayBit) != 0
    }
    
    /**
     * Check if this task should appear on a specific date
     */
    fun isScheduledFor(date: LocalDate): Boolean {
        return when (scheduleType) {
            ScheduleType.WEEKLY -> isAssignedToDay(date.dayOfWeek)
            ScheduleType.MONTHLY -> isScheduledForMonthly(date)
            ScheduleType.YEARLY -> isScheduledForYearly(date)
            ScheduleType.ONE_TIME -> {
                scheduledDate?.let { scheduled ->
                    val scheduledLocalDate = java.time.Instant.ofEpochMilli(scheduled)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    scheduledLocalDate == date
                } ?: false
            }
        }
    }
    
    private fun isScheduledForMonthly(date: LocalDate): Boolean {
        return if (monthlyDay != null) {
            // Specific day of month
            date.dayOfMonth == monthlyDay
        } else if (monthlyWeek != null && monthlyDayOfWeek != null) {
            // Nth weekday of month (e.g., 2nd Tuesday)
            val targetDayOfWeek = DayOfWeek.of(monthlyDayOfWeek)
            date.dayOfWeek == targetDayOfWeek && getWeekOfMonth(date) == monthlyWeek
        } else {
            false
        }
    }
    
    private fun isScheduledForYearly(date: LocalDate): Boolean {
        if (yearlyMonth == null || date.monthValue != yearlyMonth) return false
        
        return if (yearlyDay != null) {
            // Specific day of year
            date.dayOfMonth == yearlyDay
        } else if (yearlyWeek != null && yearlyDayOfWeek != null) {
            // Nth weekday of month in specific month
            val targetDayOfWeek = DayOfWeek.of(yearlyDayOfWeek)
            date.dayOfWeek == targetDayOfWeek && getWeekOfMonth(date) == yearlyWeek
        } else {
            false
        }
    }
    
    private fun getWeekOfMonth(date: LocalDate): Int {
        val firstDayOfMonth = date.withDayOfMonth(1)
        var count = 0
        var current = firstDayOfMonth
        while (current <= date) {
            if (current.dayOfWeek == date.dayOfWeek) {
                count++
            }
            current = current.plusDays(1)
        }
        return count
    }
    
    /**
     * Get list of assigned days for WEEKLY schedule
     */
    fun getAssignedDaysList(): List<DayOfWeek> {
        if (scheduleType != ScheduleType.WEEKLY) return emptyList()
        return DayOfWeek.entries.filter { isAssignedToDay(it) }
    }
    
    /**
     * Get human-readable schedule description
     */
    fun getScheduleDescription(): String {
        return when (scheduleType) {
            ScheduleType.WEEKLY -> {
                val days = getAssignedDaysList()
                when {
                    days.size == 7 -> "Every day"
                    days.size == 5 && days.none { it.value > 5 } -> "Weekdays"
                    days.size == 2 && days.all { it.value > 5 } -> "Weekends"
                    days.isEmpty() -> "Not assigned"
                    else -> days.joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
                }
            }
            ScheduleType.MONTHLY -> {
                if (monthlyDay != null) {
                    "Monthly on day $monthlyDay"
                } else if (monthlyWeek != null && monthlyDayOfWeek != null) {
                    val ordinal = getOrdinal(monthlyWeek)
                    val dayName = DayOfWeek.of(monthlyDayOfWeek).name.lowercase()
                        .replaceFirstChar { it.uppercase() }
                    "Monthly on $ordinal $dayName"
                } else {
                    "Monthly (not configured)"
                }
            }
            ScheduleType.YEARLY -> {
                val monthName = yearlyMonth?.let { 
                    java.time.Month.of(it).name.lowercase().replaceFirstChar { c -> c.uppercase() }
                } ?: "?"
                if (yearlyDay != null) {
                    "Yearly on $monthName $yearlyDay"
                } else if (yearlyWeek != null && yearlyDayOfWeek != null) {
                    val ordinal = getOrdinal(yearlyWeek)
                    val dayName = DayOfWeek.of(yearlyDayOfWeek).name.lowercase()
                        .replaceFirstChar { it.uppercase() }
                    "Yearly on $ordinal $dayName of $monthName"
                } else {
                    "Yearly (not configured)"
                }
            }
            ScheduleType.ONE_TIME -> {
                scheduledDate?.let { millis ->
                    val date = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    val today = LocalDate.now()
                    when (date) {
                        today -> "Today"
                        today.plusDays(1) -> "Tomorrow"
                        else -> date.toString()
                    }
                } ?: "Not scheduled"
            }
        }
    }
    
    private fun getOrdinal(n: Int): String {
        return when (n) {
            1 -> "1st"
            2 -> "2nd"
            3 -> "3rd"
            else -> "${n}th"
        }
    }
    
    companion object {
        /**
         * Get the bitmask value for a day of week
         */
        fun getDayBit(dayOfWeek: DayOfWeek): Int {
            return when (dayOfWeek) {
                DayOfWeek.MONDAY -> 1
                DayOfWeek.TUESDAY -> 2
                DayOfWeek.WEDNESDAY -> 4
                DayOfWeek.THURSDAY -> 8
                DayOfWeek.FRIDAY -> 16
                DayOfWeek.SATURDAY -> 32
                DayOfWeek.SUNDAY -> 64
            }
        }
        
        /**
         * Create a bitmask from a list of days
         */
        fun createDaysMask(days: List<DayOfWeek>): Int {
            return days.fold(0) { acc, day -> acc or getDayBit(day) }
        }
        
        /**
         * Bitmask for all days (everyday)
         */
        const val ALL_DAYS = 127 // 1+2+4+8+16+32+64
        
        /**
         * Bitmask for weekdays only
         */
        const val WEEKDAYS = 31 // 1+2+4+8+16
        
        /**
         * Bitmask for weekends only
         */
        const val WEEKENDS = 96 // 32+64
    }
}
