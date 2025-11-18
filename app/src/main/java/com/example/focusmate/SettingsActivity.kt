package com.example.focusmate

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.squareup.picasso.Picasso

class SettingsActivity : AppCompatActivity() {

    // Use the modern ProfileViewModel to get user data from Firebase
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // --- Setup Toolbar and Navigation ---
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Find the NavHostFragment and its NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.settings_container) as NavHostFragment
        val navController = navHostFragment.navController

        // Link the ActionBar to the NavController for title updates and the back arrow
        setupActionBarWithNavController(navController)

        // --- Observe LiveData from the ViewModel ---
        observeProfileData(toolbar)
    }

    override fun onResume() {
        super.onResume()
        // Refresh the user profile data every time the screen becomes visible
        profileViewModel.fetchUserProfile()
    }

    private fun observeProfileData(toolbar: Toolbar) {
        val toolbarUserName: TextView? = toolbar.findViewById(R.id.toolbar_user_name)
        val toolbarProfileImage: ImageView? = toolbar.findViewById(R.id.toolbar_profile_image)

        // Observe the username LiveData
        profileViewModel.username.observe(this) { name ->
            toolbarUserName?.text = name ?: "User"
            Log.d("SettingsActivity", "Username updated in toolbar: $name")
        }

        // Observe the profile image URI LiveData
        profileViewModel.profileImageUri.observe(this) { uri ->
            if (uri != null) {
                Picasso.get()
                    .load(uri)
                    .placeholder(R.drawable.ic_profile_placeholder) // Use a placeholder
                    .error(R.drawable.ic_profile_placeholder)       // Use an error image
                    .into(toolbarProfileImage)
            } else {
                toolbarProfileImage?.setImageResource(R.drawable.ic_profile_placeholder)
            }
            Log.d("SettingsActivity", "Profile image updated in toolbar.")
        }
    }

    // This is crucial for making the toolbar's back arrow work
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.settings_container) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
