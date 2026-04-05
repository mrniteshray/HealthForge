package com.niteshray.xapps.healthforge.feature.dietbuddy.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.dao.*
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.mappers.*
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.models.*
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.repository.DietPlanStorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DietPlanStorageRepositoryImpl @Inject constructor(
    private val dietPlanDao: DietPlanDao,
    private val mealDao: DietPlanMealDao,
    private val reportDao: MedicalReportDao,
    private val firestore: FirebaseFirestore
) : DietPlanStorageRepository {
    
    override suspend fun saveDietPlan(
        dietPlan: PersonalizedDietPlan,
        userInput: UserDietInput,
        userId: String,
        planName: String?
    ): Result<String> {
        return try {
            // Deactivate current active plan
            dietPlanDao.deactivateAllPlans(userId)
            
            // Create plan entity
            val planEntity = dietPlan.toEntity(userId, userInput).copy(
                planName = planName ?: "AI Diet Plan - ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}"
            )
            
            // Insert plan
            dietPlanDao.insertDietPlan(planEntity)
            
            // Insert meals
            val mealEntities = dietPlan.dailyMeals.mapIndexed { index, meal ->
                meal.toEntity(planEntity.id, index)
            }
            mealDao.insertMeals(mealEntities)
            
            // Upload to cloud in background
            try {
                uploadToCloud(planEntity.id)
            } catch (e: Exception) {
                // Log error but don't fail the local save
                e.printStackTrace()
            }
            
            Result.success(planEntity.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getDietPlan(planId: String): Result<PersonalizedDietPlan?> {
        return try {
            val planWithMeals = dietPlanDao.getDietPlanWithMeals(planId)
            Result.success(planWithMeals?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getActiveDietPlan(userId: String): Result<PersonalizedDietPlan?> {
        return try {
            val activePlan = dietPlanDao.getActiveDietPlanWithMeals(userId)
            Result.success(activePlan?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getSavedDietPlans(userId: String): Flow<List<SavedDietPlan>> {
        return dietPlanDao.getDietPlansWithMealsByUser(userId)
            .map { plans ->
                plans.map { it.toSavedDietPlan() }
            }
    }
    
    override suspend fun deleteDietPlan(planId: String): Result<Unit> {
        return try {
            dietPlanDao.deleteDietPlanById(planId)
            // Also delete from Firestore
            try {
                firestore.collection("diet_plans").document(planId).delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun activateDietPlan(planId: String, userId: String): Result<Unit> {
        return try {
            dietPlanDao.deactivateAllPlans(userId)
            dietPlanDao.activatePlan(planId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateMeal(planId: String, meal: DailyMeal): Result<Unit> {
        return try {
            val existingMeals = mealDao.getMealsByPlanId(planId)
            val existingMeal = existingMeals.find { it.mealType == meal.mealType.name }
            
            if (existingMeal != null) {
                val updatedMeal = existingMeal.copy(
                    mealName = meal.mealName,
                    description = meal.description,
                    calories = meal.calories
                )
                mealDao.updateMeal(updatedMeal)
                
                // Update timestamp of diet plan
                val plan = dietPlanDao.getDietPlanById(planId)
                plan?.let {
                    dietPlanDao.updateDietPlan(it.copy(updatedAt = System.currentTimeMillis()))
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteMeal(planId: String, mealType: String): Result<Unit> {
        return try {
            val meals = mealDao.getMealsByPlanId(planId)
            val mealToDelete = meals.find { it.mealType == mealType }
            
            if (mealToDelete != null) {
                mealDao.deleteMeal(mealToDelete)
                
                // Update timestamp of diet plan
                val plan = dietPlanDao.getDietPlanById(planId)
                plan?.let {
                    dietPlanDao.updateDietPlan(it.copy(updatedAt = System.currentTimeMillis()))
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun addMeal(planId: String, meal: DailyMeal): Result<Unit> {
        return try {
            val existingMeals = mealDao.getMealsByPlanId(planId)
            val orderIndex = existingMeals.size
            
            val mealEntity = meal.toEntity(planId, orderIndex)
            mealDao.insertMeal(mealEntity)
            
            // Update timestamp of diet plan
            val plan = dietPlanDao.getDietPlanById(planId)
            plan?.let {
                dietPlanDao.updateDietPlan(it.copy(updatedAt = System.currentTimeMillis()))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveMedicalReport(report: MedicalReport): Result<Unit> {
        return try {
            val entity = report.toEntity()
            reportDao.insertReport(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteMedicalReport(reportId: String): Result<Unit> {
        return try {
            reportDao.deleteReportById(reportId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getAllMedicalReports(): Flow<List<MedicalReport>> {
        return reportDao.getAllReports().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun syncWithCloud(userId: String): Result<Unit> {
        return try {
            // Download from cloud and merge with local
            downloadFromCloud(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun uploadToCloud(planId: String): Result<Unit> {
        return try {
            val planWithMeals = dietPlanDao.getDietPlanWithMeals(planId)
            if (planWithMeals != null) {
                val cloudData = mapOf(
                    "dietPlan" to planWithMeals.dietPlan,
                    "meals" to planWithMeals.meals,
                    "timestamp" to System.currentTimeMillis()
                )
                
                firestore.collection("diet_plans")
                    .document(planId)
                    .set(cloudData)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun downloadFromCloud(userId: String): Result<List<PersonalizedDietPlan>> {
        return try {
            val querySnapshot = firestore.collection("diet_plans")
                .whereEqualTo("dietPlan.userId", userId)
                .get()
                .await()
            
            val cloudPlans = mutableListOf<PersonalizedDietPlan>()
            
            for (document in querySnapshot.documents) {
                try {
                    // Parse cloud data and save locally if newer
                    val cloudData = document.data
                    // Implementation would depend on your Firestore data structure
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            Result.success(cloudPlans)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}