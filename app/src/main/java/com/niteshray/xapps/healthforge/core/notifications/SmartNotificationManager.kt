package com.niteshray.xapps.healthforge.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.niteshray.xapps.healthforge.MainActivity
import com.niteshray.xapps.healthforge.R
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskTemplate
import java.text.SimpleDateFormat
import java.util.*

object SmartNotificationManager {
    
    private const val TAG = "SmartNotificationManager"
    private const val DAILY_SUMMARY_CHANNEL_ID = "daily_summary_channel"
    private const val MOTIVATION_CHANNEL_ID = "motivation_channel"
    private const val ACHIEVEMENT_CHANNEL_ID = "achievement_channel"
    
    /**
     * Initialize notification channels
     */
    fun initializeChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Daily Summary Channel
            val summaryChannel = NotificationChannel(
                DAILY_SUMMARY_CHANNEL_ID,
                "Daily Health Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily task summaries and progress reports"
                enableVibration(false)
                enableLights(true)
            }
            
            // Motivation Channel
            val motivationChannel = NotificationChannel(
                MOTIVATION_CHANNEL_ID,
                "Health Motivation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Motivational messages and health tips"
                enableVibration(false)
                enableLights(false)
            }
            
            // Achievement Channel
            val achievementChannel = NotificationChannel(
                ACHIEVEMENT_CHANNEL_ID,
                "Health Achievements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Milestone and achievement notifications"
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
            }
            
            notificationManager.createNotificationChannels(
                listOf(summaryChannel, motivationChannel, achievementChannel)
            )
            
            Log.d(TAG, "Notification channels initialized")
        }
    }
    
    /**
     * Send daily summary notification
     */
    fun sendDailySummaryNotification(
        context: Context,
        taskCount: Int,
        completedCount: Int,
        pendingCount: Int
    ) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "Notifications are disabled")
            return
        }
        
        try {
            val completionRate = if (taskCount > 0) (completedCount * 100) / taskCount else 0
            
            val title = when {
                completionRate >= 90 -> "🎉 Excellent Progress!"
                completionRate >= 70 -> "👏 Great Job Today!"
                completionRate >= 50 -> "👍 Good Progress!"
                completionRate >= 30 -> "💪 Keep Going!"
                else -> "🌟 New Day, New Opportunities!"
            }
            
            val message = buildString {
                append("Today's Health Summary:\n")
                append("✅ Completed: $completedCount tasks\n")
                append("⏰ Pending: $pendingCount tasks\n")
                append("📊 Completion Rate: $completionRate%")
            }
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("NAVIGATE_TO", "dashboard")
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, DAILY_SUMMARY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // You'll need to add this icon
                .setContentTitle(title)
                .setContentText("$completedCount/$taskCount tasks completed ($completionRate%)")
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            
            NotificationManagerCompat.from(context).notify(100, notification)
            Log.d(TAG, "Daily summary notification sent: $completionRate% completion rate")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send daily summary notification", e)
        }
    }
    
    /**
     * Send motivational notification
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun sendMotivationalNotification(
        context: Context,
        dayStreak: Int,
        completionRate: Int
    ) {
        try {
            val (title, message) = getMotivationalContent(dayStreak, completionRate)
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("NAVIGATE_TO", "analytics")
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                200,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, MOTIVATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            
            NotificationManagerCompat.from(context).notify(200, notification)
            Log.d(TAG, "Motivational notification sent for streak: $dayStreak days")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send motivational notification", e)
        }
    }
    
    /**
     * Send achievement notification
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun sendAchievementNotification(
        context: Context,
        achievementType: AchievementType,
        value: Int
    ) {
        try {
            val (title, message, emoji) = getAchievementContent(achievementType, value)
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("NAVIGATE_TO", "analytics")
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                300,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, ACHIEVEMENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("$emoji $title")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColorized(true)
                .setColor(Color.parseColor("#4CAF50"))
                .build()
            
            NotificationManagerCompat.from(context).notify(300, notification)
            Log.d(TAG, "Achievement notification sent: $achievementType - $value")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send achievement notification", e)
        }
    }
    
    private fun getMotivationalContent(dayStreak: Int, completionRate: Int): Pair<String, String> {
        return when {
            dayStreak >= 30 -> Pair(
                "🔥 Amazing 30-Day Streak!",
                "You're building incredible healthy habits! Keep up the fantastic work."
            )
            dayStreak >= 14 -> Pair(
                "⚡ Two Weeks Strong!",
                "Your consistency is paying off! You're on the path to lasting health."
            )
            dayStreak >= 7 -> Pair(
                "🌟 One Week Victory!",
                "Seven days of dedication! You're creating positive change in your life."
            )
            completionRate >= 80 -> Pair(
                "💪 Health Champion!",
                "Your dedication to health is inspiring. Every task completed is progress!"
            )
            else -> Pair(
                "🌱 Growing Stronger!",
                "Every small step counts. Your health journey is unique and valuable."
            )
        }
    }
    
    private fun getAchievementContent(type: AchievementType, value: Int): Triple<String, String, String> {
        return when (type) {
            AchievementType.PERFECT_DAY -> Triple(
                "Perfect Day Achievement!",
                "You completed 100% of your health tasks today! This is the foundation of lasting wellness.",
                "🎯"
            )
            AchievementType.WEEK_STREAK -> Triple(
                "$value-Day Streak Achievement!",
                "You've maintained consistent healthy habits for $value days in a row. This consistency will transform your health!",
                "🔥"
            )
            AchievementType.TASK_MILESTONE -> Triple(
                "Task Master Achievement!",
                "You've completed $value health tasks! Every completed task is an investment in your wellbeing.",
                "⭐"
            )
            AchievementType.CATEGORY_MASTER -> Triple(
                "Category Expert!",
                "You've consistently completed tasks in this health category. Specialization leads to excellence!",
                "🏆"
            )
        }
    }
    
    enum class AchievementType {
        PERFECT_DAY,
        WEEK_STREAK,
        TASK_MILESTONE,
        CATEGORY_MASTER
    }
}