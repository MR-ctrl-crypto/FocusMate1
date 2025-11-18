package com.example.focusmate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ManageAccountFragment : Fragment() {

    // Use the shared ViewModel to get live data and ensure UI consistency
    private val profileViewModel: ProfileViewModel by activityViewModels()

    // Views
    private lateinit var profileImageView: CircleImageView
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageButton

    // Holds the URI of a newly selected image from the user's device
    private var newImageUri: Uri? = null

    // Activity result launcher for picking an image from the gallery
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                newImageUri = uri
                // Show a preview of the newly selected image
                profileImageView.setImageURI(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_account, container, false)

        // Initialize all views
        profileImageView = view.findViewById(R.id.image_profile_edit)
        usernameEditText = view.findViewById(R.id.edit_text_username)
        saveButton = view.findViewById(R.id.button_save)
        backButton = view.findViewById(R.id.button_back)

        setupClickListeners()
        observeProfileViewModel()

        return view
    }

    /**
     * Observes the shared ProfileViewModel for any changes to username or profile image
     * and updates the UI accordingly. This ensures the screen always shows the latest data.
     */
    private fun observeProfileViewModel() {
        // Observe username changes
        profileViewModel.username.observe(viewLifecycleOwner) { currentName ->
            usernameEditText.setText(currentName)
        }

        // Observe profile image URI changes
        profileViewModel.profileImageUri.observe(viewLifecycleOwner) { currentUri ->
            if (currentUri != null) {
                // Load the image from Firebase Storage using Picasso
                Picasso.get()
                    .load(currentUri)
                    .placeholder(R.drawable.ic_profile_placeholder) // Show a placeholder while loading
                    .into(profileImageView)
            } else {
                // If there's no image URL, show the default placeholder
                profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }

    /**
     * Sets up click listeners for all interactive elements on the screen.
     */
    private fun setupClickListeners() {
        // Handle the back button press to navigate to the previous screen
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Allow the user to select a new profile picture from their device
        profileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        // Trigger the save process when the save button is clicked
        saveButton.setOnClickListener {
            saveProfileChanges()
        }
    }

    /**
     * Main function to handle the logic of saving the profile changes.
     */
    private fun saveProfileChanges() {
        val newUsername = usernameEditText.text.toString().trim()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // --- Validations ---
        if (newUsername.isBlank()) {
            Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == null) {
            Toast.makeText(context, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable the button to prevent multiple clicks and show loading state
        setSaveButtonState(enabled = false, text = "Saving...")

        // --- Logic Branching: Check if a new image was selected ---
        if (newImageUri != null) {
            // Case 1: A new image was picked. We must upload it first.
            uploadImageAndSaveProfile(userId, newUsername, newImageUri!!)
        } else {
            // Case 2: No new image. We only need to update the username.
            updateProfileInDatabase(userId, newUsername, null)
        }
    }

    /**
     * Handles the multi-step process of uploading an image to Firebase Storage,
     * getting its public URL, and then saving all data to the Realtime Database.
     */
    private fun uploadImageAndSaveProfile(userId: String, username: String, imageUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().getReference("profile_pictures/$userId.jpg")
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                // Image uploaded successfully, now get its download URL
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // With the download URL, update the database
                    updateProfileInDatabase(userId, username, downloadUri.toString())
                }.addOnFailureListener { e ->
                    handleSaveFailure("Failed to get image URL: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                handleSaveFailure("Image upload failed: ${e.message}")
            }
    }

    /**
     * Updates the user's profile data in the Firebase Realtime Database.
     * This is the final step for both saving with and without a new image.
     */
    private fun updateProfileInDatabase(userId: String, username: String, imageUrl: String?) {
        // This database path MUST match the path used in SignUpFragment and ProfileViewModel
        val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        val updates = mutableMapOf<String, Any>()
        updates["name"] = username // Use "name" to match SignUpFragment
        // Only add the imageUrl to the update map if a new one was provided
        imageUrl?.let { updates["profileImageUrl"] = it }

        dbRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                // The ValueEventListener in ProfileViewModel will automatically refresh the UI.
                // We can now safely navigate back.
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                handleSaveFailure("Failed to save profile: ${e.message}")
            }
    }

    /**
     * A helper function to show an error message and re-enable the save button.
     */
    private fun handleSaveFailure(errorMessage: String) {
        Log.e("ManageAccountFragment", errorMessage)
        Toast.makeText(context, "An error occurred.", Toast.LENGTH_LONG).show()
        setSaveButtonState(enabled = true, text = "Save")
    }

    /**
     * A helper function to manage the state of the save button.
     */
    private fun setSaveButtonState(enabled: Boolean, text: String) {
        saveButton.isEnabled = enabled
        saveButton.text = text
    }
}
