package com.niteshray.xapps.healthforge.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.niteshray.xapps.healthforge.R
import java.util.Locale

class TTSService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val TAG = "TTSService"
        const val CHANNEL_ID = "tts_service_channel"
        const val NOTIFICATION_ID = 1001
    }

    private var tts: TextToSpeech? = null
    private var textToSpeak: String = ""
    private var utteranceId: String = ""

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TTSService created")
        
        try {
            tts = TextToSpeech(this, this)
            Log.d(TAG, "TextToSpeech initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "TTSService started")
        
        textToSpeak = intent?.getStringExtra("TEXT_TO_SPEAK") ?: ""
        utteranceId = "TASK_REMINDER_${System.currentTimeMillis()}"
        
        Log.d(TAG, "Text to speak: '$textToSpeak'")
        Log.d(TAG, "Utterance ID: $utteranceId")

        if (textToSpeak.isEmpty()) {
            Log.w(TAG, "No text to speak, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        // Create foreground notification for Android O+
        createForegroundNotification()

        return START_NOT_STICKY
    }

    private fun createForegroundNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "TTS Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Text-to-Speech service for task reminders"
                    enableVibration(false)
                    setSound(null, null)
                }
                
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)
                
                Log.d(TAG, "Notification channel created")
            }

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("HealthForge TTS")
                .setContentText("Speaking task reminder...")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure this icon exists
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(false)
                .setOngoing(true)
                .build()

            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Foreground service started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create foreground notification", e)
            // If we can't create notification, just stop the service
            stopSelf()
        }
    }

    override fun onInit(status: Int) {
        Log.d(TAG, "TTS onInit called with status: $status")
        
        if (status == TextToSpeech.SUCCESS) {
            try {
                setupTTS()
                speakText()
            } catch (e: Exception) {
                Log.e(TAG, "Error in TTS initialization", e)
                stopSelf()
            }
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
            stopSelf()
        }
    }
    
    private fun setupTTS() {
        tts?.let { ttsEngine ->
            // Set up utterance progress listener
            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS started speaking: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS finished speaking: $utteranceId")
                    // Stop service after speaking is done
                    Handler(Looper.getMainLooper()).post {
                        stopSelf()
                    }
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS error for utterance: $utteranceId")
                    Handler(Looper.getMainLooper()).post {
                        stopSelf()
                    }
                }
            })

            // Try to set language to Hindi first
            var languageResult = ttsEngine.setLanguage(Locale("hi", "IN"))
            Log.d(TAG, "Hindi language setting result: $languageResult")
            
            if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English
                languageResult = ttsEngine.setLanguage(Locale.US)
                Log.d(TAG, "English language setting result: $languageResult")
                
                if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                    languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "No supported language found")
                    stopSelf()
                    return
                }
            }
            
            // Set speech rate and pitch
            ttsEngine.setSpeechRate(0.9f) // Slightly slower for better clarity
            ttsEngine.setPitch(1.0f) // Normal pitch
            
            Log.d(TAG, "TTS configured successfully")
        }
    }
    
    private fun speakText() {
        if (textToSpeak.isNotEmpty()) {
            tts?.let { ttsEngine ->
                Log.d(TAG, "Starting to speak: '$textToSpeak'")
                
                val result = ttsEngine.speak(
                    textToSpeak,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    utteranceId
                )
                
                Log.d(TAG, "TTS speak result: $result")
                
                if (result == TextToSpeech.ERROR) {
                    Log.e(TAG, "TTS speak failed")
                    stopSelf()
                } else {
                    // Set a fallback timer in case onDone is not called
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.w(TAG, "TTS timeout, stopping service")
                        stopSelf()
                    }, 10000) // 10 second timeout
                }
            } ?: run {
                Log.e(TAG, "TTS engine is null")
                stopSelf()
            }
        } else {
            Log.w(TAG, "No text to speak")
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "TTSService destroyed")
        
        try {
            tts?.stop()
            tts?.shutdown()
            Log.d(TAG, "TTS engine stopped and shutdown")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
        
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
