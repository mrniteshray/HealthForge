package com.niteshray.xapps.healthforge.core.di

import com.niteshray.xapps.healthforge.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.niteshray.xapps.healthforge.feature.auth.domain.repo.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Provides
    @Singleton
    fun provideFirestore() : FirebaseFirestore{
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository =
        AuthRepository(firebaseAuth, firestore)

    @Provides
    @Singleton
    fun provideGeminiGenerativeModel(): GenerativeModel {
        require(BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            "GEMINI_API_KEY is missing. Add a valid key to local.properties."
        }

        return GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    @Provides
    @Singleton
    fun provideGeminiApi(generativeModel: GenerativeModel): GeminiApi {
        return GeminiApi(generativeModel)
    }
}
