package com.niteshray.xapps.healthforge.feature.home.presentation.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.BloodType
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.Gender
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.MedicalCondition
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.UserBasicHealthInfo
import com.niteshray.xapps.healthforge.feature.home.presentation.viewmodel.ProfileViewModel
import com.niteshray.xapps.healthforge.feature.home.presentation.viewmodel.UserProfileData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHealthInfoDialog(
    healthInfo: UserBasicHealthInfo?,
    profileViewModel: ProfileViewModel,
    onDismiss: () -> Unit
) {
    var weight by remember { mutableStateOf(healthInfo?.weight ?: "") }
    var height by remember { mutableStateOf(healthInfo?.height ?: "") }
    var age by remember { mutableStateOf(healthInfo?.age ?: "") }
    var selectedGender by remember { mutableStateOf(healthInfo?.gender ?: Gender.MALE) }
    var selectedBloodType by remember { mutableStateOf(healthInfo?.bloodType ?: BloodType.O_POSITIVE) }
    var allergies by remember { mutableStateOf(healthInfo?.allergies ?: "") }
    var selectedMedicalCondition by remember { mutableStateOf(healthInfo?.medicalCondition ?: MedicalCondition.NONE) }
    var emergencyContact by remember { mutableStateOf(healthInfo?.emergencyContact ?: "") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Health Info",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Basic Info Section
                Text(
                    text = "Basic Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = {
                            weight = it
                            errorMessage = null
                        },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading
                    )

                    OutlinedTextField(
                        value = height,
                        onValueChange = {
                            height = it
                            errorMessage = null
                        },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading
                    )

                    OutlinedTextField(
                        value = age,
                        onValueChange = {
                            age = it
                            errorMessage = null
                        },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading
                    )
                }

                // Gender Selection
                Text(
                    text = "Gender",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Gender.values().forEach { gender ->
                        Row(
                            modifier = Modifier
                                .selectable(
                                    selected = (selectedGender == gender),
                                    onClick = { selectedGender = gender },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedGender == gender),
                                onClick = null,
                                enabled = !isLoading
                            )
                            Text(
                                text = gender.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                // Blood Type Selection
                Text(
                    text = "Blood Type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedBloodType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Blood Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        BloodType.values().forEach { bloodType ->
                            DropdownMenuItem(
                                text = { Text(bloodType.displayName) },
                                onClick = {
                                    selectedBloodType = bloodType
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Medical Information Section
                Text(
                    text = "Medical Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text("Allergies (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading,
                    maxLines = 2
                )

                // Medical Condition Selection
                Text(
                    text = "Medical Condition",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                var medicalConditionExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = medicalConditionExpanded,
                    onExpandedChange = { medicalConditionExpanded = !medicalConditionExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedMedicalCondition.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Medical Condition") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = medicalConditionExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = medicalConditionExpanded,
                        onDismissRequest = { medicalConditionExpanded = false }
                    ) {
                        MedicalCondition.values().forEach { medicalCondition ->
                            DropdownMenuItem(
                                text = { Text(medicalCondition.displayName) },
                                onClick = {
                                    selectedMedicalCondition = medicalCondition
                                    medicalConditionExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("Emergency Contact (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                // Error Message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            // Validate required fields
                            if (weight.isBlank() || height.isBlank() || age.isBlank()) {
                                errorMessage = "Weight, height, and age are required"
                                return@Button
                            }

                            // Validate numeric fields
                            if (weight.toDoubleOrNull() == null || height.toDoubleOrNull() == null || age.toIntOrNull() == null) {
                                errorMessage = "Please enter valid numbers for weight, height, and age"
                                return@Button
                            }

                            isLoading = true
                            profileViewModel.updateHealthInfo(
                                weight = weight.trim(),
                                height = height.trim(),
                                age = age.trim(),
                                gender = selectedGender,
                                bloodType = selectedBloodType,
                                allergies = allergies.trim(),
                                medicalCondition = selectedMedicalCondition,
                                emergencyContact = emergencyContact.trim(),
                                onSuccess = {
                                    isLoading = false
                                    onDismiss()
                                },
                                onError = { error ->
                                    isLoading = false
                                    errorMessage = error
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    userProfile: UserProfileData?,
    profileViewModel: ProfileViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(userProfile?.name ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading,
                    isError = errorMessage != null
                )

                // Email (Read-only)
                OutlinedTextField(
                    value = userProfile?.email ?: "",
                    onValueChange = { },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                )

                // Role (Read-only)
                OutlinedTextField(
                    value = userProfile?.role?.replaceFirstChar { it.uppercase() } ?: "",
                    onValueChange = { },
                    label = { Text("Role") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Badge,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                )

                // Error Message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Name cannot be empty"
                                return@Button
                            }

                            isLoading = true
                            profileViewModel.updateUserProfile(
                                name = name.trim(),
                                onSuccess = {
                                    isLoading = false
                                    onDismiss()
                                },
                                onError = { error ->
                                    isLoading = false
                                    errorMessage = error
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}