package com.niteshray.xapps.healthforge.core.di

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskTemplate
import com.niteshray.xapps.healthforge.feature.home.data.models.DailyTaskRecord
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskWithDailyRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskTemplateDao {
    
    @Query("SELECT * FROM task_templates WHERE isActive = 1 ORDER BY id DESC")
    fun getAllActiveTemplates(): Flow<List<TaskTemplate>>
    
    @Query("SELECT * FROM task_templates ORDER BY id DESC")
    fun getAllTemplates(): Flow<List<TaskTemplate>>
    
    @Query("SELECT * FROM task_templates WHERE id = :templateId")
    suspend fun getTemplateById(templateId: Int): TaskTemplate?
    
    @Query("SELECT * FROM task_templates WHERE category = :category AND isActive = 1")
    fun getTemplatesByCategory(category: String): Flow<List<TaskTemplate>>
    
    @Query("SELECT * FROM task_templates WHERE timeBlock = :timeBlock AND isActive = 1")
    fun getTemplatesByTimeBlock(timeBlock: String): Flow<List<TaskTemplate>>
    
    @Query("SELECT * FROM task_templates WHERE priority = :priority AND isActive = 1")
    fun getTemplatesByPriority(priority: String): Flow<List<TaskTemplate>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TaskTemplate): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<TaskTemplate>)
    
    @Update
    suspend fun updateTemplate(template: TaskTemplate)
    
    @Delete
    suspend fun deleteTemplate(template: TaskTemplate)
    
    @Query("DELETE FROM task_templates WHERE id = :templateId")
    suspend fun deleteTemplateById(templateId: Int)
    
    @Query("UPDATE task_templates SET isActive = 0 WHERE id = :templateId")
    suspend fun deactivateTemplate(templateId: Int)
    
    @Query("UPDATE task_templates SET isActive = 1 WHERE id = :templateId")
    suspend fun activateTemplate(templateId: Int)
    
    @Query("SELECT COUNT(*) FROM task_templates WHERE isActive = 1")
    suspend fun getActiveTemplateCount(): Int
}