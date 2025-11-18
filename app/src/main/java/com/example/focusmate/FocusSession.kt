package com.example.focusmate

// A simple data class for storing in Firebase.
// It needs an empty constructor for Firebase to be able to deserialize it.
data class FocusSession(
    val durationMinutes: Long = 0,
    val endTimestamp: Long = 0
)
