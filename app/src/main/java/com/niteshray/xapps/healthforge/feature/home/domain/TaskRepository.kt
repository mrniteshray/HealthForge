package com.niteshray.xapps.healthforge.feature.home.domain

import com.niteshray.xapps.healthforge.feature.home.data.models.Task
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TaskCategory
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TimeBlock
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Priority
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    
    fun getAllTasks(): Flow<List<Task>>
    
    suspend fun getTaskById(taskId: Int): Task?
    
    fun getTasksByCompletionStatus(isCompleted: Boolean): Flow<List<Task>>
    
    fun getTasksByCategory(category: TaskCategory): Flow<List<Task>>
    
    fun getTasksByTimeBlock(timeBlock: TimeBlock): Flow<List<Task>>
    
    fun getTasksByPriority(priority: Priority): Flow<List<Task>>
    
    suspend fun insertTask(task: Task)
    
    suspend fun insertTasks(tasks: List<Task>)
    
    suspend fun updateTask(task: Task)
    
    suspend fun deleteTask(task: Task)
    
    suspend fun deleteTaskById(taskId: Int)
    
    suspend fun deleteAllTasks()
    
    suspend fun updateTaskCompletionStatus(taskId: Int, isCompleted: Boolean)
    
    suspend fun getTaskCount(): Int
    
    suspend fun getCompletedTaskCount(): Int
    
    suspend fun getPendingTaskCount(): Int
}