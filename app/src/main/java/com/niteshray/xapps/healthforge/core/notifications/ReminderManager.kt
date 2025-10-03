package com.niteshray.xapps.healthforge.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.niteshray.xapps.healthforge.feature.home.data.models.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ReminderManager {
    
    const val TAG = "ReminderManager"

    fun scheduleTaskReminder(
        context: Context,
        task: Task
    ) {
        Log.d(TAG, "Scheduling reminder for task: ${task.id} - '${task.title}' at ${task.time}")
        
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Check if we have exact alarm permission on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                    return
                }
            }

            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("TASK_ID", task.id)
                putExtra("TASK_TITLE", task.title)
                putExtra("TASK_DESCRIPTION", task.description)
                putExtra("TASK_CATEGORY", task.category.name)
                putExtra("TASK_PRIORITY", task.priority.name)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id, // Unique request code for each task
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set the alarm to trigger at the specified time
            val calendar = parseTimeStringToCalendar(task.time)
            
            Log.d(TAG, "Parsed calendar time: ${calendar.time}")
            Log.d(TAG, "Current time: ${Calendar.getInstance().time}")

            // If time has passed for today, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                Log.d(TAG, "Time has passed today, scheduling for tomorrow: ${calendar.time}")
            }

            // Use setExactAndAllowWhileIdle for better reliability on Android 6+
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm with setExactAndAllowWhileIdle")
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm with setExact")
                }
                else -> {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled alarm with set")
                }
            }
            
            Log.d(TAG, "Successfully scheduled reminder for task ${task.id} at ${calendar.time}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder for task ${task.id}", e)
        }
    }

    fun scheduleAllTasks(context: Context, tasks: List<Task>) {
        Log.d(TAG, "Scheduling ${tasks.size} task reminders")
        
        tasks.forEach { task ->
            scheduleTaskReminder(context, task)
        }
        
        Log.d(TAG, "Completed scheduling all task reminders")
    }

    fun cancelTaskReminder(context: Context, taskId: Int) {
        Log.d(TAG, "Canceling reminder for task: $taskId")
        
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TaskReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            
            Log.d(TAG, "Successfully canceled reminder for task: $taskId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel reminder for task: $taskId", e)
        }
    }
    
    fun cancelAllTaskReminders(context: Context, taskIds: List<Int>) {
        Log.d(TAG, "Canceling ${taskIds.size} task reminders")
        
        taskIds.forEach { taskId ->
            cancelTaskReminder(context, taskId)
        }
        
        Log.d(TAG, "Completed canceling all task reminders")
    }
}

fun parseTimeStringToCalendar(timeString: String): Calendar {
    val calendar = Calendar.getInstance()
    
    Log.d(ReminderManager.TAG, "Parsing time string: '$timeString'")

    try {
        // Create SimpleDateFormat with 12-hour format pattern
        val sdf = SimpleDateFormat("h:mm a", Locale.ENGLISH)
        sdf.isLenient = false

        // Parse the time string
        val date = sdf.parse(timeString)

        // Set the parsed time to calendar
        if (date != null) {
            val tempCalendar = Calendar.getInstance()
            tempCalendar.time = date

            // Extract hour and minute from parsed time
            val hour = tempCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = tempCalendar.get(Calendar.MINUTE)

            Log.d(ReminderManager.TAG, "Parsed time - Hour: $hour, Minute: $minute")

            // Set today's date with the parsed time
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            Log.d(ReminderManager.TAG, "Calendar set to: ${calendar.time}")
        } else {
            Log.w(ReminderManager.TAG, "Failed to parse date, using default time")
        }
    } catch (e: Exception) {
        Log.e(ReminderManager.TAG, "Error parsing time string: '$timeString'", e)
        e.printStackTrace()
        // Return current time + 1 hour as fallback
        calendar.add(Calendar.HOUR, 1)
    }

    return calendar
}

