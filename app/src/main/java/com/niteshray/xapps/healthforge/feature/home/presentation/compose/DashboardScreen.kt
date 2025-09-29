package com.niteshray.xapps.healthforge.feature.home.presentation.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalTime

data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val timeBlock: TimeBlock,
    val time: String,
    val category: TaskCategory,
    val isCompleted: Boolean = false,
    val icon: ImageVector
)

data class HealthInfo(
    val title: String,
    val value: String,
    val unit: String,
    val status: HealthStatus,
    val icon: ImageVector,
    val color: Color
)

enum class TimeBlock(val displayName: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night")
}

enum class TaskCategory(val displayName: String, val color: Color) {
    EXERCISE("Exercise", Color(0xFF4CAF50)),
    DIET("Diet", Color(0xFFFF9800)),
    LIFESTYLE("Lifestyle", Color(0xFF2196F3)),
    MONITORING("Monitoring", Color(0xFF9C27B0))
}

enum class HealthStatus {
    GOOD, WARNING, CRITICAL
}

@Composable
fun HealthcareDashboard() {
    var tasks by remember { mutableStateOf(getDummyTasks()) }
    val healthInfo = remember { getDummyHealthInfo() }
    val currentHour = LocalTime.now().hour

    val greeting = when (currentHour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    val completedTasks = tasks.count { it.isCompleted }
    val totalTasks = tasks.size
    val adherencePercentage = if (totalTasks > 0) (completedTasks * 100) / totalTasks else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting Section
        item {
            GreetingSection(
                greeting = greeting,
                userName = "Nitesh",
                adherencePercentage = adherencePercentage
            )
        }

        // Health Info Cards
        item {
            HealthInfoSection(healthInfo = healthInfo)
        }

        // Tasks Section Header
        item {
            Text(
                text = "Today's Care Plan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Group tasks by time block
        val groupedTasks = tasks.groupBy { it.timeBlock }

        TimeBlock.values().forEach { timeBlock ->
            val blockTasks = groupedTasks[timeBlock] ?: emptyList()
            if (blockTasks.isNotEmpty()) {
                item {
                    TimeBlockSection(
                        timeBlock = timeBlock,
                        tasks = blockTasks,
                        onTaskToggle = { taskId ->
                            tasks = tasks.map { task ->
                                if (task.id == taskId) {
                                    task.copy(isCompleted = !task.isCompleted)
                                } else task
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GreetingSection(
    greeting: String,
    userName: String,
    adherencePercentage: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side - Greeting text
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "$greeting, $userName!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Focus on recovery",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun HealthInfoSection(healthInfo: List<HealthInfo>) {
    Column {
        Text(
            text = "Health Overview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(healthInfo) { info ->
                HealthInfoCard(healthInfo = info)
            }
        }
    }
}

@Composable
fun HealthInfoCard(healthInfo: HealthInfo) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = healthInfo.color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, healthInfo.color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = healthInfo.icon,
                contentDescription = null,
                tint = healthInfo.color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${healthInfo.value}${healthInfo.unit}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = healthInfo.color
            )

            Text(
                text = healthInfo.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TimeBlockSection(
    timeBlock: TimeBlock,
    tasks: List<Task>,
    onTaskToggle: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = timeBlock.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${tasks.count { it.isCompleted }}/${tasks.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            tasks.forEach { task ->
                TaskItem(
                    task = task,
                    onToggle = { onTaskToggle(task.id) }
                )

                if (task != tasks.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simple checkbox - left aligned
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = task.category.color,
                uncheckedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Task content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium,
                color = if (task.isCompleted)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
            )

            if (task.description.isNotEmpty()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = task.time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}


fun getDummyTasks(): List<Task> {
    return listOf(
        Task(1, "Morning Blood Sugar Check", "Test glucose levels immediately after waking up. Record reading in your log. Target: 80-130 mg/dL", TimeBlock.MORNING, "6:30 AM", TaskCategory.MONITORING, false, Icons.Default.MonitorHeart),

        Task(2, "Morning Medicine", "Take diabetes medication with food as prescribed. Never skip doses. Set phone reminder if needed.", TimeBlock.MORNING, "7:00 AM", TaskCategory.MONITORING, false, Icons.Default.Medication),

        Task(3, "Morning Walk", "30 minutes brisk walking outdoors. Carry glucose tablets and water bottle. Check blood sugar if feeling dizzy.", TimeBlock.MORNING, "7:30 AM", TaskCategory.EXERCISE, false, Icons.Default.DirectionsWalk),

        Task(4, "Foot Inspection", "Check both feet for cuts, blisters, redness, or swelling. Use mirror for bottom view. Report any issues to doctor immediately.", TimeBlock.MORNING, "8:00 AM", TaskCategory.MONITORING, false, Icons.Default.Visibility),

        Task(5, "Balanced Breakfast", "Include lean protein, whole grains, fiber. Portion: protein size of palm, carbs size of fist. Avoid refined sugars.", TimeBlock.MORNING, "8:30 AM", TaskCategory.DIET, false, Icons.Default.Restaurant),


        Task(6, "Pre-Lunch Blood Check", "Test glucose 2 hours after breakfast. Target: Less than 180 mg/dL. Log results in diary.", TimeBlock.AFTERNOON, "11:30 AM", TaskCategory.MONITORING, false, Icons.Default.Bloodtype),

        Task(7, "Nutritious Lunch", "Low-carb meal with vegetables, lean protein. Avoid fried foods and sugary drinks. Eat at same time daily.", TimeBlock.AFTERNOON, "12:30 PM", TaskCategory.DIET, false, Icons.Default.LunchDining),

        Task(8, "Hydration Check", "Drink 16-20 oz water. Monitor urine color - should be light yellow. Avoid sugary beverages completely.", TimeBlock.AFTERNOON, "2:00 PM", TaskCategory.LIFESTYLE, false, Icons.Default.LocalDrink),

        Task(9, "Stress Management", "10 minutes deep breathing or meditation. High stress affects blood sugar. Try guided meditation apps.", TimeBlock.AFTERNOON, "3:30 PM", TaskCategory.LIFESTYLE, false, Icons.Default.SelfImprovement),

        Task(10, "Pre-Exercise Blood Check", "Test glucose before activity. If below 100 mg/dL, eat 15g carbs. If over 300 mg/dL, avoid exercise.", TimeBlock.EVENING, "5:30 PM", TaskCategory.MONITORING, false, Icons.Default.FitnessCenter),

        Task(11, "Evening Exercise", "20 minutes strength training or yoga. Monitor for dizziness. Keep glucose tablets nearby always.", TimeBlock.EVENING, "6:00 PM", TaskCategory.EXERCISE, false, Icons.Default.SelfImprovement),

        Task(12, "Healthy Dinner", "Small portions, high fiber vegetables, lean protein. Stop eating 3 hours before bed. No dessert or alcohol.", TimeBlock.EVENING, "7:00 PM", TaskCategory.DIET, false, Icons.Default.DinnerDining),

        Task(13, "Evening Medicine", "Take prescribed evening medications with dinner. Check blood pressure if monitoring required.", TimeBlock.EVENING, "7:30 PM", TaskCategory.MONITORING, false, Icons.Default.Medication),

        Task(14, "Bedtime Blood Sugar", "Final glucose check of day. Target: 100-140 mg/dL. Eat small snack if below 100 mg/dL.", TimeBlock.NIGHT, "9:00 PM", TaskCategory.MONITORING, false, Icons.Default.MonitorHeart),

        Task(15, "Sleep Preparation", "No screens 1 hour before bed. Keep glucose tablets bedside. Charge glucose meter. Set consistent sleep schedule.", TimeBlock.NIGHT, "9:30 PM", TaskCategory.LIFESTYLE, false, Icons.Default.Bedtime),

        Task(16, "Quality Sleep", "7-8 hours uninterrupted sleep. Poor sleep affects blood sugar control. Use blackout curtains and cool room temperature.", TimeBlock.NIGHT, "10:00 PM", TaskCategory.LIFESTYLE, false, Icons.Default.Hotel),

        Task(17, "Weekly Weight Check", "Weigh yourself same day/time weekly. Record in log. Sudden weight changes may indicate fluid retention.", TimeBlock.MORNING, "Weekly", TaskCategory.MONITORING, false, Icons.Default.Scale),

        Task(18, "Medicine Refill Check", "Check medication supplies weekly. Refill when 7 days remaining. Never run out of diabetes medications.", TimeBlock.EVENING, "Weekly", TaskCategory.MONITORING, false, Icons.Default.Medication)
    )
}


fun getDummyHealthInfo(): List<HealthInfo> {
    return listOf(
        HealthInfo("Blood Sugar", "120", "mg/dL", HealthStatus.GOOD, Icons.Default.Bloodtype, Color(0xFF4CAF50)),
        HealthInfo("Blood Pressure", "125/80", "mmHg", HealthStatus.WARNING, Icons.Default.MonitorHeart, Color(0xFFFF9800)),
        HealthInfo("Weight", "72", "kg", HealthStatus.GOOD, Icons.Default.FitnessCenter, Color(0xFF2196F3)),
        HealthInfo("Heart Rate", "78", "bpm", HealthStatus.GOOD, Icons.Default.Favorite, Color(0xFFE91E63))
    )
}
