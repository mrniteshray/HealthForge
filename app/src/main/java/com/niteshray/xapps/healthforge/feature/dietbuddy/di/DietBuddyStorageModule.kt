package com.niteshray.xapps.healthforge.feature.dietbuddy.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.dao.*
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.database.DietBuddyDatabase
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.repository.DietPlanStorageRepository
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.repository.impl.DietPlanStorageRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DietBuddyStorageModule {
    
    @Provides
    @Singleton
    fun provideDietBuddyDatabase(@ApplicationContext context: Context): DietBuddyDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            DietBuddyDatabase::class.java,
            DietBuddyDatabase.DATABASE_NAME
        ).build()
    }
    
    @Provides
    fun provideDietPlanDao(database: DietBuddyDatabase): DietPlanDao {
        return database.dietPlanDao()
    }
    
    @Provides
    fun provideDietPlanMealDao(database: DietBuddyDatabase): DietPlanMealDao {
        return database.dietPlanMealDao()
    }
    
    @Provides
    fun provideMedicalReportDao(database: DietBuddyDatabase): MedicalReportDao {
        return database.medicalReportDao()
    }
    
    @Provides
    @Singleton
    fun provideDietPlanStorageRepository(
        dietPlanDao: DietPlanDao,
        mealDao: DietPlanMealDao,
        reportDao: MedicalReportDao,
        firestore: FirebaseFirestore
    ): DietPlanStorageRepository {
        return DietPlanStorageRepositoryImpl(dietPlanDao, mealDao, reportDao, firestore)
    }
}