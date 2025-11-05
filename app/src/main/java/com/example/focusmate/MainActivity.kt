package com.example.focusmate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavView: BottomNavigationView = findViewById(R.id.bottom_navigation_view)

        // Set the listener for item clicks
        bottomNavView.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.nav_home -> selectedFragment = HomeFragment()

                R.id.nav_analytics -> selectedFragment = Analytics()
                R.id.nav_timetable -> selectedFragment = TimetableFragment() // For Later

                // --- THIS IS THE LINE YOU NEED TO ADD/UNCOMMENT ---
                R.id.nav_timer -> selectedFragment = TimerFragment()
                // ----------------------------------------------------

                R.id.nav_settings -> selectedFragment = SettingsFragment()
            }

            // Replace the content of the fragment container with the new fragment
            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment)
                    .commit()
            }
            true
        }

        // Set the default screen to be displayed when the app starts.
        // This remains unchanged, Home is still the default.
        if (savedInstanceState == null) {
            bottomNavView.selectedItemId = R.id.nav_home
        }
    }
}
