package com.example.focusmate

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity() {

    private lateinit var profileNameText: TextView
    private lateinit var profileImageView: CircleImageView

    // Initialize the shared ViewModel using the activity-ktx delegate
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        profileNameText = findViewById(R.id.text_profile_name)
        profileImageView = findViewById(R.id.image_profile_picture)

        // --- OBSERVE LIVE DATA ---
        // This block will execute whenever the username in the ViewModel changes
        profileViewModel.username.observe(this) { newName ->
            profileNameText.text = newName
        }

        // This block will execute whenever the profile image URI in the ViewModel changes
        profileViewModel.profileImageUri.observe(this) { newUri ->
            if (newUri != null) {
                profileImageView.setImageURI(newUri)
            } else {
                // If the URI is null (e.g., no picture set), show the placeholder
                profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
        // --- END OF OBSERVE ---

        val bottomNavView: BottomNavigationView = findViewById(R.id.bottom_navigation_view)

        // The listener for item clicks remains unchanged
        bottomNavView.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.nav_home -> selectedFragment = HomeFragment()
                R.id.nav_analytics -> selectedFragment = AnalyticsFragment()
                R.id.nav_timetable -> selectedFragment = TimetableFragment()
                R.id.nav_timer -> selectedFragment = TimerFragment()
                R.id.nav_settings -> selectedFragment = SettingsFragment()
            }

            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment)
                    .commit()
            }
            true
        }

        // Set the default screen to be displayed when the app starts
        if (savedInstanceState == null) {
            bottomNavView.selectedItemId = R.id.nav_home
        }
    }
}
