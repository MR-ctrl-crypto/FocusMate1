package com.example.focusmate

import android.content.Intent
import android.net.Uri // 1. IMPORT URI and Log
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        userPreferences = UserPreferences(this)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        // This tells the menu to redraw itself, which will call onCreateOptionsMenu again
        // and refresh the user's name and profile picture.
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)

        val profileMenuItem = menu.findItem(R.id.action_profile)
        val actionView = profileMenuItem.actionView

        val toolbarUserName: TextView? = actionView?.findViewById(R.id.toolbar_user_name)
        val toolbarProfileImage: ImageView? = actionView?.findViewById(R.id.toolbar_profile_image)

        actionView?.setOnClickListener {
            onOptionsItemSelected(profileMenuItem)
        }

        // --- UPDATED UI LOGIC ---
        toolbarUserName?.text = userPreferences.getUserName()
        val imageUriString = userPreferences.getProfileImageUriString()

        if (!imageUriString.isNullOrEmpty()) {
            val imageUri = Uri.parse(imageUriString)
            try {
                // Because we took persistent permission, this should now work without crashing.
                toolbarProfileImage?.setImageURI(imageUri)
            } catch (e: SecurityException) {
                // This is a safety net. If permission is lost, we won't crash.
                Log.e("SettingsActivity", "Permission denial for profile URI: $imageUri", e)
                // Fall back to a default image
                toolbarProfileImage?.setImageResource(R.mipmap.ic_launcher_round)
            }
        } else {
            // Set a default image if no URI is saved
            toolbarProfileImage?.setImageResource(R.mipmap.ic_launcher_round)
        }
        // --- END OF UPDATED LOGIC ---

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                val intent = Intent(this, ManageAccountActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
