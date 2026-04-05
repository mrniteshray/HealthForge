package com.niteshray.xapps.healthforge.core.di

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
        return GenerativeModel(
            modelName = "gemini-2-flash-lite",
            apiKey = "AIzaSyB4ag90kbHyPftHmBs6AbF8j_CDjrnlUDM"
        )
    }

    @Provides
    @Singleton
    fun provideGeminiApi(generativeModel: GenerativeModel): GeminiApi {
        return GeminiApi(generativeModel)
    }
}
