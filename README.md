# HealthForge - Task Reminder System Documentation

## 📱 Overview
HealthForge is an Android application that helps users manage their health-related tasks with smart reminders, notifications, and Text-to-Speech (TTS) functionality. This documentation explains how the reminder system works, how notifications are displayed, and how TTS provides audio feedback to users.

## 🏗️ Architecture Overview

The reminder system consists of several key components:

1. **ReminderManager** - Handles alarm scheduling and management
2. **TaskReminderReceiver** - Processes alarm triggers and shows notifications 
3. **TTSService** - Provides Text-to-Speech functionality
4. **PermissionManager** - Manages all required Android permissions
5. **BootReceiver** - Reschedules tasks after device reboot

---

## 🔔 How Reminders Work

### 1. Task Scheduling Flow

When a user creates a task in HealthForge, the following process occurs:

```kotlin
// In HomeViewModel.kt
fun addTask(task: Task, context: Context) {
    viewModelScope.launch {
        try {
            // Save task to database
            val dataTask = task.toDataTask()
            taskRepository.addTask(dataTask)
            
            // Schedule reminder using ReminderManager
            ReminderManager.scheduleTaskReminder(context, dataTask)
            Log.d(TAG, "Reminder scheduled for new task: ${task.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add task: ${task.title}", e)
        }
    }
}
```

### 2. Alarm Scheduling (ReminderManager.kt)

The `ReminderManager` is responsible for creating Android alarms that trigger at the specified task times:

```kotlin
object ReminderManager {
    fun scheduleTaskReminder(context: Context, task: Task) {
        Log.d(TAG, "Scheduling reminder for task: ${task.id} - '${task.title}' at ${task.time}")
        
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Check exact alarm permission on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                    return
                }
            }

            // Create intent with task details
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("TASK_ID", task.id)
                putExtra("TASK_TITLE", task.title)
                putExtra("TASK_DESCRIPTION", task.description)
            }

            // Create PendingIntent with unique request code
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id, // Unique request code for each task
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Parse time string and create calendar
            val calendar = parseTimeStringToCalendar(task.time)
            
            // If time has passed today, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                Log.d(TAG, "Time has passed today, scheduling for tomorrow: ${calendar.time}")
            }

            // Use different alarm methods based on Android version
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                else -> {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            }
            
            Log.d(TAG, "Successfully scheduled reminder for task ${task.id} at ${calendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder for task ${task.id}", e)
        }
    }
}
```

### 3. Time Parsing

The system parses time strings in 12-hour format (e.g., "2:30 PM"):

```kotlin
fun parseTimeStringToCalendar(timeString: String): Calendar {
    val calendar = Calendar.getInstance()
    
    try {
        // Create SimpleDateFormat with 12-hour format
        val sdf = SimpleDateFormat("h:mm a", Locale.ENGLISH)
        sdf.isLenient = false

        // Parse the time string
        val date = sdf.parse(timeString)

        if (date != null) {
            val tempCalendar = Calendar.getInstance()
            tempCalendar.time = date

            // Extract hour and minute
            val hour = tempCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = tempCalendar.get(Calendar.MINUTE)

            // Set today's date with parsed time
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing time string: '$timeString'", e)
        // Fallback: current time + 1 hour
        calendar.add(Calendar.HOUR, 1)
    }

    return calendar
}
```

---

## 📢 How Notifications Work

### 1. Notification Triggering (TaskReminderReceiver.kt)

When an alarm fires, Android calls the `TaskReminderReceiver`:

```kotlin
class TaskReminderReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "TaskReminderReceiver"
        const val CHANNEL_ID = "health_tasks_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "TaskReminderReceiver triggered")
        
        // Extract task details from intent
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        val taskDescription = intent.getStringExtra("TASK_DESCRIPTION") ?: ""

        try {
            // Show notification first
            showNotification(context, taskId, taskTitle, taskDescription)
            
            // Then start TTS service
            speakTaskTitle(context, taskTitle)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onReceive", e)
        }
    }
}
```

### 2. Notification Channel Creation

For Android O (API 26) and above, the app creates notification channels:

```kotlin
private fun showNotification(context: Context, taskId: Int, title: String, description: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Create notification channel for Android O+
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
        }
    }

    // Check if notifications are enabled by user
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        Log.w(TAG, "Notifications are disabled by user")
        return
    }

    // Create notification...
}
```

### 3. Notification Building and Display

The notification is built with rich content and actions:

```kotlin
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

// Build the notification
val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setContentTitle(title)
    .setContentText(description)
    .setStyle(NotificationCompat.BigTextStyle().bigText(description))
    .setSmallIcon(R.drawable.ic_launcher_foreground)
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setCategory(NotificationCompat.CATEGORY_REMINDER)
    .setAutoCancel(true)
    .setContentIntent(pendingIntent)
    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
    .setVibrate(longArrayOf(0, 250, 250, 250))
    .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
    .build()

// Show the notification
notificationManager.notify(taskId, notification)
```

### 4. Notification Features

