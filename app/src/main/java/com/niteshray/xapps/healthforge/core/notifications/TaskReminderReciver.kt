package com.niteshray.xapps.healthforge.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.niteshray.xapps.healthforge.MainActivity
import com.niteshray.xapps.healthforge.R

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "TaskReminderReceiver"
        const val CHANNEL_ID = "health_tasks_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "TaskReminderReceiver triggered")
        
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        val taskDescription = intent.getStringExtra("TASK_DESCRIPTION") ?: ""

        Log.d(TAG, "Task details - ID: $taskId, Title: '$taskTitle', Description: '$taskDescription'")

        try {
            // Show notification first
            showNotification(context, taskId, taskTitle, taskDescription)
            Log.d(TAG, "Notification shown successfully")

            // Then speak the task title using TTS
            speakTaskTitle(context, taskTitle)
            Log.d(TAG, "TTS service started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onReceive", e)
        }
    }

    private fun showNotification(
        context: Context,
        taskId: Int,
        title: String,
        description: String
    ) {
        Log.d(TAG, "Creating notification for task: $taskId")
        
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create notification channel for Android O and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (existingChannel == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Health Task Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        enableVibration(true)
                        enableLights(true)
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    }
                    notificationManager.createNotificationChannel(channel)
                    Log.d(TAG, "Notification channel created")
                }
            }

            // Check if notifications are enabled
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                Log.w(TAG, "Notifications are disabled by user")
                return
            }

            // Create intent for when notification is tapped
            val notificationIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("TASK_ID", taskId)
                putExtra("FROM_NOTIFICATION", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(description)
                .setStyle(NotificationCompat.BigTextStyle().bigText(description))
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure this exists
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .build()

            notificationManager.notify(taskId, notification)
            Log.d(TAG, "Notification posted successfully for task: $taskId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification", e)
        }
    }

    private fun speakTaskTitle(context: Context, text: String) {
        Log.d(TAG, "Starting TTS service for text: '$text'")
        
        try {
            // Use a service to handle TTS to avoid lifecycle issues
            val ttsIntent = Intent(context, TTSService::class.java).apply {
                putExtra("TEXT_TO_SPEAK", "Health reminder: $text") // Add prefix for context
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(ttsIntent)
                Log.d(TAG, "Started TTS foreground service")
            } else {
                context.startService(ttsIntent)
                Log.d(TAG, "Started TTS service")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start TTS service", e)
        }
    }
}
