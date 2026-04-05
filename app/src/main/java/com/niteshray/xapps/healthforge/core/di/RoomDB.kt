package com.niteshray.xapps.healthforge.core.di

import android.content.Context
import androidx.room.Room
import com.niteshray.xapps.healthforge.feature.home.domain.TaskTrackingRepository
import com.niteshray.xapps.healthforge.feature.home.domain.TaskTrackingRepositoryImpl
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
    abstract fun bindTaskTrackingRepository(
        taskTrackingRepositoryImpl: TaskTrackingRepositoryImpl
    ): TaskTrackingRepository
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
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideTaskTemplateDao(db: AppDatabase): TaskTemplateDao {
        return db.TaskTemplateDao()
    }

    @Provides
    fun provideDailyTaskRecordDao(db: AppDatabase): DailyTaskRecordDao {
        return db.DailyTaskRecordDao()
    }

    @Provides
    fun provideTaskTrackingDao(db: AppDatabase): TaskTrackingDao {
        return db.TaskTrackingDao()
    }
}