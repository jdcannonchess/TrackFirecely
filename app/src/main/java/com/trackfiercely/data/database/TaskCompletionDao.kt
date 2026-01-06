package com.trackfiercely.data.database

import androidx.room.*
import com.trackfiercely.data.model.TaskCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCompletionDao {
    
    // ============ COMPLETION CRUD ============
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completion: TaskCompletion): Long
    
    @Update
    suspend fun update(completion: TaskCompletion)
    
    @Delete
    suspend fun delete(completion: TaskCompletion)
    
    @Query("DELETE FROM task_completions WHERE id = :completionId")
    suspend fun deleteById(completionId: Long)
    
    // ============ QUERIES ============
    
    @Query("SELECT * FROM task_completions WHERE taskId = :taskId AND date = :date LIMIT 1")
    suspend fun getCompletion(taskId: Long, date: Long): TaskCompletion?
    
    @Query("SELECT * FROM task_completions WHERE taskId = :taskId AND date = :date")
    fun observeCompletion(taskId: Long, date: Long): Flow<TaskCompletion?>
    
    @Query("SELECT * FROM task_completions WHERE date = :date")
    fun observeCompletionsForDate(date: Long): Flow<List<TaskCompletion>>
    
    @Query("SELECT * FROM task_completions WHERE date >= :startDate AND date <= :endDate")
    fun observeCompletionsInRange(startDate: Long, endDate: Long): Flow<List<TaskCompletion>>
    
    @Query("SELECT * FROM task_completions WHERE taskId = :taskId ORDER BY date DESC")
    fun observeCompletionsForTask(taskId: Long): Flow<List<TaskCompletion>>
    
    // ============ TOGGLE COMPLETION ============
    
    /**
     * Mark a task as complete for a date
     */
    @Query("""
        INSERT OR REPLACE INTO task_completions (taskId, date, completedAt) 
        VALUES (:taskId, :date, :completedAt)
    """)
    suspend fun markComplete(taskId: Long, date: Long, completedAt: Long = System.currentTimeMillis())
    
    /**
     * Mark a task as incomplete (remove completion timestamp)
     */
    @Query("UPDATE task_completions SET completedAt = NULL WHERE taskId = :taskId AND date = :date")
    suspend fun markIncomplete(taskId: Long, date: Long)
    
    // ============ VALUE UPDATES ============
    
    /**
     * Update numeric value for a completion (used for slider and number inputs)
     */
    @Query("""
        UPDATE task_completions 
        SET numericValue = :value, completedAt = COALESCE(completedAt, :now)
        WHERE taskId = :taskId AND date = :date
    """)
    suspend fun updateNumericValue(taskId: Long, date: Long, value: Float, now: Long = System.currentTimeMillis())
    
    /**
     * Update star rating value for a completion
     */
    @Query("""
        UPDATE task_completions 
        SET starValue = :stars, completedAt = COALESCE(completedAt, :now)
        WHERE taskId = :taskId AND date = :date
    """)
    suspend fun updateStarValue(taskId: Long, date: Long, stars: Int, now: Long = System.currentTimeMillis())
    
    /**
     * Update photo URI for a completion
     */
    @Query("""
        UPDATE task_completions 
        SET photoUri = :uri, completedAt = COALESCE(completedAt, :now)
        WHERE taskId = :taskId AND date = :date
    """)
    suspend fun updatePhotoUri(taskId: Long, date: Long, uri: String, now: Long = System.currentTimeMillis())
    
    // ============ STATISTICS ============
    
    @Query("""
        SELECT COUNT(*) FROM task_completions 
        WHERE date >= :startDate AND date <= :endDate AND completedAt IS NOT NULL
    """)
    suspend fun getCompletedCountInRange(startDate: Long, endDate: Long): Int
    
    @Query("""
        SELECT COUNT(*) FROM task_completions 
        WHERE date >= :startDate AND date <= :endDate
    """)
    suspend fun getTotalCountInRange(startDate: Long, endDate: Long): Int
    
    @Query("""
        SELECT COUNT(*) FROM task_completions 
        WHERE taskId = :taskId AND completedAt IS NOT NULL
    """)
    suspend fun getCompletedCountForTask(taskId: Long): Int
    
    /**
     * Get dates where all tasks were completed (perfect days)
     */
    @Query("""
        SELECT date FROM task_completions
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY date
        HAVING COUNT(*) = SUM(CASE WHEN completedAt IS NOT NULL THEN 1 ELSE 0 END)
    """)
    suspend fun getPerfectDays(startDate: Long, endDate: Long): List<Long>
    
    // ============ WELLNESS STATISTICS ============
    
    /**
     * Get average numeric value for a task over a date range
     */
    @Query("""
        SELECT AVG(numericValue) FROM task_completions
        WHERE taskId = :taskId 
        AND date >= :startDate 
        AND date <= :endDate 
        AND numericValue IS NOT NULL
    """)
    suspend fun getAverageNumericValue(taskId: Long, startDate: Long, endDate: Long): Float?
    
    /**
     * Get average star value for a task over a date range
     */
    @Query("""
        SELECT AVG(CAST(starValue AS REAL)) FROM task_completions
        WHERE taskId = :taskId 
        AND date >= :startDate 
        AND date <= :endDate 
        AND starValue IS NOT NULL
    """)
    suspend fun getAverageStarValue(taskId: Long, startDate: Long, endDate: Long): Float?
    
    /**
     * Get sum of numeric values for a task over a date range (e.g., total steps)
     */
    @Query("""
        SELECT SUM(numericValue) FROM task_completions
        WHERE taskId = :taskId 
        AND date >= :startDate 
        AND date <= :endDate 
        AND numericValue IS NOT NULL
    """)
    suspend fun getSumNumericValue(taskId: Long, startDate: Long, endDate: Long): Float?
    
    // ============ PROGRESS PHOTOS ============
    
    /**
     * Get all completions with photos for a task, ordered by date descending (newest first)
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId AND photoUri IS NOT NULL AND photoUri != ''
        ORDER BY date DESC
    """)
    suspend fun getAllPhotoCompletions(taskId: Long): List<TaskCompletion>
    
    /**
     * Get all completions with photos for a task, ordered by date ascending (oldest first)
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId AND photoUri IS NOT NULL AND photoUri != ''
        ORDER BY date ASC
    """)
    suspend fun getAllPhotoCompletionsAscending(taskId: Long): List<TaskCompletion>
    
    /**
     * Observe all completions with photos for a task
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId AND photoUri IS NOT NULL AND photoUri != ''
        ORDER BY date DESC
    """)
    fun observeAllPhotoCompletions(taskId: Long): Flow<List<TaskCompletion>>
    
    /**
     * Get the closest numeric value completion to a given date (for weight lookup)
     * Returns the completion with numeric value that has the smallest date difference
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId AND numericValue IS NOT NULL
        ORDER BY ABS(date - :targetDate) ASC
        LIMIT 1
    """)
    suspend fun getClosestNumericCompletion(taskId: Long, targetDate: Long): TaskCompletion?
    
    /**
     * Get numeric value completion for exact date
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId AND date = :date AND numericValue IS NOT NULL
        LIMIT 1
    """)
    suspend fun getNumericCompletionForDate(taskId: Long, date: Long): TaskCompletion?
    
    // ============ WEIGHT TRACKER ============
    
    /**
     * Get all weight entries for a task, ordered by date descending
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId AND numericValue IS NOT NULL
        ORDER BY date DESC
    """)
    suspend fun getAllWeightEntries(taskId: Long): List<TaskCompletion>
    
    /**
     * Observe all weight entries for a task
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId AND numericValue IS NOT NULL
        ORDER BY date DESC
    """)
    fun observeAllWeightEntries(taskId: Long): Flow<List<TaskCompletion>>
    
    /**
     * Get weight entries in a date range for graphing
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId 
        AND numericValue IS NOT NULL
        AND date >= :startDate 
        AND date <= :endDate
        ORDER BY date ASC
    """)
    suspend fun getWeightEntriesInRange(taskId: Long, startDate: Long, endDate: Long): List<TaskCompletion>
    
    /**
     * Get min weight in a date range
     */
    @Query("""
        SELECT MIN(numericValue) FROM task_completions 
        WHERE taskId = :taskId 
        AND numericValue IS NOT NULL
        AND date >= :startDate 
        AND date <= :endDate
    """)
    suspend fun getMinWeightInRange(taskId: Long, startDate: Long, endDate: Long): Float?
    
    /**
     * Get max weight in a date range
     */
    @Query("""
        SELECT MAX(numericValue) FROM task_completions 
        WHERE taskId = :taskId 
        AND numericValue IS NOT NULL
        AND date >= :startDate 
        AND date <= :endDate
    """)
    suspend fun getMaxWeightInRange(taskId: Long, startDate: Long, endDate: Long): Float?
    
    /**
     * Get total count of weight entries
     */
    @Query("""
        SELECT COUNT(*) FROM task_completions 
        WHERE taskId = :taskId AND numericValue IS NOT NULL
    """)
    suspend fun getWeightEntryCount(taskId: Long): Int
    
    /**
     * Get the first (oldest) weight entry
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId AND numericValue IS NOT NULL
        ORDER BY date ASC
        LIMIT 1
    """)
    suspend fun getFirstWeightEntry(taskId: Long): TaskCompletion?
    
    /**
     * Get the latest (most recent) weight entry
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId AND numericValue IS NOT NULL
        ORDER BY date DESC
        LIMIT 1
    """)
    suspend fun getLatestWeightEntry(taskId: Long): TaskCompletion?
    
    // ============ BLOOD PRESSURE ============
    
    /**
     * Update blood pressure data for a completion
     */
    @Query("""
        UPDATE task_completions 
        SET bpData = :bpData, completedAt = COALESCE(completedAt, :now)
        WHERE taskId = :taskId AND date = :date
    """)
    suspend fun updateBpData(taskId: Long, date: Long, bpData: String, now: Long = System.currentTimeMillis())
    
    /**
     * Get all BP entries for a task, ordered by date descending
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId AND bpData IS NOT NULL
        ORDER BY date DESC
    """)
    suspend fun getAllBpEntries(taskId: Long): List<TaskCompletion>
    
    /**
     * Observe all BP entries for a task
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId AND bpData IS NOT NULL
        ORDER BY date DESC
    """)
    fun observeAllBpEntries(taskId: Long): Flow<List<TaskCompletion>>
    
    /**
     * Get BP entries in a date range for graphing
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId 
        AND bpData IS NOT NULL
        AND date >= :startDate 
        AND date <= :endDate
        ORDER BY date ASC
    """)
    suspend fun getBpEntriesInRange(taskId: Long, startDate: Long, endDate: Long): List<TaskCompletion>
    
    /**
     * Get the latest BP entry
     */
    @Query("""
        SELECT * FROM task_completions 
        WHERE taskId = :taskId AND bpData IS NOT NULL
        ORDER BY date DESC
        LIMIT 1
    """)
    suspend fun getLatestBpEntry(taskId: Long): TaskCompletion?
    
    // ============ CLEANUP ============
    
    @Query("DELETE FROM task_completions WHERE date < :beforeDate")
    suspend fun deleteOldCompletions(beforeDate: Long)
}
