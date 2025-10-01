package com.niteshray.xapps.healthforge.feature.home.presentation.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.niteshray.xapps.healthforge.feature.home.data.models.Task as DataTask

// Convert data model Task to presentation Task (with icon)
fun DataTask.toPresentationTask(): Task {
    return Task(
        id = this.id,
        title = this.title,
        description = this.description,
        timeBlock = this.timeBlock,
        time = this.time,
        category = this.category,
        isCompleted = this.isCompleted,
        icon = getCategoryIcon(this.category),
        priority = this.priority
    )
}

// Convert presentation Task to data model Task (without icon)
fun Task.toDataTask(): DataTask {
    return DataTask(
        id = this.id,
        title = this.title,
        description = this.description,
        timeBlock = this.timeBlock,
        time = this.time,
        category = this.category,
        isCompleted = this.isCompleted,
        priority = this.priority
    )
}

// Helper function to get category icon
private fun getCategoryIcon(category: TaskCategory): ImageVector {
    return when (category) {
        TaskCategory.MEDICATION -> Icons.Filled.Medication
        TaskCategory.EXERCISE -> Icons.Filled.FitnessCenter
        TaskCategory.DIET -> Icons.Filled.Restaurant
        TaskCategory.MONITORING -> Icons.Filled.Monitor
        TaskCategory.LIFESTYLE -> Icons.Filled.SelfImprovement
        TaskCategory.GENERAL -> Icons.Filled.Task
    }
}

// Presentation layer Task data class (with icon)
data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val timeBlock: TimeBlock,
    val time: String,
    val category: TaskCategory,
    val isCompleted: Boolean = false,
    val icon: ImageVector,
    val priority: Priority = Priority.MEDIUM
)