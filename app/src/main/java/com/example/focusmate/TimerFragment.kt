package com.example.focusmate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TimerFragment : Fragment() {

    // Timer state variables
    private var timeLeftInMillis: Long = 0
    private var isTimerRunning: Boolean = false
    private var startTimeInMillis: Long = 25 * 60 * 1000 // Default 25 minutes

    private lateinit var timerPrefs: SharedPreferences

    // UI Elements
    private lateinit var timerText: TextView
    private lateinit var timerLabel: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var editTimerButton: ImageButton
    private lateinit var silenceNotificationsSwitch: SwitchMaterial
    private lateinit var blockSocialMediaSwitch: SwitchMaterial
    private lateinit var badgePrefs: SharedPreferences
    private lateinit var badgesContainer: LinearLayout

    data class Badge(val name: String, val minutesRequired: Long, val iconResId: Int)
    private val allBadges = listOf(
        Badge("Focus Newbie", 25, R.drawable.ic_badge_placeholder),
        Badge("Focus Pro", 120, R.drawable.ic_badge_placeholder),
        Badge("Time Keeper", 300, R.drawable.ic_badge_placeholder),
        Badge("Focus Master", 600, R.drawable.ic_badge_placeholder),
        Badge("Streak Pro", -1, R.drawable.ic_badge_placeholder)
    )

    private val timerUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TimerService.BROADCAST_TIMER_TICK -> {
                    timeLeftInMillis = intent.getLongExtra(TimerService.EXTRA_TIME_LEFT, 0)
                    updateTimerText()
                    isTimerRunning = true
                    updateButtons()
                }
                TimerService.BROADCAST_TIMER_FINISH -> {
                    isTimerRunning = false
                    timerLabel.text = "Session Complete!"
                    timeLeftInMillis = startTimeInMillis
                    saveProgress(startTimeInMillis) // Save the full session duration on finish
                    updateButtons()
                    updateTimerText()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI elements
        timerText = view.findViewById(R.id.timer_text)
        timerLabel = view.findViewById(R.id.timer_label)
        startButton = view.findViewById(R.id.button_start)
        stopButton = view.findViewById(R.id.button_stop)
        editTimerButton = view.findViewById(R.id.button_edit_timer)
        silenceNotificationsSwitch = view.findViewById(R.id.switch_silence_notifications)
        blockSocialMediaSwitch = view.findViewById(R.id.switch_block_social_media)
        badgesContainer = view.findViewById(R.id.badges_container)

        // Initialize SharedPreferences
        badgePrefs = requireActivity().getSharedPreferences("FocusMatePrefs", Context.MODE_PRIVATE)
        timerPrefs = requireActivity().getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)

        // Set up click listeners
        startButton.setOnClickListener { startTimer() }
        stopButton.setOnClickListener { stopTimer() }

        editTimerButton.setOnClickListener {
            if (isTimerRunning) {
                Toast.makeText(context, "Cannot edit time while timer is running.", Toast.LENGTH_SHORT).show()
            } else {
                showEditTimerDialog()
            }
        }

        restoreTimerState()
        updateBadgesUI()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(TimerService.BROADCAST_TIMER_TICK)
            addAction(TimerService.BROADCAST_TIMER_FINISH)
        }
        ContextCompat.registerReceiver(requireContext(), timerUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        restoreTimerState()
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(timerUpdateReceiver)
    }

    private fun restoreTimerState() {
        isTimerRunning = timerPrefs.getBoolean("isRunning", false)
        if (isTimerRunning) {
            val endTime = timerPrefs.getLong("endTime", 0)
            timeLeftInMillis = endTime - System.currentTimeMillis()
            if (timeLeftInMillis <= 0) {
                timeLeftInMillis = 0
                isTimerRunning = false
            }
        } else {
            timeLeftInMillis = startTimeInMillis
        }
        updateButtons()
        updateTimerText()
    }

    private fun showEditTimerDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_timer, null)
        val hoursPicker = dialogView.findViewById<NumberPicker>(R.id.picker_hours)
        val minutesPicker = dialogView.findViewById<NumberPicker>(R.id.picker_minutes)
        val secondsPicker = dialogView.findViewById<NumberPicker>(R.id.picker_seconds)

        hoursPicker.minValue = 0
        hoursPicker.maxValue = 23
        minutesPicker.minValue = 0
        minutesPicker.maxValue = 59
        secondsPicker.minValue = 0
        secondsPicker.maxValue = 59

        val currentHours = (startTimeInMillis / (1000 * 60 * 60)) % 24
        val currentMinutes = (startTimeInMillis / (1000 * 60)) % 60
        val currentSeconds = (startTimeInMillis / 1000) % 60
        hoursPicker.value = currentHours.toInt()
        minutesPicker.value = currentMinutes.toInt()
        secondsPicker.value = currentSeconds.toInt()

        AlertDialog.Builder(requireContext())
            .setTitle("Set Focus Time")
            .setView(dialogView)
            .setPositiveButton("Set") { dialog, _ ->
                val hours = hoursPicker.value
                val minutes = minutesPicker.value
                val seconds = secondsPicker.value
                val totalMillis = (hours * 3600 + minutes * 60 + seconds) * 1000L

                if (totalMillis > 0) {
                    startTimeInMillis = totalMillis
                    stopTimer()
                } else {
                    Toast.makeText(context, "Please set a time greater than 0 seconds.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun startTimer() {
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TIME_IN_MILLIS, timeLeftInMillis)
        }
        requireContext().startService(intent)
        isTimerRunning = true
        updateButtons()
    }

    private fun stopTimer() {
        val timeFocused = startTimeInMillis - timeLeftInMillis
        if (timeFocused > 1000 && isTimerRunning) { // Save progress if focused for more than 1 sec
            saveProgress(timeFocused)
        }

        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        requireContext().startService(intent)

        isTimerRunning = false
        timeLeftInMillis = startTimeInMillis
        updateTimerText()
        updateButtons()
        timerLabel.text = "Focus Session"
    }

    private fun saveProgress(focusedMillis: Long) {
        val minutes = focusedMillis / 1000 / 60
        if (minutes <= 0) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "You must be logged in to save progress.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- THIS IS THE CRITICAL FIX ---
        // Specify the correct database URL when getting the instance.
        val databaseUrl = "https://focusmate-51ac3-default-rtdb.europe-west1.firebasedatabase.app"
        val database = FirebaseDatabase.getInstance(databaseUrl).getReference("sessions").child(userId)
        // --------------------------------

        val session = FocusSession(
            durationMinutes = minutes,
            endTimestamp = System.currentTimeMillis()
        )

        database.push().setValue(session)
            .addOnSuccessListener {
                Toast.makeText(context, "Saved $minutes minute(s) of focus!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Show a detailed error message to help with debugging
                Toast.makeText(context, "Failed to save session: ${e.message}", Toast.LENGTH_LONG).show()
            }

        // Update local badges
        val totalMinutesFocused = badgePrefs.getLong("totalMinutesFocused", 0)
        val newTotal = totalMinutesFocused + minutes
        badgePrefs.edit().putLong("totalMinutesFocused", newTotal).apply()
        updateBadgesUI() // Refresh badges after saving
    }

    private fun updateBadgesUI() {
        badgesContainer.removeAllViews()
        val totalMinutesFocused = badgePrefs.getLong("totalMinutesFocused", 0)

        for (badge in allBadges) {
            val badgeView = LayoutInflater.from(context).inflate(R.layout.badge_item, badgesContainer, false)
            val badgeIcon = badgeView.findViewById<ImageView>(R.id.badge_icon)
            val badgeLabel = badgeView.findViewById<TextView>(R.id.badge_label)

            badgeLabel.text = badge.name
            badgeIcon.setImageResource(badge.iconResId)

            if (badge.minutesRequired != -1L && totalMinutesFocused >= badge.minutesRequired) {
                badgeIcon.colorFilter = null
                badgeLabel.alpha = 1.0f
            } else {
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                badgeIcon.colorFilter = ColorMatrixColorFilter(matrix)
                badgeLabel.alpha = 0.6f
            }
            badgesContainer.addView(badgeView)
        }
    }

    private fun updateTimerText() {
        val hours = (timeLeftInMillis / (1000 * 60 * 60)) % 24
        val minutes = (timeLeftInMillis / (1000 * 60)) % 60
        val seconds = (timeLeftInMillis / 1000) % 60

        val timeFormatted = if (startTimeInMillis >= 3600000) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
        timerText.text = timeFormatted
    }

    private fun updateButtons() {
        startButton.visibility = if (isTimerRunning) View.INVISIBLE else View.VISIBLE
        stopButton.visibility = if (isTimerRunning) View.VISIBLE else View.INVISIBLE
    }
}
