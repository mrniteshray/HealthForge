package com.niteshray.xapps.healthforge.feature.auth.domain.repo


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.niteshray.xapps.healthforge.feature.auth.data.Authentication
import com.niteshray.xapps.healthforge.feature.auth.domain.model.AuthResponse
import com.niteshray.xapps.healthforge.feature.auth.domain.model.RegisterUser
import com.niteshray.xapps.healthforge.feature.auth.domain.model.loginUser
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.UserBasicHealthInfo
import com.niteshray.xapps.healthforge.feature.doctors.domain.model.Doctor
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: Authentication,
    private val firebasAuth : FirebaseAuth
) {
    suspend fun registerUser(registerUser: RegisterUser): Response<AuthResponse> {
        return authApiService.RegisterUser(registerUser)
    }

    suspend fun registerDoctor(doctor: Doctor): Response<AuthResponse> {
        return authApiService.RegisterDoctor(doctor)
    }

    suspend fun saveHealthInfo(userId : String,info : UserBasicHealthInfo) : Response<Authentication.ApiResponse>{
        val body = mapOf(
            "userId" to userId,
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

        return authApiService.updateHealthInfo(body)
    }

    suspend fun loginUser(loginUser: loginUser): Response<AuthResponse> {
        return authApiService.loginUser(loginUser)
    }

    suspend fun SignUpWithEmail(email: String, pass: String, displayName: String) {
        firebasAuth.createUserWithEmailAndPassword(email, pass).addOnSuccessListener { result ->
            // Update the user profile with display name
            val user = result.user
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                this.displayName = displayName
            }
            user?.updateProfile(profileUpdates)?.addOnCompleteListener { 
                // Sign in after profile update
                firebasAuth.signInWithEmailAndPassword(email, pass)
            }
        }
    }

    suspend fun SignInWithEmail(email : String , pass : String ) {
        firebasAuth.signInWithEmailAndPassword(email,pass)
    }
}
