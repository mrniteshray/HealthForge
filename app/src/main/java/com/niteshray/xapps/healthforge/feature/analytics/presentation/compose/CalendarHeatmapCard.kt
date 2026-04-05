package com.niteshray.xapps.healthforge.feature.analytics.presentation.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niteshray.xapps.healthforge.feature.home.data.models.TaskTemplate
import com.niteshray.xapps.healthforge.feature.home.data.models.DailyTaskRecord
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.TaskCategory
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*

@Composable
fun CalendarHeatmapCard(
    template: TaskTemplate,
    records: List<DailyTaskRecord>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    // Memoize expensive calculations
    val templateCreationDate = remember(template.createdAt) {
        LocalDate.ofEpochDay(template.createdAt / (24 * 60 * 60 * 1000))
    }

    // Calculate completion stats with derivedStateOf
    val stats = remember(records) {
        val completedDays = records.count { it.isCompleted }
        val totalDays = records.size
        val completionRate = if (totalDays > 0) (completedDays * 100) / totalDays else 0
        Triple(completedDays, totalDays, completionRate)
    }

    // Create completion map - memoized properly
    val completionMap = remember(records) {
        records.associate { record -> record.date to record.isCompleted }
    }

    // Calculate months to show - memoized
    val monthsToShow = remember(templateCreationDate) {
        val currentMonth = YearMonth.now()
        val startMonth = YearMonth.from(templateCreationDate)
        buildList {
            var month = startMonth
            while (!month.isAfter(currentMonth)) {
                add(month)
                month = month.plusMonths(1)
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with task info
            TaskHeader(
                template = template,
                categoryIcon = getCategoryIcon(template.category)
            )

            // Stats row
            StatsRow(
                completedDays = stats.first,
                totalDays = stats.second,
                completionRate = stats.third
            )

            // Calendar heatmap with horizontal scrolling
            if (monthsToShow.isNotEmpty()) {
                CalendarSection(
                    monthsToShow = monthsToShow,
                    completionMap = completionMap,
                    template = template,
                    onDateClick = onDateClick
                )
            }
        }
    }
}

@Composable
private fun TaskHeader(
    template: TaskTemplate,
    categoryIcon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = template.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = template.category.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = categoryIcon,
            contentDescription = null,
            tint = template.category.color,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun StatsRow(
    completedDays: Int,
    totalDays: Int,
    completionRate: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatChip(label = "Completed", value = "$completedDays", color = Color(0xFF4CAF50))
        StatChip(label = "Total Days", value = "$totalDays", color = Color(0xFF2196F3))
        StatChip(label = "Rate", value = "$completionRate%", color = Color(0xFFFF9800))
    }
}

@Composable
private fun CalendarSection(
    monthsToShow: List<YearMonth>,
    completionMap: Map<String, Boolean>,
    template: TaskTemplate,
    onDateClick: (LocalDate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Completion History",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${monthsToShow.size} months",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // CRITICAL FIX: Use LazyRow with keys
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(
                items = monthsToShow,
                key = { month -> "${month.year}-${month.monthValue}" }
            ) { month ->
                MonthlyCalendar(
                    month = month,
                    completionMap = completionMap,
                    categoryColor = template.category.color,
                    onDateClick = onDateClick,
                    modifier = Modifier.width(280.dp)
                )
            }
        }

        HeatmapLegend()
    }
}

@Composable
private fun MonthlyCalendar(
    month: YearMonth,
    completionMap: Map<String, Boolean>,
    categoryColor: Color,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    // CRITICAL FIX: Pre-calculate all calendar data
    val calendarData = remember(month, completionMap) {
        val firstDayOfMonth = month.atDay(1)
        val firstSunday = firstDayOfMonth.minusDays(firstDayOfMonth.dayOfWeek.value % 7.toLong())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        List(42) { index ->
            val date = firstSunday.plusDays(index.toLong())
            CalendarDayData(
                date = date,
                isInCurrentMonth = date.month == month.month,
                dateString = date.format(formatter),
                dayOfMonth = date.dayOfMonth
            )
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Month header
        Text(
            text = remember(month) {
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Days of week header - static composable
        DaysOfWeekHeader()

        // CRITICAL FIX: Replace LazyVerticalGrid with regular Column + Row
        // LazyVerticalGrid inside LazyRow causes severe performance issues
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 6 weeks of 7 days each
            for (weekIndex in 0 until 6) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (dayIndex in 0 until 7) {
                        val index = weekIndex * 7 + dayIndex
                        val dayData = calendarData[index]
                        val isCompleted = completionMap[dayData.dateString] ?: false
                        val hasData = completionMap.containsKey(dayData.dateString)

                        CalendarDay(
                            modifier = Modifier.weight(1f),
                            dayData = dayData,
                            isCompleted = isCompleted,
                            hasData = hasData,
                            categoryColor = categoryColor,
                            onClick = { onDateClick(dayData.date) }
                        )
                    }
                }
            }
        }
    }
}

// Data class to hold pre-calculated calendar day information
@Immutable
private data class CalendarDayData(
    val date: LocalDate,
    val isInCurrentMonth: Boolean,
    val dateString: String,
    val dayOfMonth: Int
)

@Composable
private fun DaysOfWeekHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalendarDay(
    modifier: Modifier = Modifier,
    dayData: CalendarDayData,
    isCompleted: Boolean,
    hasData: Boolean,
    categoryColor: Color,
    onClick: () -> Unit
) {
    // Memoize colors to avoid recalculation
    val backgroundColor = remember(dayData.isInCurrentMonth, hasData, isCompleted, categoryColor) {
        when {
            !dayData.isInCurrentMonth -> Color.Transparent
            !hasData -> Color(0xFFE0E0E0).copy(alpha = 0.3f)
            isCompleted -> categoryColor.copy(alpha = 0.8f)
            else -> Color(0xFFFFCDD2).copy(alpha = 0.6f)
        }
    }

    val textColor = remember(dayData.isInCurrentMonth, hasData, isCompleted) {
        when {
            !dayData.isInCurrentMonth -> Color.Gray.copy(alpha = 0.3f)
            !hasData -> Color.Gray
            isCompleted -> Color.White
            else -> Color(0xFFC62828)
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .then(
                if (dayData.isInCurrentMonth && hasData) {
                    Modifier.clickable { onClick() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayData.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal
        )

        // Completion indicator
        if (isCompleted && hasData) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
                    .align(Alignment.TopEnd)
                    .offset((-2).dp, 2.dp)
            )
        }
    }
}

@Composable
private fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Legend:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LegendItem(color = Color(0xFF4CAF50).copy(alpha = 0.8f), label = "Completed")
        LegendItem(color = Color(0xFFFFCDD2).copy(alpha = 0.6f), label = "Missed")
        LegendItem(color = Color(0xFFE0E0E0).copy(alpha = 0.3f), label = "No Data")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getCategoryIcon(category: TaskCategory): ImageVector {
    return when (category) {
        TaskCategory.MEDICATION -> Icons.Filled.Medication
        TaskCategory.EXERCISE -> Icons.Filled.FitnessCenter
        TaskCategory.DIET -> Icons.Filled.Restaurant
        TaskCategory.MONITORING -> Icons.Filled.Monitor
        TaskCategory.LIFESTYLE -> Icons.Filled.SelfImprovement
        TaskCategory.GENERAL -> Icons.Filled.Task
    }
}
