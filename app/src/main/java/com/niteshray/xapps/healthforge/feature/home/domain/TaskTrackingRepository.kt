package com.niteshray.xapps.healthforge.feature.home.domain

import com.niteshray.xapps.healthforge.feature.home.data.models.TaskTemplate
import com.niteshray.xapps.healthforge.feature.home.data.models.DailyTaskRecord
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskWithDailyRecord
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TaskCategory
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TimeBlock
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Priority
import com.niteshray.xapps.healthforge.core.di.TaskTrackingDao
import kotlinx.coroutines.flow.Flow

interface TaskTrackingRepository {
    
    // TaskTemplate operations
    fun getAllActiveTemplates(): Flow<List<TaskTemplate>>
    fun getAllTemplates(): Flow<List<TaskTemplate>>
    suspend fun getTemplateById(templateId: Int): TaskTemplate?
    fun getTemplatesByCategory(category: TaskCategory): Flow<List<TaskTemplate>>
    fun getTemplatesByTimeBlock(timeBlock: TimeBlock): Flow<List<TaskTemplate>>
    fun getTemplatesByPriority(priority: Priority): Flow<List<TaskTemplate>>
    suspend fun insertTemplate(template: TaskTemplate): Long
    suspend fun updateTemplate(template: TaskTemplate)
    suspend fun deleteTemplate(template: TaskTemplate)
    suspend fun deleteTemplateById(templateId: Int)
    suspend fun deactivateTemplate(templateId: Int)
    suspend fun activateTemplate(templateId: Int)
    suspend fun getActiveTemplateCount(): Int
    
    // DailyTaskRecord operations
    fun getRecordsByDate(date: String): Flow<List<DailyTaskRecord>>
    fun getRecordsByTemplate(templateId: Int): Flow<List<DailyTaskRecord>>
    suspend fun getRecordByTemplateAndDate(templateId: Int, date: String): DailyTaskRecord?
    suspend fun getRecordsByTemplateInDateRange(templateId: Int, startDate: String, endDate: String): List<DailyTaskRecord>
    suspend fun insertRecord(record: DailyTaskRecord): Long
    suspend fun updateRecord(record: DailyTaskRecord)
    suspend fun deleteRecord(record: DailyTaskRecord)
    suspend fun updateRecordCompletion(templateId: Int, date: String, isCompleted: Boolean, completedAt: Long?)
    suspend fun getRecordCountByDate(date: String): Int
    suspend fun getCompletedRecordCountByDate(date: String): Int
    suspend fun getPendingRecordCountByDate(date: String): Int
    
    // Combined operations for UI
    fun getTasksForDate(date: String): Flow<List<TaskWithDailyRecord>>
    fun getTasksByCategoryForDate(category: TaskCategory, date: String): Flow<List<TaskWithDailyRecord>>
    fun getTasksByTimeBlockForDate(timeBlock: TimeBlock, date: String): Flow<List<TaskWithDailyRecord>>
    fun getTasksByPriorityForDate(priority: Priority, date: String): Flow<List<TaskWithDailyRecord>>
    fun getTasksByCompletionStatusForDate(isCompleted: Boolean, date: String): Flow<List<TaskWithDailyRecord>>
    
    // Daily operations
    suspend fun createDailyRecordsForDate(date: String)
    suspend fun resetTasksForDate(date: String)
    
    // Analytics
    suspend fun getAnalyticsDataForTemplate(templateId: Int): List<TaskTrackingDao.AnalyticsRecord>
    suspend fun getRecordsFromTemplateCreation(templateId: Int): List<DailyTaskRecord>
    
    // Utility methods
    suspend fun ensureTodayRecordsExist()
    suspend fun completeTask(templateId: Int, date: String = DailyTaskRecord.getTodayDateString())
    suspend fun uncompleteTask(templateId: Int, date: String = DailyTaskRecord.getTodayDateString())
}