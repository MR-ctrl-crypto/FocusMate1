package com.example.focusmate

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // --- Handle Clicks and Changes ---

        // 1. Manage Account Click Listener
        findPreference<Preference>("manage_account")?.setOnPreferenceClickListener {
            val intent = Intent(requireActivity(), ManageAccountActivity::class.java)
            startActivity(intent)
            true
        }

        // 2. Theme Change Listener
        findPreference<ListPreference>("theme_preference")?.setOnPreferenceChangeListener { _, newValue ->
            val theme = newValue as String
            when (theme) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "auto" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            true
        }

        // --- THIS IS THE STEP WE ARE IMPLEMENTING ---
        // 3. Find the "Blocked Apps" preference and set its click listener.
        //    Ensure the key "blocked_apps" matches the key in your preferences.xml.
        findPreference<Preference>("blocked_apps")?.setOnPreferenceClickListener {
            // Create an intent to launch your new BlockedAppsActivity.
            val intent = Intent(requireActivity(), BlockedAppsActivity::class.java)
            // Start the activity.
            startActivity(intent)
            // Return true to indicate that the click has been handled.
            true
        }
    }
}
