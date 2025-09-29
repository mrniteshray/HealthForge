package com.niteshray.xapps.healthforge.feature.home.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.niteshray.xapps.healthforge.core.di.CerebrasApi
import com.niteshray.xapps.healthforge.core.di.ChatRequest
import com.niteshray.xapps.healthforge.core.di.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class CarePlan(
    @SerializedName("patientSummary") val patientSummary: String,
    @SerializedName("checklist") val checklist: List<ChecklistItem>
)

@Serializable
data class ChecklistItem(
    @SerializedName("task") val task: String,
    @SerializedName("timing") val timing: String,
    @SerializedName("details") val details: String
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cerebrasApi: CerebrasApi
) : ViewModel() {
    var carePlan = mutableStateOf<CarePlan?>(null) // Parsed object
    var errorMessage = mutableStateOf<String?>(null)

    fun generateCarePlan(medicalReport: String) {
        viewModelScope.launch {
            try {
                val prompt = """
                    Based on this medical report: '$medicalReport'
                    Generate a personalized daily care plan as a JSON object. Structure it like this:
                    {
                      "patientSummary": "Brief summary of the plan",
                      "checklist": [
                        {
                          "task": "Task name (e.g., Take medication)",
                          "timing": "Specific time or frequency (e.g., 8:00 AM daily)",
                          "details": "Additional instructions or rationale"
                        }
                      ]
                    }
                    Ensure tasks are actionable, timed for alarms, and cover key areas like medication, exercise, diet, and monitoring.
                    Respond ONLY with the JSON object—no extra text, explanations, or markdown.
                """.trimIndent()

                val request = ChatRequest(
                    messages = listOf(
                        Message("system", "You are a medical AI assistant. Respond only in valid JSON."),
                        Message("user", prompt)
                    )
                )

                val response = cerebrasApi.generateContent(request)
                val generatedContent = response.choices.firstOrNull()?.message?.content ?: "{}"

                val jsonRegex = Regex("""\{[\s\S]*\}""")
                val jsonMatch = jsonRegex.find(generatedContent)
                val jsonString = jsonMatch?.value ?: throw Exception("No valid JSON found")

                val gson = Gson()
                carePlan.value = gson.fromJson(jsonString, CarePlan::class.java)
            } catch (e: Exception) {
                errorMessage.value = "Failed: ${e.message}"
            }
        }
    }
}