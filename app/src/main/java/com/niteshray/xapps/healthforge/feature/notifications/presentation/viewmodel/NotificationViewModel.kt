package com.niteshray.xapps.healthforge.feature.notifications.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class NotificationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showNotificationDialog: Boolean = false
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    // Reserved for future non-guardian notifications.
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    // CareGuardian is removed, so notification badge is always hidden for now.
    val notificationCount: StateFlow<Int> = _uiState.map { 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    fun showNotificationDialog() {
        _uiState.value = _uiState.value.copy(showNotificationDialog = true)
    }

    fun hideNotificationDialog() {
        _uiState.value = _uiState.value.copy(showNotificationDialog = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}