package com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.database

import androidx.room.*
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.converters.DietBuddyTypeConverters
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.dao.*
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.entities.*

@Database(
    entities = [
        DietPlanEntity::class,
        DietPlanMealEntity::class,
        MedicalReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DietBuddyTypeConverters::class)
abstract class DietBuddyDatabase : RoomDatabase() {
    
    abstract fun dietPlanDao(): DietPlanDao
    abstract fun dietPlanMealDao(): DietPlanMealDao
    abstract fun medicalReportDao(): MedicalReportDao
    
    companion object {
        const val DATABASE_NAME = "diet_buddy_database"
    }
}