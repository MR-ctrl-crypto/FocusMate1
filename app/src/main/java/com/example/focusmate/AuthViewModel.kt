package com.example.focusmate

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // LiveData for password reset status
    private val _passwordResetStatus = MutableLiveData<Boolean>()
    val passwordResetStatus: LiveData<Boolean> = _passwordResetStatus

    /**
     * Signs a user up with email and password.
     * The callback will contain success status and an optional error message.
     */
    fun signUp(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Registration successful
                    callback(true, null)
                } else {
                    // Registration failed, provide the error message
                    callback(false, task.exception?.message)
                }
            }
    }

    /**
     * Logs a user in with email and password.
     * The callback will contain success status and an optional error message.
     */
    fun logIn(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login successful
                    callback(true, null)
                } else {
                    // Login failed, provide the error message
                    callback(false, task.exception?.message)
                }
            }
    }

    /**
     * Sends a password reset email to the given email address via Firebase.
     * Updates the passwordResetStatus LiveData with the result.
     */
    fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _passwordResetStatus.postValue(true)
                } else {
                    Log.w("AuthViewModel", "sendPasswordResetEmail:failure", task.exception)
                    _passwordResetStatus.postValue(false)
                }
            }
    }
}
