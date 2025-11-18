package com.example.focusmate

import android.app.AlertDialog
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
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText

class LoginFragment : Fragment() {

    // Re-use the same AuthViewModel for handling Firebase logic
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate (i.e., create) the view from your XML layout file
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the views from the layout using their IDs
        val emailEditText = view.findViewById<TextInputEditText>(R.id.edit_text_email)
        val passwordEditText = view.findViewById<TextInputEditText>(R.id.edit_text_password)
        val loginButton = view.findViewById<Button>(R.id.button_login)
        val signUpPromptTextView = view.findViewById<TextView>(R.id.text_signup_prompt)
        val forgotPasswordTextView = view.findViewById<TextView>(R.id.text_forgot_password)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)

        // --- OBSERVERS for ViewModel LiveData ---

        // Observer for the password reset result
        authViewModel.passwordResetStatus.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(context, "Password reset link sent successfully.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to send reset link. Check the email address.", Toast.LENGTH_LONG).show()
            }
        }

        // --- CLICK LISTENERS for UI elements ---

        // Set the click listener for the main login button
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            // Simple validation to ensure fields are not empty
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please enter email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show a loading indicator and disable the button to prevent multiple clicks
            progressBar.visibility = View.VISIBLE
            loginButton.isEnabled = false

            // Call the logIn function in your ViewModel
            authViewModel.logIn(email, password) { isSuccess, errorMessage ->
                // This code runs after Firebase responds

                // Hide the loading indicator and re-enable the button
                progressBar.visibility = View.GONE
                loginButton.isEnabled = true

                if (isSuccess) {
                    // If login is successful, launch the MainActivity.
                    Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()

                    // Create an Intent to start MainActivity
                    val intent = Intent(activity, MainActivity::class.java)
                    // Add flags to clear the back stack and start fresh
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                    // Finish the current AuthActivity so the user cannot press 'back' to return to it.
                    activity?.finish()

                } else {
                    // If login fails, show the error message from Firebase
                    Toast.makeText(context, "Login failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Set the click listener for the "Forgot Password?" text
        forgotPasswordTextView.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            // Check if the user has entered an email
            if (email.isEmpty()) {
                Toast.makeText(context, "Please enter your email address first.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Show a dialog to confirm the action
            AlertDialog.Builder(requireContext())
                .setTitle("Reset Password")
                .setMessage("A password reset link will be sent to '$email'. Proceed?")
                .setPositiveButton("Send") { _, _ ->
                    // Call the ViewModel to handle the password reset
                    authViewModel.sendPasswordResetEmail(email)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }


        // Set the click listener for the "Don't have an account?" text
        signUpPromptTextView.setOnClickListener {
            // Navigate the user to the SignUpFragment using the action defined in nav_graph.xml
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }
    }
}
