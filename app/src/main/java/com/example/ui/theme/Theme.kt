package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PremiumAccentMint,
    onPrimary = Color.Black,
    secondary = PremiumAccentBlue,
    onSecondary = Color.White,
    background = PremiumDeepDark,
    surface = PremiumElevatedDark,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E2942),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = PremiumAccentRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF047857), // Professional emerald/teal green with high contrast and readability
    onPrimary = Color.White,
    secondary = Color(0xFF0284C7), // Elegant sky blue
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC), // Modern ultra-clean soft slate-blue background
    surface = Color(0xFFFFFFFF), // Crisp pure white for cards
    onBackground = Color(0xFF0F172A), // Deep slate gray text
    onSurface = Color(0xFF0F172A), // Deep slate gray text
    surfaceVariant = Color(0xFFF1F5F9), // Soft light gray-blue for card variants/highlights
    onSurfaceVariant = Color(0xFF475569), // Slate gray secondary text
    outline = Color(0xFFCBD5E1), // Soft borders
    error = Color(0xFFDC2626) // Deep warning red
)

@Composable
fun MyApplicationTheme(
    themePreference: String = "Dark", // "Dark", "Light", "System"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
