package com.example.focusmate

import android.content.Context
import android.content.SharedPreferences
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial

class TimerFragment : Fragment() {

    // Timer state variables
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 0
    private var isTimerRunning: Boolean = false
    private var startTimeInMillis: Long = 25 * 60 * 1000 // Default 25 minutes

    // UI Elements
    private lateinit var timerText: TextView
    private lateinit var timerLabel: TextView
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var stopButton: Button
    private lateinit var editTimerButton: ImageButton
    private lateinit var silenceNotificationsSwitch: SwitchMaterial
    private lateinit var blockSocialMediaSwitch: SwitchMaterial

    // --- Badge System Components ---
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var badgesContainer: LinearLayout

    // Data class to define a badge's properties
    data class Badge(val name: String, val minutesRequired: Long, val iconResId: Int)

    // List of all available badges
    private val allBadges = listOf(
        Badge("Focus Newbie", 25, R.drawable.ic_badge_placeholder),
        Badge("Focus Pro", 120, R.drawable.ic_badge_placeholder), // 2 hours
        Badge("Time Keeper", 300, R.drawable.ic_badge_placeholder), // 5 hours
        Badge("Focus Master", 600, R.drawable.ic_badge_placeholder), // 10 hours
        Badge("Streak Pro", -1, R.drawable.ic_badge_placeholder) // Streak logic is special
    )
    // --- End of Badge System Components ---

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI elements
        timerText = view.findViewById(R.id.timer_text)
        timerLabel = view.findViewById(R.id.timer_label)
        startButton = view.findViewById(R.id.button_start)
        pauseButton = view.findViewById(R.id.button_pause)
        stopButton = view.findViewById(R.id.button_stop)
        editTimerButton = view.findViewById(R.id.button_edit_timer)
        silenceNotificationsSwitch = view.findViewById(R.id.switch_silence_notifications)
        blockSocialMediaSwitch = view.findViewById(R.id.switch_block_social_media)
        badgesContainer = view.findViewById(R.id.badges_container) // Badge container

        // Initialize SharedPreferences for progress tracking
        sharedPreferences = requireActivity().getSharedPreferences("FocusMatePrefs", Context.MODE_PRIVATE)

        // Set initial timer value
        timeLeftInMillis = startTimeInMillis

        // Set up click listeners
        startButton.setOnClickListener { startTimer() }
        pauseButton.setOnClickListener { pauseTimer() }
        stopButton.setOnClickListener { stopTimer() }

        editTimerButton.setOnClickListener {
            if (isTimerRunning) {
                Toast.makeText(context, "Cannot edit time while timer is running.", Toast.LENGTH_SHORT).show()
            } else {
                showEditTimerDialog()
            }
        }

        // Set the initial UI state
        updateButtons()
        updateTimerText()
        updateBadgesUI() // Load badge progress
    }

    private fun showEditTimerDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_timer, null)
        val editTextMinutes = dialogView.findViewById<EditText>(R.id.edit_text_minutes)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Set") { dialog, _ ->
                val minutesString = editTextMinutes.text.toString()
                if (minutesString.isNotEmpty()) {
                    val minutes = minutesString.toLong()
                    startTimeInMillis = minutes * 60 * 1000
                    stopTimer() // Use stopTimer to reset the state and UI
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }

    private fun startTimer() {
        if (isTimerRunning) return

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
            }

            override fun onFinish() {
                isTimerRunning = false
                timerLabel.text = "Session Complete!"
                Toast.makeText(context, "Focus session finished!", Toast.LENGTH_SHORT).show()

                // Save progress and update badges
                saveProgress(startTimeInMillis)
                updateBadgesUI()

                // Reset timer for next session
                timeLeftInMillis = startTimeInMillis
                updateButtons()
                updateTimerText()
            }
        }.start()

        isTimerRunning = true
        timerLabel.text = "Focus Session"
        updateButtons()
    }

    private fun pauseTimer() {
        if (!isTimerRunning) return

        countDownTimer?.cancel()
        isTimerRunning = false
        updateButtons()
    }

    private fun stopTimer() {
        // When stopping manually, save the elapsed time
        val timeFocused = startTimeInMillis - timeLeftInMillis
        if (timeFocused > 5000) { // Only save if more than 5 seconds have passed
            saveProgress(timeFocused)
            updateBadgesUI()
        }

        countDownTimer?.cancel()
        isTimerRunning = false
        timeLeftInMillis = startTimeInMillis // Reset the time to the configured start time
        updateTimerText()
        updateButtons()
        timerLabel.text = "Focus Session"
    }

    // --- Badge System Functions ---

    private fun saveProgress(focusedMillis: Long) {
        val totalMinutesFocused = sharedPreferences.getLong("totalMinutesFocused", 0)
        val newMinutes = focusedMillis / 1000 / 60
        if (newMinutes > 0) {
            val newTotalMinutes = totalMinutesFocused + newMinutes
            sharedPreferences.edit().putLong("totalMinutesFocused", newTotalMinutes).apply()
            Toast.makeText(context, "Saved $newMinutes minute(s) of focus!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBadgesUI() {
        badgesContainer.removeAllViews() // Clear old badges to redraw them
        val totalMinutesFocused = sharedPreferences.getLong("totalMinutesFocused", 0)

        for (badge in allBadges) {
            val badgeView = LayoutInflater.from(context).inflate(R.layout.badge_item, badgesContainer, false)
            val badgeIcon = badgeView.findViewById<ImageView>(R.id.badge_icon)
            val badgeLabel = badgeView.findViewById<TextView>(R.id.badge_label)

            badgeLabel.text = badge.name
            badgeIcon.setImageResource(badge.iconResId)

            // Check if the badge's requirement is met
            if (badge.minutesRequired != -1L && totalMinutesFocused >= badge.minutesRequired) {
                // UNLOCKED STATE: Full color
                badgeIcon.colorFilter = null
                badgeLabel.alpha = 1.0f
            } else {
                // LOCKED STATE: Grayscale
                val matrix = ColorMatrix()
                matrix.setSaturation(0f) // 0f is completely grayscale
                val filter = ColorMatrixColorFilter(matrix)
                badgeIcon.colorFilter = filter
                badgeLabel.alpha = 0.6f // Make text look faded
            }

            badgesContainer.addView(badgeView)
        }
    }

    // --- End of Badge System Functions ---

    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        timerText.text = timeFormatted
    }

    private fun updateButtons() {
        if (isTimerRunning) {
            startButton.visibility = View.INVISIBLE
            pauseButton.visibility = View.VISIBLE
            stopButton.visibility = View.VISIBLE
        } else {
            pauseButton.visibility = View.INVISIBLE
            startButton.visibility = View.VISIBLE

            if (timeLeftInMillis == startTimeInMillis) {
                stopButton.visibility = View.INVISIBLE
            } else {
                stopButton.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }
}
