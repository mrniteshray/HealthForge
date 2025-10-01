package com.niteshray.xapps.healthforge.feature.home.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Priority
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TaskCategory
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TimeBlock

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val timeBlock: TimeBlock,
    val time: String,
    val category: TaskCategory,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM
)