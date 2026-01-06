package com.trackfiercely.data.database

import androidx.room.*
import com.trackfiercely.data.model.Category
import com.trackfiercely.data.model.Task
import com.trackfiercely.data.model.TaskInputType
import com.trackfiercely.data.model.TaskWithCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    
    // ============ TASK CRUD ============
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long
    
    @Update
    suspend fun update(task: Task)
    
    @Delete
    suspend fun delete(task: Task)
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)
    
    // ============ BASIC TASK QUERIES ============
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskById(taskId: Long): Flow<Task?>
    
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<Task>
    
    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int
    
    @Query("SELECT * FROM tasks WHERE name = :name AND inputType = :inputType LIMIT 1")
    suspend fun getTaskByNameAndType(name: String, inputType: TaskInputType): Task?
    
    @Query("SELECT * FROM tasks WHERE name = :name AND inputType = :inputType LIMIT 1")
    fun observeTaskByNameAndType(name: String, inputType: TaskInputType): Flow<Task?>
    
    // ============ ACTIVE TASK QUERIES ============
    
    @Query("SELECT * FROM tasks WHERE isActive = 1 ORDER BY category, name")
    fun observeAllActiveTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE isActive = 1 AND isHidden = 0 ORDER BY category, name")
    fun observeVisibleTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE isActive = 1 AND isHidden = 1 ORDER BY category, name")
    fun observeHiddenTasks(): Flow<List<Task>>
    
    // ============ SCHEDULE TYPE QUERIES ============
    
    @Query("SELECT * FROM tasks WHERE isActive = 1 AND scheduleType != 'ONE_TIME' ORDER BY category, name")
    fun observeRecurringTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE isActive = 1 AND scheduleType = 'ONE_TIME' ORDER BY createdAt DESC")
    fun observeOneTimeTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE isActive = 1 AND scheduleType != 'ONE_TIME'")
    suspend fun getAllRecurringTasks(): List<Task>
    
    // ============ ASSIGNED/UNASSIGNED QUERIES ============
    
    /**
     * Tasks with at least one day assigned or a scheduled date
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE isActive = 1 AND isHidden = 0
        AND (
            assignedDays > 0 
            OR scheduledDate IS NOT NULL
            OR monthlyDay IS NOT NULL
            OR monthlyWeek IS NOT NULL
            OR yearlyMonth IS NOT NULL
        )
        ORDER BY category, name
    """)
    fun observeAssignedTasks(): Flow<List<Task>>
    
    /**
     * Tasks with no days assigned and no scheduled date
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE isActive = 1 AND isHidden = 0
        AND assignedDays = 0 
        AND scheduledDate IS NULL
        AND monthlyDay IS NULL
        AND monthlyWeek IS NULL
        AND yearlyMonth IS NULL
        ORDER BY category, name
    """)
    fun observeUnassignedTasks(): Flow<List<Task>>
    
    // ============ CATEGORY AND INPUT TYPE QUERIES ============
    
    @Query("SELECT * FROM tasks WHERE isActive = 1 AND category = :category ORDER BY name")
    fun observeTasksByCategory(category: Category): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE isActive = 1 AND inputType = :inputType ORDER BY category, name")
    fun observeTasksByInputType(inputType: TaskInputType): Flow<List<Task>>
    
    // ============ TASKS FOR SPECIFIC DAY ============
    
    /**
     * Get tasks assigned to a specific day.
     * For weekly tasks: uses bitwise AND to check the assignedDays bitmask.
     * For one-time tasks: checks if scheduledDate matches the given date.
     * Note: Monthly and yearly tasks are filtered in the repository layer.
     * 
     * @param dayBit The bitmask for the day (e.g., 1 for Monday, 2 for Tuesday)
     * @param dateMillis The date as epoch millis at midnight (for one-time tasks)
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE isActive = 1 
        AND (
            (scheduleType = 'WEEKLY' AND (assignedDays & :dayBit) != 0)
            OR (scheduleType = 'ONE_TIME' AND scheduledDate = :dateMillis)
            OR scheduleType = 'MONTHLY'
            OR scheduleType = 'YEARLY'
        )
        ORDER BY 
            CASE WHEN scheduledHour IS NULL THEN 0 ELSE 1 END,
            scheduledHour ASC,
            name ASC
    """)
    fun observeTasksForDay(dayBit: Int, dateMillis: Long): Flow<List<Task>>
    
    // ============ TASK WITH COMPLETIONS ============
    
    @Transaction
    @Query("SELECT * FROM tasks WHERE isActive = 1 ORDER BY category, name")
    fun observeTasksWithCompletions(): Flow<List<TaskWithCompletion>>
    
    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE isActive = 1 
        AND (
            (scheduleType = 'WEEKLY' AND (assignedDays & :dayBit) != 0)
            OR (scheduleType = 'ONE_TIME' AND scheduledDate = :dateMillis)
            OR scheduleType = 'MONTHLY'
            OR scheduleType = 'YEARLY'
        )
        ORDER BY 
            CASE WHEN scheduledHour IS NULL THEN 0 ELSE 1 END,
            scheduledHour ASC,
            name ASC
    """)
    fun observeTasksWithCompletionsForDay(dayBit: Int, dateMillis: Long): Flow<List<TaskWithCompletion>>
    
    // ============ ONE-TIME TASK QUERIES ============
    
    /**
     * Get one-time tasks that need to be rolled over.
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE isActive = 1 
        AND scheduleType = 'ONE_TIME' 
        AND autoRollover = 1 
        AND scheduledDate IS NOT NULL 
        AND scheduledDate < :todayMillis
    """)
    suspend fun getTasksNeedingRollover(todayMillis: Long): List<Task>
    
    /**
     * Update the scheduled date for a task (used for rollover)
     */
    @Query("UPDATE tasks SET scheduledDate = :newDate WHERE id = :taskId")
    suspend fun updateScheduledDate(taskId: Long, newDate: Long)
    
    // ============ VISIBILITY ============
    
    @Query("UPDATE tasks SET isHidden = :isHidden WHERE id = :taskId")
    suspend fun setTaskHidden(taskId: Long, isHidden: Boolean)
    
    @Query("UPDATE tasks SET isHidden = NOT isHidden WHERE id = :taskId")
    suspend fun toggleTaskHidden(taskId: Long)
    
    // ============ ARCHIVE/DEACTIVATE ============
    
    @Query("UPDATE tasks SET isActive = 0 WHERE id = :taskId")
    suspend fun archiveTask(taskId: Long)
    
    @Query("UPDATE tasks SET isActive = 1 WHERE id = :taskId")
    suspend fun restoreTask(taskId: Long)
    
    /**
     * Archive one-time tasks that were assigned before a certain date
     */
    @Query("""
        UPDATE tasks SET isActive = 0 
        WHERE scheduleType = 'ONE_TIME' AND createdAt < :beforeDate
    """)
    suspend fun archiveOldOneTimeTasks(beforeDate: Long)
}
