package com.niteshray.xapps.healthforge.feature.home.presentation.viewmodel

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
            taskRepository.getAllTasks()
                .catch { e ->
                    errorMessage.value = "Failed to load tasks: ${e.message}"
                    _isTasksLoading.value = false
                }
                .map { dataTaskList ->
                    dataTaskList.map { it.toPresentationTask() }
                }
                .collect { taskList ->
                    _tasks.value = taskList
                    generatedTasks.value = taskList // For backward compatibility
                    _isTasksLoading.value = false
                }
        }
    }

    //To be Set Later on
//    private val _userName = MutableStateFlow<String>("")
//    val userName = _userName.asStateFlow()


    fun generateTasksFromReport(medicalReport: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
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

                val request = ChatRequest(
                    messages = listOf(
                        Message("system", "You are a medical AI assistant. Respond only in valid JSON."),
                        Message("user", prompt)
                    )
                )

                val response = cerebrasApi.generateContent(request)
                val generatedContent = response.choices.firstOrNull()?.message?.content ?: "{}"

                val jsonRegex = Regex("""\{[\s\S]*\}""")
                val jsonMatch = jsonRegex.find(generatedContent)
                val jsonString = jsonMatch?.value ?: throw Exception("No valid JSON found")

                // Parse and convert to Task objects then save to database
                val parsedTasks = parseJsonToTasks(jsonString)
                viewModelScope.launch {
                    try {
                        taskRepository.insertTasks(parsedTasks)
                        // Tasks will be automatically updated through the flow in loadTasks()
                    } catch (e: Exception) {
                        errorMessage.value = "Failed to save tasks: ${e.message}"
                    }
                }
            } catch (e: Exception) {
                errorMessage.value = "Failed: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
    
    fun resetTasks() {
        viewModelScope.launch {
            try {
                taskRepository.deleteAllTasks()
                errorMessage.value = null
            } catch (e: Exception) {
                errorMessage.value = "Failed to reset tasks: ${e.message}"
            }
        }
    }
    
    fun addTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.insertTask(task.toDataTask())
            } catch (e: Exception) {
                errorMessage.value = "Failed to add task: ${e.message}"
            }
        }
    }
    
    fun updateTask(updatedTask: Task) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(updatedTask.toDataTask())
            } catch (e: Exception) {
                errorMessage.value = "Failed to update task: ${e.message}"
            }
        }
    }
    
    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTaskById(taskId)
            } catch (e: Exception) {
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