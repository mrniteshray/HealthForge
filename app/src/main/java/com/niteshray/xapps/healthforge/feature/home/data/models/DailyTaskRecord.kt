package com.niteshray.xapps.healthforge.feature.home.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Calendar

/**
 * DailyTaskRecord represents a daily instance of a TaskTemplate.
 * Each task template generates a new record every day for tracking completion.
 */
@Entity(
    tableName = "daily_task_records",
    foreignKeys = [
        ForeignKey(
            entity = TaskTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["date"]),
        Index(value = ["templateId", "date"], unique = true) // One record per template per day
    ]
)
data class DailyTaskRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val templateId: Int, // Reference to TaskTemplate
    val date: String, // Format: "2025-10-06" (YYYY-MM-DD)
    val isCompleted: Boolean = false,
    val completedAt: Long? = null, // Timestamp when task was marked complete
    val firestoreId: String? = null // For syncing with Firestore
) {
    companion object {
        /**
         * Generate today's date string in YYYY-MM-DD format
         */
        fun getTodayDateString(): String {
            val calendar = Calendar.getInstance()
            return String.format(
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
        
        /**
         * Generate date string for a specific calendar instance
         */
        fun getDateString(calendar: Calendar): String {
            return String.format(
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
}