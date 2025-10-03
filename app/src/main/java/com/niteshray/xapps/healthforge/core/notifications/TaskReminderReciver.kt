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
        Log.d(TAG, "Enhanced TaskReminderReceiver triggered")
        
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        val taskDescription = intent.getStringExtra("TASK_DESCRIPTION") ?: ""
        val taskCategory = intent.getStringExtra("TASK_CATEGORY") ?: "GENERAL"
        val taskPriority = intent.getStringExtra("TASK_PRIORITY") ?: "MEDIUM"

        Log.d(TAG, "Enhanced task details - ID: $taskId, Title: '$taskTitle', Category: $taskCategory, Priority: $taskPriority")
        Log.d(TAG, "Description: '$taskDescription'")

        try {
            // Show enhanced notification first
            showNotification(context, taskId, taskTitle, taskDescription)
            Log.d(TAG, "Enhanced notification shown successfully")

            // Then speak the interactive task message using TTS
            speakTaskTitle(context, taskTitle)
            Log.d(TAG, "Enhanced TTS service started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in enhanced onReceive", e)
        }
    }

    private fun showNotification(
        context: Context,
        taskId: Int,
        title: String,
        descriptions: String
    ) {
        Log.d(TAG, "Creating enhanced notification for task: $taskId")
        
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create notification channel for Android O and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (existingChannel == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "🏥 HealthForge Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Important health task reminders and medication alerts"
                        enableVibration(true)
                        enableLights(true)
                        lightColor = android.graphics.Color.BLUE
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    }
                    notificationManager.createNotificationChannel(channel)
                    Log.d(TAG, "Enhanced notification channel created")
                }
            }

            // Check if notifications are enabled
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                Log.w(TAG, "Notifications are disabled by user")
                return
            }

            // Get category-specific styling and content
            val categoryInfo = getCategoryNotificationInfo(title, descriptions)

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

            // Create enhanced notification with rich styling
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("${categoryInfo.emoji} ${categoryInfo.title}")
                .setContentText(categoryInfo.subtitle)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${categoryInfo.subtitle}\n\n💡 ${categoryInfo.actionHint}")
                        .setBigContentTitle("${categoryInfo.emoji} ${categoryInfo.title}")
                )
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setLargeIcon(createCategoryIcon(context, categoryInfo.color))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(categoryInfo.color)
                .setColorized(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 300, 200, 300, 200, 300))
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setTimeoutAfter(30 * 60 * 1000) // Auto-dismiss after 30 minutes
                .setOnlyAlertOnce(false)
                .build()

            notificationManager.notify(taskId, notification)
            Log.d(TAG, "Enhanced notification posted successfully for task: $taskId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show enhanced notification", e)
        }
    }

    private fun getCategoryNotificationInfo(title: String, description: String): CategoryNotificationInfo {
        return when {
            // Medicine/Medication category
            title.contains("medicine", ignoreCase = true) || 
            title.contains("medication", ignoreCase = true) ||
            title.contains("pill", ignoreCase = true) ||
            title.contains("tablet", ignoreCase = true) ||
            title.contains("take", ignoreCase = true) -> {
                CategoryNotificationInfo(
                    emoji = "💊",
                    title = "Medication Reminder",
                    subtitle = "Time to take your medicine: $title",
                    actionHint = "Tap to mark as taken",
                    color = android.graphics.Color.parseColor("#2196F3")
                )
            }
            
            // Exercise category
            title.contains("exercise", ignoreCase = true) ||
            title.contains("workout", ignoreCase = true) ||
            title.contains("walk", ignoreCase = true) ||
            title.contains("run", ignoreCase = true) ||
            title.contains("gym", ignoreCase = true) -> {
                CategoryNotificationInfo(
                    emoji = "🏃‍♂️",
                    title = "Exercise Time",
                    subtitle = "Let's get moving: $title",
                    actionHint = "Tap to start your workout session",
                    color = android.graphics.Color.parseColor("#4CAF50")
                )
            }
            
            // Diet/Food category
            title.contains("eat", ignoreCase = true) ||
            title.contains("meal", ignoreCase = true) ||
            title.contains("food", ignoreCase = true) ||
            title.contains("drink", ignoreCase = true) ||
            title.contains("water", ignoreCase = true) -> {
                CategoryNotificationInfo(
                    emoji = "🍎",
                    title = "Nutrition Reminder",
                    subtitle = "Time for healthy eating: $title",
                    actionHint = "Tap to log your meal",
                    color = android.graphics.Color.parseColor("#FF9800")
                )
            }
            
            // Monitoring/Checkup category
            title.contains("check", ignoreCase = true) ||
            title.contains("monitor", ignoreCase = true) ||
            title.contains("measure", ignoreCase = true) ||
            title.contains("pressure", ignoreCase = true) ||
            title.contains("sugar", ignoreCase = true) -> {
                CategoryNotificationInfo(
                    emoji = "📊",
                    title = "Health Monitoring",
                    subtitle = "Time for health check: $title",
                    actionHint = "Tap to record your readings",
                    color = android.graphics.Color.parseColor("#9C27B0")
                )
            }
            
            // Sleep/Lifestyle category
            title.contains("sleep", ignoreCase = true) ||
            title.contains("rest", ignoreCase = true) ||
            title.contains("relax", ignoreCase = true) ||
            title.contains("meditation", ignoreCase = true) -> {
                CategoryNotificationInfo(
                    emoji = "😴",
                    title = "Wellness Reminder",
                    subtitle = "Time to focus on wellbeing: $title",
                    actionHint = "Tap to complete this activity",
                    color = android.graphics.Color.parseColor("#00BCD4")
                )
            }
            
            // General/Default category
            else -> {
                CategoryNotificationInfo(
                    emoji = "⚕️",
                    title = "Health Reminder",
                    subtitle = title,
                    actionHint = "Tap to mark as completed",
                    color = android.graphics.Color.parseColor("#607D8B")
                )
            }
        }
    }

    private fun createCategoryIcon(context: Context, color: Int): android.graphics.Bitmap {
        val size = 128
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Create circular background
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = color
        }
        
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        
        // Add white medical cross
        val crossPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.WHITE
        }
        
        val crossWidth = size * 0.15f
        val crossLength = size * 0.6f
        val centerX = size / 2f
        val centerY = size / 2f
        
        // Horizontal bar
        canvas.drawRect(
            centerX - crossLength / 2,
            centerY - crossWidth / 2,
            centerX + crossLength / 2,
            centerY + crossWidth / 2,
            crossPaint
        )
        
        // Vertical bar
        canvas.drawRect(
            centerX - crossWidth / 2,
            centerY - crossLength / 2,
            centerX + crossWidth / 2,
            centerY + crossLength / 2,
            crossPaint
        )
        
        return bitmap
    }

    private data class CategoryNotificationInfo(
        val emoji: String,
        val title: String,
        val subtitle: String,
        val actionHint: String,
        val color: Int
    )

    private fun speakTaskTitle(context: Context, text: String) {
        Log.d(TAG, "Starting enhanced TTS service for text: '$text'")
        
        try {
            // Generate interactive message based on task content
            val interactiveMessage = generateInteractiveMessage(text)
            
            // Use a service to handle TTS to avoid lifecycle issues
            val ttsIntent = Intent(context, TTSService::class.java).apply {
                putExtra("TEXT_TO_SPEAK", interactiveMessage)
                putExtra("TASK_TITLE", text) // Original title for logging
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(ttsIntent)
                Log.d(TAG, "Started enhanced TTS foreground service")
            } else {
                context.startService(ttsIntent)
                Log.d(TAG, "Started enhanced TTS service")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start enhanced TTS service", e)
        }
    }

    private fun generateInteractiveMessage(taskTitle: String): String {
        return when {
            // Medicine/Medication reminders
            taskTitle.contains("medicine", ignoreCase = true) || 
            taskTitle.contains("medication", ignoreCase = true) ||
            taskTitle.contains("pill", ignoreCase = true) ||
            taskTitle.contains("tablet", ignoreCase = true) ||
            taskTitle.contains("take", ignoreCase = true) -> {
                val medicineName = extractMedicineName(taskTitle)
                "Hello! It's your medicine time. Please take your $medicineName medicine now. Don't forget to have it with water if required. Stay healthy!"
            }
            
            // Exercise reminders
            taskTitle.contains("exercise", ignoreCase = true) ||
            taskTitle.contains("workout", ignoreCase = true) ||
            taskTitle.contains("walk", ignoreCase = true) ||
            taskTitle.contains("run", ignoreCase = true) ||
            taskTitle.contains("gym", ignoreCase = true) -> {
                val exerciseType = extractExerciseType(taskTitle)
                "Hi there! Time to get your body moving. It's time for your $exerciseType. Remember, a healthy body leads to a healthy mind. Let's do this!"
            }
            
            // Food/Diet reminders
            taskTitle.contains("eat", ignoreCase = true) ||
            taskTitle.contains("meal", ignoreCase = true) ||
            taskTitle.contains("food", ignoreCase = true) ||
            taskTitle.contains("breakfast", ignoreCase = true) ||
            taskTitle.contains("lunch", ignoreCase = true) ||
            taskTitle.contains("dinner", ignoreCase = true) -> {
                val mealType = extractMealType(taskTitle)
                "Hello! It's time for your $mealType. Remember to eat nutritious food and stay hydrated. Your body deserves the best fuel!"
            }
            
            // Water reminders
            taskTitle.contains("water", ignoreCase = true) ||
            taskTitle.contains("drink", ignoreCase = true) ||
            taskTitle.contains("hydrate", ignoreCase = true) -> {
                "Hey! Time to hydrate yourself. Please drink a glass of water now. Staying hydrated is essential for your health. Cheers to good health!"
            }
            
            // Monitoring/Health check reminders
            taskTitle.contains("check", ignoreCase = true) ||
            taskTitle.contains("monitor", ignoreCase = true) ||
            taskTitle.contains("measure", ignoreCase = true) ||
            taskTitle.contains("pressure", ignoreCase = true) ||
            taskTitle.contains("sugar", ignoreCase = true) ||
            taskTitle.contains("glucose", ignoreCase = true) -> {
                val checkType = extractMonitoringType(taskTitle)
                "Hello! It's time for your health monitoring. Please check your $checkType now and record the readings. Regular monitoring helps maintain good health!"
            }
            
            // Sleep/Rest reminders
            taskTitle.contains("sleep", ignoreCase = true) ||
            taskTitle.contains("rest", ignoreCase = true) ||
            taskTitle.contains("bed", ignoreCase = true) -> {
                "Hi! It's time to rest and recharge. Good sleep is essential for your recovery and health. Please prepare for a restful sleep. Sweet dreams!"
            }
            
            // Meditation/Relaxation reminders
            taskTitle.contains("meditation", ignoreCase = true) ||
            taskTitle.contains("relax", ignoreCase = true) ||
            taskTitle.contains("breathe", ignoreCase = true) -> {
                "Hello! Time to calm your mind and relax. Take a few deep breaths and center yourself. Your mental health is just as important as your physical health!"
            }
            
            // Doctor appointment reminders
            taskTitle.contains("doctor", ignoreCase = true) ||
            taskTitle.contains("appointment", ignoreCase = true) ||
            taskTitle.contains("visit", ignoreCase = true) -> {
                "Hello! You have a medical appointment coming up. Please prepare and don't forget to bring your medical documents. Regular checkups are important for your health!"
            }
            
            // General health reminders
            else -> {
                "Hello! This is your health reminder for: $taskTitle. Please take care of this important task for your wellbeing. Your health is your wealth!"
            }
        }
    }

    private fun extractMedicineName(title: String): String {
        // Try to extract medicine name from title
        val keywords = listOf("take", "medicine", "medication", "pill", "tablet")
        var cleanTitle = title
        
        keywords.forEach { keyword ->
            cleanTitle = cleanTitle.replace(keyword, "", ignoreCase = true)
        }
        
        return cleanTitle.trim().ifEmpty { "prescribed" }
    }

    private fun extractExerciseType(title: String): String {
        return when {
            title.contains("walk", ignoreCase = true) -> "walk"
            title.contains("run", ignoreCase = true) -> "run" 
            title.contains("gym", ignoreCase = true) -> "gym workout"
            title.contains("yoga", ignoreCase = true) -> "yoga session"
            else -> "exercise session"
        }
    }

    private fun extractMealType(title: String): String {
        return when {
            title.contains("breakfast", ignoreCase = true) -> "breakfast"
            title.contains("lunch", ignoreCase = true) -> "lunch"
            title.contains("dinner", ignoreCase = true) -> "dinner"
            title.contains("snack", ignoreCase = true) -> "healthy snack"
            else -> "meal"
        }
    }

    private fun extractMonitoringType(title: String): String {
        return when {
            title.contains("pressure", ignoreCase = true) -> "blood pressure"
            title.contains("sugar", ignoreCase = true) || title.contains("glucose", ignoreCase = true) -> "blood sugar"
            title.contains("weight", ignoreCase = true) -> "weight"
            title.contains("temperature", ignoreCase = true) -> "temperature"
            else -> "health parameters"
        }
    }
}
