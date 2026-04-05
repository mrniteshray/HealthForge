package com.niteshray.xapps.healthforge.feature.careconnect.data.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data models for CareConnect feature
 */
data class Guardian(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val relationship: GuardianRelationship = GuardianRelationship.FAMILY,
    val permissions: List<PermissionType> = emptyList(),
    val isActive: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long? = null,
    val profileImageUrl: String? = null
)

data class Guardee(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val relationship: GuardianRelationship = GuardianRelationship.FAMILY,
    val permissionsGranted: List<PermissionType> = emptyList(),
    val isActive: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long? = null,
    val profileImageUrl: String? = null
)

enum class GuardianRelationship(
    val displayName: String,
    val icon: ImageVector
) {
    FAMILY("Family Member", Icons.Filled.FamilyRestroom),
    PARENT("Parent", Icons.Filled.Person),
    SPOUSE("Spouse/Partner", Icons.Filled.Favorite),
    CHILD("Child", Icons.Filled.ChildCare),
    FRIEND("Friend", Icons.Filled.People),
    DOCTOR("Doctor", Icons.Filled.MedicalServices),
    CAREGIVER("Caregiver", Icons.Filled.SupportAgent),
    EMERGENCY("Emergency Contact", Icons.Filled.Emergency),
    OTHER("Other", Icons.Filled.Person)
}

enum class PermissionType(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val isHighSensitive: Boolean = false
) {
    VIEW_HEALTH_SUMMARY("Health Summary", "View basic health metrics and summaries", Icons.Filled.HealthAndSafety),
    VIEW_TASK_PROGRESS("Task Progress", "View daily task completion and progress", Icons.Filled.Assignment),
    VIEW_DIET_PLANS("Diet Plans", "View diet plans and nutrition information", Icons.Filled.Restaurant),
    VIEW_MEDICAL_REPORTS("Medical Reports", "View uploaded medical documents", Icons.Filled.Description, true),
    VIEW_ANALYTICS("Analytics", "View detailed health analytics and trends", Icons.Filled.Analytics),
    RECEIVE_ALERTS("Emergency Alerts", "Receive notifications for health emergencies", Icons.Filled.Notifications, true),
    MANAGE_TASKS("Manage Tasks", "Add, edit, and assign health tasks", Icons.Filled.EditNote),
    FULL_ACCESS("Full Access", "Complete access to all health data", Icons.Filled.AdminPanelSettings, true)
}

data class GuardianRequest(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromUserName: String = "",
    val fromUserEmail: String = "",
    val toUserEmail: String = "",
    val requestedPermissions: List<PermissionType> = emptyList(),
    val relationship: GuardianRelationship = GuardianRelationship.OTHER,
    val message: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val respondedAt: Long? = null
)

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED
}

data class CareConnectStats(
    val totalGuardians: Int = 0,
    val totalGuardees: Int = 0,
    val activeConnections: Int = 0,
    val pendingRequests: Int = 0,
    val recentActivity: List<ActivityItem> = emptyList()
)

data class ActivityItem(
    val id: String = "",
    val type: ActivityType = ActivityType.GUARDIAN_ADDED,
    val title: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val icon: ImageVector = Icons.Filled.Info
)

enum class ActivityType(
    val displayName: String,
    val icon: ImageVector
) {
    GUARDIAN_ADDED("Guardian Added", Icons.Filled.PersonAdd),
    GUARDIAN_REMOVED("Guardian Removed", Icons.Filled.PersonRemove),
    GUARDEE_ADDED("Guardee Added", Icons.Filled.GroupAdd),
    GUARDEE_REMOVED("Guardee Removed", Icons.Filled.GroupRemove),
    PERMISSION_GRANTED("Permission Granted", Icons.Filled.Security),
    PERMISSION_REVOKED("Permission Revoked", Icons.Filled.Block),
    ALERT_SENT("Alert Sent", Icons.Filled.NotificationImportant),
    DATA_ACCESSED("Data Accessed", Icons.Filled.Visibility)
}