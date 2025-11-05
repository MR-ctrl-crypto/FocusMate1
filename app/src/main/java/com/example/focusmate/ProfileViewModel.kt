package com.example.focusmate

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProfileViewModel : ViewModel() {

    // LiveData for the user's name
    private val _username = MutableLiveData<String>().apply { value = "User" } // Default value
    val username: LiveData<String> = _username

    // LiveData for the user's profile picture URI
    private val _profileImageUri = MutableLiveData<Uri?>()
    val profileImageUri: LiveData<Uri?> = _profileImageUri

    // Function to be called from SettingsFragment to update the name
    fun updateUsername(newName: String) {
        _username.value = newName
    }

    // Function to be called from SettingsFragment to update the picture
    fun updateProfileImageUri(newUri: Uri?) {
        _profileImageUri.value = newUri
    }
}
