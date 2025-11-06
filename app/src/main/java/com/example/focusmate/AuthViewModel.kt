package com.example.focusmate

import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest

class AuthViewModel : ViewModel() {

    // Get a reference to the Firebase Authentication service
    private val auth: FirebaseAuth = Firebase.auth

    // --- Sign Up Function ---
    fun signUp(email: String, pass: String, fullName: String, onComplete: (Boolean, String?) -> Unit) {
        // Basic validation
        if (email.isEmpty() || pass.isEmpty() || fullName.isEmpty()) {
            onComplete(false, "All fields are required.")
            return
        }

        // Use Firebase to create a new user with email and password
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign up success
                    val user = auth.currentUser

                    // Create a request to update the user's profile with their name
                    val profileUpdates = userProfileChangeRequest {
                        displayName = fullName
                    }

                    // Apply the profile update
                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                // Profile updated successfully
                                onComplete(true, null)
                            } else {
                                // Profile update failed, but signup succeeded.
                                // You can decide how to handle this. For now, we'll treat it as a success.
                                onComplete(true, "Couldn't set display name.")
                            }
                        }
                } else {
                    // If sign up fails, pass the error message back
                    val errorMessage = task.exception?.message ?: "An unknown error occurred."
                    onComplete(false, errorMessage)
                }
            }
    }

    // --- Login Function (You will need this for your LoginScreen) ---
    fun logIn(email: String, pass: String, onComplete: (Boolean, String?) -> Unit) {
        if (email.isEmpty() || pass.isEmpty()) {
            onComplete(false, "Email and password cannot be empty.")
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    onComplete(true, null)
                } else {
                    // If sign in fails, pass the error message back
                    val errorMessage = task.exception?.message ?: "Login failed. Please check your credentials."
                    onComplete(false, errorMessage)
                }
            }
    }
}
