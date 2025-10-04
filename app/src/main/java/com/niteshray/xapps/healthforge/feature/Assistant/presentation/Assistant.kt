package com.niteshray.xapps.healthforge.feature.Assistant.presentation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.niteshray.xapps.healthforge.feature.Assistant.presentation.compose.AssistantContent
import com.niteshray.xapps.healthforge.feature.Assistant.presentation.viewmodel.AssistantViewModel

@Composable
fun AssistantScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AssistantViewModel = hiltViewModel()
) {
    AssistantContent(
        uiState = viewModel.uiState.value,
        onSendMessage = viewModel::sendMessage,
        onInputChange = viewModel::updateInput,
        onStartVoiceInput = viewModel::startVoiceInput,
        onStopVoiceInput = viewModel::stopVoiceInput,
        onToggleTts = viewModel::toggleTts,
        onClearError = viewModel::clearError,
        onClearChat = viewModel::clearChat,
        onNavigateBack = onNavigateBack
    )
}