package com.trackfiercely.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Utility functions for date calculations used throughout the app.
 * Week starts on Monday and ends on Sunday.
 */
object DateUtils {
    
    private val zoneId: ZoneId = ZoneId.systemDefault()
    
    /**
     * Minimum date for the pager (1 year ago from now)
     */
    private val MIN_DATE: LocalDate = LocalDate.now().minusYears(1)
    
    /**
     * Get the minimum date for pager initialization
     */
    fun getMinDate(): LocalDate = MIN_DATE
    
    /**
     * Get days since minimum date for pager page calculation
     */
    fun daysSinceMinDate(date: LocalDate): Int {
        return java.time.temporal.ChronoUnit.DAYS.between(MIN_DATE, date).toInt()
    }
    
    /**
     * Get date from pager page index
     */
    fun dateFromPageIndex(pageIndex: Int): LocalDate {
        return MIN_DATE.plusDays(pageIndex.toLong())
    }
    
    /**
     * Get the start of day (midnight) as epoch millis for a LocalDate
     */
    fun toEpochMillis(date: LocalDate): Long {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
    
    /**
     * Convert epoch millis to LocalDate
     */
    fun fromEpochMillis(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
    }
    
    /**
     * Get today's date at midnight as epoch millis
     */
    fun todayMillis(): Long {
        return toEpochMillis(LocalDate.now())
    }
    
    /**
     * Get today's LocalDate
     */
    fun today(): LocalDate {
        return LocalDate.now()
    }
    
    /**
     * Get the Monday of the week containing the given date
     */
    fun getWeekStart(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    
    /**
     * Get the Sunday of the week containing the given date
     */
    fun getWeekEnd(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    }
    
    /**
     * Get Monday of current week as epoch millis
     */
    fun currentWeekStartMillis(): Long {
        return toEpochMillis(getWeekStart(LocalDate.now()))
    }
    
    /**
     * Get Sunday of current week as epoch millis
     */
    fun currentWeekEndMillis(): Long {
        return toEpochMillis(getWeekEnd(LocalDate.now()))
    }
    
    /**
     * Get all dates in a week (Monday to Sunday)
     */
    fun getWeekDates(weekStart: LocalDate): List<LocalDate> {
        return (0..6).map { weekStart.plusDays(it.toLong()) }
    }
    
    /**
     * Get all dates for the week containing the given date
     */
    fun getWeekDatesFor(date: LocalDate): List<LocalDate> {
        val weekStart = getWeekStart(date)
        return getWeekDates(weekStart)
    }
    
    /**
     * Check if a date is in the current week
     */
    fun isCurrentWeek(date: LocalDate): Boolean {
        val today = LocalDate.now()
        val weekStart = getWeekStart(today)
        val weekEnd = getWeekEnd(today)
        return !date.isBefore(weekStart) && !date.isAfter(weekEnd)
    }
    
    /**
     * Check if today is Monday
     */
    fun isMonday(): Boolean {
        return LocalDate.now().dayOfWeek == DayOfWeek.MONDAY
    }
    
    /**
     * Check if today is Sunday
     */
    fun isSunday(): Boolean {
        return LocalDate.now().dayOfWeek == DayOfWeek.SUNDAY
    }
    
    /**
     * Get the previous week's Monday
     */
    fun getPreviousWeekStart(date: LocalDate = LocalDate.now()): LocalDate {
        return getWeekStart(date).minusWeeks(1)
    }
    
    /**
     * Get the next week's Monday
     */
    fun getNextWeekStart(date: LocalDate = LocalDate.now()): LocalDate {
        return getWeekStart(date).plusWeeks(1)
    }
    
    /**
     * Format date as "Mon, Jan 5"
     */
    fun formatShort(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }
    
    /**
     * Format date as "Monday, January 5"
     */
    fun formatFull(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
    }
    
    /**
     * Format date as "Jan 5 - Jan 11"
     */
    fun formatWeekRange(weekStart: LocalDate): String {
        val weekEnd = weekStart.plusDays(6)
        val startFormatter = DateTimeFormatter.ofPattern("MMM d")
        val endFormatter = if (weekStart.month == weekEnd.month) {
            DateTimeFormatter.ofPattern("d")
        } else {
            DateTimeFormatter.ofPattern("MMM d")
        }
        return "${weekStart.format(startFormatter)} - ${weekEnd.format(endFormatter)}"
    }
    
    /**
     * Format date as "January 5, 2026"
     */
    fun formatWithYear(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    }
    
    /**
     * Get day of week abbreviation (Mon, Tue, etc.)
     */
    fun getDayAbbreviation(dayOfWeek: DayOfWeek): String {
        return dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
    }
    
    /**
     * Get single letter day abbreviation (M, T, W, etc.)
     */
    fun getDayLetter(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "M"
            DayOfWeek.TUESDAY -> "T"
            DayOfWeek.WEDNESDAY -> "W"
            DayOfWeek.THURSDAY -> "T"
            DayOfWeek.FRIDAY -> "F"
            DayOfWeek.SATURDAY -> "S"
            DayOfWeek.SUNDAY -> "S"
        }
    }
    
    /**
     * Get week number of the year
     */
    fun getWeekNumber(date: LocalDate): Int {
        val weekFields = WeekFields.of(DayOfWeek.MONDAY, 4)
        return date.get(weekFields.weekOfWeekBasedYear())
    }
}

