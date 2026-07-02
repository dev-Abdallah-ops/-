package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val theme: String = "Dark", // "Dark", "Light", "System"
    val language: String = "English", // "English", "Spanish", "French", "German", "Arabic"
    val currency: String = "USD", // "USD", "EUR", "GBP", "JPY", "SAR", "AED", "CNY", "INR", "CAD", "AUD", "CHF", "BRL", "KWD", "MXN", "EGP"
    val monthlyLimit: Double = 0.0, // 0.0 means no limit
    val isBiometricLockEnabled: Boolean = false,
    val backupPin: String = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4",
    val securityQuestion: String = "",
    val securityAnswer: String = "",
    val isSmartAlertsEnabled: Boolean = true,
    val hasCompletedOnboarding: Boolean = false
)
