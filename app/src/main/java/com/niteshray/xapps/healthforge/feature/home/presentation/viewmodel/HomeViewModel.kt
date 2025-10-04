package com.niteshray.xapps.healthforge.feature.home.presentation.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.niteshray.xapps.healthforge.core.di.CerebrasApi
import com.niteshray.xapps.healthforge.core.di.ChatRequest
import com.niteshray.xapps.healthforge.core.di.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject
import com.niteshray.xapps.healthforge.feature.home.data.models.Task as DataTask
import com.niteshray.xapps.healthforge.feature.home.domain.TaskRepository
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Task
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TaskCategory
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TimeBlock
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Priority
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.toPresentationTask
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.toDataTask
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.google.firebase.auth.FirebaseAuth
import com.niteshray.xapps.healthforge.core.notifications.ReminderManager
import com.niteshray.xapps.healthforge.core.notifications.TaskReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cerebrasApi: CerebrasApi,
    private val firebaseAuth: FirebaseAuth,
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    companion object {
        const val TAG = "HomeViewModel"
    }
    
    var errorMessage = mutableStateOf<String?>(null)
    var isLoading = mutableStateOf(false)

    // Tasks loading state
    private val _isTasksLoading = MutableStateFlow(true)
    val isTasksLoading: StateFlow<Boolean> = _isTasksLoading.asStateFlow()

    // Tasks from database
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    // For backward compatibility with existing UI code
    var generatedTasks = mutableStateOf<List<Task>>(emptyList())

    init {
        // Load tasks from database on initialization
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _isTasksLoading.value = true
            Log.d(TAG, "Loading tasks from database")
            
            taskRepository.getAllTasks()
                .catch { e ->
                    Log.e(TAG, "Failed to load tasks from database", e)
                    errorMessage.value = "Failed to load tasks: ${e.message}"
                    _isTasksLoading.value = false
                }
                .map { dataTaskList ->
                    dataTaskList.map { it.toPresentationTask() }
                }
                .collect { taskList ->
                    Log.d(TAG, "Loaded ${taskList.size} tasks from database")
                    _tasks.value = taskList
                    generatedTasks.value = taskList // For backward compatibility
                    _isTasksLoading.value = false
                }
        }
    }

    //To be Set Later on
