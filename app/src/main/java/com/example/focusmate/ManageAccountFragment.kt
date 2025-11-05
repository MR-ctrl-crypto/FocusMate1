package com.example.focusmate

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.preference.PreferenceManager
import com.google.android.material.textfield.TextInputEditText
import de.hdodenhof.circleimageview.CircleImageView

class ManageAccountFragment : Fragment() {

    // Get a reference to the same ViewModel instance as the activity
    private val profileViewModel: ProfileViewModel by activityViewModels()

    private lateinit var profileImageView: CircleImageView
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var profilePictureLayout: FrameLayout

    private lateinit var prefs: SharedPreferences
    private var newImageUri: Uri? = null

    // Activity result launcher for picking an image from the gallery
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            if (selectedImageUri != null) {
                newImageUri = selectedImageUri
                profileImageView.setImageURI(newImageUri) // Show a preview of the new image
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_manage_account, container, false)

        // Initialize SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Find all the views
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
        // Observe the ViewModel to set the initial state
        profileViewModel.username.observe(viewLifecycleOwner) { currentName ->
            usernameEditText.setText(currentName)
        }
        profileViewModel.profileImageUri.observe(viewLifecycleOwner) { currentUri ->
            if (currentUri != null) {
                profileImageView.setImageURI(currentUri)
            } else {
                profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }

    private fun setupClickListeners() {
        // Handle back button click
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Handle profile picture click
        profilePictureLayout.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        // Handle save button click
        saveButton.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun saveProfileChanges() {
        val newUsername = usernameEditText.text.toString()

        if (newUsername.isBlank()) {
            Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // --- Save the username ---
        // 1. Update the SharedPreferences so it persists
        prefs.edit().putString("username", newUsername).apply()
        // 2. Update the ViewModel to notify the MainActivity header
        profileViewModel.updateUsername(newUsername)

        // --- Save the new profile picture, if one was selected ---
        newImageUri?.let { uri ->
            val permanentUri = takeUriPermission(uri)
            // 1. Update SharedPreferences
            prefs.edit().putString("profile_image_uri", permanentUri.toString()).apply()
            // 2. Update ViewModel
            profileViewModel.updateProfileImageUri(permanentUri)
        }

        Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
        // Go back to the settings screen after saving
        parentFragmentManager.popBackStack()
    }

    private fun takeUriPermission(uri: Uri): Uri {
        val contentResolver = requireActivity().contentResolver
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)
        return uri
    }
}
