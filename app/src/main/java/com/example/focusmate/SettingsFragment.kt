package com.example.focusmate

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    private val profileViewModel: ProfileViewModel by activityViewModels()
    private lateinit var prefs: SharedPreferences
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            if (selectedImageUri != null) {
                val permanentUri = takeUriPermission(selectedImageUri)
                prefs.edit().putString("profile_image_uri", permanentUri.toString()).apply()
                profileViewModel.updateProfileImageUri(permanentUri)
            }
        }
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        prefs = preferenceManager.sharedPreferences!!

        val usernamePreference: EditTextPreference? = findPreference("username")
        usernamePreference?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            val text = preference.text
            if (text.isNullOrBlank()) "Not set" else text
        }
        usernamePreference?.setOnPreferenceChangeListener { _, newValue ->
            profileViewModel.updateUsername(newValue.toString())
            true
        }

        val profilePicturePreference: Preference? = findPreference("profile_picture")
        profilePicturePreference?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
            true
        }

        // ======================= FIX #1: MANAGE ACCOUNT =======================
        val manageAccountPreference: Preference? = findPreference("manage_account")
        manageAccountPreference?.setOnPreferenceClickListener {
            val manageAccountFragment = ManageAccountFragment()
            parentFragmentManager.beginTransaction()
                // Use the CORRECT container ID from MainActivity's layout
                .replace(R.id.main_fragment_container, manageAccountFragment)
                .addToBackStack(null)
                .commit()
            true
        }

        // ======================= FIX #2: BLOCKED APPS =======================
        val blockedAppsPreference: Preference? = findPreference("blocked_apps")
        blockedAppsPreference?.setOnPreferenceClickListener {
            val blockedAppsFragment = BlockedAppsFragment()
            parentFragmentManager.beginTransaction()
                // Use the CORRECT container ID here as well
                .replace(R.id.main_fragment_container, blockedAppsFragment)
                .addToBackStack(null)
                .commit()

            true
        }
        // ======================= END OF FIXES =======================

        loadInitialProfileData()
    }

    private fun loadInitialProfileData() {
        val savedUsername = prefs.getString("username", "User") ?: "User"
        profileViewModel.updateUsername(savedUsername)
        val savedImageUriString = prefs.getString("profile_image_uri", null)
        if (savedImageUriString != null) {
            profileViewModel.updateProfileImageUri(Uri.parse(savedImageUriString))
        }
    }

    private fun takeUriPermission(uri: Uri): Uri {
        val contentResolver = requireActivity().contentResolver
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)
        return uri
    }
}
