package com.niteshray.xapps.healthforge.feature.home.data.models

import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Priority
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TaskCategory
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TimeBlock

/**
 * Firestore data classes for syncing task tracking data
 */
data class FirestoreTaskTemplate(
    val id: String = "", // Firestore document ID
    val localTemplateId: Int = 0,
    val title: String = "",
    val description: String = "",
    val timeBlock: String = "",
    val time: String = "",
    val category: String = "",
    val priority: String = "MEDIUM",
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class FirestoreDailyRecord(
    val id: String = "", // Firestore document ID
    val localRecordId: Int = 0,
    val templateFirestoreId: String = "", // Reference to FirestoreTaskTemplate
    val localTemplateId: Int = 0, // For local reference
    val date: String = "",
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val syncedAt: Long = 0L
)

/**
 * Extension functions for conversions
 */
fun TaskTemplate.toFirestoreTemplate(): FirestoreTaskTemplate {
    return FirestoreTaskTemplate(
        id = firestoreId ?: "",
        localTemplateId = id,
        title = title,
        description = description,
        timeBlock = timeBlock.name,
        time = time,
        category = category.name,
        priority = priority.name,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )
}

fun FirestoreTaskTemplate.toTaskTemplate(): TaskTemplate {
    return TaskTemplate(
        id = localTemplateId,
        title = title,
        description = description,
        timeBlock = TimeBlock.valueOf(timeBlock),
        time = time,
        category = TaskCategory.valueOf(category),
        priority = Priority.valueOf(priority),
        isActive = isActive,
        createdAt = createdAt,
        firestoreId = id
    )
}

fun DailyTaskRecord.toFirestoreRecord(templateFirestoreId: String): FirestoreDailyRecord {
    return FirestoreDailyRecord(
        id = firestoreId ?: "",
        localRecordId = id,
        templateFirestoreId = templateFirestoreId,
        localTemplateId = templateId,
        date = date,
        isCompleted = isCompleted,
        completedAt = completedAt,
        syncedAt = System.currentTimeMillis()
    )
}

fun FirestoreDailyRecord.toDailyTaskRecord(): DailyTaskRecord {
    return DailyTaskRecord(
        id = localRecordId,
        templateId = localTemplateId,
        date = date,
        isCompleted = isCompleted,
        completedAt = completedAt,
        firestoreId = id
    )
}