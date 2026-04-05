package com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.mappers

import com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.entities.*
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.models.*

// Domain to Entity mappers
fun PersonalizedDietPlan.toEntity(userId: String, userInput: UserDietInput): DietPlanEntity {
    return DietPlanEntity(
        userId = userId,
        planName = "AI Diet Plan - ${System.currentTimeMillis()}", // Generate name
        planOverview = this.planOverview,
        dailyCalorieTarget = this.dailyCalorieTarget,
        nutritionTips = this.nutritionTips,
        userName = userInput.name,
        userAge = userInput.age,
        userGender = userInput.gender,
        userHeight = userInput.height,
        userWeight = userInput.weight,
        userTargetWeight = userInput.targetWeight,
        userActivityLevel = userInput.activityLevel,
        userDietType = userInput.dietType
    )
}

fun DailyMeal.toEntity(dietPlanId: String, orderIndex: Int): DietPlanMealEntity {
    return DietPlanMealEntity(
        dietPlanId = dietPlanId,
        mealType = this.mealType.name,
        mealName = this.mealName,
        description = this.description,
        calories = this.calories,
        orderIndex = orderIndex
    )
}

fun MedicalReport.toEntity(): MedicalReportEntity {
    return MedicalReportEntity(
        id = this.id,
        name = this.name,
        filePath = this.uri?.toString(), // Convert URI to string
        isUploaded = this.isUploaded
    )
}

// Entity to Domain mappers
fun DietPlanEntity.toDomain(): PersonalizedDietPlan {
    return PersonalizedDietPlan(
        planOverview = this.planOverview,
        dailyCalorieTarget = this.dailyCalorieTarget,
        dailyMeals = emptyList(), // Will be populated separately
        nutritionTips = this.nutritionTips
    )
}

fun DietPlanMealEntity.toDomain(): DailyMeal {
    return DailyMeal(
        mealType = MealType.valueOf(this.mealType),
        mealName = this.mealName,
        description = this.description,
        calories = this.calories
    )
}

fun DietPlanWithMeals.toDomain(): PersonalizedDietPlan {
    return PersonalizedDietPlan(
        planOverview = this.dietPlan.planOverview,
        dailyCalorieTarget = this.dietPlan.dailyCalorieTarget,
        dailyMeals = this.meals.sortedBy { it.orderIndex }.map { it.toDomain() },
        nutritionTips = this.dietPlan.nutritionTips
    )
}

fun MedicalReportEntity.toDomain(): MedicalReport {
    return MedicalReport(
        id = this.id,
        name = this.name,
        uri = this.filePath?.let { android.net.Uri.parse(it) },
        isUploaded = this.isUploaded
    )
}

// Helper function to get UserDietInput from DietPlanEntity
fun DietPlanEntity.toUserDietInput(): UserDietInput {
    return UserDietInput(
        name = this.userName,
        age = this.userAge,
        gender = this.userGender,
        height = this.userHeight,
        weight = this.userWeight,
        targetWeight = this.userTargetWeight,
        activityLevel = this.userActivityLevel,
        dietType = this.userDietType
    )
}

// Data class for saved diet plan with metadata
data class SavedDietPlan(
    val id: String,
    val planName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean,
    val dailyCalorieTarget: Int,
    val mealCount: Int,
    val planOverview: String
)

fun DietPlanWithMeals.toSavedDietPlan(): SavedDietPlan {
    return SavedDietPlan(
        id = this.dietPlan.id,
        planName = this.dietPlan.planName,
        createdAt = this.dietPlan.createdAt,
        updatedAt = this.dietPlan.updatedAt,
        isActive = this.dietPlan.isActive,
        dailyCalorieTarget = this.dietPlan.dailyCalorieTarget,
        mealCount = this.meals.size,
        planOverview = this.dietPlan.planOverview
    )
}