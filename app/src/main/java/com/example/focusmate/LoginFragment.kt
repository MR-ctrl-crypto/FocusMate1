package com.example.focusmate

import android.content.Intent
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
        val emailEditText = view.findViewById<EditText>(R.id.edit_text_email)
        val passwordEditText = view.findViewById<EditText>(R.id.edit_text_password)
        val loginButton = view.findViewById<Button>(R.id.button_login)
        val signUpPromptTextView = view.findViewById<TextView>(R.id.text_signup_prompt)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)

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
                    startActivity(intent)

                    // Finish the current AuthActivity so the user cannot press 'back' to return to it.
                    activity?.finish()

                } else {
                    // If login fails, show the error message from Firebase
                    Toast.makeText(context, "Login failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Set the click listener for the "Don't have an account?" text
        signUpPromptTextView.setOnClickListener {
            // Navigate the user to the SignUpFragment using the action defined in nav_graph.xml
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }
    }
}
