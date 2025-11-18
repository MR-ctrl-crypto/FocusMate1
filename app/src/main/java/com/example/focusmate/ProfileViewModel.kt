package com.example.focusmate

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileViewModel : ViewModel() {

    // Your specific Firebase Database URL
    private val databaseUrl = "https://focusmate-51ac3-default-rtdb.europe-west1.firebasedatabase.app"

    // LiveData for the user's name
    private val _username = MutableLiveData<String?>()
    val username: LiveData<String?> = _username

    // LiveData for the user's profile picture URI
    private val _profileImageUri = MutableLiveData<Uri?>()
    val profileImageUri: LiveData<Uri?> = _profileImageUri

    private var valueEventListener: ValueEventListener? = null
    private val auth = FirebaseAuth.getInstance()

    /**
     * Public function to fetch the user's profile data.
     * This is called from MainActivity to initiate data loading.
     * It's safe to call multiple times; it will detach any old listener before creating a new one.
     */
    fun fetchUserProfile() {
        val userId = auth.currentUser?.uid ?: return // Exit if no user is logged in

        // Clean up any previous listener before attaching a new one
        cleanupListener()

        val dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users/$userId")

        valueEventListener = dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get username from the 'username' field
                val nameFromDb = snapshot.child("username").getValue(String::class.java)
                _username.value = nameFromDb

                // Get profile image URL from the 'profileImageUrl' field
                val imageUrlFromDb = snapshot.child("profileImageUrl").getValue(String::class.java)
                _profileImageUri.value = imageUrlFromDb?.let { Uri.parse(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                // On error or if permissions are revoked, set values to null
                _username.value = null
                _profileImageUri.value = null
            }
        })
    }

    // Called from ManageAccountFragment to provide an instant UI update
    fun updateUsername(newName: String?) {
        _username.value = newName
    }

    fun updateProfileImageUri(newUri: Uri?) {
        _profileImageUri.value = newUri
    }

    private fun cleanupListener() {
        // Detach the listener if it exists to prevent memory leaks
        valueEventListener?.let {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("users/$userId")
                    .removeEventListener(it)
            }
        }
        valueEventListener = null
    }

    // Clean up the listener when the ViewModel is finally destroyed
    override fun onCleared() {
        super.onCleared()
        cleanupListener()
    }
}
