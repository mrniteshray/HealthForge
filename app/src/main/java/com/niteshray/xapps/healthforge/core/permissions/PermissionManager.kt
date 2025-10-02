package com.niteshray.xapps.healthforge.core.permissions

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.core.app.NotificationManagerCompat

class PermissionManager(private val activity: ComponentActivity) {
    
    companion object {
        const val TAG = "PermissionManager"
        const val REQUEST_CODE_EXACT_ALARM = 1001
        const val REQUEST_CODE_NOTIFICATION = 1002
    }
    
    private var onPermissionsGranted: (() -> Unit)? = null
    private var onPermissionsDenied: (() -> Unit)? = null
    
    // Permission launcher for runtime permissions
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Log.d(TAG, "Permission results: $permissions")
            
            val deniedPermissions = permissions.filter { !it.value }.keys
            if (deniedPermissions.isEmpty()) {
                Log.d(TAG, "All runtime permissions granted")
                checkSpecialPermissions()
            } else {
                Log.w(TAG, "Denied permissions: $deniedPermissions")
                onPermissionsDenied?.invoke()
            }
        }
    
    // Exact alarm permission launcher (Android 12+)
    private val exactAlarmLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d(TAG, "Exact alarm permission result received")
            checkNotificationPermission()
        }
    
    // Notification settings launcher
    private val notificationSettingsLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d(TAG, "Notification settings result received")
            if (areAllPermissionsGranted()) {
                Log.d(TAG, "All permissions now granted after settings")
                onPermissionsGranted?.invoke()
            } else {
                Log.w(TAG, "Still missing some permissions after settings")
                onPermissionsDenied?.invoke()
            }
        }
    
    fun requestAllPermissions(
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        Log.d(TAG, "Starting permission request process")
        onPermissionsGranted = onGranted
        onPermissionsDenied = onDenied
        
        if (areAllPermissionsGranted()) {
            Log.d(TAG, "All permissions already granted")
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
                Log.d(TAG, "Need POST_NOTIFICATIONS permission")
            }
        }
        
        // Audio permissions for TTS
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.MODIFY_AUDIO_SETTINGS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
            Log.d(TAG, "Need MODIFY_AUDIO_SETTINGS permission")
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Requesting runtime permissions: $permissionsToRequest")
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d(TAG, "No runtime permissions needed, checking special permissions")
            checkSpecialPermissions()
        }
    }
    
    private fun checkSpecialPermissions() {
        Log.d(TAG, "Checking special permissions")
        
        // Check exact alarm permission first (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.d(TAG, "Need SCHEDULE_EXACT_ALARM permission")
                requestExactAlarmPermission()
                return
            }
        }
        
        checkNotificationPermission()
    }
    
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(TAG, "Requesting exact alarm permission")
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            exactAlarmLauncher.launch(intent)
        }
    }
    
    private fun checkNotificationPermission() {
        Log.d(TAG, "Checking notification permission")
        
        if (!NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
            Log.d(TAG, "Notifications are disabled, opening settings")
            openNotificationSettings()
        } else {
            Log.d(TAG, "All permissions granted successfully")
            onPermissionsGranted?.invoke()
        }
    }
    
    private fun openNotificationSettings() {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                }
                else -> {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.parse("package:${activity.packageName}")
                }
            }
        }
        notificationSettingsLauncher.launch(intent)
    }
    
    fun areAllPermissionsGranted(): Boolean {
        Log.d(TAG, "Checking all permissions status")
        
        // Runtime permissions
        val runtimePermissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runtimePermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        runtimePermissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
        
        for (permission in runtimePermissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Missing permission: $permission")
                return false
            }
        }
        
        // Exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.d(TAG, "Missing exact alarm permission")
                return false
            }
        }
        
        // Notification enabled check
        if (!NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
            Log.d(TAG, "Notifications are disabled")
            return false
        }
        
        Log.d(TAG, "All permissions are granted")
        return true
    }
    
    fun checkPermissionStatus(): String {
        val status = StringBuilder()
        status.append("Permission Status:\n")
        
        // Runtime permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val postNotificationGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            status.append("POST_NOTIFICATIONS: ${if (postNotificationGranted) "GRANTED" else "DENIED"}\n")
        }
        
        val audioPermissionGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED
        status.append("MODIFY_AUDIO_SETTINGS: ${if (audioPermissionGranted) "GRANTED" else "DENIED"}\n")
        
        // Exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val exactAlarmGranted = alarmManager.canScheduleExactAlarms()
            status.append("SCHEDULE_EXACT_ALARM: ${if (exactAlarmGranted) "GRANTED" else "DENIED"}\n")
        }
        
        // Notification enabled
        val notificationsEnabled = NotificationManagerCompat.from(activity).areNotificationsEnabled()
        status.append("NOTIFICATIONS_ENABLED: ${if (notificationsEnabled) "YES" else "NO"}\n")
        
        return status.toString()
    }
}