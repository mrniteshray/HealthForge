package com.niteshray.xapps.healthforge.core.di

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.niteshray.xapps.healthforge.feature.home.data.models.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TasksDAO {
    
    @Query("SELECT * FROM Tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM Tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?
    
    @Query("SELECT * FROM Tasks WHERE isCompleted = :isCompleted")
    fun getTasksByCompletionStatus(isCompleted: Boolean): Flow<List<Task>>
    
    @Query("SELECT * FROM Tasks WHERE category = :category")
    fun getTasksByCategory(category: String): Flow<List<Task>>
    
    @Query("SELECT * FROM Tasks WHERE timeBlock = :timeBlock")
    fun getTasksByTimeBlock(timeBlock: String): Flow<List<Task>>
    
    @Query("SELECT * FROM Tasks WHERE priority = :priority")
    fun getTasksByPriority(priority: String): Flow<List<Task>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)
    
    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
    
    @Query("DELETE FROM Tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)
    
    @Query("DELETE FROM Tasks")
    suspend fun deleteAllTasks()
    
    @Query("UPDATE Tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskCompletionStatus(taskId: Int, isCompleted: Boolean)
    
    @Query("SELECT COUNT(*) FROM Tasks")
    suspend fun getTaskCount(): Int
    
    @Query("SELECT COUNT(*) FROM Tasks WHERE isCompleted = 1")
    suspend fun getCompletedTaskCount(): Int
    
    @Query("SELECT COUNT(*) FROM Tasks WHERE isCompleted = 0")
    suspend fun getPendingTaskCount(): Int
}