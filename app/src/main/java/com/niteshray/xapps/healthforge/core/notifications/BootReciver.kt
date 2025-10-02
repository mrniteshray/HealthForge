package com.niteshray.xapps.healthforge.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.niteshray.xapps.healthforge.feature.home.data.models.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import com.niteshray.xapps.healthforge.feature.home.domain.TaskRepository

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        const val TAG = "BootReceiver"
    }
    
    @Inject
    lateinit var taskRepository: TaskRepository
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "BootReceiver triggered with action: ${intent.action}")
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, rescheduling task reminders")
            
            // Use coroutine scope for async database operation
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
                        } else {
                            Log.d(TAG, "No tasks found to reschedule")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule tasks after boot", e)
                }
            }
        }
    }
}
