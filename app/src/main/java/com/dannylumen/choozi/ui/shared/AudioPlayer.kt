package com.dannylumen.choozi.ui.shared

import android.content.Context
import android.content.ContextParams
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes
import com.dannylumen.choozi.R
import java.io.IOException

class AudioManager(context: Context) {
    private val buildUpPlayer: AudioPlayer

    private var finalNotePlayer: AudioPlayer

    init {
        val attributedContext =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // For Android 12 (API 31) and above
                val contextParams = ContextParams.Builder().setAttributionTag("audioPlayer").build()
                context.createContext(contextParams)
            } else {
                // For older versions, attribution tags are not directly used this way
                // but creating a specific context can still be good practice.
                // However, for just AudioManager, the original context is usually fine.
                // This specific error is more prominent on API 30+.
                context
            }
        buildUpPlayer = AudioPlayer(attributedContext).apply {
            loadSound(R.raw.build_up, volume = 0.4f)
        }
        finalNotePlayer = AudioPlayer(attributedContext).apply {
            loadSound(R.raw.final_bell)
        }
    }

    fun playBuildUp() {
        if (buildUpPlayer.isPlaying()) {
            buildUpPlayer.restart()
        } else {
            buildUpPlayer.play()
        }
    }

    fun playFinalNote() {
//        Log.d("AudioManager", "playFinalNote()")
        if (finalNotePlayer.isPlaying()) {
            finalNotePlayer.restart()
        } else {
            finalNotePlayer.play()
        }
    }

    fun stopAny() {
//        Log.d("AudioManager", "stopAny()")
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
                Log.d(
                    "AudioPlayer", "Failed to create MediaPlayer for resource ID: $soundResId"
                )
                onError?.invoke(IOException("Failed to create MediaPlayer for resource ID: $soundResId"))
            }
        } catch (e: Exception) {
            isPrepared = false
            Log.d(
                "AudioPlayer", "Failed to create MediaPlayer for resource ID: $soundResId"
            )
            onError?.invoke(e)
        }

        if (isPrepared && mediaPlayer != null) {
            Log.d(
                "AudioPlayer", "loadSound() for resource ID: $soundResId finished successfully"
            )
        }
    }

    /**
     * Plays the loaded sound effect.
     * If the sound is already playing, it will continue.
     * If the sound is not prepared, this method does nothing.
     *
     */
    fun play() {
        // If audio is muted in settings don't do anything.
        if (SettingsManager.isAudioMuted(context)) {
            return
        }

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
    }
}
