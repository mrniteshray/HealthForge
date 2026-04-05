package com.niteshray.xapps.healthforge.feature.home.data.models

import androidx.room.Embedded
import androidx.room.Relation

/**
 * TaskWithDailyRecord represents a TaskTemplate with its corresponding DailyTaskRecord for a specific date.
 * This is used for displaying tasks in the UI with their completion status.
 */
data class TaskWithDailyRecord(
    @Embedded val template: TaskTemplate,
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val dailyRecord: DailyTaskRecord?
) {
    // Helper properties for UI compatibility
    val isCompleted: Boolean get() = dailyRecord?.isCompleted ?: false
    val completedAt: Long? get() = dailyRecord?.completedAt
    
    /**
     * Convert to the existing Task format for backward compatibility
     */
    fun toTask(): Task {
        return Task(
            id = template.id,
            title = template.title,
            description = template.description,
            timeBlock = template.timeBlock,
            time = template.time,
            category = template.category,
            isCompleted = isCompleted,
            priority = template.priority
        )
    }
}