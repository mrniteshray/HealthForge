package com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.entities

import androidx.room.*
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.models.ActivityLevel
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.models.DietType
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.models.Gender
import java.util.*

@Entity(tableName = "diet_plans")
data class DietPlanEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val planName: String,
    val planOverview: String,
    val dailyCalorieTarget: Int,
    val nutritionTips: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    // User info for context
    val userName: String,
    val userAge: Int,
    val userGender: Gender,
    val userHeight: Float,
    val userWeight: Float,
    val userTargetWeight: Float,
    val userActivityLevel: ActivityLevel,
    val userDietType: DietType
)

@Entity(
    tableName = "diet_plan_meals",
    foreignKeys = [
        ForeignKey(
            entity = DietPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["dietPlanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dietPlanId"])]
)
data class DietPlanMealEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val dietPlanId: String,
    val mealType: String, // Store as string for Room compatibility
    val mealName: String,
    val description: String,
    val calories: Int,
    val orderIndex: Int = 0
)

@Entity(tableName = "medical_reports")
data class MedicalReportEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val filePath: String?, // Local file path
    val uploadedAt: Long = System.currentTimeMillis(),
    val fileSize: Long = 0L,
    val isUploaded: Boolean = true
)

// Relationship data classes for queries
data class DietPlanWithMeals(
    @Embedded val dietPlan: DietPlanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "dietPlanId"
    )
    val meals: List<DietPlanMealEntity>
)