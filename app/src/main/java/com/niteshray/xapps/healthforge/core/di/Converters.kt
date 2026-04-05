package com.niteshray.xapps.healthforge.core.di

import androidx.room.TypeConverter
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.Priority
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TaskCategory
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TimeBlock

class Converters {
    
    @TypeConverter
    fun fromTimeBlock(timeBlock: TimeBlock): String {
        return timeBlock.name
    }
    
    @TypeConverter
    fun toTimeBlock(timeBlockName: String): TimeBlock {
        return enumValueOf<TimeBlock>(timeBlockName)
    }
    
    @TypeConverter
    fun fromTaskCategory(taskCategory: TaskCategory): String {
        return taskCategory.name
    }
    
    @TypeConverter
    fun toTaskCategory(taskCategoryName: String): TaskCategory {
        return enumValueOf<TaskCategory>(taskCategoryName)
    }
    
    @TypeConverter
    fun fromPriority(priority: Priority): String {
        return priority.name
    }
    
    @TypeConverter
    fun toPriority(priorityName: String): Priority {
        return enumValueOf<Priority>(priorityName)
    }
}