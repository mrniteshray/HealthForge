package com.niteshray.xapps.healthforge.feature.Assistant.data.models

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false
)

data class ConversationContext(
    val messages: List<ChatMessage> = emptyList(),
    val contextWindow: Int = 10 // Number of messages to keep for context
) {
    fun addMessage(message: ChatMessage): ConversationContext {
        val updatedMessages = messages + message
        return copy(
            messages = if (updatedMessages.size > contextWindow) {
                updatedMessages.takeLast(contextWindow)
            } else {
                updatedMessages
            }
        )
    }
    
    fun getContextForApi(): List<com.niteshray.xapps.healthforge.core.di.Message> {
        return messages.map { chatMessage ->
            com.niteshray.xapps.healthforge.core.di.Message(
                role = if (chatMessage.isFromUser) "user" else "assistant",
                content = chatMessage.text
            )
        }
    }
}