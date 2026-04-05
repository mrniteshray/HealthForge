package com.niteshray.xapps.healthforge.core.di

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.niteshray.xapps.healthforge.feature.home.data.models.DailyTaskRecord
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskWithDailyRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskRecordDao {
    
    @Query("SELECT * FROM daily_task_records WHERE date = :date ORDER BY templateId")
    fun getRecordsByDate(date: String): Flow<List<DailyTaskRecord>>
    
    @Query("SELECT * FROM daily_task_records WHERE templateId = :templateId ORDER BY date DESC")
    fun getRecordsByTemplate(templateId: Int): Flow<List<DailyTaskRecord>>
    
    @Query("SELECT * FROM daily_task_records WHERE templateId = :templateId AND date = :date")
    suspend fun getRecordByTemplateAndDate(templateId: Int, date: String): DailyTaskRecord?
    
    @Query("SELECT * FROM daily_task_records WHERE templateId = :templateId AND date >= :startDate AND date <= :endDate ORDER BY date")
    suspend fun getRecordsByTemplateInDateRange(templateId: Int, startDate: String, endDate: String): List<DailyTaskRecord>
    
    @Query("SELECT * FROM daily_task_records WHERE date = :date AND isCompleted = :isCompleted")
    fun getRecordsByDateAndStatus(date: String, isCompleted: Boolean): Flow<List<DailyTaskRecord>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DailyTaskRecord): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<DailyTaskRecord>)
    
    @Update
    suspend fun updateRecord(record: DailyTaskRecord)
    
    @Delete
    suspend fun deleteRecord(record: DailyTaskRecord)
    
    @Query("DELETE FROM daily_task_records WHERE templateId = :templateId")
    suspend fun deleteRecordsByTemplate(templateId: Int)
    
    @Query("DELETE FROM daily_task_records WHERE date = :date")
    suspend fun deleteRecordsByDate(date: String)
    
    @Query("UPDATE daily_task_records SET isCompleted = :isCompleted, completedAt = :completedAt WHERE templateId = :templateId AND date = :date")
    suspend fun updateRecordCompletion(templateId: Int, date: String, isCompleted: Boolean, completedAt: Long?)
    
    @Query("SELECT COUNT(*) FROM daily_task_records WHERE date = :date")
    suspend fun getRecordCountByDate(date: String): Int
    
    @Query("SELECT COUNT(*) FROM daily_task_records WHERE date = :date AND isCompleted = 1")
    suspend fun getCompletedRecordCountByDate(date: String): Int
    
    @Query("SELECT COUNT(*) FROM daily_task_records WHERE date = :date AND isCompleted = 0")
    suspend fun getPendingRecordCountByDate(date: String): Int
    
    // Get all records for a template from its creation date
    @Query("""
        SELECT dr.* FROM daily_task_records dr
        WHERE dr.templateId = :templateId 
        ORDER BY dr.date
    """)
    suspend fun getRecordsFromTemplateCreation(templateId: Int): List<DailyTaskRecord>
    

}