- **High Priority**: Ensures notifications appear on lock screen and as heads-up notifications
- **Big Text Style**: Allows long descriptions to be fully readable
- **Sound**: Uses system default notification sound
- **Vibration**: Custom vibration pattern (250ms pulses)
- **LED Lights**: Uses default system light patterns
- **Auto Cancel**: Notification disappears when tapped
- **Content Intent**: Opens the main app when notification is tapped

---

## 🗣️ How Text-to-Speech (TTS) Works

### 1. TTS Service Architecture (TTSService.kt)

The TTS functionality is implemented as a foreground service to ensure reliable operation:

```kotlin
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
        
        // Initialize TTS engine
        tts = TextToSpeech(this, this)
    }
}
```

### 2. Service Initialization and Startup

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d(TAG, "TTSService started")
    
    // Extract text from intent
    textToSpeak = intent?.getStringExtra("TEXT_TO_SPEAK") ?: ""
    utteranceId = "TASK_REMINDER_${System.currentTimeMillis()}"
    
    if (textToSpeak.isEmpty()) {
        Log.w(TAG, "No text to speak, stopping service")
        stopSelf()
        return START_NOT_STICKY
    }

    // Create foreground notification (required for Android O+)
    createForegroundNotification()

    return START_NOT_STICKY
}
```

### 3. Foreground Service Notification

Since Android O requires foreground services to display a notification:

```kotlin
private fun createForegroundNotification() {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create low-importance channel for service notification
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
        }

        // Create service notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HealthForge TTS")
            .setContentText("Speaking task reminder...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create foreground notification", e)
        stopSelf()
    }
}
```

### 4. TTS Engine Initialization and Configuration

```kotlin
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
        // Set up progress listener for lifecycle management
        ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS started speaking: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS finished speaking: $utteranceId")
                // Stop service when done speaking
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

        // Try Hindi first, fallback to English
        var languageResult = ttsEngine.setLanguage(Locale("hi", "IN"))
        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            languageResult = ttsEngine.setLanguage(Locale.US)
        }
        
        // Configure speech parameters
        ttsEngine.setSpeechRate(0.9f) // Slightly slower for clarity
        ttsEngine.setPitch(1.0f) // Normal pitch
    }
}
```

### 5. Text-to-Speech Execution

```kotlin
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
            
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "TTS speak failed")
                stopSelf()
            } else {
                // Set fallback timeout (10 seconds)
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.w(TAG, "TTS timeout, stopping service")
                    stopSelf()
                }, 10000)
            }
        }
    }
}
```

### 6. TTS Service Cleanup

```kotlin
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
```

---

## 🔐 Permission Management

### 1. Required Permissions (AndroidManifest.xml)

```xml
<!-- Notification permissions -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />

<!-- Alarm permissions -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Audio permissions for TTS -->
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />

<!-- Boot receiver -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Foreground service for TTS -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

### 2. Runtime Permission Handling (PermissionManager.kt)

```kotlin
class PermissionManager(private val activity: ComponentActivity) {
    
    fun requestAllPermissions(onGranted: () -> Unit, onDenied: () -> Unit) {
        Log.d(TAG, "Starting permission request process")
        onPermissionsGranted = onGranted
        onPermissionsDenied = onDenied
        
        if (areAllPermissionsGranted()) {
            onPermissionsGranted?.invoke()
            return
        }
        
        requestRuntimePermissions()
    }
    
    private fun requestRuntimePermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Post notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Audio permissions for TTS
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.MODIFY_AUDIO_SETTINGS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkSpecialPermissions()
        }
    }
}
```

### 3. Special Permission Handling

```kotlin
private fun checkSpecialPermissions() {
    // Check exact alarm permission (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            exactAlarmLauncher.launch(intent)
            return
        }
    }
    
    checkNotificationPermission()
}
```

---

## 🔄 Boot Recovery System

### 1. BootReceiver Implementation

The `BootReceiver` ensures task reminders are restored after device reboot:

```kotlin
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var taskRepository: TaskRepository
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "BootReceiver triggered with action: ${intent.action}")
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, rescheduling task reminders")
            
            val coroutineScope = CoroutineScope(Dispatchers.IO)
            
            coroutineScope.launch {
                try {
                    // Load all tasks from database
                    taskRepository.getAllTasks().collect { dataTaskList ->
                        if (dataTaskList.isNotEmpty()) {
                            Log.d(TAG, "Found ${dataTaskList.size} tasks to reschedule")
                            
                            // Schedule reminders for all tasks
                            ReminderManager.scheduleAllTasks(context, dataTaskList)
                            
                            Log.d(TAG, "All task reminders rescheduled after boot")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule tasks after boot", e)
                }
            }
        }
    }
}
```

### 2. Manifest Registration

