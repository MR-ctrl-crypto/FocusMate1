package com.example.focusmate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso // You'll need to add this library, see step 4
import de.hdodenhof.circleimageview.CircleImageView

class ManageAccountFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by activityViewModels()

    private lateinit var profileImageView: CircleImageView
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var profilePictureLayout: FrameLayout

    // Your specific Firebase Database URL
    private val databaseUrl = "https://focusmate-51ac3-default-rtdb.europe-west1.firebasedatabase.app"

    private var newImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                newImageUri = it
                profileImageView.setImageURI(newImageUri) // Preview the new image
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_account, container, false)

        profileImageView = view.findViewById(R.id.image_profile_edit)
        usernameEditText = view.findViewById(R.id.edit_text_username)
        saveButton = view.findViewById(R.id.button_save)
        backButton = view.findViewById(R.id.button_back)
        profilePictureLayout = view.findViewById(R.id.profile_picture_layout)

        setupClickListeners()
        observeViewModel()

        return view
    }

    private fun observeViewModel() {
        profileViewModel.username.observe(viewLifecycleOwner) { currentName ->
            if (usernameEditText.text.toString() != currentName) {
                usernameEditText.setText(currentName)
            }
        }
        profileViewModel.profileImageUri.observe(viewLifecycleOwner) { currentUri ->
            if (currentUri != null) {
                // Use Picasso to load the image URL efficiently
                Picasso.get().load(currentUri).placeholder(R.drawable.ic_profile_placeholder).into(profileImageView)
            } else {
                profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        profilePictureLayout.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        saveButton.setOnClickListener {
            saveProfileChangesToFirebase()
        }
    }

    private fun saveProfileChangesToFirebase() {
        val newUsername = usernameEditText.text.toString().trim()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (newUsername.isBlank()) {
            Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == null) {
            Toast.makeText(context, "You are not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Show a loading indicator (optional but good UX)
        saveButton.isEnabled = false
        saveButton.text = "Saving..."

        // Check if a new image was selected
        if (newImageUri != null) {
            // --- Step 1: A new image was selected, so upload it first ---
            val storageRef = FirebaseStorage.getInstance().getReference("profile_pictures/$userId/profile.jpg")
            storageRef.putFile(newImageUri!!)
                .addOnSuccessListener {
                    // --- Step 2: Image uploaded, now get its URL ---
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        // --- Step 3: We have the URL, now save everything to Realtime Database ---
                        saveUsernameAndImageUrl(userId, newUsername, downloadUri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                    resetSaveButton()
                }
        } else {
            // --- No new image, just save the username ---
            saveUsernameAndImageUrl(userId, newUsername, null)
        }
    }

    private fun saveUsernameAndImageUrl(userId: String, username: String, imageUrl: String?) {
        val dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users/$userId/profile")

        val updates = mutableMapOf<String, Any>()
        updates["username"] = username
        imageUrl?.let { updates["profileImageUrl"] = it } // Only update image URL if it's not null

        dbRef.updateChildren(updates)
            .addOnSuccessListener {
                // --- Step 4: Update the local ViewModel to refresh UI instantly ---
                profileViewModel.updateUsername(username)
                imageUrl?.let { profileViewModel.updateProfileImageUri(Uri.parse(it)) }

                Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                resetSaveButton()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                resetSaveButton()
            }
    }

    private fun resetSaveButton() {
        saveButton.isEnabled = true
        saveButton.text = "Save"
    }
}
