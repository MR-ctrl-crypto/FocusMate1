package com.example.focusmate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpFragment : Fragment() {

    // Use the shared AuthViewModel to handle Firebase logic
    private val authViewModel: AuthViewModel by viewModels()

    // --- Views ---
    private lateinit var firstNameEditText: TextInputEditText
    private lateinit var surnameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var signUpButton: Button
    private lateinit var loginPromptTextView: TextView
    private lateinit var progressBar: ProgressBar


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- View Initialization ---
        // Using TextInputEditText to match the Material Design layouts
        firstNameEditText = view.findViewById(R.id.edit_text_first_name)
        surnameEditText = view.findViewById(R.id.edit_text_surname)
        emailEditText = view.findViewById(R.id.edit_text_email)
        passwordEditText = view.findViewById(R.id.edit_text_password)
        confirmPasswordEditText = view.findViewById(R.id.edit_text_confirm_password)
        signUpButton = view.findViewById(R.id.button_sign_up)
        loginPromptTextView = view.findViewById(R.id.text_login_prompt)
        progressBar = view.findViewById(R.id.progress_bar)

        // --- Click Listeners ---
        signUpButton.setOnClickListener {
            handleSignUp()
        }

        loginPromptTextView.setOnClickListener {
            // Navigate back to the Login screen
            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
        }
    }

    /**
     * Handles the entire sign-up process, including validation and Firebase calls.
     */
    private fun handleSignUp() {
        val firstName = firstNameEditText.text.toString().trim()
        val surname = surnameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        // --- Input Validation ---
        if (firstName.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Please fill out all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(context, "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- Start Sign-Up Process ---
        setLoadingState(true)
        val fullName = "$firstName $surname"

        authViewModel.signUp(email, password) { isSuccess, errorMessage ->
            if (isSuccess) {
                // After successful auth, save profile to Realtime Database
                saveUserProfile(fullName, firstName)
            } else {
                // If sign-up fails, show error and reset loading state
                Toast.makeText(context, "Sign-up failed: $errorMessage", Toast.LENGTH_LONG).show()
                setLoadingState(false)
            }
        }
    }

    /**
     * Saves the new user's profile information to the Firebase Realtime Database.
     */
    private fun saveUserProfile(fullName: String, firstName: String) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            Toast.makeText(context, "Could not get user. Please try again.", Toast.LENGTH_SHORT).show()
            setLoadingState(false)
            return
        }

        val database = FirebaseDatabase.getInstance().getReference("Users")
        val userProfile = mapOf(
            "name" to fullName,
            "profileImageUrl" to "" // Initially no profile image
        )

        // Use the user's unique UID as the key
        database.child(firebaseUser.uid).setValue(userProfile)
            .addOnSuccessListener {
                // --- On Success ---
                Toast.makeText(context, "Welcome, $firstName!", Toast.LENGTH_LONG).show()

                // Navigate to the main part of the app
                val intent = Intent(activity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity?.finish()
            }
            .addOnFailureListener {
                // --- On Failure ---
                Toast.makeText(context, "Failed to save profile. Please try again.", Toast.LENGTH_LONG).show()
                setLoadingState(false)
            }
    }

    /**
     * Manages the visibility of the progress bar and the enabled state of the button.
     */
    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        signUpButton.isEnabled = !isLoading
    }
}
