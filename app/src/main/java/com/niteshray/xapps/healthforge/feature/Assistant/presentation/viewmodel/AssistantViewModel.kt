package com.niteshray.xapps.healthforge.feature.Assistant.presentation.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niteshray.xapps.healthforge.feature.Assistant.data.models.ChatMessage
import com.niteshray.xapps.healthforge.feature.Assistant.data.models.ConversationContext
import com.niteshray.xapps.healthforge.feature.Assistant.domain.repository.AssistantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val assistantRepository: AssistantRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(AssistantUiState())
    val uiState: State<AssistantUiState> = _uiState

    private var listeningJob: Job? = null

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        val userMessage = ChatMessage(
            text = message.trim(),
            isFromUser = true
        )

        // Add user message
        _uiState.value = _uiState.value.copy(
            conversationContext = _uiState.value.conversationContext.addMessage(userMessage),
            isLoading = true,
            currentInput = ""
        )

        // Add loading message
        val loadingMessage = ChatMessage(
            text = "Thinking...",
            isFromUser = false,
            isLoading = true
        )
        _uiState.value = _uiState.value.copy(
            conversationContext = _uiState.value.conversationContext.addMessage(loadingMessage)
        )

        viewModelScope.launch {
            assistantRepository.sendMessage(
                message = message,
                context = _uiState.value.conversationContext.messages.dropLast(1) // Exclude the loading message
            ).fold(
                onSuccess = { response ->
                    // Remove loading message and add actual response
                    val updatedMessages = _uiState.value.conversationContext.messages.dropLast(1)
                    val cleanedResponse = cleanTextForDisplay(response)
                    val assistantMessage = ChatMessage(
                        text = cleanedResponse,
                        isFromUser = false
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        conversationContext = ConversationContext(updatedMessages).addMessage(assistantMessage),
                        isLoading = false
                    )

                    // Speak the response if TTS is enabled
                    if (_uiState.value.isTtsEnabled && assistantRepository.isTtsReady()) {
                        assistantRepository.speakText(response)
                    }
                },
                onFailure = { error ->
                    // Remove loading message and show error
                    val updatedMessages = _uiState.value.conversationContext.messages.dropLast(1)
                    val errorMessage = ChatMessage(
                        text = "Sorry, I encountered an error: ${error.message}. Please try again.",
                        isFromUser = false
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        conversationContext = ConversationContext(updatedMessages).addMessage(errorMessage),
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun updateInput(input: String) {
        _uiState.value = _uiState.value.copy(currentInput = input)
    }

    fun startVoiceInput() {
        if (_uiState.value.isListening) return

        _uiState.value = _uiState.value.copy(isListening = true)

        listeningJob = viewModelScope.launch {
            try {
                assistantRepository.startListening()
                    .catch { error ->
                        _uiState.value = _uiState.value.copy(
                            isListening = false,
                            error = "Voice recognition error: ${error.message}"
                        )
                    }
                    .collect { recognizedText ->
                        _uiState.value = _uiState.value.copy(
                            isListening = false,
                            currentInput = recognizedText
                        )
                        
                        if (recognizedText.isNotBlank()) {
                            sendMessage(recognizedText)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isListening = false,
                    error = "Failed to start voice input: ${e.message}"
                )
            }
        }
    }

    fun stopVoiceInput() {
        listeningJob?.cancel()
        viewModelScope.launch {
            assistantRepository.stopListening()
        }
        _uiState.value = _uiState.value.copy(isListening = false)
    }

    fun toggleTts() {
        _uiState.value = _uiState.value.copy(
            isTtsEnabled = !_uiState.value.isTtsEnabled
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearChat() {
        _uiState.value = AssistantUiState()
    }

    fun checkSpeechRecognitionAvailability() {
        if (assistantRepository is com.niteshray.xapps.healthforge.feature.Assistant.data.repository.AssistantRepositoryImpl) {
            val isAvailable = assistantRepository.isSpeechRecognitionAvailable()
            if (!isAvailable) {
                _uiState.value = _uiState.value.copy(
                    error = "Speech recognition is not available on this device"
                )
            }
        }
    }

    private fun cleanTextForDisplay(text: String): String {
        return text
            .replace(Regex("\\*+"), "") // Remove asterisks
            .replace(Regex("#+\\s*"), "") // Remove markdown headers
            .replace(Regex("_+"), "") // Remove underscores
            .replace(Regex("`+"), "") // Remove code markers
            .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
            .trim()
    }

    override fun onCleared() {
        super.onCleared()
        listeningJob?.cancel()
    }
}

data class AssistantUiState(
    val conversationContext: ConversationContext = ConversationContext(),
    val currentInput: String = "",
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val isTtsEnabled: Boolean = true,
    val error: String? = null
)