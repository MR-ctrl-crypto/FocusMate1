package com.example.focusmate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class SoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val NOTIFICATION_CHANNEL_ID = "FocusMateSoundChannel"
    private val NOTIFICATION_ID = 1

    companion object {
        const val ACTION_PLAY = "com.example.focusmate.ACTION_PLAY"
        const val ACTION_STOP = "com.example.focusmate.ACTION_STOP"
        const val EXTRA_SOUND_RES_ID = "EXTRA_SOUND_RES_ID"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val soundResId = intent.getIntExtra(EXTRA_SOUND_RES_ID, -1)
                if (soundResId != -1) {
                    playSound(soundResId)
                }
            }
            ACTION_STOP -> {
                stopSound()
            }
        }
        return START_STICKY // If the service is killed, it will try to restart.
    }

    private fun playSound(soundResId: Int) {
        // Stop any previous sound
        mediaPlayer?.stop()
        mediaPlayer?.release()

        try {
            mediaPlayer = MediaPlayer.create(this, soundResId).apply {
                isLooping = true
                start()
            }
            // Promote to a foreground service
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            Log.e("SoundService", "Error playing sound", e)
            stopSelf() // Stop service if sound fails to play
        }
    }

    private fun stopSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopForeground(true) // Remove notification
        stopSelf() // Stop the service itself
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("FocusMate")
            .setContentText("Ambient sound is playing.")
            .setSmallIcon(R.drawable.ic_music_note) // Use a simple icon you have
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "FocusMate Sound Service",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Channel for the background sound service."
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure media player is released when service is destroyed
        mediaPlayer?.release()
    }

    // We don't need to bind to this service, so we return null.
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
