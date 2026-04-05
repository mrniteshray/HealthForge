package com.niteshray.xapps.healthforge.feature.auth.domain.repo


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun signUpWithEmail(email: String, pass: String, displayName: String): Boolean {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val user = result.user
            if (user != null) {
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    this.displayName = displayName
                }
                user.updateProfile(profileUpdates).await()
                
                // Store user role in Firestore for patients
                val userData = mapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "name" to displayName,
                    "role" to "patient",
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(user.uid).set(userData).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Boolean {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            val user = result.user
            if (user != null) {
                val userDoc = firestore.collection("users").document(user.uid).get().await()
                if (!userDoc.exists()) {
                    val userData = mapOf(
                        "uid" to user.uid,
                        "email" to (user.email ?: email),
                        "name" to (user.displayName ?: "User"),
                        "role" to "patient",
                        "createdAt" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(user.uid).set(userData).await()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getUserRole(uid: String): String? {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            document.getString("role")
        } catch (e: Exception) {
            null
        }
    }
}
