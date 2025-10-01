package com.niteshray.xapps.healthforge.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.niteshray.xapps.healthforge.feature.home.domain.TaskRepository
import com.niteshray.xapps.healthforge.feature.home.domain.TaskRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository
}

@Module
@InstallIn(SingletonComponent::class)
class RoomDBModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    fun provideTaskDao(db: AppDatabase): TasksDAO {
        return db.TaskDao()
    }
}