package com.niteshray.xapps.healthforge.feature.auth.presentation.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.niteshray.xapps.healthforge.feature.auth.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var acceptTerms by remember { mutableStateOf(false) }

    // Validation errors
    var fullNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }
    var termsError by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.PATIENT) }

    val authState = authViewModel.authState
    val focusManager = LocalFocusManager.current

    // Validation functions
    fun validateFullName(): Boolean {
        return when {
            fullName.isBlank() -> {
                fullNameError = "Full name is required"
                false
            }
            fullName.length < 2 -> {
                fullNameError = "Name must be at least 2 characters"
                false
            }
            else -> {
                fullNameError = ""
                true
            }
        }
    }

    fun validateEmail(): Boolean {
        return when {
            email.isBlank() -> {
                emailError = "Email is required"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailError = "Please enter a valid email"
                false
            }
            else -> {
                emailError = ""
                true
            }
        }
    }

    fun validatePassword(): Boolean {
        return when {
            password.isBlank() -> {
                passwordError = "Password is required"
                false
            }
            password.length < 8 -> {
                passwordError = "Password must be at least 8 characters"
                false
            }
            !password.any { it.isDigit() } -> {
                passwordError = "Password must contain at least one number"
                false
            }
            !password.any { it.isUpperCase() } -> {
                passwordError = "Password must contain at least one uppercase letter"
                false
            }
            else -> {
                passwordError = ""
                true
            }
        }
    }

    fun validateConfirmPassword(): Boolean {
        return when {
            confirmPassword.isBlank() -> {
                confirmPasswordError = "Please confirm your password"
                false
            }
            confirmPassword != password -> {
                confirmPasswordError = "Passwords do not match"
                false
            }
            else -> {
                confirmPasswordError = ""
                true
            }
        }
    }

    fun validateTerms(): Boolean {
        return if (!acceptTerms) {
            termsError = "Please accept the terms and conditions"
            false
        } else {
            termsError = ""
            true
        }
    }

    // Navigate when authenticated
    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            onSignupSuccess()
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Header Section
            SignupHeader()

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "I'm a",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            RoleSelectionCard(
                selectedRole = selectedRole,
                onRoleSelected = { selectedRole = it },
                enabled = !authState.isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))


            // Signup Form Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Create Account as $selectedRole",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Full Name Field
                    ModernTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (fullNameError.isNotEmpty()) fullNameError = ""
                            authViewModel.clearError()
                        },
                        label = "Full Name",
                        placeholder = "Enter your full name",
                        leadingIcon = Icons.Filled.Person,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        isError = fullNameError.isNotEmpty(),
                        errorMessage = fullNameError,
                        enabled = !authState.isLoading
                    )

                    // Email Field
                    ModernTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailError.isNotEmpty()) emailError = ""
                            authViewModel.clearError()
                        },
                        label = "Email Address",
                        placeholder = "Enter your email",
                        leadingIcon = Icons.Filled.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        isError = emailError.isNotEmpty(),
                        errorMessage = emailError,
                        enabled = !authState.isLoading
                    )

                    // Password Field
                    ModernTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (passwordError.isNotEmpty()) passwordError = ""
                            if (confirmPasswordError.isNotEmpty() && confirmPassword.isNotEmpty()) {
                                confirmPasswordError = ""
                            }
                            authViewModel.clearError()
                        },
                        label = "Password",
                        placeholder = "Create a strong password",
                        leadingIcon = Icons.Filled.Lock,
                        trailingIcon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        onTrailingIconClick = { passwordVisible = !passwordVisible },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        isError = passwordError.isNotEmpty(),
                        errorMessage = passwordError,
                        enabled = !authState.isLoading
                    )

                    // Confirm Password Field
                    ModernTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            if (confirmPasswordError.isNotEmpty()) confirmPasswordError = ""
                            authViewModel.clearError()
                        },
                        label = "Confirm Password",
                        placeholder = "Confirm your password",
                        leadingIcon = Icons.Filled.Lock,
                        trailingIcon = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        onTrailingIconClick = { confirmPasswordVisible = !confirmPasswordVisible },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        isError = confirmPasswordError.isNotEmpty(),
                        errorMessage = confirmPasswordError,
                        enabled = !authState.isLoading
                    )

                    // Password Strength Indicator
                    AnimatedVisibility(
                        visible = password.isNotEmpty(),
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        PasswordStrengthIndicator(password = password)
                    }

                    // Terms and Conditions
                    TermsAndConditionsSection(
                        acceptTerms = acceptTerms,
                        onTermsChange = {
                            acceptTerms = it
                            if (termsError.isNotEmpty()) termsError = ""
                        },
                        isError = termsError.isNotEmpty(),
                        errorMessage = termsError,
                        enabled = !authState.isLoading
                    )

                    // Error Message from API
                    AnimatedVisibility(
                        visible = authState.errorMessage != null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        authState.errorMessage?.let { errorMessage ->
                            ErrorCard(message = errorMessage)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sign Up Button
                    Button(
                        onClick = {
                            val isFullNameValid = validateFullName()
                            val isEmailValid = validateEmail()
                            val isPasswordValid = validatePassword()
                            val isConfirmPasswordValid = validateConfirmPassword()
                            val isTermsValid = validateTerms()

                            if (isFullNameValid && isEmailValid && isPasswordValid &&
                                isConfirmPasswordValid && isTermsValid) {
                                authViewModel.registerUser(fullName, email, password)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
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
                                text = "Create Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login Section
            LoginSection(
                onLoginClick = onLoginClick,
                enabled = !authState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SignupHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // App Logo placeholder
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "H",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "Join Us Today!",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Create your account to get started",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PasswordStrengthIndicator(password: String) {
    val strength = calculatePasswordStrength(password)
    val strengthColor = when (strength.level) {
        PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
        PasswordStrength.MEDIUM -> MaterialTheme.colorScheme.tertiary
        PasswordStrength.STRONG -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Password Strength",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = strength.level.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = strengthColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LinearProgressIndicator(
                progress = { strength.score / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = strengthColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun TermsAndConditionsSection(
    acceptTerms: Boolean,
    onTermsChange: (Boolean) -> Unit,
    isError: Boolean,
    errorMessage: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Main terms acceptance row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = enabled) { onTermsChange(!acceptTerms) }
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Custom checkbox with better touch target
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = acceptTerms,
                    onCheckedChange = null, // Handled by row click
                    enabled = enabled,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = if (isError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.outline,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                        disabledCheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        disabledUncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Terms text with better layout
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                // Using AnnotatedString for better text handling
                val annotatedString = buildAnnotatedString {
                    append("I agree to the ")

                    // Terms & Conditions link
                    pushStringAnnotation(tag = "terms", annotation = "terms_click")
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("Terms & Conditions")
                    }
                    pop()

                    append(" and ")

                    // Privacy Policy link
                    pushStringAnnotation(tag = "privacy", annotation = "privacy_click")
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("Privacy Policy")
                    }
                    pop()
                }

                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        lineHeight = 20.sp
                    ),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(
                            tag = "terms",
                            start = offset,
                            end = offset
                        ).firstOrNull()
                        annotatedString.getStringAnnotations(
                            tag = "privacy",
                            start = offset,
                            end = offset
                        ).firstOrNull()
                    }
                )
            }
        }

        // Error message with better animation and layout
        AnimatedVisibility(
            visible = isError && errorMessage.isNotEmpty(),
            enter = slideInVertically(
                initialOffsetY = { -it / 2 }
            ) + fadeIn(
                animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it / 2 }
            ) + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, top = 8.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}


@Composable
private fun LoginSection(
    onLoginClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Divider with "OR"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                text = "OR",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        // Login Link
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Sign In",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = enabled) { onLoginClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

// Password strength calculation
private data class PasswordStrengthResult(
    val level: PasswordStrength,
    val score: Int
)

private enum class PasswordStrength {
    WEAK, MEDIUM, STRONG
}

private fun calculatePasswordStrength(password: String): PasswordStrengthResult {
    var score = 0

    if (password.length >= 8) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++

    val level = when (score) {
        0, 1 -> PasswordStrength.WEAK
        2 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.STRONG
    }

    return PasswordStrengthResult(level, score)
}
