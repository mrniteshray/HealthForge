package com.niteshray.xapps.healthforge.core.di

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface CerebrasApi {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun generateContent(@Body request: ChatRequest): ChatResponse
}

data class ChatRequest(
    val model: String = "llama-4-scout-17b-16e-instruct",
    val messages: List<Message>,
    val max_tokens: Int? = null,
    val temperature: Float? = null
)

data class Message(
    val role: String, // "user" or "assistant"
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
