package com.example.focusmate

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_PROFILE_IMAGE_URI = "user_profile_uri"
        private const val DEFAULT_NAME = "Student01" // Your desired default name
    }

    // --- UPDATED to accept a String URI ---
    // This function is now more aligned with what's saved in ManageAccountActivity
    fun saveUserData(name: String, profileImageUriString: String?) {
        val editor = prefs.edit()
        editor.putString(KEY_USER_NAME, name)
        editor.putString(KEY_PROFILE_IMAGE_URI, profileImageUriString)
        editor.apply()
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, DEFAULT_NAME) ?: DEFAULT_NAME
    }

    // --- DEPRECATED/REPLACED ---
    // We will use the String version directly to be more explicit
    /*
    fun getProfileImageUri(): Uri? {
        val uriString = prefs.getString(KEY_PROFILE_IMAGE_URI, null)
        return if (uriString != null) Uri.parse(uriString) else null
    }
    */

    // --- NEW function to get the URI as a String ---
    fun getProfileImageUriString(): String? {
        return prefs.getString(KEY_PROFILE_IMAGE_URI, null)
    }
}
