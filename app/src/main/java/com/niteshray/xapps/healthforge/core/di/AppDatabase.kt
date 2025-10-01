package com.niteshray.xapps.healthforge.core.di

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.niteshray.xapps.healthforge.feature.home.data.models.Task

@Database(
    entities = [Task::class], 
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun TaskDao(): TasksDAO
}