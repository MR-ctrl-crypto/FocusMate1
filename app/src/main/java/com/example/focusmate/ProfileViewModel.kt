package com.example.focusmate

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val _username = MutableLiveData<String?>()
    val username: LiveData<String?> = _username

    private val _profileImageUri = MutableLiveData<Uri?>()
    val profileImageUri: LiveData<Uri?> = _profileImageUri

    private var databaseReference: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null

    init {
        // Fetch the profile as soon as the ViewModel is created
        fetchUserProfile()
    }

    fun fetchUserProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _username.postValue(null)
            _profileImageUri.postValue(null)
            return
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        // This listener will automatically trigger when data changes in the database
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                _username.postValue(name) // Update the username LiveData

                if (!imageUrl.isNullOrEmpty()) {
                    _profileImageUri.postValue(Uri.parse(imageUrl)) // Update image URI LiveData
                } else {
                    _profileImageUri.postValue(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Optionally handle errors, e.g., log them
                _username.postValue(null)
                _profileImageUri.postValue(null)
            }
        }
        // Attach the listener
        databaseReference?.addValueEventListener(valueEventListener!!)
    }

    // Clean up the listener when the ViewModel is destroyed to prevent memory leaks
    override fun onCleared() {
        super.onCleared()
        valueEventListener?.let {
            databaseReference?.removeEventListener(it)
        }
    }
}