```xml
<receiver
    android:name=".core.notifications.BootReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

---

## 🧪 Testing and Debugging

### 1. Debug Panel in Dashboard

The app includes a debug panel for testing functionality:

```kotlin
// In DashboardScreen.kt
Column {
    Text(
        text = "🔧 Debug & Test",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    
    Row {
        Button(onClick = onTestNotificationAndTTS) {
            Text("Test Now")
        }
        
        Button(onClick = onScheduleTestReminder) {
            Text("Test 30s")
        }
    }
}
```

### 2. Test Functions in HomeViewModel

```kotlin
fun testNotificationAndTTS(context: Context) {
    try {
        // Create test task reminder intent
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra("TASK_ID", 9999)
            putExtra("TASK_TITLE", "Test Health Reminder")
            putExtra("TASK_DESCRIPTION", "This is a test notification...")
        }
        
        // Trigger receiver directly for immediate testing
        val receiver = TaskReminderReceiver()
        receiver.onReceive(context, intent)
        
    } catch (e: Exception) {
        Log.e(TAG, "Failed to test notification and TTS", e)
    }
}

fun scheduleTestReminder(context: Context) {
    try {
        // Create test task for 30-second delayed testing
        val testTask = Task(
            id = 9999,
            title = "Test Reminder",
            description = "This is a test reminder scheduled for 30 seconds"
        )
        
        // Schedule using AlarmManager for 30 seconds from now
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra("TASK_ID", testTask.id)
            putExtra("TASK_TITLE", testTask.title)
            putExtra("TASK_DESCRIPTION", testTask.description)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            testTask.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerTime = System.currentTimeMillis() + 30000 // 30 seconds
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
        
    } catch (e: Exception) {
        Log.e(TAG, "Failed to schedule test reminder", e)
    }
}
```

### 3. Comprehensive Logging

Every component includes detailed logging for debugging:

- **ReminderManager**: Alarm scheduling status and errors
- **TaskReminderReceiver**: Notification triggering and display
- **TTSService**: TTS initialization, language setup, and speech execution
- **PermissionManager**: Permission status and request results
- **BootReceiver**: Task rescheduling after reboot

### 4. Common Log Tags for Debugging

```
MainActivity - App lifecycle and permission requests
PermissionManager - Permission status and requests  
TaskReminderReceiver - Notification triggering
TTSService - Text-to-speech operations
ReminderManager - Alarm scheduling
HomeViewModel - Task operations
BootReceiver - Device reboot handling
```

---

## 🔧 System Requirements and Compatibility

### Android Version Support

- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Special handling for**:
  - Android 6.0+ (API 23): Doze mode handling with `setExactAndAllowWhileIdle`
  - Android 8.0+ (API 26): Notification channels and foreground services
  - Android 12+ (API 31): Exact alarm permissions
  - Android 13+ (API 33): POST_NOTIFICATIONS runtime permission

### Device Requirements

- **Notifications**: Must be enabled in device settings
- **Battery Optimization**: Should be disabled for reliable alarms
- **TTS Engine**: System TTS engine with Hindi/English support
- **Storage**: Local database for task persistence

### Performance Considerations

- **Foreground Service**: TTS runs as foreground service for reliability
- **Battery Efficiency**: Services stop automatically after completion
- **Memory Management**: Proper lifecycle handling in all components
- **Network**: No internet required for core reminder functionality

---

## 🚀 Key Features Summary

### ✅ Reminder System
- Precise alarm scheduling using Android AlarmManager
- Support for different Android versions with appropriate methods
- Automatic rescheduling for next day if time has passed
- Bulk scheduling and cancellation of multiple tasks

### ✅ Notification System  
- Rich notifications with title, description, and big text style
- Custom notification channel with high importance
- Sound, vibration, and LED light patterns
- Clickable notifications that open the app
- Automatic cleanup after user interaction

### ✅ Text-to-Speech System
- Foreground service architecture for reliability
- Multi-language support (Hindi with English fallback)
- Proper service lifecycle management
- Automatic cleanup after speech completion
- Configurable speech rate and pitch

### ✅ Permission Management
- Comprehensive runtime permission handling
- Special permission management for alarms and notifications
- User-friendly permission request flow
- Fallback handling for denied permissions

### ✅ Boot Recovery
- Automatic task rescheduling after device reboot
- Database-driven task persistence
- Hilt dependency injection for clean architecture
- Error handling and logging for troubleshooting

### ✅ Testing and Debugging
- Built-in debug panel for immediate testing
- Comprehensive logging throughout all components
- Test functions for both immediate and scheduled notifications
- Easy troubleshooting with tagged log messages

---

## 📱 User Experience Flow

1. **Task Creation**: User creates a health task with specific time
2. **Permission Check**: App ensures all necessary permissions are granted
3. **Alarm Scheduling**: System schedules precise alarm for task time
4. **Alarm Trigger**: Android AlarmManager fires at scheduled time
5. **Notification Display**: Rich notification appears with sound and vibration
6. **TTS Playback**: Foreground service speaks the task title in user's language
7. **User Interaction**: User can tap notification to open app
8. **Task Management**: User can complete, reschedule, or delete tasks
9. **Boot Recovery**: System automatically reschedules all tasks after reboot

This comprehensive system ensures users never miss their important health tasks while providing a smooth, accessible, and reliable experience across all Android devices and versions.