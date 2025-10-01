package com.niteshray.xapps.healthforge.feature.home.presentation.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalTime
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.niteshray.xapps.healthforge.feature.home.presentation.viewmodel.HomeViewModel
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import java.io.InputStream
import androidx.compose.runtime.LaunchedEffect
import android.content.Context
import androidx.compose.ui.platform.LocalContext

@Composable
fun HealthcareDashboard(
    viewModel: HomeViewModel = hiltViewModel()
) {
    var medications by remember { mutableStateOf<List<Medication>>(emptyList()) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showEditTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    val currentHour = LocalTime.now().hour
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Use StateFlow from ViewModel for reactive task updates
    val tasks by viewModel.tasks.collectAsState()
    val isTasksLoading by viewModel.isTasksLoading.collectAsState()
    val isProcessingReport by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
//    val userName by viewModel.userName.collectAsStateWithLifecycle()
    
    // PDF picker launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val extractedText = extractTextFromPDF(context, uri)
                    viewModel.generateTasksFromReport(extractedText)
                } catch (e: Exception) {
                    viewModel.errorMessage.value = "Failed to process PDF: ${e.message}"
                }
            }
        }
    }
    


    val greeting = when (currentHour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    val currentTimeBlock = when (currentHour) {
        in 6..11 -> TimeBlock.MORNING
        in 12..16 -> TimeBlock.AFTERNOON
        in 17..20 -> TimeBlock.EVENING
        else -> TimeBlock.NIGHT
    }

    // Show all tasks instead of filtering by time block
    val currentTasks = tasks // Show all tasks for now
    // val currentTasks = tasks.filter { it.timeBlock == currentTimeBlock } // Original time block filtering
    val completedTasks = currentTasks.count { it.isCompleted }
    val totalCurrentTasks = currentTasks.size
    val adherencePercentage = if (totalCurrentTasks > 0) (completedTasks * 100) / totalCurrentTasks else 0

    // Show loading screen when tasks are loading initially
    if (isTasksLoading && tasks.isEmpty()) {
        CircularProgressIndicator()
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        item {
            HeaderSection(
                greeting = greeting,
                userName = "",
                currentTimeBlock = currentTimeBlock
            )
        }

        // Recovery Progress
        item {
            RecoveryProgressSection(
                completedToday = tasks.count { it.isCompleted },
                totalTasks = tasks.size
            )
        }

        // Quick Actions Section
        item {
            QuickActionsSection(
                onUploadReport = { 
                    pdfPickerLauncher.launch("application/pdf")
                },
                onViewAnalytics = {  },
                onAddMedication = { showAddMedicationDialog = true },
                onAddTask = { showAddTaskDialog = true }
            )
        }
        
        // Processing indicator
        if (isProcessingReport) {
            item {
                ProcessingIndicator()
            }
        }

        // All Tasks Section or Empty State
        if (currentTasks.isNotEmpty()) {
            item {
                AllTasksSection(
                    tasks = currentTasks,
                    onTaskToggle = { taskId ->
                        val task = tasks.find { it.id == taskId }
                        task?.let {
                            viewModel.toggleTaskCompletion(taskId, !it.isCompleted)
                        }
                    },
                    onTaskEdit = { task ->
                        taskToEdit = task
                        showEditTaskDialog = true
                    },
                    onTaskDelete = { taskId ->
                        viewModel.deleteTask(taskId)
                    }
                )
            }
        } else if (tasks.isEmpty() && !isProcessingReport) {
            item {
                EmptyTasksState(onUploadReport = { 
                    pdfPickerLauncher.launch("application/pdf")
                })
            }
        }
    }

    // Add Medication Dialog
    if (showAddMedicationDialog) {
        AddMedicationDialog(
            onDismiss = { showAddMedicationDialog = false },
            onAddMedication = { medication ->
                medications = medications + medication

                // Add each medication time as a separate task to the database
                medication.times.forEach { medicationTime ->
                    val task = Task(
                        id = 0, // Let Room auto-generate the ID
                        title = "Take ${medication.name}",
                        description = if (medication.instructions.isNotEmpty()) medication.instructions else "Medication reminder",
                        timeBlock = medicationTime.timeBlock,
                        time = medicationTime.get12HourFormat(),
                        category = TaskCategory.MEDICATION,
                        isCompleted = false,
                        icon = Icons.Filled.Medication,
                        priority = Priority.HIGH
                    )
                    viewModel.addTask(task)
                }
                showAddMedicationDialog = false
            }
        )
    }
    
    // Add Task Dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { newTask ->
                viewModel.addTask(newTask)
                showAddTaskDialog = false
            }
        )
    }
    
    // Edit Task Dialog
    if (showEditTaskDialog && taskToEdit != null) {
        EditTaskDialog(
            task = taskToEdit!!,
            onDismiss = { 
                showEditTaskDialog = false 
                taskToEdit = null
            },
            onConfirm = { updatedTask ->
                viewModel.updateTask(updatedTask)
                showEditTaskDialog = false
                taskToEdit = null
            }
        )
    }
}

