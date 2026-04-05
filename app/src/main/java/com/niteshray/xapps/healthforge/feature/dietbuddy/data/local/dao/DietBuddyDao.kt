package com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.dao

import androidx.room.*
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DietPlanDao {
    
    @Query("SELECT * FROM diet_plans WHERE userId = :userId ORDER BY createdAt DESC")
    fun getDietPlansByUser(userId: String): Flow<List<DietPlanEntity>>
    
    @Query("SELECT * FROM diet_plans WHERE id = :planId")
    suspend fun getDietPlanById(planId: String): DietPlanEntity?
    
    @Transaction
    @Query("SELECT * FROM diet_plans WHERE id = :planId")
    suspend fun getDietPlanWithMeals(planId: String): DietPlanWithMeals?
    
    @Transaction
    @Query("SELECT * FROM diet_plans WHERE userId = :userId ORDER BY createdAt DESC")
    fun getDietPlansWithMealsByUser(userId: String): Flow<List<DietPlanWithMeals>>
    
    @Query("SELECT * FROM diet_plans WHERE isActive = 1 AND userId = :userId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveDietPlan(userId: String): DietPlanEntity?
    
    @Transaction
    @Query("SELECT * FROM diet_plans WHERE isActive = 1 AND userId = :userId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveDietPlanWithMeals(userId: String): DietPlanWithMeals?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietPlan(dietPlan: DietPlanEntity): Long
    
    @Update
    suspend fun updateDietPlan(dietPlan: DietPlanEntity)
    
    @Query("UPDATE diet_plans SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateAllPlans(userId: String)
    
    @Query("UPDATE diet_plans SET isActive = 1, updatedAt = :timestamp WHERE id = :planId")
    suspend fun activatePlan(planId: String, timestamp: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deleteDietPlan(dietPlan: DietPlanEntity)
    
    @Query("DELETE FROM diet_plans WHERE id = :planId")
    suspend fun deleteDietPlanById(planId: String)
    
    @Query("SELECT COUNT(*) FROM diet_plans WHERE userId = :userId")
    suspend fun getDietPlanCount(userId: String): Int
}

@Dao
interface DietPlanMealDao {
    
    @Query("SELECT * FROM diet_plan_meals WHERE dietPlanId = :planId ORDER BY orderIndex ASC")
    suspend fun getMealsByPlanId(planId: String): List<DietPlanMealEntity>
    
    @Query("SELECT * FROM diet_plan_meals WHERE id = :mealId")
    suspend fun getMealById(mealId: String): DietPlanMealEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: DietPlanMealEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<DietPlanMealEntity>)
    
    @Update
    suspend fun updateMeal(meal: DietPlanMealEntity)
    
    @Delete
    suspend fun deleteMeal(meal: DietPlanMealEntity)
    
    @Query("DELETE FROM diet_plan_meals WHERE id = :mealId")
    suspend fun deleteMealById(mealId: String)
    
    @Query("DELETE FROM diet_plan_meals WHERE dietPlanId = :planId")
    suspend fun deleteMealsByPlanId(planId: String)
}

@Dao
interface MedicalReportDao {
    
    @Query("SELECT * FROM medical_reports ORDER BY uploadedAt DESC")
    fun getAllReports(): Flow<List<MedicalReportEntity>>
    
    @Query("SELECT * FROM medical_reports WHERE id = :reportId")
    suspend fun getReportById(reportId: String): MedicalReportEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: MedicalReportEntity): Long
    
    @Update
    suspend fun updateReport(report: MedicalReportEntity)
    
    @Delete
    suspend fun deleteReport(report: MedicalReportEntity)
    
    @Query("DELETE FROM medical_reports WHERE id = :reportId")
    suspend fun deleteReportById(reportId: String)
    
    @Query("SELECT COUNT(*) FROM medical_reports")
    suspend fun getReportCount(): Int
}