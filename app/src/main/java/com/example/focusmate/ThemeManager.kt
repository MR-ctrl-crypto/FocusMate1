package com.example.focusmate

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.BuildCompat

object ThemeManager {
    // Define constants for the theme values
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SYSTEM = "system"

    fun applyTheme(themeValue: String) {
        when (themeValue) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> {
                // For API 29+ we can use MODE_NIGHT_FOLLOW_SYSTEM.
                // For older versions, we use MODE_NIGHT_AUTO_BATTERY.
                if (BuildCompat.isAtLeastQ()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
        }
    }
}
