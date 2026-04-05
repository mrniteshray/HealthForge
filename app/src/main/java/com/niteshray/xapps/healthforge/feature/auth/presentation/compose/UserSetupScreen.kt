package com.niteshray.xapps.healthforge.feature.auth.presentation.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

data class UserBasicHealthInfo(
    val age: String = "",
    val weight: String = "",
    val height: String = "",
    val gender: Gender = Gender.PREFER_NOT_TO_SAY,
    val activityLevel: ActivityLevel = ActivityLevel.SEDENTARY,
    val medicalCondition: MedicalCondition = MedicalCondition.NONE, // Changed from List to single
    val allergies: String = "",
    val emergencyContact: String = "",
    val bloodType: BloodType = BloodType.UNKNOWN
)


enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    PREFER_NOT_TO_SAY("Prefer not to say")
}

enum class ActivityLevel(val displayName: String, val description: String) {
    SEDENTARY("Sedentary", "Little to no exercise"),
    LIGHTLY_ACTIVE("Lightly Active", "Light exercise 1-3 days/week"),
    MODERATELY_ACTIVE("Moderately Active", "Moderate exercise 3-5 days/week"),
    VERY_ACTIVE("Very Active", "Hard exercise 6-7 days/week"),
    EXTREMELY_ACTIVE("Extremely Active", "Very hard exercise, physical job")
}

enum class MedicalCondition(val displayName: String) {
    DIABETES("Diabetes"),
    HYPERTENSION("High Blood Pressure"),
    HEART_DISEASE("Heart Disease"),
    ASTHMA("Asthma"),
    ARTHRITIS("Arthritis"),
    THYROID("Thyroid Disorder"),
    DEPRESSION("Depression/Anxiety"),
    NONE("None of the above")
}

enum class BloodType(val displayName: String) {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-"),
    UNKNOWN("Unknown")
}

