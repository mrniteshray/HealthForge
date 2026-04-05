package com.niteshray.xapps.healthforge.feature.dietbuddy.di

import com.niteshray.xapps.healthforge.feature.dietbuddy.data.api.DietBuddyApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DietBuddyModule {

    @Provides
    @Singleton
    fun provideDietBuddyApiService(
        geminiApi: com.niteshray.xapps.healthforge.core.di.GeminiApi
    ): DietBuddyApiService {
        return DietBuddyApiService(geminiApi)
    }
}