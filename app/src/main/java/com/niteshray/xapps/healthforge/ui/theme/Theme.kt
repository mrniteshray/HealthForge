package com.niteshray.xapps.healthforge.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC4C6FF),           // Lighter blue for dark theme
    onPrimary = Color(0xFF001B3C),         // Dark text on primary
    primaryContainer = Color(0xFF003D6B),   // Darker blue container
    onPrimaryContainer = Color(0xFFE3E5FF), // Light text on container

    secondary = Color(0xFFC2C5DD),          // Light blue-gray
    onSecondary = Color(0xFF2C2F42),        // Dark text on secondary
    secondaryContainer = Color(0xFF424659), // Medium secondary container
    onSecondaryContainer = Color(0xFFDEE1F9), // Light text on secondary container

    tertiary = Color(0xFFE5BAD8),           // Light purple-pink
    onTertiary = Color(0xFF44263E),         // Dark text on tertiary
    tertiaryContainer = Color(0xFF5D3D55),  // Medium tertiary container
    onTertiaryContainer = Color(0xFFFFD7F3), // Light text on tertiary container

    error = Color(0xFFFFB4AB),              // Light error color
    errorContainer = Color(0xFF93000A),     // Dark error container
    onError = Color(0xFF690005),            // Dark text on error
    onErrorContainer = Color(0xFFFFDAD6),   // Light text on error container

    background = Color(0xFF121218),         // Dark background
    onBackground = Color(0xFFE4E1E6),       // Light text on background
    surface = Color(0xFF121218),            // Surface same as background
    onSurface = Color(0xFFE4E1E6),          // Light text on surface

    surfaceVariant = Color(0xFF45464A),     // Dark surface variant
    onSurfaceVariant = Color(0xFFC6C5D0),   // Light text on surface variant
    outline = Color(0xFF8F909A),            // Border/outline color
    outlineVariant = Color(0xFF45464A),     // Darker outline

    inverseSurface = Color(0xFFE4E1E6),     // Light inverse surface
    inverseOnSurface = Color(0xFF303034),   // Dark text on inverse surface
    inversePrimary = Color(0xFF5F6FFF),     // Original primary for light backgrounds

    scrim = Color(0xFF000000),              // Black overlay
    surfaceTint = Color(0xFFC4C6FF)         // Light primary as surface tint
)


private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5F6FFF),           // Your existing blue
    onPrimary = Color(0xFFFFFFFF),         // White text on primary
    primaryContainer = Color(0xFFE3E5FF),  // Light blue container
    onPrimaryContainer = Color(0xFF001B3C), // Dark text on container

    secondary = Color(0xFF5A5D72),          // Neutral blue-gray
    onSecondary = Color(0xFFFFFFFF),        // White text on secondary
    secondaryContainer = Color(0xFFDEE1F9), // Light secondary container
    onSecondaryContainer = Color(0xFF171B2C), // Dark text on secondary container

    tertiary = Color(0xFF76546D),           // Complementary purple-pink
    onTertiary = Color(0xFFFFFFFF),         // White text on tertiary
    tertiaryContainer = Color(0xFFFFD7F3),  // Light tertiary container
    onTertiaryContainer = Color(0xFF2D1228), // Dark text on tertiary container

    error = Color(0xFFBA1A1A),              // Standard error red
    errorContainer = Color(0xFFFFDAD6),     // Light error container
    onError = Color(0xFFFFFFFF),            // White text on error
    onErrorContainer = Color(0xFF410002),   // Dark text on error container

    background = Color(0xFFFFFBFF),         // Clean white background
    onBackground = Color(0xFF1B1B1F),       // Dark text on background
    surface = Color(0xFFFFFBFF),            // Surface same as background
    onSurface = Color(0xFF1B1B1F),          // Dark text on surface

    surfaceVariant = Color(0xFFE2E1EC),     // Light surface variant
    onSurfaceVariant = Color(0xFF45464A),   // Medium text on surface variant
    outline = Color(0xFF757579),            // Border/outline color
    outlineVariant = Color(0xFFC6C5D0),     // Lighter outline

    inverseSurface = Color(0xFF303034),     // Dark inverse surface
    inverseOnSurface = Color(0xFFF2F0F4),   // Light text on inverse surface
    inversePrimary = Color(0xFFC4C6FF),     // Light primary for dark backgrounds

    scrim = Color(0xFF000000),              // Black overlay
    surfaceTint = Color(0xFF5F6FFF)         // Primary as surface tint
)


@Composable
fun HealthForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }

    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}