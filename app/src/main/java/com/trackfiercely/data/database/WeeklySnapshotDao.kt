package com.trackfiercely.data.database

import androidx.room.*
import com.trackfiercely.data.model.WeeklySnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklySnapshotDao {
    
    // ============ SNAPSHOT CRUD ============
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: WeeklySnapshot): Long
    
    @Update
    suspend fun update(snapshot: WeeklySnapshot)
    
    @Delete
    suspend fun delete(snapshot: WeeklySnapshot)
    
    @Query("DELETE FROM weekly_snapshots WHERE id = :snapshotId")
    suspend fun deleteById(snapshotId: Long)
    
    // ============ QUERIES ============
    
    @Query("SELECT * FROM weekly_snapshots WHERE id = :snapshotId")
    suspend fun getSnapshotById(snapshotId: Long): WeeklySnapshot?
    
    @Query("SELECT * FROM weekly_snapshots WHERE weekStartDate = :weekStartDate LIMIT 1")
    suspend fun getSnapshotForWeek(weekStartDate: Long): WeeklySnapshot?
    
    @Query("SELECT * FROM weekly_snapshots WHERE weekStartDate = :weekStartDate")
    fun observeSnapshotForWeek(weekStartDate: Long): Flow<WeeklySnapshot?>
    
    @Query("SELECT * FROM weekly_snapshots ORDER BY weekStartDate DESC")
    fun observeAllSnapshots(): Flow<List<WeeklySnapshot>>
    
    @Query("SELECT * FROM weekly_snapshots ORDER BY weekStartDate DESC LIMIT :limit")
    fun observeRecentSnapshots(limit: Int): Flow<List<WeeklySnapshot>>
    
    @Query("SELECT * FROM weekly_snapshots ORDER BY weekStartDate DESC LIMIT :limit")
    suspend fun getRecentSnapshots(limit: Int): List<WeeklySnapshot>
    
    // ============ CHECK EXISTS ============
    
    @Query("SELECT EXISTS(SELECT 1 FROM weekly_snapshots WHERE weekStartDate = :weekStartDate)")
    suspend fun snapshotExists(weekStartDate: Long): Boolean
    
    // ============ STATISTICS ============
    
    @Query("SELECT AVG(tasksCompleted * 1.0 / totalTasks) FROM weekly_snapshots WHERE totalTasks > 0")
    suspend fun getAverageCompletionRate(): Float?
    
    @Query("SELECT MAX(tasksCompleted * 1.0 / totalTasks) FROM weekly_snapshots WHERE totalTasks > 0")
    suspend fun getBestCompletionRate(): Float?
    
    @Query("SELECT COUNT(*) FROM weekly_snapshots")
    suspend fun getTotalWeeksTracked(): Int
    
    // ============ CLEANUP ============
    
    @Query("DELETE FROM weekly_snapshots WHERE weekStartDate < :beforeDate")
    suspend fun deleteOldSnapshots(beforeDate: Long)
}

