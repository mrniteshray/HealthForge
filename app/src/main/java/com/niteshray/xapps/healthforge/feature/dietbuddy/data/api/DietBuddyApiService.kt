package com.niteshray.xapps.healthforge.feature.dietbuddy.data.api

import com.niteshray.xapps.healthforge.core.di.GeminiApi
import com.niteshray.xapps.healthforge.core.di.ChatRequest
import com.niteshray.xapps.healthforge.core.di.Message
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.models.MedicalReport
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.models.PersonalizedDietPlan
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.models.UserDietInput
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.models.DailyMeal
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.models.MealType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DietBuddyApiService @Inject constructor(
    private val geminiApi: GeminiApi
) {
    
    suspend fun generatePersonalizedDietPlan(
        userInput: UserDietInput,
        medicalReports: List<MedicalReport>
    ): Result<PersonalizedDietPlan> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildDietPlanPrompt(userInput, medicalReports)
            
            val request = ChatRequest(
                messages = listOf(
                    Message(
                        role = "system",
                        content = """You are an expert nutritionist and dietitian AI. Create personalized diet plans based on user profile and medical reports. 
                        Always provide responses in valid JSON format only, no additional text.
                        Consider medical conditions, allergies, and health goals when creating plans.
                        Provide practical, Indian-friendly meal suggestions with calorie information."""
                    ),
                    Message(role = "user", content = prompt)
                ),
                max_tokens = 2000,
                temperature = 0.7f
            )
            
            val response = geminiApi.generateContent(request)
            val content = response.choices.firstOrNull()?.message?.content
                ?: throw Exception("Empty response from AI service")
            
            val dietPlan = parseDietPlanResponse(content, userInput.name)
            Result.success(dietPlan)
            
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate diet plan: ${e.message}"))
        }
    }
    
    private fun buildDietPlanPrompt(
        userInput: UserDietInput,
        medicalReports: List<MedicalReport>
    ): String {
        return """
        Create a personalized 7-day diet plan based on the following information:
        
        USER PROFILE:
        - Name: ${userInput.name}
        - Age: ${userInput.age} years
        - Gender: ${userInput.gender}
        - Height: ${userInput.height} cm
        - Current Weight: ${userInput.weight} kg
        - Target Weight: ${userInput.targetWeight} kg
        - Activity Level: ${userInput.activityLevel.displayName}
        - Diet Type: ${userInput.dietType.displayName}
        - Health Goals: ${userInput.healthGoals.joinToString(", ") { it.displayName }}
        - Chronic Conditions: ${userInput.chronicConditions.joinToString(", ") { it.displayName }}
        - Allergies: ${userInput.allergies.joinToString(", ")}
        - Additional Notes: ${userInput.additionalNotes}
        
        MEDICAL REPORTS UPLOADED: ${medicalReports.size} reports
        ${medicalReports.joinToString("\n") { "- ${it.name} (${it.reportType.displayName})" }}
        
        Please respond in this EXACT JSON format:
        {
          "planOverview": "Brief overview of the diet plan approach",
          "dailyCalorieTarget": calculated_daily_calories_as_number,
          "dailyMeals": [
            {
              "mealType": "BREAKFAST",
              "mealName": "Meal name",
              "description": "Brief description",
              "calories": calories_as_number,
              "ingredients": ["ingredient1", "ingredient2"],
              "preparationTime": "X minutes",
              "instructions": "Simple preparation steps"
            }
          ],
          "nutritionTips": ["tip1", "tip2", "tip3"],
          "warnings": ["warning1 if any"],
          "followUpRecommendations": ["recommendation1", "recommendation2"]
        }
        
        Include meals for: BREAKFAST, MID_MORNING, LUNCH, EVENING_SNACK, DINNER
        Focus on Indian cuisine and ${userInput.dietType.displayName} options.
        Consider the medical conditions and provide appropriate warnings if needed.
        """.trimIndent()
    }
    
    private fun parseDietPlanResponse(content: String, userName: String): PersonalizedDietPlan {
        try {
            // Clean the response content
            val cleanedContent = content.trim()
                .removePrefix("```json")
                .removeSuffix("```")
                .trim()
            
            val jsonObject = JSONObject(cleanedContent)
            
            val mealsArray = jsonObject.getJSONArray("dailyMeals")
            val dailyMeals = mutableListOf<DailyMeal>()
            
            for (i in 0 until mealsArray.length()) {
                val mealObj = mealsArray.getJSONObject(i)
                val mealType = MealType.valueOf(mealObj.getString("mealType"))
                
                val ingredientsArray = mealObj.getJSONArray("ingredients")
                val ingredients = mutableListOf<String>()
                for (j in 0 until ingredientsArray.length()) {
                    ingredients.add(ingredientsArray.getString(j))
                }
                
                dailyMeals.add(
                    DailyMeal(
                        mealType = mealType,
                        mealName = mealObj.getString("mealName"),
                        description = mealObj.getString("description"),
                        calories = mealObj.getInt("calories"),
                        ingredients = ingredients,
                        preparationTime = mealObj.getString("preparationTime"),
                        instructions = mealObj.getString("instructions")
                    )
                )
            }
            
            return PersonalizedDietPlan(
                id = "plan_${System.currentTimeMillis()}",
                userId = userName,
                planOverview = jsonObject.getString("planOverview"),
                dailyCalorieTarget = jsonObject.getInt("dailyCalorieTarget"),
                dailyMeals = dailyMeals,
                nutritionTips = jsonArrayToStringList(jsonObject.getJSONArray("nutritionTips")),
                warnings = jsonArrayToStringList(jsonObject.getJSONArray("warnings")),
                followUpRecommendations = jsonArrayToStringList(jsonObject.getJSONArray("followUpRecommendations"))
            )
            
        } catch (e: Exception) {
            // Fallback response if parsing fails
            return PersonalizedDietPlan(
                id = "fallback_plan",
                userId = userName,
                planOverview = "A balanced diet plan has been created for you based on your profile.",
                dailyCalorieTarget = 1800,
                dailyMeals = createFallbackMeals(),
                nutritionTips = listOf(
                    "Drink plenty of water throughout the day",
                    "Eat meals at regular intervals",
                    "Include variety in your diet"
                ),
                warnings = listOf("Please consult with a healthcare provider for personalized advice"),
                followUpRecommendations = listOf(
                    "Monitor your progress weekly",
                    "Adjust portions as needed",
                    "Stay consistent with the plan"
                )
            )
        }
    }
    
    private fun jsonArrayToStringList(jsonArray: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }
    
    private fun createFallbackMeals(): List<DailyMeal> {
        return listOf(
            DailyMeal(
                mealType = MealType.BREAKFAST,
                mealName = "Oats with Fruits",
                description = "Healthy breakfast with fiber and vitamins",
                calories = 300,
                ingredients = listOf("Oats", "Banana", "Milk", "Honey"),
                preparationTime = "10 minutes",
                instructions = "Cook oats with milk, add chopped banana and honey"
            ),
            DailyMeal(
                mealType = MealType.MID_MORNING,
                mealName = "Green Tea & Nuts",
                description = "Antioxidant-rich snack",
                calories = 150,
                ingredients = listOf("Green tea", "Mixed nuts"),
                preparationTime = "5 minutes",
                instructions = "Brew green tea and have with a handful of nuts"
            ),
            DailyMeal(
                mealType = MealType.LUNCH,
                mealName = "Dal Rice with Vegetables",
                description = "Complete protein with complex carbs",
                calories = 450,
                ingredients = listOf("Dal", "Rice", "Mixed vegetables", "Spices"),
                preparationTime = "30 minutes",
                instructions = "Cook dal and rice, prepare vegetable curry"
            ),
            DailyMeal(
                mealType = MealType.EVENING_SNACK,
                mealName = "Herbal Tea & Biscuits",
                description = "Light evening refreshment",
                calories = 120,
                ingredients = listOf("Herbal tea", "Digestive biscuits"),
                preparationTime = "5 minutes",
                instructions = "Brew herbal tea and enjoy with 2-3 biscuits"
            ),
            DailyMeal(
                mealType = MealType.DINNER,
                mealName = "Chapati with Curry",
                description = "Light dinner with protein and fiber",
                calories = 400,
                ingredients = listOf("Whole wheat chapati", "Vegetable curry", "Salad"),
                preparationTime = "25 minutes",
                instructions = "Prepare chapati and vegetable curry, serve with fresh salad"
            )
        )
    }
}