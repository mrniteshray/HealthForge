package com.niteshray.xapps.healthforge.feature.home.presentation.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

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

data class HealthMetric(
    val title: String,
    val value: String,
    val unit: String,
    val trend: Trend,
    val icon: ImageVector,
    val color: Color
)

data class MedicalReport(
    val id: Int,
    val name: String,
    val uploadDate: String,
    val type: ReportType,
    val status: ReportStatus
)

data class Medication(
    val id: Int,
    val name: String,
    val instructions: String = "",
    val times: List<MedicationTime>,
    val startDate: String,
    val endDate: String? = null,
    val isActive: Boolean = true
)

data class MedicationTime(
    val hour: Int,
    val minute: Int,
    val timeBlock: TimeBlock,
    val displayTime: String // 24-hour format for AlarmManager
) {
    // Convert to milliseconds for AlarmManager
    fun toMillis(): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
        }
        return calendar.timeInMillis
    }

    // 12-hour format for user display
    fun get12HourFormat(): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return String.format("%d:%02d %s", displayHour, minute, period)
    }
}



enum class TimeBlock(val displayName: String, val icon: ImageVector) {
    MORNING("Morning", Icons.Filled.WbSunny),
    AFTERNOON("Afternoon", Icons.Filled.LightMode),
    EVENING("Evening", Icons.Filled.Brightness3),
    NIGHT("Night", Icons.Filled.Bedtime)
}


enum class TaskCategory(val displayName: String, val color: Color) {
    MEDICATION("Medication", Color(0xFF2196F3)),
    EXERCISE("Exercise", Color(0xFF4CAF50)),
    DIET("Diet", Color(0xFFFF9800)),
    MONITORING("Monitoring", Color(0xFF9C27B0)),
    LIFESTYLE("Lifestyle", Color(0xFF00BCD4)),
    GENERAL("General", Color(0xFF607D8B))
}

enum class Priority(val displayName: String, val color: Color) {
    HIGH("High", Color(0xFFE53E3E)),
    MEDIUM("Medium", Color(0xFFED8936)),
    LOW("Low", Color(0xFF38A169))
}

enum class Trend { UP, DOWN, STABLE }

enum class ReportType(val displayName: String, val icon: ImageVector) {
    BLOOD_TEST("Blood Test", Icons.Filled.Bloodtype),
    XRAY("X-Ray", Icons.Filled.MedicalServices),
    MRI("MRI Scan", Icons.Filled.Scanner),
    ECG("ECG Report", Icons.Filled.MonitorHeart),
    PRESCRIPTION("Prescription", Icons.Filled.Receipt),
    OTHER("Other", Icons.Filled.Description)
}

enum class ReportStatus { PROCESSING, ANALYZED, PENDING }

enum class FrequencyType(val displayName: String) {
    ONCE_DAILY("Once daily"),
    TWICE_DAILY("Twice daily"),
    THREE_TIMES("3 times daily"),
    FOUR_TIMES("4 times daily"),
    CUSTOM("Custom times")
}