package com.example.focusmate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    private var countDownTimer: CountDownTimer? = null

    companion object {
        // --- Actions for the Intent ---
        const val ACTION_START = "com.example.focusmate.ACTION_START"
        const val ACTION_STOP = "com.example.focusmate.ACTION_STOP"
        const val EXTRA_TIME_IN_MILLIS = "com.example.focusmate.EXTRA_TIME_IN_MILLIS"

        // --- Actions for the Broadcast ---
        const val BROADCAST_TIMER_TICK = "com.example.focusmate.BROADCAST_TIMER_TICK"
        const val BROADCAST_TIMER_FINISH = "com.example.focusmate.BROADCAST_TIMER_FINISH"
        const val EXTRA_TIME_LEFT = "com.example.focusmate.EXTRA_TIME_LEFT"

        // --- Notification ---
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "TimerChannel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val startTime = intent.getLongExtra(EXTRA_TIME_IN_MILLIS, 0)
                startTimer(startTime)
            }
            ACTION_STOP -> {
                stopTimer()
            }
        }
        return START_STICKY // If the service is killed, it will be automatically restarted.
    }

    private fun startTimer(startTimeInMillis: Long) {
        val prefs = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("isRunning", true)
            putLong("endTime", System.currentTimeMillis() + startTimeInMillis)
            apply()
        }

        countDownTimer = object : CountDownTimer(startTimeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Broadcast the tick update to the fragment
                val intent = Intent(BROADCAST_TIMER_TICK)
                intent.putExtra(EXTRA_TIME_LEFT, millisUntilFinished)
                sendBroadcast(intent)

                // Update the persistent notification
                updateNotification(millisUntilFinished)
            }

            override fun onFinish() {
                // Broadcast the finish event
                sendBroadcast(Intent(BROADCAST_TIMER_FINISH))

                // Play a sound
                playSound()

                // Stop the service and remove the notification
                stopTimer()
            }
        }.start()

        // Promote the service to a foreground service
        startForeground(NOTIFICATION_ID, createNotification(startTimeInMillis))
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        val prefs = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply() // Clear state
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun playSound() {
        try {
            val notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notificationSoundUri)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Notification Methods ---
    private fun createNotification(timeLeftInMillis: Long) = buildNotification(timeLeftInMillis)

    private fun updateNotification(timeLeftInMillis: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(timeLeftInMillis))
    }

    private fun buildNotification(timeLeftInMillis: Long) : android.app.Notification {
        createNotificationChannel()

        // Intent to open MainActivity when notification is tapped
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Session in Progress")
            .setContentText("Time remaining: $timeFormatted")
            .setSmallIcon(R.drawable.ic_timer) // You need to have this icon
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true) // Don't make a sound/vibration for every update
            .build()
    }

    private fun createNotificationChannel() {
        val name = "Timer Service Channel"
        val descriptionText = "Shows the current timer progress"
        val importance = NotificationManager.IMPORTANCE_LOW // Use LOW to avoid sound on each update
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
