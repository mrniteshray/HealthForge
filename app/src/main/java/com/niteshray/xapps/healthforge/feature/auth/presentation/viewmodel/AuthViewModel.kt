package com.niteshray.xapps.healthforge.feature.auth.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niteshray.xapps.healthforge.core.dataStore.DataStore
import com.niteshray.xapps.healthforge.core.dataStore.PreferenceKey
import com.niteshray.xapps.healthforge.feature.auth.domain.model.RegisterUser
import com.niteshray.xapps.healthforge.feature.auth.domain.model.loginUser
import com.niteshray.xapps.healthforge.feature.auth.domain.repo.AuthRepository
import com.niteshray.xapps.healthforge.feature.auth.domain.repo.UserRepository
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.UserBasicHealthInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
//    val user: com.yourpackage.data.model.User? = null,
    val errorMessage: String? = null,
    val token: String? = null,
    val SetupSuccess : Boolean = false,
    val isSetupComplete: Boolean = false,
    val isSetupLoading: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefStore : DataStore,
    private val userRepo : UserRepository
) : ViewModel() {

    var authState by mutableStateOf(AuthState())
        private set

    private val _authToken = MutableStateFlow<String>("")
    val authtoken get() = _authToken

    init {
        viewModelScope.launch {
            _authToken.value = prefStore.getString(PreferenceKey.AUTH_TOKEN).first()
        }
    }

    fun LoginUser(email: String, password: String) {
        viewModelScope.launch {
            authState = authState.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val response = authRepository.loginUser(
                    loginUser(email = email, password = password)
                )
                authRepository.SignInWithEmail(email,password)
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    if (authResponse.success) {
                        authState = authState.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            token = authResponse.token,
                            errorMessage = null
                        )

                        prefStore.saveString(PreferenceKey.AUTH_TOKEN , authResponse.token)
                    } else {
                        authState = authState.copy(
                            isLoading = false
                        )
                    }
                } else {
                    authState = authState.copy(
                        isLoading = false,
                        errorMessage = "Login failed. Please try again."
                    )
                }
            } catch (e: Exception) {
                authState = authState.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun registerUser(name: String, email: String, password: String) {
        viewModelScope.launch {
            authState = authState.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val response = authRepository.registerUser(
                    RegisterUser(name = name, email = email, password = password)
                )
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    if (authResponse.success) {
                        authState = authState.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            token = authResponse.token,
                            errorMessage = null
                        )
                        prefStore.saveString(PreferenceKey.AUTH_TOKEN , authResponse.token)
                        authRepository.SignUpWithEmail(email, password, name)
                    } else {
                        authState = authState.copy(
                            isLoading = false,
                        )
                    }
                } else {
                    authState = authState.copy(
                        isLoading = false,
                        errorMessage = "Registration failed. Please try again."
                    )
                }
            } catch (e: Exception) {
                authState = authState.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun saveHealthInfo(userBasicHealthInfo: UserBasicHealthInfo){
        viewModelScope.launch {
            authState = authState.copy(
                isSetupLoading = true,
                errorMessage = null,
                isSetupComplete = false
            )
            
            try {
                val response = authRepository.saveHealthInfo(_authToken.value, userBasicHealthInfo)
                if (response.isSuccessful){
                    // Save locally only if API call was successful
                    userRepo.saveUserHealthInfo(userBasicHealthInfo)
                    
                    authState = authState.copy(
                        isSetupLoading = false,
                        isSetupComplete = true,
                        SetupSuccess = true,
                        errorMessage = null
                    )
                } else {
                    authState = authState.copy(
                        isSetupLoading = false,
                        isSetupComplete = false,
                        SetupSuccess = false,
                        errorMessage = "Failed to save health information. Please try again."
                    )
                }
            } catch (e: Exception) {
                authState = authState.copy(
                    isSetupLoading = false,
                    isSetupComplete = false,
                    SetupSuccess = false,
                    errorMessage = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun clearError() {
        authState = authState.copy(errorMessage = null)
    }

    fun resetSetupState() {
        authState = authState.copy(isSetupComplete = false, SetupSuccess = false)
    }

    fun logout() {
        authState = AuthState()
    }

    fun performLogout() {
        viewModelScope.launch {
            try {
                // Clear Firebase authentication
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                
                // Clear AuthToken from DataStore
                prefStore.remove(PreferenceKey.AUTH_TOKEN)
                
                // Clear other user-related data
                prefStore.remove(PreferenceKey.USER_ID)
                prefStore.remove(PreferenceKey.USER_EMAIL)
                prefStore.remove(PreferenceKey.USER_NAME)
                prefStore.saveBoolean(PreferenceKey.IS_LOGGED_IN, false)
                
                // Reset auth state
                authState = AuthState()
                _authToken.value = ""
                
            } catch (e: Exception) {
                // Handle logout error if needed
                authState = authState.copy(
                    errorMessage = "Error during logout: ${e.message}"
                )
            }
        }
    }
}
