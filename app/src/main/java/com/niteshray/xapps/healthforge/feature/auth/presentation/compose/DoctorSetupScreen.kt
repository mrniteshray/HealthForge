package com.niteshray.xapps.healthforge.feature.auth.presentation.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.niteshray.xapps.healthforge.feature.auth.presentation.viewmodel.AuthViewModel
import com.niteshray.xapps.healthforge.feature.doctors.domain.model.Address

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorSetupScreen(
    userName: String,
    userEmail: String,
    userPassword: String,
    onSetupComplete: () -> Unit = {},
    onBackClick: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var speciality by remember { mutableStateOf("") }
    var degree by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var fees by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    val authState = authViewModel.authState
    val focusManager = LocalFocusManager.current

    // Navigate when doctor registration is complete
    LaunchedEffect(authState.isDoctorRegistrationComplete) {
        if (authState.isDoctorRegistrationComplete) {
            onSetupComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.MedicalServices,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Complete Doctor Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Welcome $userName!\nPlease complete your professional information",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Speciality
                    ModernTextField(
                        value = speciality,
                        onValueChange = { speciality = it },
                        label = "Medical Speciality",
                        placeholder = "e.g., Cardiology, Dermatology",
                        leadingIcon = Icons.Filled.MedicalServices,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        enabled = !authState.isLoading
                    )

                    // Degree
                    ModernTextField(
                        value = degree,
                        onValueChange = { degree = it },
                        label = "Medical Degree",
                        placeholder = "e.g., MBBS, MD, MS",
                        leadingIcon = Icons.Filled.School,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        enabled = !authState.isLoading
                    )

                    // Experience
                    ModernTextField(
                        value = experience,
                        onValueChange = { experience = it },
                        label = "Years of Experience",
                        placeholder = "Enter years",
                        leadingIcon = Icons.Filled.WorkHistory,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        enabled = !authState.isLoading
                    )

                    // Consultation Fees
                    ModernTextField(
                        value = fees,
                        onValueChange = { fees = it },
                        label = "Consultation Fees (₹)",
                        placeholder = "Enter amount",
                        leadingIcon = Icons.Filled.CurrencyRupee,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        enabled = !authState.isLoading
                    )

                    // City
                    ModernTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = "City",
                        placeholder = "Practice city",
                        leadingIcon = Icons.Filled.LocationCity,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        enabled = !authState.isLoading
                    )

                    // Address
                    ModernTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Practice Address",
                        placeholder = "Clinic/Hospital address",
                        leadingIcon = Icons.Filled.LocationOn,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        enabled = !authState.isLoading
                    )

                    // About
                    ModernTextField(
                        value = about,
                        onValueChange = { about = it },
                        label = "About You",
                        placeholder = "Brief description about your practice",
                        leadingIcon = Icons.Filled.Info,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        enabled = !authState.isLoading
                    )

                    // Error Message
                    AnimatedVisibility(
                        visible = authState.errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        authState.errorMessage?.let { errorMessage ->
                            ErrorCard(message = errorMessage)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Complete Button
                    Button(
                        onClick = {
                            val doctorAddress = Address(
                                line1 = address,
                                line2 = null,
                                city = city,
                                state = "",
                                zip = ""
                            )

                            authViewModel.registerDoctor(
                                name = userName,
                                email = userEmail,
                                password = userPassword,
                                speciality = speciality,
                                degree = degree,
                                experience = experience.toIntOrNull() ?: 0,
                                about = about,
                                fees = fees.toIntOrNull() ?: 0,
                                address = doctorAddress
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !authState.isLoading
                    ) {
                        if (authState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Complete Registration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}