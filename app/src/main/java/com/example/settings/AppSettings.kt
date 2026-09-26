package com.example.settings

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिंदी (Hindi)")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val defaultGst: Double = 3.0,
    val isRoundingEnabled: Boolean = true,
    val isTtsEnabled: Boolean = true,
    val speechSpeed: Float = 1.0f,
    val autoSaveHistory: Boolean = true
)
