package com.niteshray.xapps.healthforge.feature.home.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Priority
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TaskCategory
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TimeBlock

/**
 * TaskTemplate represents a reusable task definition that gets created once
 * and generates daily records for tracking completion.
 */
@Entity(tableName = "task_templates")
data class TaskTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val timeBlock: TimeBlock,
    val time: String, // Format: "9:00 AM"
    val category: TaskCategory,
    val priority: Priority = Priority.MEDIUM,
    val isActive: Boolean = true, // Whether this template should generate daily records
    val createdAt: Long = System.currentTimeMillis(), // Creation timestamp
    val firestoreId: String? = null // For syncing with Firestore
)