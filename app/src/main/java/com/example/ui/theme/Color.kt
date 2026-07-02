package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Global state to track theme mode dynamically on the UI Thread
var isDarkThemeGlobal: Boolean = true

// Premium Dark Slate FinTech Palette
val PremiumDeepDark = Color(0xFF0B0F19)
val PremiumElevatedDark = Color(0xFF151D30)
val PremiumCardDark = Color(0xFF1E2942)

// Static Base Accents for static configurations (e.g. Schemes)
val BasePremiumAccentMint = Color(0xFF10B981)
val BasePremiumAccentRed = Color(0xFFEF4444)
val BasePremiumAccentPurple = Color(0xFF8B5CF6)
val BasePremiumAccentBlue = Color(0xFF3B82F6)
val BasePremiumAccentOrange = Color(0xFFF59E0B)

val PremiumAccentMint: Color
    get() = if (isDarkThemeGlobal) BasePremiumAccentMint else Color(0xFF059669) // Darkened radiant green for Light contrast

val PremiumAccentRed: Color
    get() = if (isDarkThemeGlobal) BasePremiumAccentRed else Color(0xFFDC2626)  // Darkened warning red for Light contrast
val PremiumAccentPurple: Color
    get() = if (isDarkThemeGlobal) BasePremiumAccentPurple else Color(0xFF6D28D9) // Darkened bill purple for Light contrast

val PremiumAccentBlue: Color
    get() = if (isDarkThemeGlobal) BasePremiumAccentBlue else Color(0xFF1D4ED8)   // Darkened sky blue for Light contrast

val PremiumAccentOrange: Color
    get() = if (isDarkThemeGlobal) BasePremiumAccentOrange else Color(0xFFC2410C) // Darkened yellow/amber for Light contrast

val PremiumTextSecondaryDark: Color
    get() = if (isDarkThemeGlobal) Color(0xFF94A3B8) else Color(0xFF64748B) // Slate gray with high contrast for Light mode and standard slate for Dark mode

// Premium Light Slate Palette
val PremiumLightBg = Color(0xFFF3F4F6)
val PremiumLightSurface = Color(0xFFFFFFFF)
val PremiumLightCard = Color(0xFFE5E7EB)

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
