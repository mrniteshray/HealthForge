package com.niteshray.xapps.healthforge.feature.auth.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.niteshray.xapps.healthforge.core.di.DataStore
import com.niteshray.xapps.healthforge.core.di.PreferenceKey
import com.niteshray.xapps.healthforge.feature.auth.domain.repo.AuthRepository
import com.niteshray.xapps.healthforge.feature.auth.domain.repo.UserRepository
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.UserBasicHealthInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null,
    val token: String? = null,
    val SetupSuccess : Boolean = false,
    val isSetupComplete: Boolean = false,
    val isSetupLoading: Boolean = false,
    val userRole: String? = null // "patient" or "doctor"
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
            val savedToken = prefStore.getString(PreferenceKey.AUTH_TOKEN).first()
            val currentUser = FirebaseAuth.getInstance().currentUser
            _authToken.value = currentUser?.uid ?: savedToken
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            authState = authState.copy(
                isLoading = true,
                errorMessage = null,
                userRole = null
            )

            try {
                val firebaseSignInSuccess = authRepository.signInWithEmail(email, password)
                
                if (firebaseSignInSuccess) {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    
                    if (currentUser != null) {
                        val userRole = authRepository.getUserRole(currentUser.uid)
                        val token = currentUser.uid
                        
                        authState = authState.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            token = token,
                            userRole = userRole,
                            errorMessage = null
                        )
                        
                        // Save token to preferences
                        prefStore.saveString(PreferenceKey.AUTH_TOKEN, token)
                        _authToken.value = token
                    } else {
                        authState = authState.copy(
                            isLoading = false,
                            errorMessage = "Authentication failed. Please try again."
                        )
                    }
                } else {
                    authState = authState.copy(
                        isLoading = false,
                        errorMessage = "Invalid email or password. Please try again."
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
                errorMessage = null,
                userRole = null
            )

            try {
                val firebaseSignUpSuccess = authRepository.signUpWithEmail(email, password, name)
                
                if (firebaseSignUpSuccess) {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    
                    if (currentUser != null) {
                        val token = currentUser.uid
                        
                        authState = authState.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            token = token,
                            userRole = "patient", // Default role for regular registration
                            errorMessage = null
                        )
                        
                        prefStore.saveString(PreferenceKey.AUTH_TOKEN, token)
                        _authToken.value = token
                    } else {
                        authState = authState.copy(
                            isLoading = false,
                            errorMessage = "Registration failed. Please try again."
                        )
                    }
                } else {
                    authState = authState.copy(
                        isLoading = false,
                        errorMessage = "Registration failed. Please check your details and try again."
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
                userRepo.saveUserHealthInfo(userBasicHealthInfo)

                authState = authState.copy(
                    isSetupLoading = false,
                    isSetupComplete = true,
                    SetupSuccess = true,
                    errorMessage = null
                )
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

    fun checkUserRoleAndNavigate() {
        viewModelScope.launch {
            try {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val userRole = authRepository.getUserRole(currentUser.uid)
                    authState = authState.copy(
                        userRole = userRole,
                        isAuthenticated = true,
                        token = currentUser.uid
                    )
                    _authToken.value = currentUser.uid
                }
            } catch (e: Exception) {
                // If there's an error checking role, logout
                performLogout()
            }
        }
    }
}
