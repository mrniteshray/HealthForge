package com.niteshray.xapps.healthforge.feature.notifications.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.niteshray.xapps.healthforge.feature.careconnect.data.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class NotificationUiState(
    val pendingRequests: List<GuardianRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showNotificationDialog: Boolean = false
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    // Computed property for notification badge count
    val notificationCount: StateFlow<Int> = _uiState.map { it.pendingRequests.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    private var pendingRequestsListener: ListenerRegistration? = null
    private val currentUserId get() = firebaseAuth.currentUser?.uid
    private val currentUserEmail get() = firebaseAuth.currentUser?.email

    init {
        setupRealtimeListener()
    }

    private fun setupRealtimeListener() {
        currentUserId?.let { userId ->
            currentUserEmail?.let { email ->
                // Listen for real-time changes to pending guardian requests
                pendingRequestsListener = firestore
                    .collection("guardianRequests")
                    .whereEqualTo("toUserEmail", email)
                    .whereEqualTo("status", "PENDING")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _uiState.value = _uiState.value.copy(
                                error = error.message ?: "Failed to load notifications"
                            )
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val pendingRequests = snapshot.documents.mapNotNull { doc ->
                                try {
                                    GuardianRequest(
                                        id = doc.id,
                                        fromUserId = doc.getString("fromUserId") ?: "",
                                        toUserId = userId,
                                        fromUserName = doc.getString("fromUserName") ?: "",
                                        fromUserEmail = doc.getString("fromUserEmail") ?: "",
                                        toUserEmail = email,
                                        requestedPermissions = (doc.get("requestedPermissions") as? List<*>)?.mapNotNull { permission ->
                                            try { 
                                                PermissionType.valueOf(permission.toString()) 
                                            } catch (e: Exception) { null }
                                        } ?: emptyList(),
                                        relationship = GuardianRelationship.valueOf(
                                            doc.getString("relationship") ?: "OTHER"
                                        ),
                                        message = doc.getString("message") ?: "",
                                        status = RequestStatus.valueOf(
                                            doc.getString("status") ?: "PENDING"
                                        ),
                                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                        respondedAt = doc.getLong("respondedAt")
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            _uiState.value = _uiState.value.copy(
                                pendingRequests = pendingRequests,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            }
        }
    }

    fun showNotificationDialog() {
        _uiState.value = _uiState.value.copy(showNotificationDialog = true)
    }

    fun hideNotificationDialog() {
        _uiState.value = _uiState.value.copy(showNotificationDialog = false)
    }

    fun acceptGuardianRequest(requestId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val request = _uiState.value.pendingRequests.find { it.id == requestId }
                if (request == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Request not found"
                    )
                    return@launch
                }

                currentUserId?.let { userId ->
                    // Create guardian connection
                    // Note: request.fromUserId sent request asking current user to be their guardian  
                    // So current user becomes guardian, fromUserId becomes guardee
                    val connectionData = mapOf(
                        "guardianId" to userId,          // Person who accepted becomes guardian
                        "guardeeId" to request.fromUserId,  // Person who sent request becomes guardee
                        "relationship" to request.relationship.name,
                        "permissions" to request.requestedPermissions.map { it.name },
                        "status" to "ACCEPTED",
                        "isActive" to true,
                        "createdAt" to System.currentTimeMillis(),
                        "lastActiveAt" to System.currentTimeMillis()
                    )

                    // Add connection
                    firestore.collection("guardianConnections").add(connectionData).await()

                    // Update request status
                    firestore.collection("guardianRequests")
                        .document(requestId)
                        .update(
                            mapOf(
                                "status" to "ACCEPTED",
                                "respondedAt" to System.currentTimeMillis()
                            )
                        ).await()

                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to accept request"
                )
            }
        }
    }

    fun rejectGuardianRequest(requestId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Update request status to rejected
                firestore.collection("guardianRequests")
                    .document(requestId)
                    .update(
                        mapOf(
                            "status" to "REJECTED",
                            "respondedAt" to System.currentTimeMillis()
                        )
                    ).await()

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to reject request"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        pendingRequestsListener?.remove()
    }
}