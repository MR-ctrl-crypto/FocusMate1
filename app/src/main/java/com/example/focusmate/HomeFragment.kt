package com.example.focusmate

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var totalFocusTimeText: TextView
    private lateinit var focusStreakText: TextView
    private lateinit var motivationalQuoteText: TextView
    private lateinit var startSessionButton: Button
    private lateinit var viewTimetableCard: MaterialCardView
    private lateinit var viewBlockedAppsCard: MaterialCardView // <-- ADDED THIS

    private val databaseUrl = "https://focusmate-51ac3-default-rtdb.europe-west1.firebasedatabase.app"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize all the views from the layout
        totalFocusTimeText = view.findViewById(R.id.text_total_focus_time)
        focusStreakText = view.findViewById(R.id.text_focus_streak)
        motivationalQuoteText = view.findViewById(R.id.text_motivational_quote)
        startSessionButton = view.findViewById(R.id.button_start_new_session)
        viewTimetableCard = view.findViewById(R.id.card_view_timetable)
        viewBlockedAppsCard = view.findViewById(R.id.card_view_blocked_apps) // <-- ADDED THIS

        loadMotivationalQuote()
        setupClickListeners() // Setup navigation listeners
    }

    override fun onResume() {
        super.onResume()
        loadFocusDataFromFirebase()
    }

    private fun loadFocusDataFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            totalFocusTimeText.text = "0h 0m"
            focusStreakText.text = "0 Days"
            return
        }

        val databaseRef = FirebaseDatabase.getInstance(databaseUrl).getReference("sessions").child(userId)

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessions = snapshot.children.mapNotNull { it.getValue(FocusSession::class.java) }
                val totalMinutes = sessions.sumOf { it.durationMinutes }
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                totalFocusTimeText.text = "${hours}h ${minutes}m"
                val streakDays = calculateFocusStreak(sessions)
                focusStreakText.text = "$streakDays Days"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load home screen data: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun calculateFocusStreak(sessions: List<FocusSession>): Int {
        if (sessions.isEmpty()) {
            return 0
        }
        val uniqueDays = sessions
            .map { session ->
                val cal = Calendar.getInstance().apply { timeInMillis = session.endTimestamp }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            .toSet()
            .sortedDescending()

        var streak = 0
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        if (uniqueDays.first() >= today.timeInMillis || uniqueDays.first() >= today.timeInMillis - TimeUnit.DAYS.toMillis(1)) {
            streak = 1
            var lastDay = uniqueDays.first()
            for (i in 1 until uniqueDays.size) {
                val currentDay = uniqueDays[i]
                if (lastDay - currentDay == TimeUnit.DAYS.toMillis(1)) {
                    streak++
                    lastDay = currentDay
                } else {
                    break
                }
            }
        }
        return streak
    }

    private fun loadMotivationalQuote() {
        val quotes = listOf(
            "\"The secret of getting ahead is getting started.\"",
            "\"The best way to predict the future is to create it.\"",
            "\"Don't watch the clock; do what it does. Keep going.\"",
            "\"Focus on being productive instead of busy.\"",
            "\"The key is not to prioritize what's on your schedule, but to schedule your priorities.\""
        )
        motivationalQuoteText.text = quotes.random()
    }

    private fun setupClickListeners() {
        startSessionButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_timerFragment)
        }

        viewTimetableCard.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_timetableFragment)
        }

        // --- NEW CLICK LISTENER FOR BLOCKED APPS ---
        viewBlockedAppsCard.setOnClickListener {
            // Check for notification permission first
            if (!isNotificationServiceEnabled()) {
                // Guide user to settings if permission is not granted
                showPermissionDialog()
            } else {
                // If permission is granted, navigate to the fragment
                findNavController().navigate(R.id.action_homeFragment_to_blockedAppsFragment)
            }
        }
    }

    // --- NEW HELPER FUNCTIONS FOR PERMISSION ---
    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(requireContext().packageName) == true
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage("To block notifications, FocusMate needs access to your notifications. Please enable it in the next screen.")
            .setPositiveButton("Go to Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
