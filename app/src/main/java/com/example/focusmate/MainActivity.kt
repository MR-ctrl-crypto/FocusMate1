package com.example.focusmate

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity() {

    // Use lateinit for views that are guaranteed to be in the layout
    private lateinit var bottomNavView: BottomNavigationView
    private lateinit var profileNameTextView: TextView
    private lateinit var profileImageView: CircleImageView

    // Firebase properties
    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser

        // Initialize Views
        bottomNavView = findViewById(R.id.bottom_navigation_view)
        profileNameTextView = findViewById(R.id.text_profile_name)
        profileImageView = findViewById(R.id.image_profile_picture)

        // --- FIX 1: Set up the user profile info ---
        // If currentUser is null, this will prevent a crash by providing default text.
        profileNameTextView.text = currentUser?.displayName ?: "Guest"
        // You can add a placeholder for the image here if you use a library like Glide/Picasso
        // profileImageView.setImageResource(R.drawable.ic_profile_placeholder)

        // Set up the listener for bottom navigation
        bottomNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_analytics -> {
                    replaceFragment(AnalyticsFragment())
                    true
                }
                R.id.nav_timer -> {
                    replaceFragment(TimerFragment())
                    true
                }
                R.id.nav_timetable -> {
                    replaceFragment(TimetableFragment())
                    true
                }
                R.id.nav_settings -> {
                    replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }

        // --- FIX 2: Set the initial fragment ---
        // This ensures the app starts on the home screen when MainActivity loads.
        // It's safer to check if savedInstanceState is null to avoid re-adding the fragment on rotation.
        if (savedInstanceState == null) {
            bottomNavView.selectedItemId = R.id.nav_home
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        // --- FIX 3: Use the CORRECT FrameLayout ID from your activity_main.xml ---
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.main_fragment_container, fragment) // Changed from nav_host_fragment
        transaction.commit()
    }
}
