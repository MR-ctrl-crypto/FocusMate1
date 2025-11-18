package com.example.focusmate

import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val themePreference: ListPreference? = findPreference("theme_preference")

        // --- SET UP A LISTENER FOR IT ---
        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            // newValue is the selected value (e.g., "light", "dark")
            if (newValue is String) {
                ThemeManager.applyTheme(newValue)
                true // Indicate that the change was handled
            } else {
                false // Indicate that the change was not handled
            }
        }

        // ======================= FIX #1: MANAGE ACCOUNT =======================
        val manageAccountPreference: Preference? = findPreference("manage_account")
        manageAccountPreference?.setOnPreferenceClickListener {
            // Use the NavController to navigate safely.
            findNavController().navigate(R.id.action_settingsFragment_to_manageAccountFragment)
            true
        }

        // ======================= FIX #3: LOG OUT =======================
        val logoutPreference: Preference? = findPreference("logout")
        logoutPreference?.setOnPreferenceClickListener {
            FirebaseAuth.getInstance().signOut()
            // Navigate back to the auth graph after logging out
            val intent = Intent(activity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            activity?.finish()
            true
        }
    }
}
