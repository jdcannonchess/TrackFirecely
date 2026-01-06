package com.trackfiercely.data.repository

import com.trackfiercely.data.database.TaskCompletionDao
import com.trackfiercely.data.database.TaskDao
import com.trackfiercely.data.model.Category
import com.trackfiercely.data.model.Task
import com.trackfiercely.data.model.TaskCompletion
import com.trackfiercely.data.model.TaskInputType
import com.trackfiercely.data.model.TaskWithCompletion
import com.trackfiercely.data.model.ScheduleType
import com.trackfiercely.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate

class TaskRepository(
    private val taskDao: TaskDao,
    val completionDao: TaskCompletionDao // Made public for weight tracker access
) {
    
    // ============ TASK CRUD OPERATIONS ============
    
    suspend fun createTask(task: Task): Long {
        return taskDao.insert(task)
    }
    
    /**
     * Create a weekly recurring task assigned to specific days of the week
     */
    suspend fun createWeeklyTask(
        name: String,
        category: Category,
        assignedDays: List<DayOfWeek>,
        inputType: TaskInputType = TaskInputType.CHECKBOX,
        inputConfig: String = "{}",
        isSystemTask: Boolean = false
    ): Long {
        val task = Task(
            name = name,
            category = category,
            inputType = inputType,
            inputConfig = inputConfig,
            scheduleType = ScheduleType.WEEKLY,
            assignedDays = Task.createDaysMask(assignedDays),
            isSystemTask = isSystemTask
        )
        return taskDao.insert(task)
    }
    
    /**
     * Create a monthly recurring task
     */
    suspend fun createMonthlyTask(
        name: String,
        category: Category,
        monthlyDay: Int? = null,
        monthlyWeek: Int? = null,
        monthlyDayOfWeek: Int? = null,
        inputType: TaskInputType = TaskInputType.CHECKBOX,
        inputConfig: String = "{}",
        isSystemTask: Boolean = false
    ): Long {
        val task = Task(
            name = name,
            category = category,
            inputType = inputType,
            inputConfig = inputConfig,
            scheduleType = ScheduleType.MONTHLY,
            monthlyDay = monthlyDay,
            monthlyWeek = monthlyWeek,
            monthlyDayOfWeek = monthlyDayOfWeek,
            isSystemTask = isSystemTask
        )
        return taskDao.insert(task)
    }
    
    /**
     * Create a yearly recurring task
     */
    suspend fun createYearlyTask(
        name: String,
        category: Category,
        yearlyMonth: Int,
        yearlyDay: Int? = null,
        yearlyWeek: Int? = null,
        yearlyDayOfWeek: Int? = null,
        inputType: TaskInputType = TaskInputType.CHECKBOX,
        inputConfig: String = "{}",
        isSystemTask: Boolean = false
    ): Long {
        val task = Task(
            name = name,
            category = category,
            inputType = inputType,
            inputConfig = inputConfig,
            scheduleType = ScheduleType.YEARLY,
            yearlyMonth = yearlyMonth,
            yearlyDay = yearlyDay,
            yearlyWeek = yearlyWeek,
            yearlyDayOfWeek = yearlyDayOfWeek,
            isSystemTask = isSystemTask
        )
        return taskDao.insert(task)
    }
    
    /**
     * Create a one-time task scheduled for a specific date
     */
    suspend fun createOneTimeTask(
        name: String,
        category: Category,
        scheduledDate: LocalDate,
        autoRollover: Boolean = true,
        inputType: TaskInputType = TaskInputType.CHECKBOX,
        inputConfig: String = "{}",
        isSystemTask: Boolean = false
    ): Long {
        val task = Task(
            name = name,
            category = category,
            inputType = inputType,
            inputConfig = inputConfig,
            scheduleType = ScheduleType.ONE_TIME,
            scheduledDate = DateUtils.toEpochMillis(scheduledDate),
            autoRollover = autoRollover,
            isSystemTask = isSystemTask
        )
        return taskDao.insert(task)
    }
    
    suspend fun updateTask(task: Task) {
        taskDao.update(task)
    }
    
    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteById(taskId)
    }
    
    suspend fun archiveTask(taskId: Long) {
        taskDao.archiveTask(taskId)
    }
    
    suspend fun getTask(taskId: Long): Task? {
        return taskDao.getTaskById(taskId)
    }
    
    fun observeTask(taskId: Long): Flow<Task?> {
        return taskDao.observeTaskById(taskId)
    }
    
    // ============ TASK VISIBILITY ============
    
    suspend fun hideTask(taskId: Long) {
        taskDao.setTaskHidden(taskId, true)
    }
    
    suspend fun showTask(taskId: Long) {
        taskDao.setTaskHidden(taskId, false)
    }
    
    suspend fun toggleTaskHidden(taskId: Long) {
        taskDao.toggleTaskHidden(taskId)
    }
    
    // ============ TASK QUERIES ============
    
    fun observeAllActiveTasks(): Flow<List<Task>> {
        return taskDao.observeAllActiveTasks()
    }
    
    fun observeVisibleTasks(): Flow<List<Task>> {
        return taskDao.observeVisibleTasks()
    }
    
    fun observeHiddenTasks(): Flow<List<Task>> {
        return taskDao.observeHiddenTasks()
    }
    
    fun observeAssignedTasks(): Flow<List<Task>> {
        return taskDao.observeAssignedTasks()
    }
    
    fun observeUnassignedTasks(): Flow<List<Task>> {
        return taskDao.observeUnassignedTasks()
    }
    
    fun observeRecurringTasks(): Flow<List<Task>> {
        return taskDao.observeRecurringTasks()
    }
    
    fun observeOneTimeTasks(): Flow<List<Task>> {
        return taskDao.observeOneTimeTasks()
    }
    
    fun observeTasksByCategory(category: Category): Flow<List<Task>> {
        return taskDao.observeTasksByCategory(category)
    }
    
    fun observeTasksByInputType(inputType: TaskInputType): Flow<List<Task>> {
        return taskDao.observeTasksByInputType(inputType)
    }
    
    suspend fun getAllTasks(): List<Task> {
        return taskDao.getAllTasks()
    }
    
    suspend fun getTaskCount(): Int {
        return taskDao.getTaskCount()
    }
    
    // ============ TASKS FOR SPECIFIC DAY ============
    
    /**
     * Get all tasks scheduled for a specific date
     */
    fun observeTasksForDate(date: LocalDate): Flow<List<Task>> {
        val dayBit = Task.getDayBit(date.dayOfWeek)
        val dateMillis = DateUtils.toEpochMillis(date)
        val dayOfMonth = date.dayOfMonth
        val month = date.monthValue
        
        return taskDao.observeTasksForDay(dayBit, dateMillis)
            .map { tasks ->
                tasks.filter { it.isScheduledFor(date) }
            }
    }
    
    fun observeTasksWithCompletionsForDate(date: LocalDate): Flow<List<TaskWithCompletion>> {
        val dayBit = Task.getDayBit(date.dayOfWeek)
        val dateMillis = DateUtils.toEpochMillis(date)
        return taskDao.observeTasksWithCompletionsForDay(dayBit, dateMillis)
            .map { tasksWithCompletions ->
                tasksWithCompletions.filter { it.task.isScheduledFor(date) }
            }
    }
    
    /**
     * Get tasks scheduled for a specific date with completion status
     */
    fun observeTasksForDateWithStatus(date: LocalDate): Flow<List<Pair<Task, Boolean>>> {
        val dateMillis = DateUtils.toEpochMillis(date)
        val dayBit = Task.getDayBit(date.dayOfWeek)
        
        return taskDao.observeTasksWithCompletionsForDay(dayBit, dateMillis)
            .map { tasksWithCompletions ->
                tasksWithCompletions
                    .filter { it.task.isScheduledFor(date) }
                    .map { taskWithCompletion ->
                        val isCompleted = taskWithCompletion.isCompletedForDate(dateMillis)
                        taskWithCompletion.task to isCompleted
                    }
            }
    }
    
    // ============ ROLLOVER OPERATIONS ============
    
    /**
     * Roll over incomplete one-time tasks to today.
     */
    suspend fun rolloverIncompleteTasks() {
        val todayMillis = DateUtils.toEpochMillis(LocalDate.now())
        val tasksToRollover = taskDao.getTasksNeedingRollover(todayMillis)
        
        for (task in tasksToRollover) {
            val originalDate = task.scheduledDate ?: continue
            val completion = completionDao.getCompletion(task.id, originalDate)
            
            if (completion == null || completion.completedAt == null) {
                taskDao.updateScheduledDate(task.id, todayMillis)
            }
        }
    }
    
    // ============ COMPLETION OPERATIONS ============
    
    /**
     * Toggle completion for a checkbox-type task
     */
    suspend fun toggleTaskCompletion(taskId: Long, date: LocalDate) {
        val dateMillis = DateUtils.toEpochMillis(date)
        val existing = completionDao.getCompletion(taskId, dateMillis)
        
        if (existing == null) {
            completionDao.insert(
                TaskCompletion(
                    taskId = taskId,
                    date = dateMillis,
                    completedAt = System.currentTimeMillis()
                )
            )
        } else if (existing.completedAt != null) {
            completionDao.markIncomplete(taskId, dateMillis)
        } else {
            completionDao.markComplete(taskId, dateMillis)
        }
    }
    
    /**
     * Set a numeric value for slider or number input tasks
     * This also marks the task as completed
     */
    suspend fun setTaskNumericValue(taskId: Long, date: LocalDate, value: Float) {
        val dateMillis = DateUtils.toEpochMillis(date)
        val existing = completionDao.getCompletion(taskId, dateMillis)
        
        if (existing == null) {
            completionDao.insert(
                TaskCompletion(
                    taskId = taskId,
                    date = dateMillis,
                    completedAt = System.currentTimeMillis(),
                    numericValue = value
                )
            )
        } else {
            // updateNumericValue also sets completedAt via COALESCE if null
            completionDao.updateNumericValue(taskId, dateMillis, value)
        }
    }
    
    /**
     * Set a star rating value
     * This also marks the task as completed
     */
    suspend fun setTaskStarValue(taskId: Long, date: LocalDate, stars: Int) {
        val dateMillis = DateUtils.toEpochMillis(date)
        val existing = completionDao.getCompletion(taskId, dateMillis)
        
        if (existing == null) {
            completionDao.insert(
                TaskCompletion(
                    taskId = taskId,
                    date = dateMillis,
                    completedAt = System.currentTimeMillis(),
                    starValue = stars
                )
            )
        } else {
            // updateStarValue also sets completedAt via COALESCE if null
            completionDao.updateStarValue(taskId, dateMillis, stars)
        }
    }
    
    /**
     * Set a photo URI for photo tasks
     * This also marks the task as completed
     */
    suspend fun setTaskPhotoUri(taskId: Long, date: LocalDate, uri: String) {
        val dateMillis = DateUtils.toEpochMillis(date)
        val existing = completionDao.getCompletion(taskId, dateMillis)
        
        if (existing == null) {
            completionDao.insert(
                TaskCompletion(
                    taskId = taskId,
                    date = dateMillis,
                    completedAt = System.currentTimeMillis(),
                    photoUri = uri
                )
            )
        } else {
            // updatePhotoUri also sets completedAt via COALESCE if null
            completionDao.updatePhotoUri(taskId, dateMillis, uri)
        }
    }
    
    /**
     * Get completion for a specific task and date
     */
    suspend fun getTaskCompletion(taskId: Long, date: LocalDate): TaskCompletion? {
        val dateMillis = DateUtils.toEpochMillis(date)
        return completionDao.getCompletion(taskId, dateMillis)
    }
    
    fun observeCompletionsForDate(date: LocalDate): Flow<List<TaskCompletion>> {
        val dateMillis = DateUtils.toEpochMillis(date)
        return completionDao.observeCompletionsForDate(dateMillis)
    }
    
    fun observeCompletionsInWeek(weekStart: LocalDate): Flow<List<TaskCompletion>> {
        val startMillis = DateUtils.toEpochMillis(weekStart)
        val endMillis = DateUtils.toEpochMillis(weekStart.plusDays(6))
        return completionDao.observeCompletionsInRange(startMillis, endMillis)
    }
    
    // ============ WEEKLY SETUP ============
    
    /**
     * Ensure completion records exist for all recurring tasks for the current week.
     */
    suspend fun setupWeekCompletions(weekStart: LocalDate) {
        val recurringTasks = taskDao.getAllRecurringTasks()
        val weekDates = DateUtils.getWeekDates(weekStart)
        
        for (task in recurringTasks) {
            for (date in weekDates) {
                if (task.isScheduledFor(date)) {
                    val dateMillis = DateUtils.toEpochMillis(date)
                    val existing = completionDao.getCompletion(task.id, dateMillis)
                    if (existing == null) {
                        completionDao.insert(
                            TaskCompletion(
                                taskId = task.id,
                                date = dateMillis,
                                completedAt = null
                            )
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Add completion records for a new task for remaining days in current week
     */
    suspend fun setupTaskForCurrentWeek(task: Task) {
        if (task.scheduleType == ScheduleType.ONE_TIME) return
        
        val today = LocalDate.now()
        val weekDates = DateUtils.getWeekDatesFor(today)
        
        for (date in weekDates) {
            if (!date.isBefore(today) && task.isScheduledFor(date)) {
                val dateMillis = DateUtils.toEpochMillis(date)
                val existing = completionDao.getCompletion(task.id, dateMillis)
                if (existing == null) {
                    completionDao.insert(
                        TaskCompletion(
                            taskId = task.id,
                            date = dateMillis,
                            completedAt = null
                        )
                    )
                }
            }
        }
    }
    
    // ============ STATISTICS ============
    
    suspend fun getCompletionStatsForWeek(weekStart: LocalDate): Pair<Int, Int> {
        val startMillis = DateUtils.toEpochMillis(weekStart)
        val endMillis = DateUtils.toEpochMillis(weekStart.plusDays(6))
        val completed = completionDao.getCompletedCountInRange(startMillis, endMillis)
        val total = completionDao.getTotalCountInRange(startMillis, endMillis)
        return completed to total
    }
    
    suspend fun getPerfectDaysInWeek(weekStart: LocalDate): List<LocalDate> {
        val startMillis = DateUtils.toEpochMillis(weekStart)
        val endMillis = DateUtils.toEpochMillis(weekStart.plusDays(6))
        return completionDao.getPerfectDays(startMillis, endMillis)
            .map { DateUtils.fromEpochMillis(it) }
    }
    
    /**
     * Get wellness statistics from wellness tasks (mood, sleep, weight, steps) for a week
     */
    suspend fun getWellnessStatsForWeek(weekStart: LocalDate): WellnessStats {
        val startMillis = DateUtils.toEpochMillis(weekStart)
        val endMillis = DateUtils.toEpochMillis(weekStart.plusDays(6))
        
        // Get wellness task completions
        val moodTask = taskDao.getTaskByNameAndType("Log Mood", TaskInputType.SLIDER)
        val sleepHoursTask = taskDao.getTaskByNameAndType("Log Sleep Hours", TaskInputType.NUMBER)
        val sleepQualityTask = taskDao.getTaskByNameAndType("Sleep Quality", TaskInputType.STARS)
        val weightTask = taskDao.getTaskByNameAndType("Log Weight", TaskInputType.NUMBER)
        val stepsTask = taskDao.getTaskByNameAndType("Log Steps", TaskInputType.NUMBER)
        
        return WellnessStats(
            avgMood = moodTask?.id?.let { completionDao.getAverageNumericValue(it, startMillis, endMillis) },
            avgSleepHours = sleepHoursTask?.id?.let { completionDao.getAverageNumericValue(it, startMillis, endMillis) },
            avgSleepQuality = sleepQualityTask?.id?.let { completionDao.getAverageStarValue(it, startMillis, endMillis) },
            avgWeight = weightTask?.id?.let { completionDao.getAverageNumericValue(it, startMillis, endMillis) },
            totalSteps = stepsTask?.id?.let { completionDao.getSumNumericValue(it, startMillis, endMillis)?.toInt() } ?: 0
        )
    }
    
    // ============ PROGRESS PHOTOS ============
    
    /**
     * Get the progress photo task (task named "Progress Photo" with PHOTO input type)
     */
    suspend fun getProgressPhotoTask(): Task? {
        return taskDao.getTaskByNameAndType("Progress Photo", TaskInputType.PHOTO)
    }
    
    /**
     * Get all progress photos (completions with photo URIs for the Progress Photo task)
     */
    suspend fun getProgressPhotos(): List<TaskCompletion> {
        val photoTask = getProgressPhotoTask() ?: return emptyList()
        return completionDao.getAllPhotoCompletions(photoTask.id)
    }
    
    /**
     * Observe all progress photos
     */
    fun observeProgressPhotos(): Flow<List<TaskCompletion>> {
        return taskDao.observeTaskByNameAndType("Progress Photo", TaskInputType.PHOTO)
            .map { task ->
                task?.let { completionDao.getAllPhotoCompletions(it.id) } ?: emptyList()
            }
    }
    
    /**
     * Get the weight task
     */
    suspend fun getWeightTask(): Task? {
        return taskDao.getTaskByNameAndType("Log Weight", TaskInputType.NUMBER)
    }
    
    /**
     * Get weight for a specific date, or the closest weight if not available
     * Returns a pair of (weight value, isExactMatch)
     */
    suspend fun getWeightForDate(date: LocalDate): Pair<Float?, Boolean> {
        val weightTask = getWeightTask() ?: return null to false
        val dateMillis = DateUtils.toEpochMillis(date)
        
        // First try exact match
        val exactCompletion = completionDao.getNumericCompletionForDate(weightTask.id, dateMillis)
        if (exactCompletion?.numericValue != null) {
            return exactCompletion.numericValue to true
        }
        
        // Fall back to closest
        val closestCompletion = completionDao.getClosestNumericCompletion(weightTask.id, dateMillis)
        return closestCompletion?.numericValue to false
    }
    
    /**
     * Get weight for a date in epoch millis
     */
    suspend fun getWeightForDateMillis(dateMillis: Long): Pair<Float?, Boolean> {
        val weightTask = getWeightTask() ?: return null to false
        
        // First try exact match
        val exactCompletion = completionDao.getNumericCompletionForDate(weightTask.id, dateMillis)
        if (exactCompletion?.numericValue != null) {
            return exactCompletion.numericValue to true
        }
        
        // Fall back to closest
        val closestCompletion = completionDao.getClosestNumericCompletion(weightTask.id, dateMillis)
        return closestCompletion?.numericValue to false
    }
    
    // ============ BLOOD PRESSURE ============
    
    /**
     * Get the blood pressure task
     */
    suspend fun getBloodPressureTask(): Task? {
        return taskDao.getTaskByNameAndType("Blood Pressure", TaskInputType.BLOOD_PRESSURE)
    }
    
    /**
     * Set blood pressure data for a task
     */
    suspend fun setTaskBpData(taskId: Long, date: LocalDate, bpData: String) {
        val dateMillis = DateUtils.toEpochMillis(date)
        val existing = completionDao.getCompletion(taskId, dateMillis)
        
        if (existing == null) {
            completionDao.insert(
                TaskCompletion(
                    taskId = taskId,
                    date = dateMillis,
                    completedAt = System.currentTimeMillis(),
                    bpData = bpData
                )
            )
        } else {
            completionDao.updateBpData(taskId, dateMillis, bpData)
        }
    }
    
    /**
     * Get all blood pressure entries
     */
    suspend fun getBloodPressureEntries(): List<TaskCompletion> {
        val bpTask = getBloodPressureTask() ?: return emptyList()
        return completionDao.getAllBpEntries(bpTask.id)
    }
    
    /**
     * Get blood pressure entries in a date range
     */
    suspend fun getBloodPressureEntriesInRange(startDate: LocalDate, endDate: LocalDate): List<TaskCompletion> {
        val bpTask = getBloodPressureTask() ?: return emptyList()
        val startMillis = DateUtils.toEpochMillis(startDate)
        val endMillis = DateUtils.toEpochMillis(endDate)
        return completionDao.getBpEntriesInRange(bpTask.id, startMillis, endMillis)
    }
}
