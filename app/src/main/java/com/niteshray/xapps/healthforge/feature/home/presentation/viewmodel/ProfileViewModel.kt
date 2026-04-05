package com.niteshray.xapps.healthforge.feature.home.presentation.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.niteshray.xapps.healthforge.feature.auth.domain.repo.UserRepository
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfileData? = null,
    val userHealthInfo: UserBasicHealthInfo? = null,
    val errorMessage: String? = null
)

data class UserProfileData(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val createdAt: Long = 0L
)


@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    // Load basic user profile
                    val userProfile = getUserBasicProfile(currentUser.uid)

                    // Load health information
                    val healthInfo = userRepository.getUserHealthInfo()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userProfile = userProfile,
                        userHealthInfo = healthInfo,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "User not authenticated"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load profile"
                )
            }
        }
    }

    private suspend fun getUserBasicProfile(uid: String): UserProfileData? {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            if (document.exists()) {
                UserProfileData(
                    uid = document.getString("uid") ?: "",
                    name = document.getString("name") ?: "",
                    email = document.getString("email") ?: "",
                    role = document.getString("role") ?: "",
                    createdAt = document.getLong("createdAt") ?: 0L
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun calculateBMI(weight: String, height: String): String {
        return try {
            val weightKg = weight.toDoubleOrNull() ?: return "N/A"
            val heightCm = height.toDoubleOrNull() ?: return "N/A"
            val heightM = heightCm / 100.0
            val bmi = weightKg / (heightM * heightM)
            String.format("%.1f", bmi)
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun getBMICategory(bmi: String): String {
        return try {
            val bmiValue = bmi.toDouble()
            when {
                bmiValue < 18.5 -> "Underweight"
                bmiValue < 25.0 -> "Normal"
                bmiValue < 30.0 -> "Overweight"
                else -> "Obese"
            }
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun refreshProfile() {
        loadUserProfile()
    }

    // Edit functionality
    fun updateUserProfile(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    firestore.collection("users")
                        .document(currentUser.uid)
                        .update("name", name)
                        .await()

                    // Refresh profile after update
                    loadUserProfile()
                    onSuccess()
                } else {
                    onError("User not authenticated")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update profile")
            }
        }
    }

    fun updateHealthInfo(
        weight: String,
        height: String,
        age: String,
        gender: Gender,
        bloodType: BloodType,
        allergies: String,
        medicalCondition: MedicalCondition,
        emergencyContact: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    val healthData = mapOf(
                        "weight" to weight,
                        "height" to height,
                        "age" to age,
                        "gender" to gender,
                        "bloodType" to bloodType,
                        "allergies" to allergies,
                        "medicalCondition" to medicalCondition,
                        "emergencyContact" to emergencyContact,
                        "updatedAt" to System.currentTimeMillis()
                    )

                    firestore.collection("userHealthInfo")
                        .document(currentUser.uid)
                        .set(healthData)
                        .await()

                    // Also update backend
                    userRepository.saveUserHealthInfo(
                        UserBasicHealthInfo(
                            weight = weight,
                            height = height,
                            age = age,
                            gender = gender,
                            bloodType = bloodType,
                            allergies = allergies,
                            medicalCondition = medicalCondition,
                            emergencyContact = emergencyContact
                        )
                    )

                    // Refresh profile after update
                    loadUserProfile()
                    onSuccess()
                } else {
                    onError("User not authenticated")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update health information")
            }
        }
    }
}