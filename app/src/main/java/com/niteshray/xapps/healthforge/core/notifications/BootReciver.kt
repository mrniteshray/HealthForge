package com.niteshray.xapps.healthforge.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import com.niteshray.xapps.healthforge.feature.home.domain.TaskTrackingRepository
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.toDataTask

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        const val TAG = "BootReceiver"
    }
    
    @Inject
    lateinit var taskTrackingRepository: TaskTrackingRepository
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "BootReceiver triggered with action: ${intent.action}")
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, rescheduling task reminders")
            
            // Use coroutine scope for async database operation
            val coroutineScope = CoroutineScope(Dispatchers.IO)
            
            coroutineScope.launch {
                try {
                    // Load all active templates from database
                    val templates = taskTrackingRepository.getAllActiveTemplates().first()
                    if (templates.isNotEmpty()) {
                        Log.d(TAG, "Found ${templates.size} task templates to reschedule")
                        
                        // Convert templates to tasks and schedule reminders
                        val dataTaskList = templates.map { it.toDataTask() }
                        ReminderManager.scheduleAllTasks(context, dataTaskList)
                        
                        Log.d(TAG, "All task reminders rescheduled after boot")
                    } else {
                        Log.d(TAG, "No task templates found to reschedule")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule tasks after boot", e)
                }
            }
        }
    }
}
