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

    // Get a reference to the same ViewModel instance as the activity
    private val profileViewModel: ProfileViewModel by activityViewModels()

    private lateinit var prefs: SharedPreferences

    // Activity result launcher for picking an image from the gallery
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

        // --- USERNAME PREFERENCE --- (No changes here)
        val usernamePreference: EditTextPreference? = findPreference("username")
        usernamePreference?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            val text = preference.text
            if (text.isNullOrBlank()) "Not set" else text
        }
        usernamePreference?.setOnPreferenceChangeListener { _, newValue ->
            profileViewModel.updateUsername(newValue.toString())
            true
        }

        // --- PROFILE PICTURE PREFERENCE --- (No changes here)
        val profilePicturePreference: Preference? = findPreference("profile_picture")
        profilePicturePreference?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
            true
        }

        // ======================= NEW CODE START =======================
        // --- MANAGE ACCOUNT PREFERENCE ---
        val manageAccountPreference: Preference? = findPreference("manage_account")
        manageAccountPreference?.setOnPreferenceClickListener {
            // Create an instance of the new fragment
            val manageAccountFragment = ManageAccountFragment()

            // Use the FragmentManager to replace the current fragment with the new one
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, manageAccountFragment)
                .addToBackStack(null) // This allows the user to press the back button to return to settings
                .commit()

            true // Return true to indicate the click was handled
        }
        // ======================= NEW CODE END =======================

        // --- Load initial values on fragment creation ---
        loadInitialProfileData()
    }

    private fun loadInitialProfileData() {
        // Load username from SharedPreferences and update the ViewModel
        val savedUsername = prefs.getString("username", "User") ?: "User"
        profileViewModel.updateUsername(savedUsername)

        // Load profile picture URI from SharedPreferences and update the ViewModel
        val savedImageUriString = prefs.getString("profile_image_uri", null)
        if (savedImageUriString != null) {
            profileViewModel.updateProfileImageUri(Uri.parse(savedImageUriString))
        }
    }

    private fun takeUriPermission(uri: Uri): Uri {
        // Persist access permissions so the app can still access the image after a restart.
        val contentResolver = requireActivity().contentResolver
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)
        return uri
    }
}
