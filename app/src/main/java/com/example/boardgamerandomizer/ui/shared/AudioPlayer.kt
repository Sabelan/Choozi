package com.example.boardgamerandomizer.ui.shared

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes
import com.example.boardgamerandomizer.R
import java.io.IOException

class AudioManager(private val context: Context) {
    private val buildUpPlayer: AudioPlayer = AudioPlayer(context).apply {
        loadSound(R.raw.build_up, volume = 0.4f)
    }
    private var finalNotePlayer: AudioPlayer = AudioPlayer(context).apply {
        loadSound(R.raw.final_bell)
    }

    fun playBuildUp() {
        if (buildUpPlayer.isPlaying()) {
            buildUpPlayer.restart()
        } else {
            buildUpPlayer.play()
        }
    }

    fun playFinalNote() {
        Log.d("AudioManager", "playFinalNote()")
        if (finalNotePlayer.isPlaying()) {
            finalNotePlayer.restart()
        } else {
            finalNotePlayer.play()
        }
    }

    fun stopAny() {
        Log.d("AudioManager", "stopAny()")
        buildUpPlayer.stop()
        finalNotePlayer.stop()
    }

    fun release() {
        buildUpPlayer.release()
        finalNotePlayer.release()
    }
}

/**
 * A utility class for playing a single sound effect with restart functionality.
 *
 * Usage:
 * 1. Create an instance: `val audioPlayer = AudioPlayer(context)`
 * 2. Load a sound: `audioPlayer.loadSound(R.raw.my_sound_effect)`
 * 3. Play the sound: `audioPlayer.play()`
 * 4. Restart the sound: `audioPlayer.restart()`
 * 5. Release resources when done: `audioPlayer.release()`
 */
class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSoundResId: Int? = null
    private var isPrepared: Boolean = false
    private var currentSoundVolume: Float = 1.0f

    /**
     * Loads a sound resource. If a sound is already loaded, it will be released first.
     *
     * @param soundResId The raw resource ID of the sound effect (e.g., R.raw.my_sound).
     */
    fun loadSound(
        @RawRes soundResId: Int, volume: Float = 1.0f, onError: ((Exception) -> Unit)? = null

    ) {
        release() // Release any existing MediaPlayer instance

        currentSoundResId = soundResId
        isPrepared = false
        currentSoundVolume = volume

        try {
            mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer?.setVolume(currentSoundVolume, currentSoundVolume)
            isPrepared = true
            mediaPlayer?.setOnErrorListener { _, what, extra ->
                isPrepared = false
                val errorMessage = "MediaPlayer error - what: $what, extra: $extra"
                Log.d("AudioPlayer", "MediaPlayer error: $errorMessage")
                onError?.invoke(IOException(errorMessage))
                true // True if the method handled the error, false if it didn't.
            }
            // MediaPlayer.create() calls prepare() internally, so setOnPreparedListener
            // will be called if successful. If create() itself fails (e.g., file not found),
            // it will return null or throw an exception.
            if (mediaPlayer == null) {
                isPrepared = false
                Log.d("AudioPlayer", "Failed to create MediaPlayer for resource ID: $soundResId")
                onError?.invoke(IOException("Failed to create MediaPlayer for resource ID: $soundResId"))
            }
        } catch (e: Exception) {
            isPrepared = false
            Log.d("AudioPlayer", "Failed to create MediaPlayer for resource ID: $soundResId")
            onError?.invoke(e)
        }

        if (isPrepared && mediaPlayer != null) {
            Log.d("AudioPlayer", "loadSound() for resource ID: $soundResId finished successfully")
        }
    }

    /**
     * Plays the loaded sound effect.
     * If the sound is already playing, it will continue.
     * If the sound is not prepared, this method does nothing.
     *
     */
    fun play() {
        if (mediaPlayer?.isPlaying == true) {
            // Already playing, do nothing or you could choose to restart by calling restart()
            return
        }
        if (isPrepared) {
            mediaPlayer?.start()
        }
    }

    /**
     * Restarts the sound effect from the beginning.
     * If the sound is currently playing, it will be stopped and restarted.
     * If the sound is not prepared, this method does nothing.
     *
     */
    fun restart() {
        if (isPrepared && isPlaying()) {
            mediaPlayer?.seekTo(0) // Go to the beginning
            mediaPlayer?.start()
        }
    }

    /**
     * Stops the sound effect and resets its position to the beginning.
     * The sound will need to be prepared again (or re-loaded) if you want to play it after stopping.
     * Consider using pause() if you intend to resume later.
     */
    fun stop() {
        if (isPrepared && isPlaying()) {
            mediaPlayer?.pause()
            mediaPlayer?.seekTo(0)
        }
    }

    /**
     * Checks if the sound is currently playing.
     *
     * @return True if playing, false otherwise.
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    /**
     * Releases the MediaPlayer resources.
     * This should be called when the AudioPlayer is no longer needed (e.g., in Activity's onDestroy).
     */
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
        currentSoundResId = null
    }
}
