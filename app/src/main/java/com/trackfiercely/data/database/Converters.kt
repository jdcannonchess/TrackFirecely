package com.trackfiercely.data.database

import androidx.room.TypeConverter
import com.trackfiercely.data.model.Category
import com.trackfiercely.data.model.TaskInputType
import com.trackfiercely.data.model.ScheduleType

/**
 * Type converters for Room database to handle enum types
 */
class Converters {
    
    // Category converters
    @TypeConverter
    fun fromCategory(category: Category): String {
        return category.name
    }
    
    @TypeConverter
    fun toCategory(value: String): Category {
        return Category.valueOf(value)
    }
    
    // TaskInputType converters
    @TypeConverter
    fun fromTaskInputType(inputType: TaskInputType): String {
        return inputType.name
    }
    
    @TypeConverter
    fun toTaskInputType(value: String): TaskInputType {
        return try {
            TaskInputType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            TaskInputType.CHECKBOX
        }
    }
    
    // ScheduleType converters
    @TypeConverter
    fun fromScheduleType(scheduleType: ScheduleType): String {
        return scheduleType.name
    }
    
    @TypeConverter
    fun toScheduleType(value: String): ScheduleType {
        return try {
            ScheduleType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ScheduleType.WEEKLY
        }
    }
}
