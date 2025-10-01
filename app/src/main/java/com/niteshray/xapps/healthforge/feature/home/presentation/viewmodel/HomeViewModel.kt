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
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Task
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TaskCategory
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TimeBlock
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Priority
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cerebrasApi: CerebrasApi,
    private val firebaseAuth : FirebaseAuth
) : ViewModel() {
    var errorMessage = mutableStateOf<String?>(null)
    var generatedTasks = mutableStateOf<List<Task>>(emptyList())
    var isLoading = mutableStateOf(false)

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

                // Parse and convert to Task objects
                generatedTasks.value = parseJsonToTasks(jsonString)
            } catch (e: Exception) {
                errorMessage.value = "Failed: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
    
    fun resetTasks() {
        generatedTasks.value = emptyList()
        errorMessage.value = null
    }
    
    fun addTask(task: Task) {
        generatedTasks.value = generatedTasks.value + task
    }
    
    fun updateTask(updatedTask: Task) {
        generatedTasks.value = generatedTasks.value.map { task ->
            if (task.id == updatedTask.id) updatedTask else task
        }
    }
    
    fun deleteTask(taskId: Int) {
        generatedTasks.value = generatedTasks.value.filter { task ->
            task.id != taskId
        }
    }
    
    private fun parseJsonToTasks(jsonString: String): List<Task> {
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
                
                Task(
                    id = index + 1,
                    title = title,
                    description = description,
                    timeBlock = timeBlock,
                    time = time,
                    category = category,
                    isCompleted = false,
                    icon = getCategoryIcon(category),
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
    
    private fun getCategoryIcon(category: TaskCategory) = when (category) {
        TaskCategory.MEDICATION -> Icons.Filled.Medication
        TaskCategory.EXERCISE -> Icons.Filled.DirectionsRun
        TaskCategory.DIET -> Icons.Filled.Restaurant
        TaskCategory.MONITORING -> Icons.Filled.MonitorHeart
        TaskCategory.LIFESTYLE -> Icons.Filled.Spa
        TaskCategory.GENERAL -> TODO()
    }
}