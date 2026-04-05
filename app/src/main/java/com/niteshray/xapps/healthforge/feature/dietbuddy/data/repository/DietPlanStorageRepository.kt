package com.niteshray.xapps.healthforge.feature.dietbuddy.data.repository

import com.niteshray.xapps.healthforge.feature.dietbuddy.data.models.*
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.mappers.SavedDietPlan
import kotlinx.coroutines.flow.Flow

interface DietPlanStorageRepository {
    
    // Diet Plan Operations
    suspend fun saveDietPlan(
        dietPlan: PersonalizedDietPlan,
        userInput: UserDietInput,
        userId: String,
        planName: String? = null
    ): Result<String>
    
    suspend fun getDietPlan(planId: String): Result<PersonalizedDietPlan?>
    
    suspend fun getActiveDietPlan(userId: String): Result<PersonalizedDietPlan?>
    
    fun getSavedDietPlans(userId: String): Flow<List<SavedDietPlan>>
    
    suspend fun deleteDietPlan(planId: String): Result<Unit>
    
    suspend fun activateDietPlan(planId: String, userId: String): Result<Unit>
    
    // Meal Operations
    suspend fun updateMeal(planId: String, meal: DailyMeal): Result<Unit>
    
    suspend fun deleteMeal(planId: String, mealType: String): Result<Unit>
    
    suspend fun addMeal(planId: String, meal: DailyMeal): Result<Unit>
    
    // Medical Report Operations
    suspend fun saveMedicalReport(report: MedicalReport): Result<Unit>
    
    suspend fun deleteMedicalReport(reportId: String): Result<Unit>
    
    fun getAllMedicalReports(): Flow<List<MedicalReport>>
    
    // Sync Operations
    suspend fun syncWithCloud(userId: String): Result<Unit>
    
    suspend fun uploadToCloud(planId: String): Result<Unit>
    
    suspend fun downloadFromCloud(userId: String): Result<List<PersonalizedDietPlan>>
}