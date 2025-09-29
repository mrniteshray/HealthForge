package com.niteshray.xapps.healthforge.feature.auth.domain.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.ActivityLevel
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.BloodType
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.Gender
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.MedicalCondition
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.UserBasicHealthInfo
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    suspend fun saveUserHealthInfo(info: UserBasicHealthInfo) {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")

        val data = mapOf(
            "age" to info.age,
            "weight" to info.weight,
            "height" to info.height,
            "gender" to info.gender.name,
            "activityLevel" to info.activityLevel.name,
            "medicalCondition" to info.medicalCondition.name,
            "allergies" to info.allergies,
            "emergencyContact" to info.emergencyContact,
            "bloodType" to info.bloodType.name
        )

        firestore.collection("users")
            .document(uid)
            .collection("healthInfo")
            .document("basicInfo")
            .set(data)
            .await()
    }

    suspend fun getUserHealthInfo(): UserBasicHealthInfo? {
        val uid = auth.currentUser?.uid ?: return null
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("healthInfo")
            .document("basicInfo")
            .get()
            .await()

        val map = snapshot.data ?: return null

        return UserBasicHealthInfo(
            age = map["age"] as? String ?: "",
            weight = map["weight"] as? String ?: "",
            height = map["height"] as? String ?: "",
            gender = (map["gender"] as? String)?.let { Gender.valueOf(it) } ?: Gender.PREFER_NOT_TO_SAY,
            activityLevel = (map["activityLevel"] as? String)?.let { ActivityLevel.valueOf(it) } ?: ActivityLevel.SEDENTARY,
            medicalCondition = (map["medicalCondition"] as? String)?.let { MedicalCondition.valueOf(it) } ?: MedicalCondition.NONE,
            allergies = map["allergies"] as? String ?: "",
            emergencyContact = map["emergencyContact"] as? String ?: "",
            bloodType = (map["bloodType"] as? String)?.let { BloodType.valueOf(it) } ?: BloodType.UNKNOWN
        )
    }
}