@Composable
fun HeaderSection(
    greeting: String,
    userName: String,
    currentTimeBlock: TimeBlock
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "$greeting $userName!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = currentTimeBlock.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Complete your daily health tasks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    onUploadReport: () -> Unit,
    onViewAnalytics: () -> Unit,
    onAddMedication: () -> Unit,
    onAddTask: () -> Unit
) {
    Column {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Upload Report Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onUploadReport() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = "Upload Report",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Upload\nReport",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Add Medication Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAddMedication() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Medication",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add\nMedication",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // View Reports Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onViewAnalytics() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Assessment,
                        contentDescription = "View Analytics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "View\nAnalytics",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Add Task Card (full width)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAddTask() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Task",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add New Task",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun HealthMetricCard(metric: HealthMetric) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = metric.color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, metric.color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = metric.icon,
                    contentDescription = null,
                    tint = metric.color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = when (metric.trend) {
                        Trend.UP -> Icons.Filled.TrendingUp
                        Trend.DOWN -> Icons.Filled.TrendingDown
                        Trend.STABLE -> Icons.Filled.TrendingFlat
                    },
                    contentDescription = null,
                    tint = when (metric.trend) {
                        Trend.UP -> Color(0xFF4CAF50)
                        Trend.DOWN -> Color(0xFFE53E3E)
                        Trend.STABLE -> Color(0xFF9E9E9E)
                    },
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${metric.value}${metric.unit}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = metric.color
                )
                Text(
                    text = metric.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AllTasksSection(
    tasks: List<Task>,
    onTaskToggle: (Int) -> Unit,
    onTaskEdit: (Task) -> Unit,
    onTaskDelete: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "All Tasks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${tasks.count { it.isCompleted }}/${tasks.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Group tasks by time block and display them
            val tasksByTimeBlock = tasks.groupBy { it.timeBlock }
            val orderedTimeBlocks = listOf(TimeBlock.MORNING, TimeBlock.AFTERNOON, TimeBlock.EVENING, TimeBlock.NIGHT)
            
            orderedTimeBlocks.forEach { timeBlock ->
                val timeBlockTasks = tasksByTimeBlock[timeBlock] ?: emptyList()
                if (timeBlockTasks.isNotEmpty()) {
                    // Time block header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = timeBlock.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeBlock.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${timeBlockTasks.count { it.isCompleted }}/${timeBlockTasks.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Tasks for this time block
                    timeBlockTasks.forEachIndexed { index, task ->
                        ModernTaskItem(
                            task = task,
                            onToggle = { onTaskToggle(task.id) },
                            onEdit = { onTaskEdit(task) },
                            onDelete = { onTaskDelete(task.id) }
                        )
                        if (index < timeBlockTasks.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        }
                    }
                    
                    // Add spacing between time blocks
                    if (timeBlock != orderedTimeBlocks.last { tasksByTimeBlock[it]?.isNotEmpty() == true }) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentTasksSection(
    timeBlock: TimeBlock,
    tasks: List<Task>,
    onTaskToggle: (Int) -> Unit,
    onTaskEdit: ((Task) -> Unit)? = null,
    onTaskDelete: ((Int) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = timeBlock.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${timeBlock.displayName} Tasks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${tasks.count { it.isCompleted }}/${tasks.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            tasks.forEachIndexed { index, task ->
                ModernTaskItem(
                    task = task,
                    onToggle = { onTaskToggle(task.id) },
                    onEdit = onTaskEdit?.let { { onTaskEdit(task) } },
                    onDelete = { onTaskDelete?.invoke(task.id) }
                )
                if (index < tasks.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernTaskItem(
    task: Task,
    onToggle: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Task icon and checkbox
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Task icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = task.category.color.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = task.icon,
                    contentDescription = null,
                    tint = task.category.color,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Checkbox indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = if (task.isCompleted) {
                            task.category.color
                        } else {
                            Color.Transparent
                        },
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = task.category.color,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Task content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )
                
                // Priority dot for high priority tasks
                if (task.priority == Priority.HIGH && !task.isCompleted) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = Priority.HIGH.color,
                                shape = CircleShape
                            )
                    )
                }
            }

            if (task.description.isNotEmpty()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Compact time and category
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = task.time,
                    style = MaterialTheme.typography.labelMedium,
                    color = task.category.color,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = " • ${task.category.displayName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Menu button for actions
        if (onEdit != null || onDelete != null) {
            Box {
                IconButton(
                    onClick = { showMenu = !showMenu },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Task Options",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onEdit != null) {
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Edit")
                                }
                            },
                            onClick = {
                                onEdit()
                                showMenu = false
                            }
                        )
                    }
                    
                    if (onDelete != null) {
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            onClick = {
                                showDeleteDialog = true
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Clean delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Delete Task?")
            },
            text = {
                Text("\"${task.title}\" will be permanently deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete?.invoke()
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MedicalReportsSection(reports: List<MedicalReport>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Recent Reports",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { /* View All */ }
                ) {
                    Text("View All")
                }
            }

            reports.forEachIndexed { index, report ->
                MedicalReportItem(report = report)
                if (index < reports.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
fun MedicalReportItem(report: MedicalReport) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Open report */ }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = report.type.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${report.type.displayName} • ${report.uploadDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        StatusChip(status = report.status)
    }
}

@Composable
fun StatusChip(status: ReportStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        ReportStatus.PROCESSING -> Triple(
            Color(0xFFFFF3CD),
            Color(0xFF856404),
            "Processing"
        )
        ReportStatus.ANALYZED -> Triple(
            Color(0xFFD1ECF1),
            Color(0xFF0C5460),
            "Analyzed"
        )
        ReportStatus.PENDING -> Triple(
            Color(0xFFF8D7DA),
            Color(0xFF721C24),
            "Pending"
        )
    }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RecoveryProgressSection(
    completedToday: Int,
    totalTasks: Int
) {
    val progress by animateFloatAsState(
        targetValue = if (totalTasks > 0) completedToday.toFloat() / totalTasks else 0f,
        animationSpec = tween(1000),
        label = "progress"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "$completedToday/$totalTasks tasks",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onAddMedication: (Medication) -> Unit
) {
    var medicationName by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf(FrequencyType.ONCE_DAILY) }
    var medicationTimes by remember { mutableStateOf<List<MedicationTime>>(emptyList()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var currentTimeIndex by remember { mutableStateOf(-1) }
    
    val timePickerState = rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0
    )

    // Initialize times based on frequency
    LaunchedEffect(selectedFrequency) {
        medicationTimes = when (selectedFrequency) {
            FrequencyType.ONCE_DAILY -> listOf(
                MedicationTime(8, 0, TimeBlock.MORNING, "08:00")
            )
            FrequencyType.TWICE_DAILY -> listOf(
                MedicationTime(8, 0, TimeBlock.MORNING, "08:00"),
                MedicationTime(20, 0, TimeBlock.EVENING, "20:00")
            )
            FrequencyType.THREE_TIMES -> listOf(
                MedicationTime(8, 0, TimeBlock.MORNING, "08:00"),
                MedicationTime(14, 0, TimeBlock.AFTERNOON, "14:00"),
                MedicationTime(20, 0, TimeBlock.EVENING, "20:00")
            )
            FrequencyType.FOUR_TIMES -> listOf(
                MedicationTime(8, 0, TimeBlock.MORNING, "08:00"),
                MedicationTime(12, 0, TimeBlock.AFTERNOON, "12:00"),
                MedicationTime(16, 0, TimeBlock.AFTERNOON, "16:00"),
                MedicationTime(20, 0, TimeBlock.EVENING, "20:00")
            )
            FrequencyType.CUSTOM -> emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Medication Reminder",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(400.dp)
            ) {
                // Medication Name
                item {
                    OutlinedTextField(
                        value = medicationName,
                        onValueChange = { medicationName = it },
                        label = { Text("Medication Name *") },
                        placeholder = { Text("e.g., Metformin, Lisinopril, Vitamin D") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Filled.Medication, contentDescription = null)
                        },
                        isError = medicationName.isBlank()
                    )
                }

                // Instructions
                item {
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Instructions (Optional)") },
                        placeholder = { Text("e.g., Take with food, Before meals, After dinner") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Filled.Info, contentDescription = null)
                        },
                        maxLines = 2
                    )
                }

                // Frequency Selection
                item {
                    Text(
                        text = "How often?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                item {
                    FrequencySelector(
                        selectedFrequency = selectedFrequency,
                        onFrequencySelected = { selectedFrequency = it }
                    )
                }

                // Time Schedule
                if (medicationTimes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Reminder Times",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    itemsIndexed(medicationTimes) { index, time ->
                        TimeScheduleItem(
                            time = time,
                            onTimeClick = { 
                                currentTimeIndex = index
                                showTimePicker = true
                            }
                        )
                    }
                }

                // Custom time option
                if (selectedFrequency == FrequencyType.CUSTOM) {
                    item {
                        Button(
                            onClick = { 
                                currentTimeIndex = medicationTimes.size
                                showTimePicker = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Time")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (medicationName.isNotBlank() && medicationTimes.isNotEmpty()) {
                        val medication = Medication(
                            id = System.currentTimeMillis().toInt(),
                            name = medicationName,
                            instructions = instructions,
                            times = medicationTimes,
                            startDate = java.time.LocalDate.now().toString(),
                            endDate = null,
                            isActive = true
                        )
                        onAddMedication(medication)
                    }
                },
                enabled = medicationName.isNotBlank() && medicationTimes.isNotEmpty()
            ) {
                Text("Set Reminders")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Time Picker Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hour = timePickerState.hour
                        val minute = timePickerState.minute
                        val timeBlock = getTimeBlockFromHour(hour)
                        val displayTime = String.format("%02d:%02d", hour, minute)
                        
                        val newTime = MedicationTime(hour, minute, timeBlock, displayTime)
                        
                        medicationTimes = if (currentTimeIndex < medicationTimes.size) {
                            // Update existing time
                            medicationTimes.toMutableList().apply {
                                set(currentTimeIndex, newTime)
                            }
                        } else {
                            // Add new time
                            medicationTimes + newTime
                        }
                        
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Select Time")
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@Composable
fun FrequencySelector(
    selectedFrequency: FrequencyType,
    onFrequencySelected: (FrequencyType) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(FrequencyType.values()) { frequency ->
            FilterChip(
                selected = selectedFrequency == frequency,
                onClick = { onFrequencySelected(frequency) },
                label = {
                    Text(
                        text = frequency.displayName,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = if (selectedFrequency == frequency) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
fun TimeScheduleItem(
    time: MedicationTime,
    onTimeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTimeClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = time.timeBlock.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = time.get12HourFormat(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = time.timeBlock.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit time",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

fun getTimeBlockFromHour(hour: Int): TimeBlock {
    return when (hour) {
        in 6..11 -> TimeBlock.MORNING
        in 12..16 -> TimeBlock.AFTERNOON
        in 17..20 -> TimeBlock.EVENING
        else -> TimeBlock.NIGHT
    }
}

fun parseTimeString(timeString: String): Pair<Int, Int> {
    return try {
        val parts = timeString.split(":")
        val hourMinute = parts[1].split(" ")
        val hour = parts[0].toInt()
        val minute = hourMinute[0].toInt()
        val period = if (hourMinute.size > 1) hourMinute[1] else ""
        
        val adjustedHour = when {
            period == "AM" && hour == 12 -> 0
            period == "PM" && hour != 12 -> hour + 12
            else -> hour
        }
        
        Pair(adjustedHour, minute)
    } catch (e: Exception) {
        Pair(9, 0) // Default to 9:00 AM if parsing fails
    }
}

fun getTimeBlockFromTime(time: String): TimeBlock {
    val hour = time.split(":")[0].toInt()
    return when (hour) {
        in 6..11 -> TimeBlock.MORNING
        in 12..16 -> TimeBlock.AFTERNOON
        in 17..20 -> TimeBlock.EVENING
        else -> TimeBlock.NIGHT
    }
}

@Composable
fun ProcessingIndicator() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Analyzing Medical Report...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Generating personalized health plan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// PDF Text Extraction
suspend fun extractTextFromPDF(context: Context, pdfUri: Uri): String {
    return try {
        val inputStream: InputStream = context.contentResolver.openInputStream(pdfUri)
            ?: throw Exception("Could not open PDF file")
        
        val pdfReader = PdfReader(inputStream)
        val pdfDocument = PdfDocument(pdfReader)
        val text = StringBuilder()
        
        for (i in 1..pdfDocument.numberOfPages) {
            val page = pdfDocument.getPage(i)
            val pageText = PdfTextExtractor.getTextFromPage(page)
            text.append(pageText).append("\n")
        }
        
        pdfDocument.close()
        pdfReader.close()
        inputStream.close()
        
        text.toString().trim()
    } catch (e: Exception) {
        throw Exception("Failed to extract text from PDF: ${e.message}")
    }
}

@Composable
fun EmptyTasksState(onUploadReport: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Assignment,
                contentDescription = "No Tasks",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            
            Text(
                text = "No Tasks Yet",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )
            
            Text(
                text = "Upload your medical report to generate personalized daily health tasks",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center
            )
            
            FilledTonalButton(
                onClick = onUploadReport,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Medical Report")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (Task) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.GENERAL) }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedPriority by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val timePickerState = rememberTimePickerState(
        initialHour = 9,
        initialMinute = 0
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedCategory,
                        onExpandedChange = { expandedCategory = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false }
                        ) {
                            TaskCategory.values().forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.displayName) },
                                    onClick = {
                                        selectedCategory = category
                                        expandedCategory = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedPriority,
                        onExpandedChange = { expandedPriority = it }
                    ) {
                        OutlinedTextField(
                            value = when(selectedPriority) {
                                Priority.HIGH -> "High"
                                Priority.MEDIUM -> "Medium"
                                Priority.LOW -> "Low"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Priority") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPriority) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedPriority,
                            onDismissRequest = { expandedPriority = false }
                        ) {
                            Priority.values().forEach { priority ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(when(priority) {
                                            Priority.HIGH -> "High"
                                            Priority.MEDIUM -> "Medium"
                                            Priority.LOW -> "Low"
                                        })
                                    },
                                    onClick = {
                                        selectedPriority = priority
                                        expandedPriority = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = String.format("%02d:%02d %s", 
                            if (timePickerState.hour == 0) 12 
                            else if (timePickerState.hour > 12) timePickerState.hour - 12 
                            else timePickerState.hour,
                            timePickerState.minute,
                            if (timePickerState.hour < 12) "AM" else "PM"
                        ),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reminder Time") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Time"
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Select Time"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val newTask = Task(
                            id = System.currentTimeMillis().toInt(),
                            title = title.trim(),
                            description = description.trim(),
                            timeBlock = getTimeBlockFromHour(timePickerState.hour),
                            time = String.format("%02d:%02d %s", 
                                if (timePickerState.hour == 0) 12 
                                else if (timePickerState.hour > 12) timePickerState.hour - 12 
                                else timePickerState.hour,
                                timePickerState.minute,
                                if (timePickerState.hour < 12) "AM" else "PM"
                            ),
                            category = selectedCategory,
                            isCompleted = false,
                            icon = when(selectedCategory) {
                                TaskCategory.MEDICATION -> Icons.Filled.Medication
                                TaskCategory.EXERCISE -> Icons.Filled.FitnessCenter
                                TaskCategory.DIET -> Icons.Filled.Restaurant
                                TaskCategory.MONITORING -> Icons.Filled.Monitor
                                TaskCategory.LIFESTYLE -> Icons.Filled.SelfImprovement
                                TaskCategory.GENERAL -> Icons.Filled.Task
                            },
                            priority = selectedPriority
                        )
                        onConfirm(newTask)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    // Time Picker Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Select Reminder Time")
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (Task) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var selectedCategory by remember { mutableStateOf(task.category) }
    var selectedPriority by remember { mutableStateOf(task.priority) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedPriority by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    // Parse existing task time
    val timePickerState = rememberTimePickerState(
        initialHour = parseTimeString(task.time).first,
        initialMinute = parseTimeString(task.time).second
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedCategory,
                        onExpandedChange = { expandedCategory = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false }
                        ) {
                            TaskCategory.values().forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.displayName) },
                                    onClick = {
                                        selectedCategory = category
                                        expandedCategory = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedPriority,
                        onExpandedChange = { expandedPriority = it }
                    ) {
                        OutlinedTextField(
                            value = when(selectedPriority) {
                                Priority.HIGH -> "High"
                                Priority.MEDIUM -> "Medium"
                                Priority.LOW -> "Low"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Priority") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPriority) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedPriority,
                            onDismissRequest = { expandedPriority = false }
                        ) {
                            Priority.values().forEach { priority ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(when(priority) {
                                            Priority.HIGH -> "High"
                                            Priority.MEDIUM -> "Medium"
                                            Priority.LOW -> "Low"
                                        })
                                    },
                                    onClick = {
                                        selectedPriority = priority
                                        expandedPriority = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = String.format("%02d:%02d %s", 
                            if (timePickerState.hour == 0) 12 
                            else if (timePickerState.hour > 12) timePickerState.hour - 12 
                            else timePickerState.hour,
                            timePickerState.minute,
                            if (timePickerState.hour < 12) "AM" else "PM"
                        ),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reminder Time") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Time"
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Select Time"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val updatedTask = task.copy(
                            title = title.trim(),
                            description = description.trim(),
                            category = selectedCategory,
                            priority = selectedPriority,
                            timeBlock = getTimeBlockFromHour(timePickerState.hour),
                            time = String.format("%02d:%02d %s", 
                                if (timePickerState.hour == 0) 12 
                                else if (timePickerState.hour > 12) timePickerState.hour - 12 
                                else timePickerState.hour,
                                timePickerState.minute,
                                if (timePickerState.hour < 12) "AM" else "PM"
                            )
                        )
                        onConfirm(updatedTask)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    // Time Picker Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Select Reminder Time")
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}