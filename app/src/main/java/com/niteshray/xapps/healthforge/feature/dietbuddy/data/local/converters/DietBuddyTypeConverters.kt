package com.niteshray.xapps.healthforge.feature.dietbuddy.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.niteshray.xapps.healthforge.feature.dietbuddy.data.models.*

class DietBuddyTypeConverters {
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return Gson().fromJson(value, object : TypeToken<List<String>>() {}.type)
    }
    
    @TypeConverter
    fun fromGender(gender: Gender): String {
        return gender.name
    }
    
    @TypeConverter
    fun toGender(gender: String): Gender {
        return Gender.valueOf(gender)
    }
    
    @TypeConverter
    fun fromActivityLevel(activityLevel: ActivityLevel): String {
        return activityLevel.name
    }
    
    @TypeConverter
    fun toActivityLevel(activityLevel: String): ActivityLevel {
        return ActivityLevel.valueOf(activityLevel)
    }
    
    @TypeConverter
    fun fromDietType(dietType: DietType): String {
        return dietType.name
    }
    
    @TypeConverter
    fun toDietType(dietType: String): DietType {
        return DietType.valueOf(dietType)
    }
}