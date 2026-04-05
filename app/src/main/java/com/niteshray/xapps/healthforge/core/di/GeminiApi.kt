package com.niteshray.xapps.healthforge.core.di

import com.google.ai.client.generativeai.GenerativeModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiApi @Inject constructor(
    private val generativeModel: GenerativeModel
) {
    suspend fun generateContent(request: ChatRequest): ChatResponse {
        try {
            // Build the conversation content for Gemini
            val conversationContent = buildString {
                request.messages.forEach { message ->
                    when (message.role) {
                        "system" -> append("System: ${message.content}\n\n")
                        "user" -> append("User: ${message.content}\n\n")
                        "assistant" -> append("Assistant: ${message.content}\n\n")
                    }
                }
            }

            // Generate content using Gemini
            val response = generativeModel.generateContent(conversationContent)
            val generatedText = response.text ?: ""

            // Return in the same format as Cerebras for compatibility
            return ChatResponse(
                choices = listOf(
                    Choice(
                        message = Message(
                            role = "assistant",
                            content = generatedText
                        )
                    )
                )
            )
        } catch (e: Exception) {
            throw Exception("Gemini API Error: ${e.message}", e)
        }
    }
}
