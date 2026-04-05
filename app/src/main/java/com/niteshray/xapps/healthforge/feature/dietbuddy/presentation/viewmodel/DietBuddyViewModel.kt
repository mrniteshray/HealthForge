package com.niteshray.xapps.healthforge.feature.dietbuddy.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.api.DietBuddyApiService
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.models.*
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.repository.DietPlanStorageRepository
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.mappers.SavedDietPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DietBuddyViewModel @Inject constructor(
    private val apiService: DietBuddyApiService,
    private val storageRepository: DietPlanStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DietBuddyUiState())
    val uiState: StateFlow<DietBuddyUiState> = _uiState.asStateFlow()
    
    // Storage-related state
    private val _savedPlans = MutableStateFlow<List<SavedDietPlan>>(emptyList())
    val savedPlans: StateFlow<List<SavedDietPlan>> = _savedPlans.asStateFlow()
    
    private val _currentPlanId = MutableStateFlow<String?>(null)
    val currentPlanId: StateFlow<String?> = _currentPlanId.asStateFlow()
    
    init {
        // Load active diet plan and saved plans on initialization
        loadActiveDietPlan()
        loadSavedPlans()
    }

    fun uploadReportAndGeneratePlan(uri: Uri, reportName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

                // Add the report to the list
                val newReport = MedicalReport(
                    id = System.currentTimeMillis().toString(),
                    name = reportName,
                    uri = uri,
                    isUploaded = true
                )

                val updatedReports = _uiState.value.uploadedReports + newReport
                _uiState.value = _uiState.value.copy(
                    uploadedReports = updatedReports
                )

                // Create a simple user input with minimal required data
                val simpleUserInput = UserDietInput(
                    name = "User",
                    age = 30,
                    gender = Gender.OTHER,
                    height = 170f,
                    weight = 70f,
                    targetWeight = 65f,
                    activityLevel = ActivityLevel.MODERATE,
                    dietType = DietType.VEGETARIAN
                )

                // Generate diet plan using the API
                val result = apiService.generatePersonalizedDietPlan(
                    userInput = simpleUserInput,
                    medicalReports = updatedReports
                )

                result.onSuccess { dietPlan ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generatedPlan = dietPlan,
                        error = null
                    )
                    
                    // Auto-save the generated plan
                    saveDietPlan(dietPlan, simpleUserInput)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to generate diet plan"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
            }
        }
    }

    fun removeMedicalReport(reportId: String) {
        _uiState.value = _uiState.value.copy(
            uploadedReports = _uiState.value.uploadedReports.filter { it.id != reportId }
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun regeneratePlan() {
        val currentReports = _uiState.value.uploadedReports
        if (currentReports.isNotEmpty()) {
            // Use the first report to regenerate
            val firstReport = currentReports.first()
            firstReport.uri?.let { uri ->
                uploadReportAndGeneratePlan(uri, firstReport.name)
            }
        }
    }

    fun updateMeal(updatedMeal: DailyMeal) {
        val currentPlan = _uiState.value.generatedPlan ?: return
        val updatedMeals = currentPlan.dailyMeals.map { meal ->
            if (meal.mealType == updatedMeal.mealType) updatedMeal else meal
        }
        val updatedPlan = currentPlan.copy(dailyMeals = updatedMeals)
        _uiState.value = _uiState.value.copy(generatedPlan = updatedPlan)
        
        // Update in storage if we have a saved plan
        val currentPlanId = _currentPlanId.value
        if (currentPlanId != null) {
            viewModelScope.launch {
                storageRepository.updateMeal(currentPlanId, updatedMeal)
            }
        }
    }

    fun deleteMeal(mealToDelete: DailyMeal) {
        val currentPlan = _uiState.value.generatedPlan ?: return
        val currentPlanId = _currentPlanId.value
        
        val updatedMeals = currentPlan.dailyMeals.filter { meal ->
            meal.mealType != mealToDelete.mealType
        }
        val updatedPlan = currentPlan.copy(dailyMeals = updatedMeals)
        _uiState.value = _uiState.value.copy(generatedPlan = updatedPlan)
        
        // Update in storage if we have a saved plan
        if (currentPlanId != null) {
            viewModelScope.launch {
                storageRepository.deleteMeal(currentPlanId, mealToDelete.mealType.name)
            }
        }
    }
    
    // Storage-related functions
    private fun loadActiveDietPlan() {
        viewModelScope.launch {
            val result = storageRepository.getActiveDietPlan("current_user") // Replace with actual user ID
            result.onSuccess { dietPlan ->
                if (dietPlan != null) {
                    _uiState.value = _uiState.value.copy(generatedPlan = dietPlan)
                }
            }
        }
    }
    
    private fun loadSavedPlans() {
        viewModelScope.launch {
            storageRepository.getSavedDietPlans("current_user").collect { plans ->
                _savedPlans.value = plans
            }
        }
    }
    
    fun saveDietPlan(dietPlan: PersonalizedDietPlan, userInput: UserDietInput, planName: String? = null) {
        viewModelScope.launch {
            val result = storageRepository.saveDietPlan(
                dietPlan = dietPlan,
                userInput = userInput,
                userId = "current_user", // Replace with actual user ID
                planName = planName
            )
            
            result.onSuccess { planId ->
                _currentPlanId.value = planId
                loadSavedPlans() // Refresh the list
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    error = "Failed to save diet plan: ${exception.message}"
                )
            }
        }
    }

    // Overloaded method for UI calls with just plan name
    fun saveDietPlan(planName: String) {
        val currentPlan = _uiState.value.generatedPlan
        if (currentPlan != null) {
            // Create a simple UserDietInput from available data or use defaults
            val defaultUserInput = UserDietInput(
                name = "User",
                age = 25,
                gender = Gender.OTHER,
                height = 170f,
                weight = 70f,
                targetWeight = 65f,
                activityLevel = ActivityLevel.MODERATE,
                dietType = DietType.VEGETARIAN
            )
            
            saveDietPlan(currentPlan, defaultUserInput, planName)
        } else {
            _uiState.value = _uiState.value.copy(
                error = "No diet plan available to save"
            )
        }
    }
    
    fun loadDietPlan(planId: String) {
        viewModelScope.launch {
            val result = storageRepository.getDietPlan(planId)
            result.onSuccess { dietPlan ->
                if (dietPlan != null) {
                    _uiState.value = _uiState.value.copy(generatedPlan = dietPlan)
                    _currentPlanId.value = planId
                    
                    // Activate this plan
                    storageRepository.activateDietPlan(planId, "current_user")
                }
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load diet plan: ${exception.message}"
                )
            }
        }
    }
    
    fun deleteSavedPlan(planId: String) {
        viewModelScope.launch {
            val result = storageRepository.deleteDietPlan(planId)
            result.onSuccess {
                // If we deleted the current active plan, clear it
                if (_currentPlanId.value == planId) {
                    _currentPlanId.value = null
                    _uiState.value = _uiState.value.copy(generatedPlan = null)
                }
                loadSavedPlans() // Refresh the list
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete diet plan: ${exception.message}"
                )
            }
        }
    }
    
    fun saveMealUpdate(updatedMeal: DailyMeal) {
        updateMeal(updatedMeal)
        
        // Also save to storage if we have a current plan
        val currentPlanId = _currentPlanId.value
        if (currentPlanId != null) {
            viewModelScope.launch {
                storageRepository.updateMeal(currentPlanId, updatedMeal)
            }
        }
    }
    
    fun syncWithCloud() {
        viewModelScope.launch {
            val result = storageRepository.syncWithCloud("current_user")
            result.onSuccess {
                loadSavedPlans() // Refresh after sync
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    error = "Sync failed: ${exception.message}"
                )
            }
        }
    }
}