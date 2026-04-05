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
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.niteshray.xapps.healthforge.core.di.GeminiApi
import com.niteshray.xapps.healthforge.core.di.ChatRequest
import com.niteshray.xapps.healthforge.core.di.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskTemplate
import com.niteshray.xapps.healthforge.feature.home.data.models.DailyTaskRecord
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskWithDailyRecord
import com.niteshray.xapps.healthforge.feature.home.domain.TaskTrackingRepository
import com.niteshray.xapps.healthforge.feature.home.domain.FirestoreTaskSyncService
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Task
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TaskCategory
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TimeBlock
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Priority
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.toPresentationTask
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.toDataTask
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.niteshray.xapps.healthforge.core.notifications.ReminderManager
import com.niteshray.xapps.healthforge.core.notifications.TaskReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.apply
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.mapIndexedNotNull
import kotlin.jvm.java
import kotlin.jvm.javaClass
import kotlin.text.contains
import kotlin.text.endsWith
import kotlin.text.indexOf
import kotlin.text.isNullOrBlank
import kotlin.text.lastIndexOf
import kotlin.text.lowercase
import kotlin.text.removePrefix
import kotlin.text.removeSuffix
import kotlin.text.split
import kotlin.text.startsWith
import kotlin.text.substring
import kotlin.text.toInt
import kotlin.text.trim
import kotlin.text.trimIndent
import kotlin.text.uppercase

