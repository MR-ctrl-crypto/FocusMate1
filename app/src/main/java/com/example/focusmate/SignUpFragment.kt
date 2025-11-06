package com.example.focusmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController

class SignUpFragment : Fragment() {

    // Use the same AuthViewModel to handle Firebase logic
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find all the views from the XML layout
        val firstNameEditText = view.findViewById<EditText>(R.id.edit_text_first_name)
        val surnameEditText = view.findViewById<EditText>(R.id.edit_text_surname)
        val emailEditText = view.findViewById<EditText>(R.id.edit_text_email)
        val passwordEditText = view.findViewById<EditText>(R.id.edit_text_password)
        val confirmPasswordEditText = view.findViewById<EditText>(R.id.edit_text_confirm_password)
        val signUpButton = view.findViewById<Button>(R.id.button_sign_up)
        val loginPromptTextView = view.findViewById<TextView>(R.id.text_login_prompt)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)

        // Set a click listener for the sign-up button
        signUpButton.setOnClickListener {
            val firstName = firstNameEditText.text.toString().trim()
            val surname = surnameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Basic validation
            if (password != confirmPassword) {
                Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading state
            progressBar.visibility = View.VISIBLE
            signUpButton.isEnabled = false

            // Call the ViewModel to perform the sign-up
            authViewModel.signUp(email, password, "$firstName $surname") { isSuccess, errorMessage ->
                // Hide loading state
                progressBar.visibility = View.GONE
                signUpButton.isEnabled = true

                if (isSuccess) {
                    // On success, navigate to the login screen
                    Toast.makeText(context, "Account created! Please log in.", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
                } else {
                    // On failure, show an error message
                    Toast.makeText(context, "Sign-up failed: ${errorMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Set a click listener to navigate to the Login screen
        loginPromptTextView.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
        }
    }
}
