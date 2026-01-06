package com.trackfiercely.data

import com.trackfiercely.data.model.Category
import com.trackfiercely.data.model.InputConfig
import com.trackfiercely.data.model.Task
import com.trackfiercely.data.model.TaskInputType
import com.trackfiercely.data.model.ScheduleType
import com.trackfiercely.data.repository.TaskRepository
import org.json.JSONObject

/**
 * Seeds the database with pre-defined tasks on first launch.
 * These tasks serve as templates that users can assign to specific days.
 */
class DatabaseSeeder(
    private val taskRepository: TaskRepository
) {
    
    /**
     * Seed the database if it's empty (first launch)
     */
    suspend fun seedIfNeeded() {
        val existingTaskCount = taskRepository.getTaskCount()
        if (existingTaskCount == 0) {
            seedAllTasks()
        }
    }
    
    private suspend fun seedAllTasks() {
        // Wellness tasks (replacing DailyLog functionality)
        seedWellnessTasks()
        
        // User-requested tasks
        seedUserRequestedTasks()
        
        // Suggested common tasks
        seedCommonTasks()
    }
    
    // ============ WELLNESS TASKS ============
    
    private suspend fun seedWellnessTasks() {
        // Log Mood - Slider 1-10
        taskRepository.createTask(
            Task(
                name = "Log Mood",
                category = Category.PERSONAL_GROWTH,
                inputType = TaskInputType.SLIDER,
                inputConfig = createSliderConfig(1, 10),
                scheduleType = ScheduleType.WEEKLY,
                assignedDays = Task.ALL_DAYS,
                isSystemTask = true
            )
        )
        
        // Log Sleep Hours - Number input
        taskRepository.createTask(
            Task(
                name = "Log Sleep Hours",
                category = Category.HEALTH_FITNESS,
                inputType = TaskInputType.NUMBER,
                inputConfig = createNumberConfig("hrs", false),
                scheduleType = ScheduleType.WEEKLY,
                assignedDays = Task.ALL_DAYS,
                isSystemTask = true
            )
        )
        
        // Sleep Quality - Stars 1-5
        taskRepository.createTask(
            Task(
                name = "Sleep Quality",
                category = Category.HEALTH_FITNESS,
                inputType = TaskInputType.STARS,
                inputConfig = createStarsConfig(5),
                scheduleType = ScheduleType.WEEKLY,
                assignedDays = Task.ALL_DAYS,
                isSystemTask = true
            )
        )
        
        // Log Weight - Number input
        taskRepository.createTask(
            Task(
                name = "Log Weight",
                category = Category.HEALTH_FITNESS,
                inputType = TaskInputType.NUMBER,
                inputConfig = createNumberConfig("lbs", false),
                scheduleType = ScheduleType.WEEKLY,
                assignedDays = Task.ALL_DAYS,
                isSystemTask = true
            )
        )
        
        // Log Steps - Number input (integer)
        taskRepository.createTask(
            Task(
                name = "Log Steps",
                category = Category.HEALTH_FITNESS,
                inputType = TaskInputType.NUMBER,
                inputConfig = createNumberConfig("steps", true),
                scheduleType = ScheduleType.WEEKLY,
                assignedDays = Task.ALL_DAYS,
                isSystemTask = true
            )
        )
    }
    
    // ============ USER-REQUESTED TASKS ============
    
    private suspend fun seedUserRequestedTasks() {
        // Home & Chores
        createSimpleTask("Water Plants", Category.HOME_CHORES)
        createSimpleTask("Take out the Trash", Category.HOME_CHORES)
        createSimpleTask("Fill up Water in Vacuum", Category.HOME_CHORES)
        createSimpleTask("Clean Abby Bath Room", Category.HOME_CHORES)
        
        // Health & Fitness
        createSimpleTask("Run", Category.HEALTH_FITNESS)
        createSimpleTask("Lift", Category.HEALTH_FITNESS)
        createSimpleTask("Climb", Category.HEALTH_FITNESS)
        createSimpleTask("Acro Yoga", Category.HEALTH_FITNESS)
        createSimpleTask("Stretch", Category.HEALTH_FITNESS)
        
        // Family
        createSimpleTask("Read with Abby", Category.FAMILY)
        createSimpleTask("Call my dad", Category.FAMILY)
        createSimpleTask("Call my mom", Category.FAMILY)
        
        // Progress Photo - Special photo task
        taskRepository.createTask(
            Task(
                name = "Progress Photo",
                category = Category.HEALTH_FITNESS,
                inputType = TaskInputType.PHOTO,
                inputConfig = createPhotoConfig(5),
                scheduleType = ScheduleType.WEEKLY,
                assignedDays = 0, // Not assigned to any day initially
                isSystemTask = true
            )
        )
    }
    
    // ============ COMMON TASKS ============
    
    private suspend fun seedCommonTasks() {
        // Personal Growth
        createSimpleTask("Meditate", Category.PERSONAL_GROWTH)
        createSimpleTask("Journal", Category.PERSONAL_GROWTH)
        createSimpleTask("Read", Category.PERSONAL_GROWTH)
        createSimpleTask("Study/Learn", Category.PERSONAL_GROWTH)
        
        // Health
        taskRepository.createTask(
            Task(
                name = "Drink Water",
                category = Category.HEALTH_FITNESS,
                inputType = TaskInputType.NUMBER,
                inputConfig = createNumberConfig("glasses", true),
                scheduleType = ScheduleType.WEEKLY,
                assignedDays = 0,
                isSystemTask = true
            )
        )
        createSimpleTask("Take Vitamins", Category.HEALTH_FITNESS)
        createSimpleTask("Walk", Category.HEALTH_FITNESS)
        
        // Home
        createSimpleTask("Meal Prep", Category.HOME_CHORES)
        createSimpleTask("Grocery Shopping", Category.HOME_CHORES)
        createSimpleTask("Laundry", Category.HOME_CHORES)
        createSimpleTask("Clean Kitchen", Category.HOME_CHORES)
        createSimpleTask("Vacuum", Category.HOME_CHORES)
        
        // Hobbies
        createSimpleTask("Practice Instrument", Category.HOBBIES)
        createSimpleTask("Creative Project", Category.HOBBIES)
        createSimpleTask("Gaming", Category.HOBBIES)
        createSimpleTask("Photography", Category.HOBBIES)
        
        // Work
        createSimpleTask("Check Email", Category.WORK)
        createSimpleTask("Review Finances", Category.WORK)
        createSimpleTask("Plan Week", Category.WORK)
        
        // Family
        createSimpleTask("Family Dinner", Category.FAMILY)
        createSimpleTask("Date Night", Category.FAMILY)
    }
    
    // ============ HELPER METHODS ============
    
    private suspend fun createSimpleTask(name: String, category: Category) {
        taskRepository.createTask(
            Task(
                name = name,
                category = category,
                inputType = TaskInputType.CHECKBOX,
                inputConfig = "{}",
                scheduleType = ScheduleType.WEEKLY,
                assignedDays = 0, // Not assigned to any day initially
                isSystemTask = true
            )
        )
    }
    
    private fun createSliderConfig(min: Int, max: Int): String {
        return JSONObject().apply {
            put("minValue", min)
            put("maxValue", max)
        }.toString()
    }
    
    private fun createStarsConfig(count: Int): String {
        return JSONObject().apply {
            put("starCount", count)
        }.toString()
    }
    
    private fun createNumberConfig(suffix: String, isInteger: Boolean): String {
        return JSONObject().apply {
            put("suffix", suffix)
            put("isInteger", isInteger)
        }.toString()
    }
    
    private fun createPhotoConfig(timerSeconds: Int): String {
        return JSONObject().apply {
            put("defaultTimerSeconds", timerSeconds)
        }.toString()
    }
}