@Composable
fun UserSetupScreen(
    onProfileComplete: (UserBasicHealthInfo) -> Unit,
    authViewModel: com.niteshray.xapps.healthforge.feature.auth.presentation.viewmodel.AuthViewModel,
    onNavigateToHome: () -> Unit,
    onBackClick: () -> Unit = {}
) {
    var profileData by remember { mutableStateOf(UserBasicHealthInfo()) }
    var currentStep by remember { mutableStateOf(0) }
    
    val authState = authViewModel.authState
    val isLoading = authState.isSetupLoading
    
    // Handle successful setup completion
    LaunchedEffect(authState.isSetupComplete) {
        if (authState.isSetupComplete) {
            onNavigateToHome()
            authViewModel.resetSetupState()
        }
    }

    // Show error snackbar if there's an error
    authState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // You can add SnackbarHost here if needed
            // For now, we'll just clear the error after showing
            authViewModel.clearError()
        }
    }

    // Validation errors
    var ageError by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf("") }
    var heightError by remember { mutableStateOf("") }
    var emergencyContactError by remember { mutableStateOf("") }

    // Steps configuration
    val steps = listOf("Basic Info", "Activity", "Health", "Emergency")
    val totalSteps = steps.size

    // Keep all your validation functions
    fun validateAge(): Boolean {
        val age = profileData.age.toIntOrNull()
        return when {
            profileData.age.isBlank() -> {
                ageError = "Age is required"
                false
            }
            age == null || age < 13 || age > 120 -> {
                ageError = "Please enter a valid age (13-120)"
                false
            }
            else -> {
                ageError = ""
                true
            }
        }
    }

    fun validateWeight(): Boolean {
        val weight = profileData.weight.toFloatOrNull()
        return when {
            profileData.weight.isBlank() -> {
                weightError = "Weight is required"
                false
            }
            weight == null || weight < 20 || weight > 300 -> {
                weightError = "Please enter a valid weight (20-300 kg)"
                false
            }
            else -> {
                weightError = ""
                true
            }
        }
    }

    fun validateHeight(): Boolean {
        val height = profileData.height.toFloatOrNull()
        return when {
            profileData.height.isBlank() -> {
                heightError = "Height is required"
                false
            }
            height == null || height < 50 || height > 250 -> {
                heightError = "Please enter a valid height (50-250 cm)"
                false
            }
            else -> {
                heightError = ""
                true
            }
        }
    }

    fun validateEmergencyContact(): Boolean {
        return when {
            profileData.emergencyContact.isBlank() -> {
                emergencyContactError = "Emergency contact is required"
                false
            }
            profileData.emergencyContact.length < 10 -> {
                emergencyContactError = "Please enter a valid phone number"
                false
            }
            else -> {
                emergencyContactError = ""
                true
            }
        }
    }

    fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            0 -> validateAge() && validateWeight() && validateHeight()
            1 -> true // Activity level always has default
            2 -> true // Medical condition always has default
            3 -> validateEmergencyContact()
            else -> true
        }
    }

    Scaffold{ innerpadding->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerpadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp, 32.dp, 24.dp, 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Setup Profile",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress indicator
                    StepProgressIndicator(
                        currentStep = currentStep,
                        totalSteps = totalSteps,
                        stepLabels = steps
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentStep) {
                    0 -> BasicInfoStep(
                        profileData = profileData,
                        onProfileDataChange = { profileData = it },
                        ageError = ageError,
                        weightError = weightError,
                        heightError = heightError,
                        onAgeErrorChange = { ageError = it },
                        onWeightErrorChange = { weightError = it },
                        onHeightErrorChange = { heightError = it },
                        isEnabled = !isLoading
                    )
                    1 -> ActivityStep(
                        profileData = profileData,
                        onProfileDataChange = { profileData = it },
                        isEnabled = !isLoading
                    )
                    2 -> HealthInfoStep(
                        profileData = profileData,
                        onProfileDataChange = { profileData = it },
                        isEnabled = !isLoading
                    )
                    3 -> EmergencyContactStep(
                        profileData = profileData,
                        onProfileDataChange = { profileData = it },
                        emergencyContactError = emergencyContactError,
                        onEmergencyContactErrorChange = { emergencyContactError = it },
                        isEnabled = !isLoading
                    )
                }
            }

            // Bottom action buttons
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Back button (except on first step)
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            Text("Back")
                        }
                    }

                    // Next/Complete button
                    Button(
                        onClick = {
                            if (validateCurrentStep()) {
                                if (currentStep < totalSteps - 1) {
                                    currentStep++
                                } else {
                                    onProfileComplete(profileData)
                                }
                            }
                        },
                        modifier = Modifier.weight(if (currentStep > 0) 1f else 1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = if (currentStep < totalSteps - 1) "Continue" else "Complete Setup",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    stepLabels: List<String>
) {
    Column {
        // Progress bar
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Step indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stepLabels[currentStep],
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "${currentStep + 1} of $totalSteps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Step Components
@Composable
private fun BasicInfoStep(
    profileData: UserBasicHealthInfo,
    onProfileDataChange: (UserBasicHealthInfo) -> Unit,
    ageError: String,
    weightError: String,
    heightError: String,
    onAgeErrorChange: (String) -> Unit,
    onWeightErrorChange: (String) -> Unit,
    onHeightErrorChange: (String) -> Unit,
    isEnabled: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Let's start with the basics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
        }

        item {
            ModernTextField(
                value = profileData.age,
                onValueChange = {
                    onProfileDataChange(profileData.copy(age = it))
                    if (ageError.isNotEmpty()) onAgeErrorChange("")
                },
                label = "Age",
                placeholder = "25",
                leadingIcon = Icons.Filled.Cake,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                isError = ageError.isNotEmpty(),
                errorMessage = ageError,
                enabled = isEnabled
            )
        }

        item {
            GenderDropdown(
                selectedGender = profileData.gender,
                onGenderSelected = { onProfileDataChange(profileData.copy(gender = it)) },
                enabled = isEnabled
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ModernTextField(
                        value = profileData.weight,
                        onValueChange = {
                            onProfileDataChange(profileData.copy(weight = it))
                            if (weightError.isNotEmpty()) onWeightErrorChange("")
                        },
                        label = "Weight",
                        placeholder = "70 kg",
                        leadingIcon = Icons.Filled.FitnessCenter,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        isError = weightError.isNotEmpty(),
                        errorMessage = weightError,
                        enabled = isEnabled
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ModernTextField(
                        value = profileData.height,
                        onValueChange = {
                            onProfileDataChange(profileData.copy(height = it))
                            if (heightError.isNotEmpty()) onHeightErrorChange("")
                        },
                        label = "Height",
                        placeholder = "175 cm",
                        leadingIcon = Icons.Filled.Height,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        isError = heightError.isNotEmpty(),
                        errorMessage = heightError,
                        enabled = isEnabled
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityStep(
    profileData: UserBasicHealthInfo,
    onProfileDataChange: (UserBasicHealthInfo) -> Unit,
    isEnabled: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Activity Level",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "This helps us personalize your care plan",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(ActivityLevel.values().toList()) { level ->
            ActivityLevelCard(
                activityLevel = level,
                isSelected = profileData.activityLevel == level,
                onClick = { onProfileDataChange(profileData.copy(activityLevel = level)) },
                enabled = isEnabled
            )
        }
    }
}

@Composable
private fun HealthInfoStep(
    profileData: UserBasicHealthInfo,
    onProfileDataChange: (UserBasicHealthInfo) -> Unit,
    isEnabled: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Health Information",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
        }

        item {
            MedicalConditionDropdown(
                selectedCondition = profileData.medicalCondition,
                onConditionSelected = { onProfileDataChange(profileData.copy(medicalCondition = it)) },
                enabled = isEnabled
            )
        }

        item {
            BloodTypeDropdown(
                selectedBloodType = profileData.bloodType,
                onBloodTypeSelected = { onProfileDataChange(profileData.copy(bloodType = it)) },
                enabled = isEnabled
            )
        }

        item {
            ModernTextField(
                value = profileData.allergies,
                onValueChange = { onProfileDataChange(profileData.copy(allergies = it)) },
                label = "Allergies",
                placeholder = "Any known allergies (optional)",
                leadingIcon = Icons.Filled.Warning,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                enabled = isEnabled
            )
        }
    }
}

@Composable
private fun EmergencyContactStep(
    profileData: UserBasicHealthInfo,
    onProfileDataChange: (UserBasicHealthInfo) -> Unit,
    emergencyContactError: String,
    onEmergencyContactErrorChange: (String) -> Unit,
    isEnabled: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Emergency Contact",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Someone we can contact in case of emergency",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ModernTextField(
                value = profileData.emergencyContact,
                onValueChange = {
                    onProfileDataChange(profileData.copy(emergencyContact = it))
                    if (emergencyContactError.isNotEmpty()) onEmergencyContactErrorChange("")
                },
                label = "Phone Number",
                placeholder = "+91 98765 43210",
                leadingIcon = Icons.Filled.ContactPhone,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                isError = emergencyContactError.isNotEmpty(),
                errorMessage = emergencyContactError,
                enabled = isEnabled
            )
        }
    }
}

@Composable
private fun ActivityLevelCard(
    activityLevel: ActivityLevel,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { if (enabled) onClick() },
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activityLevel.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = activityLevel.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicalConditionDropdown(
    selectedCondition: MedicalCondition,
    onConditionSelected: (MedicalCondition) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded && enabled },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCondition.displayName,
            onValueChange = { },
            readOnly = true,
            label = { Text("Medical Condition") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.MedicalServices,
                    contentDescription = null
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            enabled = enabled,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            MedicalCondition.values().forEach { condition ->
                DropdownMenuItem(
                    text = { Text(condition.displayName) },
                    onClick = {
                        onConditionSelected(condition)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MinimalActivitySelector(
    selectedLevel: ActivityLevel,
    onLevelSelected: (ActivityLevel) -> Unit,
    enabled: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActivityLevel.values().forEach { level ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled) { onLevelSelected(level) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedLevel == level,
                    onClick = { if (enabled) onLevelSelected(level) },
                    enabled = enabled,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = level.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = level.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// Simplified Dropdown Components
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderDropdown(
    selectedGender: Gender,
    onGenderSelected: (Gender) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded && enabled },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedGender.displayName,
            onValueChange = { },
            readOnly = true,
            label = { Text("Gender") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            enabled = enabled,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Gender.values().forEach { gender ->
                DropdownMenuItem(
                    text = { Text(gender.displayName) },
                    onClick = {
                        onGenderSelected(gender)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BloodTypeDropdown(
    selectedBloodType: BloodType,
    onBloodTypeSelected: (BloodType) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded && enabled }
    ) {
        OutlinedTextField(
            value = selectedBloodType.displayName,
            onValueChange = { },
            readOnly = true,
            label = { Text("Blood Type") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Bloodtype,
                    contentDescription = null
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            enabled = enabled,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BloodType.values().forEach { bloodType ->
                DropdownMenuItem(
                    text = { Text(bloodType.displayName) },
                    onClick = {
                        onBloodTypeSelected(bloodType)
                        expanded = false
                    }
                )
            }
        }
    }
}

