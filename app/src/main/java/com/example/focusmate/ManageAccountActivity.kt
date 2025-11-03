package com.example.focusmate

import android.content.Intent // 1. IMPORT INTENT
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class ManageAccountActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var nameEditText: TextInputEditText
    private lateinit var saveButton: Button

    private lateinit var userPreferences: UserPreferences
    private var selectedImageUri: Uri? = null

    // ActivityResultLauncher for picking an image from the gallery
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // --- FIX PART 1: TAKE PERSISTENT PERMISSION FOR THE NEW URI ---
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: SecurityException) {
                // This can happen on some devices/file managers.
                Log.e("ManageAccountActivity", "Failed to take persistable permission for URI: $it", e)
                Toast.makeText(this, "Could not save image permission.", Toast.LENGTH_SHORT).show()
            }
            // --- END OF FIX ---

            // Store the selected URI and update the ImageView
            selectedImageUri = it
            profileImageView.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_account)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Manage Account"

        userPreferences = UserPreferences(this)

        profileImageView = findViewById(R.id.profile_image_view)
        nameEditText = findViewById(R.id.name_edit_text)
        saveButton = findViewById(R.id.save_button)

        profileImageView.setOnClickListener {
            pickImage.launch("image/*")
        }

        saveButton.setOnClickListener {
            saveAccountChanges()
        }

        loadUserData()
    }

    private fun loadUserData() {
        val savedName = userPreferences.getUserName()
        // It's better to get the URI as a string and parse it
        val savedImageUriString = userPreferences.getProfileImageUriString()

        nameEditText.setText(savedName)

        if (!savedImageUriString.isNullOrEmpty()) {
            val savedUri = Uri.parse(savedImageUriString)
            selectedImageUri = savedUri

            // --- FIX PART 2: ADD A TRY-CATCH BLOCK FOR SAFETY ---
            try {
                // Check if we still have permission to read the URI
                // The takePersistableUriPermission call ensures this list is not empty.
                val persistedUris = contentResolver.persistedUriPermissions
                if (persistedUris.any { it.uri == savedUri }) {
                    profileImageView.setImageURI(savedUri)
                } else {
                    // If permission was lost, show a default image.
                    profileImageView.setImageResource(R.mipmap.ic_launcher_round)
                }
            } catch (e: SecurityException) {
                Log.e("ManageAccountActivity", "Permission denial for saved URI: $savedUri", e)
                // If permission was revoked, fall back to a default image.
                profileImageView.setImageResource(R.mipmap.ic_launcher_round)
            }
            // --- END OF FIX ---
        }
    }

    private fun saveAccountChanges() {
        val newName = nameEditText.text.toString()

        if (newName.isBlank()) {
            nameEditText.error = "Name cannot be empty"
            return
        }

        // Convert the URI to a string for saving. If null, save null.
        val imageUriString = selectedImageUri?.toString()
        userPreferences.saveUserData(newName, imageUriString)

        Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
