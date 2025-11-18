package com.example.focusmate

import android.content.Context
import android.content.Intent

/**
 * Acts as a controller to send commands to the SoundService.
 * It does not manage the MediaPlayer or LiveData directly anymore.
 * This version uses a singleton pattern to keep track of the playing state app-wide.
 */
class SoundManager(private val context: Context) {

    companion object {
        // Singleton instance to hold the playing state across the entire app
        @Volatile
        private var INSTANCE: SoundManager? = null

        fun getInstance(context: Context): SoundManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SoundManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    var isPlaying: Boolean = false
        private set
    private var currentSoundResId: Int? = null


    fun playSound(soundResId: Int) {
        // If the user taps the same sound button while it's playing, stop it.
        if (isPlaying && currentSoundResId == soundResId) {
            stopSound()
            return
        }

        // This is the command to start the background service and play the sound.
        val intent = Intent(context, SoundService::class.java).apply {
            action = SoundService.ACTION_PLAY
            putExtra(SoundService.EXTRA_SOUND_RES_ID, soundResId)
        }
        context.startService(intent)

        // Update the state
        isPlaying = true
        currentSoundResId = soundResId
    }

    fun stopSound() {
        // This is the command to stop the background service.
        val intent = Intent(context, SoundService::class.java).apply {
            action = SoundService.ACTION_STOP
        }
        context.startService(intent)

        // Update the state
        isPlaying = false
        currentSoundResId = null
    }
}
