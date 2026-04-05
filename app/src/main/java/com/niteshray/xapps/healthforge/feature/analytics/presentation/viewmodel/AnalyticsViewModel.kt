package com.niteshray.xapps.healthforge.feature.analytics.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskTemplate
import com.niteshray.xapps.healthforge.feature.home.data.models.DailyTaskRecord
import com.niteshray.xapps.healthforge.feature.home.domain.TaskTrackingRepository
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TaskCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val taskTrackingRepository: TaskTrackingRepository
) : ViewModel()
{

    companion object {
        private const val TAG = "AnalyticsViewModel"
    }

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        // Automatically observe templates and reload analytics when they change
        viewModelScope.launch {
            taskTrackingRepository.getAllActiveTemplates().collectLatest { 
                loadAnalyticsData()
            }
        }
    }

    fun loadAnalyticsData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                Log.d(TAG, "Loading analytics data")
                
                // Ensure daily records exist for today first
                taskTrackingRepository.ensureTodayRecordsExist()
                
                // Get all active templates
                val templates = taskTrackingRepository.getAllActiveTemplates().first()
                Log.d(TAG, "Loaded ${templates.size} active templates")
                
                if (templates.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        templates = emptyList(),
                        templateRecords = emptyMap()
                    )
                    return@launch
                }
                
                // Load records for each template from creation date
                val templateRecordsMap = mutableMapOf<Int, List<DailyTaskRecord>>()
                var totalTasks = 0
                var completedTasks = 0
                val categoryStatsMap = mutableMapOf<TaskCategory, Pair<Int, Int>>()
                
                templates.forEach { template ->
                    try {
                        val records = taskTrackingRepository.getRecordsFromTemplateCreation(template.id)
                        templateRecordsMap[template.id] = records
                        
                        // Calculate stats
                        val templateTotal = records.size
                        val templateCompleted = records.count { it.isCompleted }
                        
                        totalTasks += templateTotal
                        completedTasks += templateCompleted
                        
                        // Update category stats
                        val currentCategoryStats = categoryStatsMap[template.category] ?: Pair(0, 0)
                        categoryStatsMap[template.category] = Pair(
                            currentCategoryStats.first + templateCompleted,
                            currentCategoryStats.second + templateTotal
                        )
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading records for template ${template.id}", e)
                    }
                }
                
                // Calculate overall completion rate
                val overallCompletionRate = if (totalTasks > 0) {
                    (completedTasks * 100) / totalTasks
                } else 0
                
                // Calculate current streak
                val currentStreak = calculateCurrentStreak(templateRecordsMap.values.flatten())
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    templates = templates,
                    templateRecords = templateRecordsMap,
                    totalTasksCount = totalTasks,
                    completedTasksCount = completedTasks,
                    overallCompletionRate = overallCompletionRate,
                    currentStreak = currentStreak,
                    categoryStats = categoryStatsMap
                )
                
                Log.d(TAG, "Analytics data loaded successfully. Overall completion: $overallCompletionRate%, Streak: $currentStreak days")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading analytics data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load analytics data: ${e.message}"
                )
            }
        }
    }
    
    private fun calculateCurrentStreak(allRecords: List<DailyTaskRecord>): Int {
        if (allRecords.isEmpty()) return 0
        
        try {
            // Group records by date and check if all tasks for that date were completed
            val recordsByDate = allRecords.groupBy { it.date }
            val sortedDates = recordsByDate.keys.sorted().reversed() // Most recent first
            
            var streak = 0
            val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            
            for (date in sortedDates) {
                // Skip future dates
                if (date > today) continue
                
                val dayRecords = recordsByDate[date] ?: continue
                val allCompleted = dayRecords.isNotEmpty() && dayRecords.all { it.isCompleted }
                
                if (allCompleted) {
                    streak++
                } else {
                    // If it's not today and tasks are not completed, break the streak
                    // If it's today, continue counting (user might still complete tasks)
                    if (date != today) {
                        break
                    }
                }
            }
            
            Log.d(TAG, "Calculated current streak: $streak days")
            return streak
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating streak", e)
            return 0
        }
    }
    
    fun refreshData() {
        Log.d(TAG, "Refreshing analytics data")
        loadAnalyticsData()
    }
}
data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val templates: List<TaskTemplate> = emptyList(),
    val templateRecords: Map<Int, List<DailyTaskRecord>> = emptyMap(),
    val totalTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val overallCompletionRate: Int = 0,
    val currentStreak: Int = 0,
    val categoryStats: Map<TaskCategory, Pair<Int, Int>> = emptyMap() // category to (completed, total)
)