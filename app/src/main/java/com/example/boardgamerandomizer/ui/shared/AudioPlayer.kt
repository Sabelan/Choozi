package com.example.boardgamerandomizer.ui.shared

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.annotation.RawRes
import java.io.IOException

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

    /**
     * Loads a sound resource. If a sound is already loaded, it will be released first.
     *
     * @param soundResId The raw resource ID of the sound effect (e.g., R.raw.my_sound).
     * @param onLoaded Optional callback invoked when the sound is successfully loaded and prepared.
     * @param onError Optional callback invoked if an error occurs during loading or preparation.
     */
    fun loadSound(
        @RawRes soundResId: Int,
        onLoaded: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        release() // Release any existing MediaPlayer instance

        currentSoundResId = soundResId
        isPrepared = false

        try {
            mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer?.setOnPreparedListener {
                isPrepared = true
                onLoaded?.invoke()
            }
            mediaPlayer?.setOnErrorListener { _, what, extra ->
                isPrepared = false
                val errorMessage = "MediaPlayer error - what: $what, extra: $extra"
                onError?.invoke(IOException(errorMessage))
                true // True if the method handled the error, false if it didn't.
            }
            // MediaPlayer.create() calls prepare() internally, so setOnPreparedListener
            // will be called if successful. If create() itself fails (e.g., file not found),
            // it will return null or throw an exception.
            if (mediaPlayer == null) {
                isPrepared = false
                onError?.invoke(IOException("Failed to create MediaPlayer for resource ID: $soundResId"))
            }
        } catch (e: Exception) {
            isPrepared = false
            onError?.invoke(e)
        }
    }

    /**
     * Plays the loaded sound effect.
     * If the sound is already playing, it will continue.
     * If the sound is not prepared, this method does nothing.
     *
     * @param onCompletion Optional callback invoked when the sound playback completes.
     */
    fun play(onCompletion: (() -> Unit)? = null) {
        if (mediaPlayer?.isPlaying == true) {
            // Already playing, do nothing or you could choose to restart by calling restart()
            return
        }
        if (isPrepared) {
            mediaPlayer?.setOnCompletionListener {
                onCompletion?.invoke()
            }
            mediaPlayer?.start()
        }
    }

    /**
     * Restarts the sound effect from the beginning.
     * If the sound is currently playing, it will be stopped and restarted.
     * If the sound is not prepared, this method does nothing.
     *
     * @param onCompletion Optional callback invoked when the sound playback completes after restarting.
     */
    fun restart(onCompletion: (() -> Unit)? = null) {
        if (isPrepared) {
            mediaPlayer?.seekTo(0) // Go to the beginning
            mediaPlayer?.setOnCompletionListener {
                onCompletion?.invoke()
            }
            mediaPlayer?.start()
        }
    }

    /**
     * Pauses the currently playing sound effect.
     */
    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    /**
     * Stops the sound effect and resets its position to the beginning.
     * The sound will need to be prepared again (or re-loaded) if you want to play it after stopping.
     * Consider using pause() if you intend to resume later.
     */
    fun stop() {
        if (isPrepared) {
            mediaPlayer?.stop()
            isPrepared = false // After stop(), MediaPlayer needs to be prepared again.
            // To simplify, we'll require re-loading the sound or calling prepare explicitly.
            // For this utility, re-loading via loadSound() is the intended path after stop().
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
