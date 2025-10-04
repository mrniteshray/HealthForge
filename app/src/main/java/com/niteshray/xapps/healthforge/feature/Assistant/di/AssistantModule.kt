package com.niteshray.xapps.healthforge.feature.Assistant.di

import com.niteshray.xapps.healthforge.feature.Assistant.data.repository.AssistantRepositoryImpl
import com.niteshray.xapps.healthforge.feature.Assistant.domain.repository.AssistantRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantModule {
    
    @Binds
    @Singleton
    abstract fun bindAssistantRepository(
        assistantRepositoryImpl: AssistantRepositoryImpl
    ): AssistantRepository
}