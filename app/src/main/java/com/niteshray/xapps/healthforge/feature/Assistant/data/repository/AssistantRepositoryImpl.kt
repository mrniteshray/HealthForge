package com.niteshray.xapps.healthforge.feature.Assistant.data.repository

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.niteshray.xapps.healthforge.core.di.CerebrasApi
import com.niteshray.xapps.healthforge.core.di.ChatRequest
import com.niteshray.xapps.healthforge.core.di.Message
import com.niteshray.xapps.healthforge.feature.Assistant.data.models.ChatMessage
import com.niteshray.xapps.healthforge.feature.Assistant.domain.repository.AssistantRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AssistantRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cerebrasApi: CerebrasApi
) : AssistantRepository {

    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isTtsInitialized = false

    init {
        initializeTts()
        initializeSpeechRecognizer()
    }

    private fun initializeTts() {
        textToSpeech = TextToSpeech(context) { status ->
            isTtsInitialized = status == TextToSpeech.SUCCESS
            if (isTtsInitialized) {
                textToSpeech?.language = Locale.getDefault()
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }
        } catch (e: Exception) {
            speechRecognizer = null
        }
    }

    override suspend fun sendMessage(message: String, context: List<ChatMessage>): Result<String> {
        return try {
            val systemMessage = Message(
                role = "system",
                content = """You are a health assistant for HealthForge app. Keep responses SHORT (2-3 sentences max). 
                Provide concise, helpful health guidance. Always remind users to consult healthcare professionals for serious concerns.
                Focus only on health topics. Be direct and brief."""
            )

            val contextMessages = context.map { chatMessage ->
                Message(
                    role = if (chatMessage.isFromUser) "user" else "assistant",
                    content = chatMessage.text
                )
            }

            val userMessage = Message(role = "user", content = message)
            
            val allMessages = listOf(systemMessage) + contextMessages + listOf(userMessage)
            
            val request = ChatRequest(
                messages = allMessages,
                temperature = 0.5f,
                max_tokens = 150
            )
            
            val response = cerebrasApi.generateContent(request)
            val assistantMessage = response.choices.firstOrNull()?.message?.content
                ?: "I'm sorry, I couldn't generate a response. Please try again."
            Result.success(assistantMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun speakText(text: String) {
        if (isTtsInitialized) {
            // Clean text for TTS - remove markdown symbols and special characters
            val cleanText = text
                .replace("*", "")
                .replace("#", "")
                .replace("_", "")
                .replace("`", "")
                .replace("- ", "")
                .replace("• ", "")
                .replace(Regex("[\\[\\]()]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
            
            if (cleanText.isNotEmpty()) {
                textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    override suspend fun startListening(): Flow<String> = callbackFlow {
        if (speechRecognizer == null) {
            close(Exception("Speech recognition not available on this device"))
            return@callbackFlow
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            close(Exception("Speech recognition service not available"))
            return@callbackFlow
        }

        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // Recognition is ready
            }
            override fun onBeginningOfSpeech() {
                // User started speaking
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech input detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                    else -> "Unknown speech recognition error: $error"
                }
                close(Exception(errorMessage))
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                trySend(text)
                close()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                // Handle partial results if needed
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        try {
            speechRecognizer?.setRecognitionListener(listener)
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            close(Exception("Failed to start speech recognition: ${e.message}"))
        }

        awaitClose {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.setRecognitionListener(null)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
            isListening = false
        }
    }

    override suspend fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    override fun isListening(): Boolean = isListening

    override fun isTtsReady(): Boolean = isTtsInitialized
    
    fun isSpeechRecognitionAvailable(): Boolean {
        return speechRecognizer != null && SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun cleanup() {
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
    }
}