@HiltViewModel
class NewHomeViewModel @Inject constructor(
    private val geminiApi: GeminiApi,
    private val firebaseAuth: FirebaseAuth,
    private val taskTrackingRepository: TaskTrackingRepository,
    private val firestoreSyncService: FirestoreTaskSyncService,
    private val firestore: FirebaseFirestore
) : ViewModel()
{

    companion object {
        const val TAG = "NewHomeViewModel"
    }

    var errorMessage = mutableStateOf<String?>(null)
    var isLoading = mutableStateOf(false)

    // Tasks loading state
    private val _isTasksLoading = MutableStateFlow(true)
    val isTasksLoading: StateFlow<Boolean> = _isTasksLoading.asStateFlow()

    // Tasks from database (using new system)
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    // For backward compatibility with existing UI code
    var generatedTasks = mutableStateOf<List<Task>>(emptyList())

    init {
        loadTasks()
        setupDailyReset()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _isTasksLoading.value = true
            Log.d(TAG, "Loading tasks from new tracking system")

            try {
                // Ensure today's records exist first
                taskTrackingRepository.ensureTodayRecordsExist()

                // Get today's date
                val today = DailyTaskRecord.getTodayDateString()

                taskTrackingRepository.getTasksForDate(today)
                    .catch { e ->
                        Log.e(TAG, "Failed to load tasks from database", e)
                        errorMessage.value = "Failed to load tasks: ${e.message}"
                        _isTasksLoading.value = false
                    }
                    .map { taskWithRecordList ->
                        taskWithRecordList.map { it.toTask().toPresentationTask() }
                    }
                    .collect { taskList ->
                        Log.d(TAG, "Loaded ${taskList.size} tasks from database")
                        _tasks.value = taskList
                        generatedTasks.value = taskList // For backward compatibility
                        _isTasksLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadTasks", e)
                errorMessage.value = "Failed to load tasks: ${e.message}"
                _isTasksLoading.value = false
            }
        }
    }

    private fun setupDailyReset() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Setting up daily reset worker")
                // This will be called from Application class or MainActivity
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup daily reset", e)
            }
        }
    }

    fun generateTasksFromReport(medicalReport: String, context: Context) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            Log.d(TAG, "Generating tasks from medical report")

            try {
                val prompt = """
                    Analyze this medical report and create a list of health tasks only 15 tasks, 5 Morning task, 5 - Afternoon and 5 - Evening tasks and do not include medicines. Return ONLY a JSON object with this exact structure:
                    {
                        "tasks": [
                            {
                                "title": "Take Medicine Name",
                                "description": "Brief description",
                                "time": "9:00 AM",
                                "category": "EXERCISE|DIET|MONITORING|LIFESTYLE|GENERAL",
                                "priority": "HIGH|MEDIUM|LOW"
                            }
                        ]
                    }
                    
                    Medical Report:
                    $medicalReport
                """.trimIndent()

                val request = ChatRequest(
                    messages = listOf(
                        Message(role = "system", content = "You are a healthcare assistant. You must respond ONLY with valid JSON. Do not include any explanations, markdown formatting, or additional text. Return only the raw JSON object."),
                        Message(role = "user", content = prompt)
                    ),
                    temperature = 0.3f // Lower temperature for more consistent JSON formatting
                )

                val response = geminiApi.generateContent(request)

                val aiResponse = response.choices.firstOrNull()?.message?.content
                if (aiResponse.isNullOrBlank()) {
                    throw Exception("Empty response from AI service")
                }

                // Clean the response to extract JSON
                val cleanedResponse = cleanAiResponse(aiResponse)
                val aiGeneratedTasks = parseJsonToTaskTemplates(cleanedResponse)

                if (aiGeneratedTasks.isNotEmpty()) {

                    // Insert task templates
                    aiGeneratedTasks.forEach { template ->
                        val templateId = taskTrackingRepository.insertTemplate(template)

                        // Sync to Firestore
                        firestoreSyncService.syncTaskTemplateToFirestore(template.copy(id = templateId.toInt()))

                        // Schedule reminder
                        val task = com.niteshray.xapps.healthforge.feature.home.data.models.Task(
                            id = templateId.toInt(),
                            title = template.title,
                            description = template.description,
                            timeBlock = template.timeBlock,
                            time = template.time,
                            category = template.category,
                            isCompleted = false,
                            priority = template.priority
                        )
                        ReminderManager.scheduleTaskReminder(context, task)
                    }

                    // Reload tasks to show new ones
                    loadTasks()

                } else {
                    Log.w(TAG, "No tasks could be parsed from AI response")
                    // Create fallback tasks based on medical report content
                    val fallbackTasks = createFallbackTasks(medicalReport)
                    if (fallbackTasks.isNotEmpty()) {
                        fallbackTasks.forEach { template ->
                            val templateId = taskTrackingRepository.insertTemplate(template)
                            Log.d(TAG, "Inserted fallback template: ${template.title} with ID: $templateId")
                        }
                        loadTasks()
                    } else {
                        errorMessage.value = "Could not generate tasks from this report. Please try again or add tasks manually."
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate tasks from report", e)
                errorMessage.value = "Failed to analyze report: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun addTask(task: Task, context: Context) {
        viewModelScope.launch {
            Log.d(TAG, "Adding new task template: ${task.title} at ${task.time}")

            try {
                // Create TaskTemplate from Task
                val template = TaskTemplate(
                    title = task.title,
                    description = task.description,
                    timeBlock = task.timeBlock,
                    time = task.time,
                    category = task.category,
                    priority = task.priority,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )

                // Insert template
                val templateId = taskTrackingRepository.insertTemplate(template)
                Log.d(TAG, "Task template saved to database successfully with ID: $templateId")

                // Create today's record
                taskTrackingRepository.ensureTodayRecordsExist()

                // Sync to Firestore
                firestoreSyncService.syncTaskTemplateToFirestore(template.copy(id = templateId.toInt()))

                // Schedule reminder
                val dataTask = task.toDataTask().copy(id = templateId.toInt())
                ReminderManager.scheduleTaskReminder(context, dataTask)
                Log.d(TAG, "Reminder scheduled for new task: ${task.title}")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to add task: ${task.title}", e)
                errorMessage.value = "Failed to add task: ${e.message}"
            }
        }
    }

    fun updateTask(updatedTask: Task, context: Context) {
        viewModelScope.launch {
            Log.d(TAG, "Updating task template: ${updatedTask.id} - ${updatedTask.title}")

            try {
                // Get the existing template
                val existingTemplate = taskTrackingRepository.getTemplateById(updatedTask.id)
                if (existingTemplate == null) {
                    Log.e(TAG, "Template not found: ${updatedTask.id}")
                    return@launch
                }

                // Update template
                val updatedTemplate = existingTemplate.copy(
                    title = updatedTask.title,
                    description = updatedTask.description,
                    timeBlock = updatedTask.timeBlock,
                    time = updatedTask.time,
                    category = updatedTask.category,
                    priority = updatedTask.priority
                )

                taskTrackingRepository.updateTemplate(updatedTemplate)
                Log.d(TAG, "Task template updated in database successfully")

                // Sync to Firestore
                firestoreSyncService.syncTaskTemplateToFirestore(updatedTemplate)

                // Cancel old reminder and schedule new one
                ReminderManager.cancelTaskReminder(context, updatedTask.id)
                val dataTask = updatedTask.toDataTask()
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
            Log.d(TAG, "Deleting task template: $taskId")

            try {
                // Get template for Firestore deletion
                val template = taskTrackingRepository.getTemplateById(taskId)

                // Cancel reminder first
                ReminderManager.cancelTaskReminder(context, taskId)
                Log.d(TAG, "Reminder canceled for task: $taskId")

                // Delete from database (this will cascade delete daily records)
                taskTrackingRepository.deleteTemplateById(taskId)
                Log.d(TAG, "Task template deleted from database successfully")

                // Delete from Firestore
                if (template != null) {
                    firestoreSyncService.deleteTaskTemplateFromFirestore(template)
                }

                errorMessage.value = null

            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete task: $taskId", e)
                errorMessage.value = "Failed to delete task: ${e.message}"
            }
        }
    }

    fun toggleTaskCompletion(taskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Toggling task completion: $taskId -> $isCompleted")

            try {
                val today = DailyTaskRecord.getTodayDateString()

                if (isCompleted) {
                    taskTrackingRepository.completeTask(taskId, today)

                    // Check for achievements and send notifications
                    checkAndSendAchievementNotifications()
                } else {
                    taskTrackingRepository.uncompleteTask(taskId, today)
                }

                // Sync the daily record to Firestore
                val record = taskTrackingRepository.getRecordByTemplateAndDate(taskId, today)
                if (record != null) {
                    try {
                        // First ensure template is synced to Firestore
                        val template = taskTrackingRepository.getTemplateById(taskId)
                        if (template != null && template.firestoreId.isNullOrBlank()) {
                            firestoreSyncService.syncTaskTemplateToFirestore(template)
                        }

                        // Now sync the daily record
                        firestoreSyncService.syncDailyRecordToFirestore(record)
                    } catch (syncError: Exception) {
                        Log.e(TAG, "Failed to sync to Firestore", syncError)
                        // Continue execution - local update is successful
                    }
                }

                Log.d(TAG, "Task completion updated successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle task completion: $taskId", e)
                errorMessage.value = "Failed to update task: ${e.message}"
            }
        }
    }

    fun resetTasks(context: Context) {
        viewModelScope.launch {
            Log.d(TAG, "Resetting all tasks")

            try {
                // Get all current task IDs before resetting
                val currentTasks = _tasks.value
                val taskIds = currentTasks.map { it.id }

                Log.d(TAG, "Canceling ${taskIds.size} task reminders")

                // Cancel all reminders first
                ReminderManager.cancelAllTaskReminders(context, taskIds)

                // Reset today's tasks
                val today = DailyTaskRecord.getTodayDateString()
                taskTrackingRepository.resetTasksForDate(today)
                Log.d(TAG, "All tasks reset for today successfully")

                errorMessage.value = null

            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset tasks", e)
                errorMessage.value = "Failed to reset tasks: ${e.message}"
            }
        }
    }

    // Filter methods for backward compatibility
    fun getTasksByCategory(category: TaskCategory) =
        taskTrackingRepository.getTasksByCategoryForDate(category, DailyTaskRecord.getTodayDateString())
            .map { list -> list.map { it.toTask().toPresentationTask() } }

    fun getTasksByTimeBlock(timeBlock: TimeBlock) =
        taskTrackingRepository.getTasksByTimeBlockForDate(timeBlock, DailyTaskRecord.getTodayDateString())
            .map { list -> list.map { it.toTask().toPresentationTask() } }

    fun getTasksByPriority(priority: Priority) =
        taskTrackingRepository.getTasksByPriorityForDate(priority, DailyTaskRecord.getTodayDateString())
            .map { list -> list.map { it.toTask().toPresentationTask() } }

    fun getCompletedTasks() =
        taskTrackingRepository.getTasksByCompletionStatusForDate(true, DailyTaskRecord.getTodayDateString())
            .map { list -> list.map { it.toTask().toPresentationTask() } }

    fun getPendingTasks() =
        taskTrackingRepository.getTasksByCompletionStatusForDate(false, DailyTaskRecord.getTodayDateString())
            .map { list -> list.map { it.toTask().toPresentationTask() } }

    private suspend fun checkAndSendAchievementNotifications() {
        try {
            val today = DailyTaskRecord.getTodayDateString()

            // Check if all tasks for today are completed
            val completedCount = taskTrackingRepository.getCompletedRecordCountByDate(today)
            val totalCount = taskTrackingRepository.getRecordCountByDate(today)

            if (completedCount == totalCount && totalCount > 0) {
                Log.d(TAG, "Perfect day detected! All $totalCount tasks completed.")
                // Achievement notification will be sent by the system
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking achievements", e)
        }
    }

    private fun parseJsonToTaskTemplates(jsonString: String): List<TaskTemplate> {
        return try {
            Log.d(TAG, "Raw AI response: $jsonString")

            val gson = Gson()

            // First, let's try to parse and see what we got
            val jsonElement = gson.fromJson(jsonString, JsonElement::class.java)
            Log.d(TAG, "Parsed JsonElement type: ${jsonElement.javaClass.simpleName}")

            if (jsonElement.isJsonPrimitive) {
                Log.w(TAG, "Received primitive instead of JSON object. Content: ${jsonElement.asString}")
                return emptyList()
            }

            if (!jsonElement.isJsonObject) {
                Log.w(TAG, "Received non-object JSON element")
                return emptyList()
            }

            val jsonObject = jsonElement.asJsonObject
            Log.d(TAG, "JSON object keys: ${jsonObject.keySet()}")

            // Try to get tasks array
            val tasksArray = when {
                jsonObject.has("tasks") -> jsonObject.getAsJsonArray("tasks")
                jsonObject.has("task_list") -> jsonObject.getAsJsonArray("task_list")
                jsonObject.has("recommendations") -> jsonObject.getAsJsonArray("recommendations")
                else -> {
                    Log.w(TAG, "No tasks array found in response")
                    return emptyList()
                }
            }

            if (tasksArray == null || tasksArray.size() == 0) {
                Log.w(TAG, "Empty or null tasks array")
                return emptyList()
            }

            Log.d(TAG, "Found ${tasksArray.size()} tasks in response")

            tasksArray.mapIndexedNotNull { index, taskElement ->
                try {
                    if (!taskElement.isJsonObject) {
                        Log.w(TAG, "Task element at index $index is not a JSON object")
                        return@mapIndexedNotNull null
                    }

                    val taskObj = taskElement.asJsonObject
                    val title = taskObj.get("title")?.asString ?: taskObj.get("name")?.asString ?: "Health Task ${index + 1}"
                    val description = taskObj.get("description")?.asString ?: taskObj.get("details")?.asString ?: ""
                    val time = taskObj.get("time")?.asString ?: taskObj.get("suggested_time")?.asString ?: "9:00 AM"
                    val categoryStr = taskObj.get("category")?.asString ?: taskObj.get("type")?.asString ?: "LIFESTYLE"
                    val priorityStr = taskObj.get("priority")?.asString ?: taskObj.get("importance")?.asString ?: "MEDIUM"

                    val timeBlock = getTimeBlockFromTime(time)
                    val category = try {
                        TaskCategory.valueOf(categoryStr.uppercase())
                    } catch (e: Exception) {
                        Log.w(TAG, "Invalid category: $categoryStr, using LIFESTYLE")
                        TaskCategory.LIFESTYLE
                    }
                    val priority = try {
                        Priority.valueOf(priorityStr.uppercase())
                    } catch (e: Exception) {
                        Log.w(TAG, "Invalid priority: $priorityStr, using MEDIUM")
                        Priority.MEDIUM
                    }

                    TaskTemplate(
                        title = title,
                        description = description,
                        timeBlock = timeBlock,
                        time = time,
                        category = category,
                        priority = priority,
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse task at index $index", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON to TaskTemplates", e)
            Log.e(TAG, "Problematic JSON: $jsonString")
            emptyList()
        }
    }

    private fun getTimeBlockFromTime(time: String): TimeBlock {
        val hour = try {
            val timePart = time.split(" ")[0]
            val hourPart = timePart.split(":")[0].toInt()

            when {
                time.contains("AM", ignoreCase = true) -> {
                    if (hourPart == 12) 0 else hourPart
                }
                time.contains("PM", ignoreCase = true) -> {
                    if (hourPart == 12) 12 else hourPart + 12
                }
                else -> hourPart
            }
        } catch (e: Exception) {
            9 // Default to 9 AM
        }

        return when (hour) {
            in 5..11 -> TimeBlock.MORNING
            in 12..16 -> TimeBlock.AFTERNOON
            in 17..20 -> TimeBlock.EVENING
            else -> TimeBlock.NIGHT
        }
    }

    // Test methods for backward compatibility
    fun testNotificationAndTTS(context: Context) {
        Log.d(TAG, "Testing notification and TTS system")

        try {
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("TASK_ID", 9999)
                putExtra("TASK_TITLE", "Take Supradyn Medicine")
                putExtra("TASK_DESCRIPTION", "Hello User , it's your medicine time")
            }

            val receiver = TaskReminderReceiver()
            receiver.onReceive(context, intent)

            Log.d(TAG, "Test notification and TTS triggered successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to test notification and TTS", e)
        }
    }

    fun scheduleTestReminder(context: Context) {
        Log.d(TAG, "Scheduling test reminder for 5 seconds from now")

        try {
            val testTask = com.niteshray.xapps.healthforge.feature.home.data.models.Task(
                id = 9999,
                title = "Take Supradyn Medicine",
                description = "This is a test reminder scheduled for 5 seconds",
                timeBlock = TimeBlock.MORNING,
                time = "9:00 AM",
                category = TaskCategory.MEDICATION,
                isCompleted = false,
                priority = Priority.HIGH
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("TASK_ID", testTask.id)
                putExtra("TASK_TITLE", testTask.title)
                putExtra("TASK_DESCRIPTION", testTask.description)
                putExtra("TASK_CATEGORY", testTask.category.name)
                putExtra("TASK_PRIORITY", testTask.priority.name)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                testTask.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + 5000 // 5 seconds

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

        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule test reminder", e)
        }
    }

    private fun cleanAiResponse(response: String): String {
        Log.d(TAG, "Cleaning AI response")

        // Remove markdown code blocks
        var cleaned = response.trim()

        // Remove ```json and ``` if present
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json").trim()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trim()
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trim()
        }

        // Try to find JSON object boundaries
        val jsonStart = cleaned.indexOf('{')
        val jsonEnd = cleaned.lastIndexOf('}')

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1)
        }

        Log.d(TAG, "Cleaned response: $cleaned")
        return cleaned
    }

    private fun createFallbackTasks(medicalReport: String): List<TaskTemplate> {
        Log.d(TAG, "Creating fallback tasks from medical report")

        val reportLower = medicalReport.lowercase()
        val fallbackTasks = mutableListOf<TaskTemplate>()

        // Analyze report content and create relevant tasks
        when {
            reportLower.contains("medication") || reportLower.contains("medicine") || reportLower.contains("prescription") -> {
                fallbackTasks.add(TaskTemplate(
                    title = "Take Prescribed Medication",
                    description = "Take your prescribed medication as directed by your doctor",
                    timeBlock = TimeBlock.MORNING,
                    time = "8:00 AM",
                    category = TaskCategory.MEDICATION,
                    priority = Priority.HIGH,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                ))
            }
            reportLower.contains("blood pressure") || reportLower.contains("hypertension") -> {
                fallbackTasks.add(TaskTemplate(
                    title = "Monitor Blood Pressure",
                    description = "Check and record your blood pressure daily",
                    timeBlock = TimeBlock.MORNING,
                    time = "9:00 AM",
                    category = TaskCategory.MONITORING,
                    priority = Priority.HIGH,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                ))
            }
            reportLower.contains("exercise") || reportLower.contains("physical activity") -> {
                fallbackTasks.add(TaskTemplate(
                    title = "Daily Exercise",
                    description = "Engage in recommended physical activity",
                    timeBlock = TimeBlock.EVENING,
                    time = "6:00 PM",
                    category = TaskCategory.EXERCISE,
                    priority = Priority.MEDIUM,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                ))
            }
            reportLower.contains("diet") || reportLower.contains("nutrition") -> {
                fallbackTasks.add(TaskTemplate(
                    title = "Follow Dietary Guidelines",
                    description = "Maintain recommended dietary habits",
                    timeBlock = TimeBlock.AFTERNOON,
                    time = "12:00 PM",
                    category = TaskCategory.DIET,
                    priority = Priority.MEDIUM,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                ))
            }
        }

        // If no specific conditions found, create a general health task
        if (fallbackTasks.isEmpty()) {
            fallbackTasks.add(TaskTemplate(
                title = "Follow Medical Recommendations",
                description = "Follow the recommendations from your medical report",
                timeBlock = TimeBlock.MORNING,
                time = "9:00 AM",
                category = TaskCategory.GENERAL,
                priority = Priority.MEDIUM,
                isActive = true,
                createdAt = System.currentTimeMillis()
            ))
        }

        return fallbackTasks
    }
}