//    private val _userName = MutableStateFlow<String>("")
//    val userName = _userName.asStateFlow()


    fun generateTasksFromReport(medicalReport: String, context: Context) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            Log.d(TAG, "Starting task generation from medical report")
            
            try {
                val prompt = """
                    Based on this medical report: '$medicalReport'
                    Generate a personalized daily care plan as a JSON array of tasks. Structure it like this:
                    {
                      "tasks": [
                        {
                          "title": "Task name (max 50 chars)",
                          "description": "Detailed instructions (max 100 chars)",
                          "time": "8:00 AM",
                          "category": "MEDICATION|EXERCISE|DIET|MONITORING|LIFESTYLE",
                          "priority": "HIGH|MEDIUM|LOW"
                        }
                      ]
                    }
                    
                    Guidelines:
                    - Create 10-12 tasks covering medication, exercise, diet, monitoring, lifestyle
                    - Use specific times (8:00 AM, 1:30 PM, etc.)
                    - HIGH priority for medications/critical monitoring
                    - MEDIUM for diet/regular monitoring
                    - LOW for lifestyle/optional activities
                    
                    Respond ONLY with the JSON object—no extra text.
                """.trimIndent()

                Log.d(TAG, "Sending request to AI service")
                
                val request = ChatRequest(
                    messages = listOf(
                        Message("system", "You are a medical AI assistant. Respond only in valid JSON."),
                        Message("user", prompt)
                    )
                )

                val response = cerebrasApi.generateContent(request)
                val generatedContent = response.choices.firstOrNull()?.message?.content ?: "{}"
                
                Log.d(TAG, "Received AI response: ${generatedContent.take(200)}...")

                val jsonRegex = Regex("""\{[\s\S]*\}""")
                val jsonMatch = jsonRegex.find(generatedContent)
                val jsonString = jsonMatch?.value ?: throw Exception("No valid JSON found")

                Log.d(TAG, "Extracted JSON: ${jsonString.take(200)}...")

                // Parse and convert to Task objects then save to database
                val parsedTasks = parseJsonToTasks(jsonString)
                
                Log.d(TAG, "Parsed ${parsedTasks.size} tasks successfully")
                
                viewModelScope.launch {
                    try {
                        // Save tasks to database first
                        taskRepository.insertTasks(parsedTasks)
                        Log.d(TAG, "Tasks saved to database successfully")
                        
                        // Then schedule reminders for all tasks
                        ReminderManager.scheduleAllTasks(context, parsedTasks)
                        Log.d(TAG, "Reminders scheduled for all generated tasks")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save tasks or schedule reminders", e)
                        errorMessage.value = "Failed to save tasks: ${e.message}"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate tasks from report", e)
                errorMessage.value = "Failed: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
    
    fun resetTasks(context: Context) {
        viewModelScope.launch {
            Log.d(TAG, "Resetting all tasks")
            
            try {
                // Get all current task IDs before deleting
                val currentTasks = _tasks.value
                val taskIds = currentTasks.map { it.id }
                
                Log.d(TAG, "Canceling ${taskIds.size} task reminders")
                
                // Cancel all reminders first
                ReminderManager.cancelAllTaskReminders(context, taskIds)
                
                // Then delete all tasks from database
                taskRepository.deleteAllTasks()
                Log.d(TAG, "All tasks deleted from database successfully")
                
                errorMessage.value = null
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset tasks", e)
                errorMessage.value = "Failed to reset tasks: ${e.message}"
            }
        }
    }
    
    fun addTask(task: Task, context : Context) {
        viewModelScope.launch {
            Log.d(TAG, "Adding new task: ${task.title} at ${task.time}")
            
            try {
                val dataTask = task.toDataTask()
                taskRepository.insertTask(dataTask)
                Log.d(TAG, "Task saved to database successfully")

                ReminderManager.scheduleTaskReminder(context, dataTask)
                Log.d(TAG, "Reminder scheduled for new task: ${task.title}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add task: ${task.title}", e)
                errorMessage.value = "Failed to add task: ${e.message}"
            }
        }
    }

    fun testNotificationAndTTS(context: Context) {
        Log.d(TAG, "Testing notification and TTS system")
        
        try {
            // Create a test task reminder
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("TASK_ID", 9999)
                putExtra("TASK_TITLE", "Medicine Reminder")
                putExtra("TASK_DESCRIPTION", "Hello User , it's your medicine time")
            }
            
            // Trigger the receiver directly for testing
            val receiver = TaskReminderReceiver()
            receiver.onReceive(context, intent)
            
            Log.d(TAG, "Test notification and TTS triggered successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to test notification and TTS", e)
        }
    }
    
    fun scheduleTestReminder(context: Context) {
        Log.d(TAG, "Scheduling test reminder for 30 seconds from now")
        
        try {
            // Create a test task that will trigger in 30 seconds
            val testTask = com.niteshray.xapps.healthforge.feature.home.data.models.Task(
                id = 9999,
                title = "Test Reminder",
                description = "This is a test reminder scheduled for 30 seconds",
                time = "12:00 PM", // This will be ignored for the test
                timeBlock = TimeBlock.AFTERNOON,
                category = TaskCategory.GENERAL,
                priority = Priority.HIGH,
                isCompleted = false
            )
            
            // Schedule it using AlarmManager but override the time
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("TASK_ID", testTask.id)
                putExtra("TASK_TITLE", testTask.title)
                putExtra("TASK_DESCRIPTION", testTask.description)
                putExtra("TASK_CATEGORY", "MEDICATION")
                putExtra("TASK_PRIORITY", "HIGH")
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                testTask.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Schedule for 30 seconds from now
            val triggerTime = System.currentTimeMillis() + 30000 // 30 seconds
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Test reminder scheduled for 30 seconds from now")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule test reminder", e)
        }
    }
    
    fun updateTask(updatedTask: Task, context: Context) {
        viewModelScope.launch {
            Log.d(TAG, "Updating task: ${updatedTask.id} - ${updatedTask.title}")
            
            try {
                val dataTask = updatedTask.toDataTask()
                taskRepository.updateTask(dataTask)
                Log.d(TAG, "Task updated in database successfully")
                
                // Cancel old reminder and schedule new one
                ReminderManager.cancelTaskReminder(context, updatedTask.id)
                ReminderManager.scheduleTaskReminder(context, dataTask)
                Log.d(TAG, "Task reminder rescheduled for updated task: ${updatedTask.title}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update task: ${updatedTask.title}", e)
                errorMessage.value = "Failed to update task: ${e.message}"
            }
        }
    }
    
    fun deleteTask(taskId: Int, context: Context) {
        viewModelScope.launch {
            Log.d(TAG, "Deleting task: $taskId")
            
            try {
                // Cancel reminder first
                ReminderManager.cancelTaskReminder(context, taskId)
                Log.d(TAG, "Reminder canceled for task: $taskId")
                
                // Then delete from database
                taskRepository.deleteTaskById(taskId)
                Log.d(TAG, "Task deleted from database successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete task: $taskId", e)
                errorMessage.value = "Failed to delete task: ${e.message}"
            }
        }
    }
    
    fun toggleTaskCompletion(taskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                taskRepository.updateTaskCompletionStatus(taskId, isCompleted)
            } catch (e: Exception) {
                errorMessage.value = "Failed to update task status: ${e.message}"
            }
        }
    }
    
    // Additional repository methods
    fun getTasksByCategory(category: TaskCategory) = taskRepository.getTasksByCategory(category)
    
    fun getTasksByTimeBlock(timeBlock: TimeBlock) = taskRepository.getTasksByTimeBlock(timeBlock)
    
    fun getTasksByPriority(priority: Priority) = taskRepository.getTasksByPriority(priority)
    
    fun getCompletedTasks() = taskRepository.getTasksByCompletionStatus(true)
    
    fun getPendingTasks() = taskRepository.getTasksByCompletionStatus(false)
    
    private fun parseJsonToTasks(jsonString: String): List<DataTask> {
        return try {
            val gson = Gson()
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
            val tasksArray = jsonObject.getAsJsonArray("tasks")
            
            tasksArray.mapIndexed { index, taskElement ->
                val taskObj = taskElement.asJsonObject
                val title = taskObj.get("title")?.asString ?: "Health Task"
                val description = taskObj.get("description")?.asString ?: ""
                val time = taskObj.get("time")?.asString ?: "9:00 AM"
                val categoryStr = taskObj.get("category")?.asString ?: "LIFESTYLE"
                val priorityStr = taskObj.get("priority")?.asString ?: "MEDIUM"
                
                val timeBlock = getTimeBlockFromTime(time)
                val category = try { TaskCategory.valueOf(categoryStr) } catch (e: Exception) { TaskCategory.LIFESTYLE }
                val priority = try { Priority.valueOf(priorityStr) } catch (e: Exception) { Priority.MEDIUM }
                
                DataTask(
                    id = 0, // Let Room auto-generate the ID
                    title = title,
                    description = description,
                    timeBlock = timeBlock,
                    time = time,
                    category = category,
                    isCompleted = false,
                    priority = priority
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun getTimeBlockFromTime(time: String): TimeBlock {
        val hour = try {
            val timePattern = Regex("""(\d{1,2}):\d{2}\s*([AaPp][Mm])""")
            val match = timePattern.find(time)
            val hourStr = match?.groupValues?.get(1) ?: "9"
            val period = match?.groupValues?.get(2)?.uppercase() ?: "AM"
            var hour = hourStr.toInt()
            if (period == "PM" && hour != 12) hour += 12
            if (period == "AM" && hour == 12) hour = 0
            hour
        } catch (e: Exception) { 9 }
        
        return when (hour) {
            in 6..11 -> TimeBlock.MORNING
            in 12..16 -> TimeBlock.AFTERNOON
            in 17..20 -> TimeBlock.EVENING
            else -> TimeBlock.NIGHT
        }
    }
}