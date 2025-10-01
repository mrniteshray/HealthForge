package com.niteshray.xapps.healthforge.feature.home.domain

import com.niteshray.xapps.healthforge.core.di.TasksDAO
import com.niteshray.xapps.healthforge.feature.home.data.models.Task
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TaskCategory
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TimeBlock
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Priority
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TasksDAO
) : TaskRepository {
    
    override fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()
    
    override suspend fun getTaskById(taskId: Int): Task? = taskDao.getTaskById(taskId)
    
    override fun getTasksByCompletionStatus(isCompleted: Boolean): Flow<List<Task>> {
        return taskDao.getTasksByCompletionStatus(isCompleted)
    }
    
    override fun getTasksByCategory(category: TaskCategory): Flow<List<Task>> {
        return taskDao.getTasksByCategory(category.name)
    }
    
    override fun getTasksByTimeBlock(timeBlock: TimeBlock): Flow<List<Task>> {
        return taskDao.getTasksByTimeBlock(timeBlock.name)
    }
    
    override fun getTasksByPriority(priority: Priority): Flow<List<Task>> {
        return taskDao.getTasksByPriority(priority.name)
    }
    
    override suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }
    
    override suspend fun insertTasks(tasks: List<Task>) {
        taskDao.insertTasks(tasks)
    }
    
    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }
    
    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
    
    override suspend fun deleteTaskById(taskId: Int) {
        taskDao.deleteTaskById(taskId)
    }
    
    override suspend fun deleteAllTasks() {
        taskDao.deleteAllTasks()
    }
    
    override suspend fun updateTaskCompletionStatus(taskId: Int, isCompleted: Boolean) {
        taskDao.updateTaskCompletionStatus(taskId, isCompleted)
    }
    
    override suspend fun getTaskCount(): Int = taskDao.getTaskCount()
    
    override suspend fun getCompletedTaskCount(): Int = taskDao.getCompletedTaskCount()
    
    override suspend fun getPendingTaskCount(): Int = taskDao.getPendingTaskCount()
}