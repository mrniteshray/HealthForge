package com.niteshray.xapps.healthforge.feature.Assistant.domain.repository

import com.niteshray.xapps.healthforge.feature.Assistant.data.models.ChatMessage
import kotlinx.coroutines.flow.Flow

interface AssistantRepository {
    suspend fun sendMessage(message: String, context: List<ChatMessage>): Result<String>
    suspend fun speakText(text: String)
    suspend fun startListening(): Flow<String>
    suspend fun stopListening()
    fun isListening(): Boolean
    fun isTtsReady(): Boolean
}