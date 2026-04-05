package com.niteshray.xapps.healthforge.core.di

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskTemplate
import com.niteshray.xapps.healthforge.feature.home.data.models.DailyTaskRecord
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskWithDailyRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskTrackingDao {
    
    /**
     * Get all active tasks for today with their completion status
     */
    @Transaction
    @Query("""
        SELECT tt.*, 
               dr.id as dailyRecord_id,
               dr.templateId as dailyRecord_templateId,
               dr.date as dailyRecord_date,
               dr.isCompleted as dailyRecord_isCompleted,
               dr.completedAt as dailyRecord_completedAt,
               dr.firestoreId as dailyRecord_firestoreId
        FROM task_templates tt
        LEFT JOIN daily_task_records dr ON tt.id = dr.templateId AND dr.date = :date
        WHERE tt.isActive = 1
        ORDER BY tt.timeBlock, tt.time
    """)
    fun getTasksForDate(date: String): Flow<List<TaskWithDailyRecord>>
    
    /**
     * Get tasks by category for a specific date
     */
    @Transaction
    @Query("""
        SELECT tt.*, 
               dr.id as dailyRecord_id,
               dr.templateId as dailyRecord_templateId,
               dr.date as dailyRecord_date,
               dr.isCompleted as dailyRecord_isCompleted,
               dr.completedAt as dailyRecord_completedAt,
               dr.firestoreId as dailyRecord_firestoreId
        FROM task_templates tt
        LEFT JOIN daily_task_records dr ON tt.id = dr.templateId AND dr.date = :date
        WHERE tt.isActive = 1 AND tt.category = :category
        ORDER BY tt.time
    """)
    fun getTasksByCategoryForDate(category: String, date: String): Flow<List<TaskWithDailyRecord>>
    
    /**
     * Get tasks by time block for a specific date
     */
    @Transaction
    @Query("""
        SELECT tt.*, 
               dr.id as dailyRecord_id,
               dr.templateId as dailyRecord_templateId,
               dr.date as dailyRecord_date,
               dr.isCompleted as dailyRecord_isCompleted,
               dr.completedAt as dailyRecord_completedAt,
               dr.firestoreId as dailyRecord_firestoreId
        FROM task_templates tt
        LEFT JOIN daily_task_records dr ON tt.id = dr.templateId AND dr.date = :date
        WHERE tt.isActive = 1 AND tt.timeBlock = :timeBlock
        ORDER BY tt.time
    """)
    fun getTasksByTimeBlockForDate(timeBlock: String, date: String): Flow<List<TaskWithDailyRecord>>
    
    /**
     * Get tasks by priority for a specific date
     */
    @Transaction
    @Query("""
        SELECT tt.*, 
               dr.id as dailyRecord_id,
               dr.templateId as dailyRecord_templateId,
               dr.date as dailyRecord_date,
               dr.isCompleted as dailyRecord_isCompleted,
               dr.completedAt as dailyRecord_completedAt,
               dr.firestoreId as dailyRecord_firestoreId
        FROM task_templates tt
        LEFT JOIN daily_task_records dr ON tt.id = dr.templateId AND dr.date = :date
        WHERE tt.isActive = 1 AND tt.priority = :priority
        ORDER BY tt.time
    """)
    fun getTasksByPriorityForDate(priority: String, date: String): Flow<List<TaskWithDailyRecord>>
    
    /**
     * Get completed/pending tasks for a specific date
     */
    @Transaction
    @Query("""
        SELECT tt.*, 
               dr.id as dailyRecord_id,
               dr.templateId as dailyRecord_templateId,
               dr.date as dailyRecord_date,
               dr.isCompleted as dailyRecord_isCompleted,
               dr.completedAt as dailyRecord_completedAt,
               dr.firestoreId as dailyRecord_firestoreId
        FROM task_templates tt
        INNER JOIN daily_task_records dr ON tt.id = dr.templateId AND dr.date = :date
        WHERE tt.isActive = 1 AND dr.isCompleted = :isCompleted
        ORDER BY tt.time
    """)
    fun getTasksByCompletionStatusForDate(isCompleted: Boolean, date: String): Flow<List<TaskWithDailyRecord>>
    
    /**
     * Create daily task records for all active templates for a specific date
     */
    @Query("""
        INSERT INTO daily_task_records (templateId, date, isCompleted)
        SELECT id, :date, 0
        FROM task_templates 
        WHERE isActive = 1 
        AND id NOT IN (
            SELECT templateId 
            FROM daily_task_records 
            WHERE date = :date
        )
    """)
    suspend fun createDailyRecordsForDate(date: String)
    
    /**
     * Reset all task completions for a specific date
     */
    @Query("UPDATE daily_task_records SET isCompleted = 0, completedAt = NULL WHERE date = :date")
    suspend fun resetTasksForDate(date: String)
    
    /**
     * Get analytics data for a specific template
     */
    @Query("""
        SELECT dr.date, dr.isCompleted, dr.completedAt, tt.createdAt
        FROM daily_task_records dr
        INNER JOIN task_templates tt ON dr.templateId = tt.id
        WHERE dr.templateId = :templateId
        AND dr.date >= date(tt.createdAt / 1000, 'unixepoch')
        ORDER BY dr.date
    """)
    suspend fun getAnalyticsDataForTemplate(templateId: Int): List<AnalyticsRecord>
    
    data class AnalyticsRecord(
        val date: String,
        val isCompleted: Boolean,
        val completedAt: Long?,
        val createdAt: Long
    )
}