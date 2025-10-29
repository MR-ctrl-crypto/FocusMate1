package com.example.focusmate

import android.net.Uri
import android.os.Bundle
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

    // ActivityResultLauncher for picking an image from the gallery
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Set the selected image to the ImageView
            profileImageView.setImageURI(it)
            // Here you would typically save this URI string to SharedPreferences or a database
            // For now, we just display it.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_account)

        // Add a back button to the ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Manage Account"

        // Initialize views
        profileImageView = findViewById(R.id.profile_image_view)
        nameEditText = findViewById(R.id.name_edit_text)
        saveButton = findViewById(R.id.save_button)

        // Set click listener for the profile image
        profileImageView.setOnClickListener {
            // Launch the image picker
            pickImage.launch("image/*")
        }

        // Set click listener for the save button
        saveButton.setOnClickListener {
            saveAccountChanges()
        }

        // Load existing user data (e.g., from SharedPreferences)
        loadUserData()
    }

    private fun loadUserData() {
        // In a real app, you would load the saved name and profile image URI here.
        // For example, from SharedPreferences:
        // val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        // val savedName = prefs.getString("user_name", "Default Name")
        // val imageUriString = prefs.getString("user_profile_uri", null)
        //
        // nameEditText.setText(savedName)
        // imageUriString?.let { profileImageView.setImageURI(Uri.parse(it)) }

        // For this example, we'll just set some placeholder text.
        nameEditText.setText("Your Name")
    }

    private fun saveAccountChanges() {
        val newName = nameEditText.text.toString()

        if (newName.isBlank()) {
            nameEditText.error = "Name cannot be empty"
            return
        }

        // --- Save the data ---
        // Here you would save the new name and the profile image URI to a persistent storage
        // like SharedPreferences or a database.
        //
        // Example with SharedPreferences:
        // val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
        // prefs.putString("user_name", newName)
        // If an image was selected, you would save its URI (as a string) as well.
        // prefs.apply()

        Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show()

        // Finish the activity and go back to the previous screen
        finish()
    }

    // Handle the ActionBar back button press
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
