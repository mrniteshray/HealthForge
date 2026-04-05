package com.niteshray.xapps.healthforge.core.di

data class ChatRequest(
    val messages: List<Message>,
    val max_tokens: Int? = null,
    val temperature: Float? = null
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
