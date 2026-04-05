package com.niteshray.xapps.healthforge.feature.careconnect.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.niteshray.xapps.healthforge.feature.careconnect.data.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class CareConnectUiState(
    val isLoading: Boolean = false,
    val guardians: List<Guardian> = emptyList(),
    val guardees: List<Guardee> = emptyList(),
    val pendingRequests: List<GuardianRequest> = emptyList(),
    val stats: CareConnectStats = CareConnectStats(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showAddGuardianDialog: Boolean = false,
    val selectedTab: CareConnectTab = CareConnectTab.OVERVIEW,
    val currentUserId: String? = null
)

enum class CareConnectTab(val displayName: String) {
    OVERVIEW("Overview"),
    GUARDIANS("My Guardians"),
    GUARDEES("I'm Guarding"),
    REQUESTS("Requests")
}

@HiltViewModel
class CareConnectViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CareConnectUiState())
    val uiState: StateFlow<CareConnectUiState> = _uiState.asStateFlow()

    private val currentUserId get() = firebaseAuth.currentUser?.uid
    private val currentUserEmail get() = firebaseAuth.currentUser?.email

    // Real-time listeners
    private var guardiansListener: ListenerRegistration? = null
    private var guardeesListener: ListenerRegistration? = null
    private var pendingRequestsListener: ListenerRegistration? = null

    init {
        setupRealtimeListeners()
    }

    private fun setupRealtimeListeners() {
        currentUserId?.let { userId ->
            currentUserEmail?.let { email ->
                _uiState.value = _uiState.value.copy(
                    isLoading = true, 
                    errorMessage = null,
                    currentUserId = userId
                )
                setupGuardiansListener(userId)
                setupGuardeesListener(userId)
                setupPendingRequestsListener(email)
            }
        }
    }


    // Setup real-time listener for guardians (people who can monitor current user)
    private fun setupGuardiansListener(userId: String) {
        guardiansListener = firestore
            .collection("guardianConnections")
            .whereEqualTo("guardeeId", userId)
            .whereEqualTo("status", "ACCEPTED")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to load guardians"
                    )
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    viewModelScope.launch {
                        try {
                            val guardians = snapshot.documents.mapNotNull { doc ->
                                val guardianId = doc.getString("guardianId") ?: return@mapNotNull null
                                val guardianData = firestore.collection("users").document(guardianId).get().await()
                                
                                if (guardianData.exists()) {
                                    Guardian(
                                        id = guardianId,
                                        name = guardianData.getString("name") ?: "",
                                        email = guardianData.getString("email") ?: "",
                                        phoneNumber = guardianData.getString("phoneNumber") ?: "",
                                        relationship = GuardianRelationship.valueOf(
                                            doc.getString("relationship") ?: "OTHER"
                                        ),
                                        permissions = (doc.get("permissions") as? List<*>)?.mapNotNull { permission ->
                                            try { PermissionType.valueOf(permission.toString()) } catch (e: Exception) { null }
                                        } ?: emptyList(),
                                        isActive = doc.getBoolean("isActive") ?: true,
                                        addedAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                        lastActiveAt = doc.getLong("lastActiveAt")
                                    )
                                } else null
                            }

                            _uiState.value = _uiState.value.copy(
                                guardians = guardians,
                                isLoading = false,
                                errorMessage = null
                            )
                            updateStats()
                        } catch (e: Exception) {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = e.message ?: "Failed to process guardians data"
                            )
                        }
                    }
                }
            }
    }

    // Setup real-time listener for guardees (people current user is monitoring)  
    private fun setupGuardeesListener(userId: String) {
        guardeesListener = firestore
            .collection("guardianConnections")
            .whereEqualTo("guardianId", userId)
            .whereEqualTo("status", "ACCEPTED")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to load guardees"
                    )
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    viewModelScope.launch {
                        try {
                            val guardees = snapshot.documents.mapNotNull { doc ->
                                val guardeeId = doc.getString("guardeeId") ?: return@mapNotNull null
                                val guardeeData = firestore.collection("users").document(guardeeId).get().await()
                                
                                if (guardeeData.exists()) {
                                    Guardee(
                                        id = guardeeId,
                                        name = guardeeData.getString("name") ?: "",
                                        email = guardeeData.getString("email") ?: "",
                                        phoneNumber = guardeeData.getString("phoneNumber") ?: "",
                                        relationship = GuardianRelationship.valueOf(
                                            doc.getString("relationship") ?: "OTHER"
                                        ),
                                        permissionsGranted = (doc.get("permissions") as? List<*>)?.mapNotNull { permission ->
                                            try { PermissionType.valueOf(permission.toString()) } catch (e: Exception) { null }
                                        } ?: emptyList(),
                                        isActive = doc.getBoolean("isActive") ?: true,
                                        addedAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                        lastActiveAt = doc.getLong("lastActiveAt")
                                    )
                                } else null
                            }

                            _uiState.value = _uiState.value.copy(
                                guardees = guardees,
                                isLoading = false,
                                errorMessage = null
                            )
                            updateStats()
                        } catch (e: Exception) {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = e.message ?: "Failed to process guardees data"
                            )
                        }
                    }
                }
            }
    }

    // Setup real-time listener for pending guardian requests
    private fun setupPendingRequestsListener(userEmail: String) {
        pendingRequestsListener = firestore
            .collection("guardianRequests")
            .whereEqualTo("toUserEmail", userEmail)
            .whereEqualTo("status", RequestStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to load pending requests"
                    )
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val pendingRequests = snapshot.documents.mapNotNull { doc ->
                        try {
                            GuardianRequest(
                                id = doc.id,
                                fromUserId = doc.getString("fromUserId") ?: "",
                                toUserId = doc.getString("toUserId") ?: "",
                                fromUserName = doc.getString("fromUserName") ?: "",
                                fromUserEmail = doc.getString("fromUserEmail") ?: "",
                                toUserEmail = doc.getString("toUserEmail") ?: "",
                                requestedPermissions = (doc.get("requestedPermissions") as? List<*>)?.mapNotNull { permission ->
                                    try { PermissionType.valueOf(permission.toString()) } catch (e: Exception) { null }
                                } ?: emptyList(),
                                relationship = GuardianRelationship.valueOf(
                                    doc.getString("relationship") ?: "OTHER"
                                ),
                                message = doc.getString("message") ?: "",
                                status = RequestStatus.valueOf(doc.getString("status") ?: "PENDING"),
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                respondedAt = doc.getLong("respondedAt")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        pendingRequests = pendingRequests,
                        errorMessage = null
                    )
                    updateStats()
                }
            }
    }

    // Update stats based on loaded data
    private fun updateStats() {
        val currentState = _uiState.value
        val stats = CareConnectStats(
            totalGuardians = currentState.guardians.size,
            totalGuardees = currentState.guardees.size,
            activeConnections = currentState.guardians.size + currentState.guardees.size,
            pendingRequests = currentState.pendingRequests.size
        )
        _uiState.value = _uiState.value.copy(stats = stats)
    }

    // Main function to add guardian - checks if user exists and sends request
    fun addGuardian(
        email: String,
        relationship: GuardianRelationship,
        message: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, showAddGuardianDialog = false)
                
                val currentUserId = currentUserId ?: throw Exception("User not authenticated")
                val currentUserEmail = currentUserEmail ?: throw Exception("User email not found")
                val currentUserName = getCurrentUserName()

                // Check if target user exists in Firestore
                val targetUser = findUserByEmail(email)
                if (targetUser == null) {
                    throw Exception("User with email $email not found. They need to register first.")
                }

                // Check if connection already exists
                if (connectionExists(currentUserId, targetUser.id)) {
                    throw Exception("Connection already exists with this user")
                }

                // Create guardian request
                val requestId = createGuardianRequest(
                    fromUserId = currentUserId,
                    fromUserName = currentUserName,
                    fromUserEmail = currentUserEmail,
                    toUserId = targetUser.id,
                    toUserEmail = email,
                    relationship = relationship,
                    permissions = emptyList(), // No permissions required, just basic task analysis access
                    message = message
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showAddGuardianDialog = false,
                    successMessage = "Guardian request sent successfully!"
                )
                
                // Real-time listeners will automatically update the data

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to send guardian request"
                )
            }
        }
    }

    // Check if user exists by email
    private suspend fun findUserByEmail(email: String): UserData? {
        return try {
            val querySnapshot = firestore
                .collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (querySnapshot.documents.isNotEmpty()) {
                val doc = querySnapshot.documents.first()
                UserData(
                    id = doc.id,
                    email = doc.getString("email") ?: "",
                    name = doc.getString("name") ?: ""
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // Check if connection already exists between two users
    private suspend fun connectionExists(userId1: String, userId2: String): Boolean {
        return try {
            val connection1 = firestore
                .collection("guardianConnections")
                .whereEqualTo("guardianId", userId1)
                .whereEqualTo("guardeeId", userId2)
                .get()
                .await()

            val connection2 = firestore
                .collection("guardianConnections")
                .whereEqualTo("guardianId", userId2)
                .whereEqualTo("guardeeId", userId1)
                .get()
                .await()

            connection1.documents.isNotEmpty() || connection2.documents.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    // Get current user's name
    private suspend fun getCurrentUserName(): String {
        return try {
            currentUserId?.let { userId ->
                val userDoc = firestore.collection("users").document(userId).get().await()
                userDoc.getString("name") ?: "Unknown User"
            } ?: "Unknown User"
        } catch (e: Exception) {
            "Unknown User"
        }
    }

    // Create guardian request in Firestore
    private suspend fun createGuardianRequest(
        fromUserId: String,
        fromUserName: String,
        fromUserEmail: String,
        toUserId: String,
        toUserEmail: String,
        relationship: GuardianRelationship,
        permissions: List<PermissionType>,
        message: String
    ): String {
        val requestData = mapOf(
            "fromUserId" to fromUserId,
            "fromUserName" to fromUserName,
            "fromUserEmail" to fromUserEmail,
            "toUserId" to toUserId,
            "toUserEmail" to toUserEmail,
            "relationship" to relationship.name,
            "requestedPermissions" to permissions.map { it.name },
            "message" to message,
            "status" to RequestStatus.PENDING.name,
            "createdAt" to System.currentTimeMillis(),
            "respondedAt" to null
        )

        // Store request in target user's requests subcollection (so they can see it)
        val requestRef = firestore
            .collection("users")
            .document(toUserId)
            .collection("requests")
            .add(requestData)
            .await()

        // Also store in global requests collection for easier management
        firestore
            .collection("guardianRequests")
            .document(requestRef.id)
            .set(requestData.plus("id" to requestRef.id))
            .await()

        return requestRef.id
    }

    // Accept guardian request
    fun acceptGuardianRequest(requestId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val currentUserId = currentUserId ?: throw Exception("User not authenticated")
                
                // Get request details
                val requestDoc = firestore
                    .collection("guardianRequests")
                    .document(requestId)
                    .get()
                    .await()

                if (!requestDoc.exists()) {
                    throw Exception("Request not found")
                }

                val fromUserId = requestDoc.getString("fromUserId") ?: throw Exception("Invalid request")
                val relationship = requestDoc.getString("relationship") ?: "OTHER"
                val permissions = requestDoc.get("requestedPermissions") as? List<*> ?: null

                // Create guardian connection
                // Note: fromUserId sent request asking currentUserId to be their guardian
                // So currentUserId becomes guardian, fromUserId becomes guardee
                val connectionData = mapOf(
                    "guardianId" to currentUserId,  // Person who accepted becomes guardian
                    "guardeeId" to fromUserId,      // Person who sent request becomes guardee
                    "relationship" to relationship,
                    "permissions" to permissions,
                    "status" to "ACCEPTED",
                    "isActive" to true,
                    "createdAt" to System.currentTimeMillis(),
                    "lastActiveAt" to System.currentTimeMillis()
                )

                firestore
                    .collection("guardianConnections")
                    .add(connectionData)
                    .await()

                // Update request status
                firestore
                    .collection("guardianRequests")
                    .document(requestId)
                    .update(
                        mapOf(
                            "status" to RequestStatus.ACCEPTED.name,
                            "respondedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()

                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to accept request"
                )
            }
        }
    }

    // Reject guardian request
    fun rejectGuardianRequest(requestId: String) {
        viewModelScope.launch {
            try {
                // Update request status
                firestore
                    .collection("guardianRequests")
                    .document(requestId)
                    .update(
                        mapOf(
                            "status" to RequestStatus.REJECTED.name,
                            "respondedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to reject request"
                )
            }
        }
    }

    // UI State Management Functions
    fun selectTab(tab: CareConnectTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun showAddGuardianDialog() {
        _uiState.value = _uiState.value.copy(showAddGuardianDialog = true)
    }

    fun hideAddGuardianDialog() {
        _uiState.value = _uiState.value.copy(showAddGuardianDialog = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun refreshData() {
        // Remove existing listeners and setup new ones
        guardiansListener?.remove()
        guardeesListener?.remove()
        pendingRequestsListener?.remove()
        
        // Re-setup listeners
        setupRealtimeListeners()
    }

    // Remove guardian connection
    fun removeGuardian(guardianId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = currentUserId ?: return@launch
                
                // Find and delete guardian connection
                val connections = firestore
                    .collection("guardianConnections")
                    .whereEqualTo("guardianId", guardianId)
                    .whereEqualTo("guardeeId", currentUserId)
                    .get()
                    .await()

                connections.documents.forEach { doc ->
                    doc.reference.delete().await()
                }

                // Real-time listeners will automatically update the data

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to remove guardian"
                )
            }
        }
    }

    // Remove guardee connection
    fun removeGuardee(guardeeId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = currentUserId ?: return@launch
                
                // Find and delete guardee connection
                val connections = firestore
                    .collection("guardianConnections")
                    .whereEqualTo("guardianId", currentUserId)
                    .whereEqualTo("guardeeId", guardeeId)
                    .get()
                    .await()

                connections.documents.forEach { doc ->
                    doc.reference.delete().await()
                }

                // Real-time listeners will automatically update the data

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to remove guardee"
                )
            }
        }
    }

    // Data class for user lookup
    private data class UserData(
        val id: String,
        val email: String,
        val name: String
    )

    override fun onCleared() {
        super.onCleared()
        guardiansListener?.remove()
        guardeesListener?.remove()
        pendingRequestsListener?.remove()
    }
}