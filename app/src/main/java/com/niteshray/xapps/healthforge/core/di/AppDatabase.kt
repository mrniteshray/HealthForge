package com.niteshray.xapps.healthforge.core.di

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.niteshray.xapps.healthforge.feature.home.data.models.Task
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskTemplate
import com.niteshray.xapps.healthforge.feature.home.data.models.DailyTaskRecord

@Database(
    entities = [
        Task::class, // Keep for migration compatibility
        TaskTemplate::class,
        DailyTaskRecord::class
    ], 
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun TaskDao(): TasksDAO
    abstract fun TaskTemplateDao(): TaskTemplateDao
    abstract fun DailyTaskRecordDao(): DailyTaskRecordDao
    abstract fun TaskTrackingDao(): TaskTrackingDao
    
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create TaskTemplate table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS task_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        timeBlock TEXT NOT NULL,
                        time TEXT NOT NULL,
                        category TEXT NOT NULL,
                        priority TEXT NOT NULL DEFAULT 'MEDIUM',
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        firestoreId TEXT
                    )
                """)
                
                // Create DailyTaskRecord table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_task_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        templateId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER,
                        firestoreId TEXT,
                        FOREIGN KEY(templateId) REFERENCES task_templates(id) ON DELETE CASCADE
                    )
                """)
                
                // Create indices for DailyTaskRecord
                database.execSQL("CREATE INDEX IF NOT EXISTS index_daily_task_records_templateId ON daily_task_records(templateId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_daily_task_records_date ON daily_task_records(date)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_daily_task_records_templateId_date ON daily_task_records(templateId, date)")
                
                // Migrate existing tasks to TaskTemplate
                database.execSQL("""
                    INSERT INTO task_templates (title, description, timeBlock, time, category, priority, isActive, createdAt)
                    SELECT title, description, timeBlock, time, category, priority, 1, ${System.currentTimeMillis()}
                    FROM Tasks
                """)
            }
        }
    }
}