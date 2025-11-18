package com.example.focusmate

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import androidx.recyclerview.widget.RecyclerView // Add this import

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    private lateinit var headerUsername: TextView
    private lateinit var headerProfileImage: CircleImageView
    private lateinit var bottomNav: BottomNavigationView

    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // --- THIS MUST BE THE VERY FIRST THING THAT HAPPENS ---
        // Apply the saved theme BEFORE super.onCreate() is called.
        // This ensures the Activity is created with the correct theme from the start.
        val sharedPreferences = getSharedPreferences("com.example.focusmate_preferences", MODE_PRIVATE)
        val themeValue = sharedPreferences.getString("theme_preference", ThemeManager.THEME_SYSTEM) ?: ThemeManager.THEME_SYSTEM
        ThemeManager.applyTheme(themeValue)
        // -----------------------------------------------------

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        headerUsername = findViewById(R.id.text_header_username)
        headerProfileImage = findViewById(R.id.image_header_profile)
        bottomNav = findViewById(R.id.bottom_navigation)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // THIS IS THE LINE THAT MAKES THE BUTTONS WORK.
        // It automatically matches the menu item ID to the fragment ID in the graph.
        bottomNav.setupWithNavController(navController)

        observeProfileData()

        // This listener handles hiding and showing the bar.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // If the destination is part of the main app graph, show the UI.
            if (destination.parent?.id == R.id.main_graph) {
                headerUsername.visibility = View.VISIBLE
                headerProfileImage.visibility = View.VISIBLE
                bottomNav.visibility = View.VISIBLE
            } else { // Otherwise (it's in the auth graph), hide the UI.
                headerUsername.visibility = View.GONE
                headerProfileImage.visibility = View.GONE
                bottomNav.visibility = View.GONE
            }
        }
    }

    private fun observeProfileData() {
        profileViewModel.username.observe(this) { name ->
            headerUsername.text = name ?: "User"
        }
        profileViewModel.profileImageUri.observe(this) { uri ->
            if (uri != null) {
                Picasso.get().load(uri)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(headerProfileImage)
            } else {
                headerProfileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // This logic correctly directs the user based on login state.
        if (auth.currentUser == null) {
            if (navController.currentDestination?.parent?.id != R.id.auth_graph) {
                navController.navigate(R.id.auth_graph)
            }
        } else {
            profileViewModel.fetchUserProfile()
            if (navController.currentDestination?.parent?.id == R.id.auth_graph) {
                navController.navigate(R.id.action_global_main_graph)
            }
        }
    }
}
