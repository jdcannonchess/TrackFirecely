package com.trackfiercely.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trackfiercely.data.model.Task
import com.trackfiercely.data.model.TaskCompletion
import com.trackfiercely.data.model.WeeklySnapshot

@Database(
    entities = [
        Task::class,
        TaskCompletion::class,
        WeeklySnapshot::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TrackFiercelyDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun taskCompletionDao(): TaskCompletionDao
    abstract fun weeklySnapshotDao(): WeeklySnapshotDao
    
    companion object {
        @Volatile
        private var INSTANCE: TrackFiercelyDatabase? = null
        
        // Migration from version 3 to 4: Add scheduledHour column
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN scheduledHour INTEGER DEFAULT NULL")
            }
        }
        
        // Migration from version 4 to 5: Add bpData column for blood pressure
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE task_completions ADD COLUMN bpData TEXT DEFAULT NULL")
            }
        }
        
        fun getDatabase(context: Context): TrackFiercelyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackFiercelyDatabase::class.java,
                    "track_fiercely_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    // Destructive migration for major schema changes
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
