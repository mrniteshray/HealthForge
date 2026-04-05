package com.niteshray.xapps.healthforge.feature.dietbuddy.data.models

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// User Input Models
data class UserDietInput(
    val name: String = "",
    val age: Int = 0,
    val gender: Gender = Gender.OTHER,
    val height: Float = 0f, // in cm
    val weight: Float = 0f, // in kg
    val targetWeight: Float = 0f, // in kg
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val dietType: DietType = DietType.VEGETARIAN,
    val healthGoals: List<HealthGoal> = emptyList(),
    val allergies: List<String> = emptyList(),
    val chronicConditions: List<ChronicCondition> = emptyList(),
    val additionalNotes: String = ""
)

enum class Gender {
    MALE, FEMALE, OTHER
}

enum class ActivityLevel(val displayName: String, val multiplier: Float) {
    SEDENTARY("Sedentary", 1.2f),
    LIGHT("Light Activity", 1.375f),
    MODERATE("Moderate Activity", 1.55f),
    ACTIVE("Very Active", 1.725f),
    EXTREMELY_ACTIVE("Extremely Active", 1.9f)
}

enum class DietType(val displayName: String) {
    VEGETARIAN("Vegetarian"),
    NON_VEGETARIAN("Non-Vegetarian"),
    VEGAN("Vegan"),
    EGGETARIAN("Eggetarian"),
    JAIN("Jain"),
    KETO("Keto"),
    PALEO("Paleo")
}

enum class HealthGoal(val displayName: String, val icon: ImageVector) {
    WEIGHT_LOSS("Weight Loss", Icons.Filled.TrendingDown),
    WEIGHT_GAIN("Weight Gain", Icons.Filled.TrendingUp),
    MUSCLE_GAIN("Muscle Gain", Icons.Filled.FitnessCenter),
    MAINTAIN_WEIGHT("Maintain Weight", Icons.Filled.Balance),
    IMPROVE_ENERGY("Improve Energy", Icons.Filled.Bolt),
    BETTER_DIGESTION("Better Digestion", Icons.Filled.Healing),
    HEART_HEALTH("Heart Health", Icons.Filled.Favorite),
    DIABETES_MANAGEMENT("Diabetes Management", Icons.Filled.Bloodtype)
}

enum class ChronicCondition(val displayName: String) {
    DIABETES("Diabetes"),
    HYPERTENSION("High Blood Pressure"),
    HIGH_CHOLESTEROL("High Cholesterol"),
    HEART_DISEASE("Heart Disease"),
    KIDNEY_DISEASE("Kidney Disease"),
    THYROID("Thyroid Issues"),
    PCOD_PCOS("PCOD/PCOS"),
    OBESITY("Obesity")
}

// Medical Report Models
data class MedicalReport(
    val id: String = "",
    val name: String = "",
    val uri: Uri? = null,
    val reportType: ReportType = ReportType.GENERAL_HEALTH,
    val uploadDate: Long = System.currentTimeMillis(),
    val isUploaded: Boolean = false
)

enum class ReportType(val displayName: String, val icon: ImageVector) {
    BLOOD_TEST("Blood Test", Icons.Filled.Bloodtype),
    DIABETES_PANEL("Diabetes Panel", Icons.Filled.Monitor),
    LIPID_PROFILE("Lipid Profile", Icons.Filled.Favorite),
    THYROID_FUNCTION("Thyroid Function", Icons.Filled.Psychology),
    VITAMIN_DEFICIENCY("Vitamin Panel", Icons.Filled.Healing),
    KIDNEY_FUNCTION("Kidney Function", Icons.Filled.WaterDrop),
    LIVER_FUNCTION("Liver Function", Icons.Filled.LocalHospital),
    GENERAL_HEALTH("General Health", Icons.Filled.HealthAndSafety)
}

// Diet Plan Response Models
data class PersonalizedDietPlan(
    val id: String = "",
    val userId: String = "",
    val generatedDate: Long = System.currentTimeMillis(),
    val dailyCalorieTarget: Int = 0,
    val planOverview: String = "",
    val dailyMeals: List<DailyMeal> = emptyList(),
    val nutritionTips: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val followUpRecommendations: List<String> = emptyList()
)

data class DailyMeal(
    val mealType: MealType,
    val mealName: String,
    val description: String,
    val calories: Int,
    val ingredients: List<String> = emptyList(),
    val preparationTime: String = "",
    val instructions: String = ""
)

enum class MealType(val displayName: String, val icon: ImageVector) {
    BREAKFAST("Breakfast", Icons.Filled.LightMode),
    MID_MORNING("Mid Morning", Icons.Filled.Coffee),
    LUNCH("Lunch", Icons.Filled.Restaurant),
    EVENING_SNACK("Evening Snack", Icons.Filled.Cookie),
    DINNER("Dinner", Icons.Filled.DinnerDining)
}

// UI State Models
data class DietBuddyUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val uploadedReports: List<MedicalReport> = emptyList(),
    val generatedPlan: PersonalizedDietPlan? = null
)

// API Models for Cerebras
data class DietPlanRequest(
    val userProfile: UserDietInput,
    val medicalReports: List<String>, // Base64 encoded or URLs
    val additionalContext: String = ""
)

data class DietPlanResponse(
    val success: Boolean,
    val dietPlan: PersonalizedDietPlan?,
    val error: String? = null
)