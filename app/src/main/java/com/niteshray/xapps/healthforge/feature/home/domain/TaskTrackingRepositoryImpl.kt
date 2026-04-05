package com.niteshray.xapps.healthforge.feature.home.domain

import android.util.Log
import com.niteshray.xapps.healthforge.core.di.TaskTemplateDao
import com.niteshray.xapps.healthforge.core.di.DailyTaskRecordDao
import com.niteshray.xapps.healthforge.core.di.TaskTrackingDao
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskTemplate
import com.niteshray.xapps.healthforge.feature.home.data.models.DailyTaskRecord
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskWithDailyRecord
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TaskCategory
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TimeBlock
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Priority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskTrackingRepositoryImpl @Inject constructor(
    private val taskTemplateDao: TaskTemplateDao,
    private val dailyTaskRecordDao: DailyTaskRecordDao,
    private val taskTrackingDao: TaskTrackingDao
) : TaskTrackingRepository {

    companion object {
        private const val TAG = "TaskTrackingRepository"
    }

    // TaskTemplate operations
    override fun getAllActiveTemplates(): Flow<List<TaskTemplate>> {
        return taskTemplateDao.getAllActiveTemplates()
    }

    override fun getAllTemplates(): Flow<List<TaskTemplate>> {
        return taskTemplateDao.getAllTemplates()
    }

    override suspend fun getTemplateById(templateId: Int): TaskTemplate? {
        return taskTemplateDao.getTemplateById(templateId)
    }

    override fun getTemplatesByCategory(category: TaskCategory): Flow<List<TaskTemplate>> {
        return taskTemplateDao.getTemplatesByCategory(category.name)
    }

    override fun getTemplatesByTimeBlock(timeBlock: TimeBlock): Flow<List<TaskTemplate>> {
        return taskTemplateDao.getTemplatesByTimeBlock(timeBlock.name)
    }

    override fun getTemplatesByPriority(priority: Priority): Flow<List<TaskTemplate>> {
        return taskTemplateDao.getTemplatesByPriority(priority.name)
    }

    override suspend fun insertTemplate(template: TaskTemplate): Long {
        Log.d(TAG, "Inserting new task template: ${template.title}")
        return taskTemplateDao.insertTemplate(template)
    }

    override suspend fun updateTemplate(template: TaskTemplate) {
        Log.d(TAG, "Updating task template: ${template.id} - ${template.title}")
        taskTemplateDao.updateTemplate(template)
    }

    override suspend fun deleteTemplate(template: TaskTemplate) {
        Log.d(TAG, "Deleting task template: ${template.id} - ${template.title}")
        taskTemplateDao.deleteTemplate(template)
    }

    override suspend fun deleteTemplateById(templateId: Int) {
        Log.d(TAG, "Deleting task template by ID: $templateId")
        taskTemplateDao.deleteTemplateById(templateId)
    }

    override suspend fun deactivateTemplate(templateId: Int) {
        Log.d(TAG, "Deactivating task template: $templateId")
        taskTemplateDao.deactivateTemplate(templateId)
    }

    override suspend fun activateTemplate(templateId: Int) {
        Log.d(TAG, "Activating task template: $templateId")
        taskTemplateDao.activateTemplate(templateId)
    }

    override suspend fun getActiveTemplateCount(): Int {
        return taskTemplateDao.getActiveTemplateCount()
    }

    // DailyTaskRecord operations
    override fun getRecordsByDate(date: String): Flow<List<DailyTaskRecord>> {
        return dailyTaskRecordDao.getRecordsByDate(date)
    }

    override fun getRecordsByTemplate(templateId: Int): Flow<List<DailyTaskRecord>> {
        return dailyTaskRecordDao.getRecordsByTemplate(templateId)
    }

    override suspend fun getRecordByTemplateAndDate(templateId: Int, date: String): DailyTaskRecord? {
        return dailyTaskRecordDao.getRecordByTemplateAndDate(templateId, date)
    }

    override suspend fun getRecordsByTemplateInDateRange(templateId: Int, startDate: String, endDate: String): List<DailyTaskRecord> {
        return dailyTaskRecordDao.getRecordsByTemplateInDateRange(templateId, startDate, endDate)
    }

    override suspend fun insertRecord(record: DailyTaskRecord): Long {
        Log.d(TAG, "Inserting daily task record for template ${record.templateId} on ${record.date}")
        return dailyTaskRecordDao.insertRecord(record)
    }

    override suspend fun updateRecord(record: DailyTaskRecord) {
        Log.d(TAG, "Updating daily task record: ${record.id}")
        dailyTaskRecordDao.updateRecord(record)
    }

    override suspend fun deleteRecord(record: DailyTaskRecord) {
        Log.d(TAG, "Deleting daily task record: ${record.id}")
        dailyTaskRecordDao.deleteRecord(record)
    }

    override suspend fun updateRecordCompletion(templateId: Int, date: String, isCompleted: Boolean, completedAt: Long?) {
        Log.d(TAG, "Updating completion status for template $templateId on $date: $isCompleted")
        dailyTaskRecordDao.updateRecordCompletion(templateId, date, isCompleted, completedAt)
    }

    override suspend fun getRecordCountByDate(date: String): Int {
        return dailyTaskRecordDao.getRecordCountByDate(date)
    }

    override suspend fun getCompletedRecordCountByDate(date: String): Int {
        return dailyTaskRecordDao.getCompletedRecordCountByDate(date)
    }

    override suspend fun getPendingRecordCountByDate(date: String): Int {
        return dailyTaskRecordDao.getPendingRecordCountByDate(date)
    }

    // Combined operations for UI
    override fun getTasksForDate(date: String): Flow<List<TaskWithDailyRecord>> {
        return taskTrackingDao.getTasksForDate(date)
    }

    override fun getTasksByCategoryForDate(category: TaskCategory, date: String): Flow<List<TaskWithDailyRecord>> {
        return taskTrackingDao.getTasksByCategoryForDate(category.name, date)
    }

    override fun getTasksByTimeBlockForDate(timeBlock: TimeBlock, date: String): Flow<List<TaskWithDailyRecord>> {
        return taskTrackingDao.getTasksByTimeBlockForDate(timeBlock.name, date)
    }

    override fun getTasksByPriorityForDate(priority: Priority, date: String): Flow<List<TaskWithDailyRecord>> {
        return taskTrackingDao.getTasksByPriorityForDate(priority.name, date)
    }

    override fun getTasksByCompletionStatusForDate(isCompleted: Boolean, date: String): Flow<List<TaskWithDailyRecord>> {
        return taskTrackingDao.getTasksByCompletionStatusForDate(isCompleted, date)
    }

    // Daily operations
    override suspend fun createDailyRecordsForDate(date: String) {
        try {
            taskTrackingDao.createDailyRecordsForDate(date)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create daily records for $date", e)
            
            // Try manual creation as fallback
            val activeTemplates = taskTemplateDao.getAllActiveTemplates().first()
            activeTemplates.forEach { template ->
                try {
                    val existingRecord = dailyTaskRecordDao.getRecordByTemplateAndDate(template.id, date)
                    if (existingRecord == null) {
                        val record = DailyTaskRecord(
                            templateId = template.id,
                            date = date,
                            isCompleted = false,
                            completedAt = null
                        )
                        dailyTaskRecordDao.insertRecord(record)
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to create record for template ${template.id} on $date", ex)
                }
            }
        }
    }

    override suspend fun resetTasksForDate(date: String) {
        Log.d(TAG, "Resetting tasks for date: $date")
        taskTrackingDao.resetTasksForDate(date)
    }

    // Analytics
    override suspend fun getAnalyticsDataForTemplate(templateId: Int): List<TaskTrackingDao.AnalyticsRecord> {
        return taskTrackingDao.getAnalyticsDataForTemplate(templateId)
    }

    override suspend fun getRecordsFromTemplateCreation(templateId: Int): List<DailyTaskRecord> {
        return dailyTaskRecordDao.getRecordsFromTemplateCreation(templateId)
    }

    // Utility methods
    override suspend fun ensureTodayRecordsExist() {
        val today = DailyTaskRecord.getTodayDateString()
        createDailyRecordsForDate(today)
    }

    override suspend fun completeTask(templateId: Int, date: String) {
        Log.d(TAG, "Completing task $templateId for $date")
        val completedAt = System.currentTimeMillis()
        
        // Check if record exists, create if not
        var record = getRecordByTemplateAndDate(templateId, date)
        if (record == null) {
            record = DailyTaskRecord(
                templateId = templateId,
                date = date,
                isCompleted = true,
                completedAt = completedAt
            )
            insertRecord(record)
        } else {
            updateRecordCompletion(templateId, date, true, completedAt)
        }
    }

    override suspend fun uncompleteTask(templateId: Int, date: String) {
        Log.d(TAG, "Uncompleting task $templateId for $date")
        updateRecordCompletion(templateId, date, false, null)
    }
}