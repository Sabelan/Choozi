package com.dannylumen.choozi.ui.shared

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import androidx.annotation.RawRes
import com.dannylumen.choozi.R
import java.io.IOException

class AudioManager(private val context: Context) {
    private val buildUpPlayer: AudioPlayer
    private var finalNotePlayer: AudioPlayer

    private var currentLoadedThemeId: String? = null

    init {
        buildUpPlayer = AudioPlayer(context, preferLowLatency = false)
        finalNotePlayer = AudioPlayer(context, preferLowLatency = true)
        loadThemeSounds()
    }

    fun loadThemeSounds() {
        val theme = com.dannylumen.choozi.theme.ThemeManager.getCurrentTheme(context)
        currentLoadedThemeId = theme.id
        buildUpPlayer.loadSound(
            soundResId = theme.buildUpAudioRes,
            assetPath = theme.buildUpAudioAsset,
            volume = theme.buildUpVolume,
            looping = theme.loopBuildUpAudio
        )
        finalNotePlayer.loadSound(
            soundResId = theme.finalAudioRes,
            assetPath = theme.finalAudioAsset,
            volume = theme.finalAudioVolume,
            looping = false
        )
    }

    private fun checkAndReloadThemeIfChanged() {
        val currentThemeId = com.dannylumen.choozi.theme.ThemeManager.getCurrentTheme(context).id
        if (currentLoadedThemeId != currentThemeId) {
            loadThemeSounds()
        }
    }

    fun playBuildUp() {
        if (SettingsManager.isMusicMuted(context)) {
            return
        }
        checkAndReloadThemeIfChanged()
        val theme = com.dannylumen.choozi.theme.ThemeManager.getCurrentTheme(context)

        if (buildUpPlayer.isPlaying()) {
            if (theme.restartAudioOnNewFinger) {
                buildUpPlayer.restart()
            }
            // If restartAudioOnNewFinger is false, let the audio continue seamlessly
        } else {
            buildUpPlayer.play()
        }
    }

    fun playFinalNote() {
        if (SettingsManager.isSelectionMuted(context)) {
            return
        }
        checkAndReloadThemeIfChanged()
        val theme = com.dannylumen.choozi.theme.ThemeManager.getCurrentTheme(context)

        Log.d("SelectionTiming", "AudioManager.playFinalNote() called at ${System.currentTimeMillis()} ms")
        finalNotePlayer.restart()

        if (theme.stopBuildUpOnFinalNote) {
            buildUpPlayer.pause()
        }
    }

    fun stopAny() {
        buildUpPlayer.stop()
        finalNotePlayer.stop()
    }

    fun release() {
        buildUpPlayer.release()
        finalNotePlayer.release()
    }
}

/**
 * A utility class for playing sound effects or background music with restart and low-latency functionality.
 *
 * When preferLowLatency is true, short sound effects (<= 1MB) are loaded into [SoundPool] for sub-10ms
 * instant playback, falling back to [MediaPlayer] if decoding or memory limits fail.
 *
 * Usage:
 * 1. Create an instance: `val audioPlayer = AudioPlayer(context, preferLowLatency = true)`
 * 2. Load a sound: `audioPlayer.loadSound(assetPath = "themes/pirate/final_long.wav")`
 * 3. Play the sound: `audioPlayer.play()`
 * 4. Restart the sound: `audioPlayer.restart()`
 * 5. Release resources when done: `audioPlayer.release()`
 */
class AudioPlayer(
    private val context: Context,
    private val preferLowLatency: Boolean = false
) {
    // SoundPool path (for short, low-latency sound effects)
    private var soundPool: SoundPool? = null
    private var soundId: Int = 0
    private var activeStreamId: Int = 0
    private var isSoundPoolLoaded: Boolean = false
    private var pendingPlay: Boolean = false
    private var isSoundPoolPlaying: Boolean = false

    // MediaPlayer path (for longer tracks or fallback)
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared: Boolean = false
    private var needsRewind: Boolean = false

    private var currentSoundVolume: Float = 1.0f
    private var isLooping: Boolean = false

    fun setLooping(looping: Boolean) {
        this.isLooping = looping
        mediaPlayer?.isLooping = looping
        if (activeStreamId != 0) {
            soundPool?.setLoop(activeStreamId, if (looping) -1 else 0)
        }
    }

    /**
     * Loads a sound resource from an Android raw resource ID or an asset file path.
     *
     * @param soundResId The raw resource ID of the sound effect (e.g., R.raw.my_sound).
     * @param assetPath The relative asset path (e.g., "themes/pirate/build_up.wav").
     * @param volume Volume scalar between 0.0f and 1.0f.
     * @param looping Whether the sound should automatically loop when it finishes.
     */
    fun loadSound(
        @RawRes soundResId: Int? = null,
        assetPath: String? = null,
        volume: Float = 1.0f,
        looping: Boolean = false,
        onError: ((Exception) -> Unit)? = null
    ) {
        release()

        currentSoundVolume = volume
        isLooping = looping

        if (preferLowLatency) {
            loadSoundPool(soundResId, assetPath, onError)
        } else {
            loadMediaPlayer(soundResId, assetPath, onError)
        }
    }

    private fun loadSoundPool(
        @RawRes soundResId: Int? = null,
        assetPath: String? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        isSoundPoolLoaded = false
        pendingPlay = false
        activeStreamId = 0
        isSoundPoolPlaying = false

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                Log.d("AudioPlayer", "SoundPool onLoadComplete: sampleId=$sampleId, status=$status, expectedSoundId=$soundId")
                if (sampleId == soundId || soundId == 0) {
                    soundId = sampleId
                    if (status == 0) {
                        isSoundPoolLoaded = true
                        Log.d("AudioPlayer", "SoundPool successfully loaded sample $sampleId")
                        if (pendingPlay) {
                            pendingPlay = false
                            restart()
                        }
                    } else {
                        Log.w("AudioPlayer", "SoundPool load failed (status $status), falling back to MediaPlayer")
                        releaseSoundPool()
                        loadMediaPlayer(soundResId, assetPath, onError)
                    }
                }
            }
            soundPool = pool

            soundId = if (assetPath != null) {
                val cacheFile = java.io.File(context.cacheDir, "sound_${assetPath.replace('/', '_')}")
                context.assets.open(assetPath).use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val id = pool.load(cacheFile.absolutePath, 1)
                Log.d("AudioPlayer", "SoundPool requested load for ${cacheFile.absolutePath} -> sampleId=$id")
                id
            } else if (soundResId != null && soundResId != 0) {
                val id = pool.load(context, soundResId, 1)
                Log.d("AudioPlayer", "SoundPool requested load for resId=$soundResId -> sampleId=$id")
                id
            } else {
                0
            }

            if (soundId == 0) {
                Log.w("AudioPlayer", "SoundPool failed to acquire sound ID, falling back to MediaPlayer")
                releaseSoundPool()
                loadMediaPlayer(soundResId, assetPath, onError)
            }
        } catch (e: Exception) {
            Log.w("AudioPlayer", "SoundPool load error, falling back to MediaPlayer", e)
            releaseSoundPool()
            loadMediaPlayer(soundResId, assetPath, onError)
        }
    }

    private fun loadMediaPlayer(
        @RawRes soundResId: Int? = null,
        assetPath: String? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        isPrepared = false
        needsRewind = false

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            if (assetPath != null) {
                val afd = context.assets.openFd(assetPath)
                val player = MediaPlayer()
                player.setAudioAttributes(audioAttributes)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                player.setVolume(currentSoundVolume, currentSoundVolume)
                player.isLooping = isLooping
                player.setOnErrorListener { _, what, extra ->
                    isPrepared = false
                    val errorMessage = "MediaPlayer asset error - what: $what, extra: $extra"
                    Log.d("AudioPlayer", errorMessage)
                    onError?.invoke(IOException(errorMessage))
                    true
                }
                player.prepare()
                mediaPlayer = player
                isPrepared = true
            } else if (soundResId != null && soundResId != 0) {
                val player = MediaPlayer.create(context, soundResId, audioAttributes, 0)
                    ?: MediaPlayer.create(context, soundResId)
                player?.setAudioAttributes(audioAttributes)
                player?.setVolume(currentSoundVolume, currentSoundVolume)
                player?.isLooping = isLooping
                isPrepared = true
                player?.setOnErrorListener { _, what, extra ->
                    isPrepared = false
                    val errorMessage = "MediaPlayer error - what: $what, extra: $extra"
                    Log.d("AudioPlayer", errorMessage)
                    onError?.invoke(IOException(errorMessage))
                    true
                }
                mediaPlayer = player
                if (mediaPlayer == null) {
                    isPrepared = false
                    val msg = "Failed to create MediaPlayer for resource ID: $soundResId"
                    Log.d("AudioPlayer", msg)
                    onError?.invoke(IOException(msg))
                }
            }
        } catch (e: Exception) {
            isPrepared = false
            Log.e("AudioPlayer", "Failed to load sound (res: $soundResId, asset: $assetPath)", e)
            onError?.invoke(e)
        }
    }

    /**
     * Backward-compatible overload for raw resource IDs.
     */
    fun loadSound(
        @RawRes soundResId: Int,
        volume: Float = 1.0f,
        looping: Boolean = false,
        onError: ((Exception) -> Unit)? = null
    ) = loadSound(soundResId = soundResId, assetPath = null, volume = volume, looping = looping, onError = onError)

    /**
     * Plays the loaded sound effect.
     * If the sound is already playing, it will continue.
     */
    fun play() {
        if (SettingsManager.isAudioMuted(context)) {
            return
        }

        val now = System.currentTimeMillis()
        Log.d("SelectionTiming", "AudioPlayer.play() called at $now ms (preferLowLatency=$preferLowLatency)")

        soundPool?.let { pool ->
            if (!isSoundPoolLoaded) {
                Log.d("SelectionTiming", "SoundPool play() queued as pendingPlay (soundId=$soundId) at ${System.currentTimeMillis()} ms")
                pendingPlay = true
                return
            }
            val loop = if (isLooping) -1 else 0
            val stream = pool.play(soundId, currentSoundVolume, currentSoundVolume, 1, loop, 1.0f)
            Log.d("SelectionTiming", "SoundPool.play() called at ${System.currentTimeMillis()} ms -> streamId=$stream, soundId=$soundId")
            if (stream != 0) {
                activeStreamId = stream
                isSoundPoolPlaying = true
            }
            return
        }

        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                return
            }
            if (isPrepared) {
                if (needsRewind) {
                    player.seekTo(0)
                    needsRewind = false
                }
                player.start()
                Log.d("SelectionTiming", "MediaPlayer.start() called at ${System.currentTimeMillis()} ms")
            }
        }
    }

    /**
     * Restarts the sound effect from the beginning.
     * With SoundPool, this triggers a new low-latency playback stream immediately.
     */
    fun restart() {
        if (SettingsManager.isAudioMuted(context)) {
            return
        }

        val now = System.currentTimeMillis()
        Log.d("SelectionTiming", "AudioPlayer.restart() called at $now ms (preferLowLatency=$preferLowLatency)")

        soundPool?.let { pool ->
            if (!isSoundPoolLoaded) {
                Log.d("AudioPlayer", "SoundPool restart() called before load finished (soundId=$soundId). Queued pendingPlay.")
                Log.d("SelectionTiming", "SoundPool restart() queued as pendingPlay (soundId=$soundId) at ${System.currentTimeMillis()} ms")
                pendingPlay = true
                return
            }
            if (activeStreamId != 0) {
                pool.stop(activeStreamId)
            }
            val loop = if (isLooping) -1 else 0
            val stream = pool.play(soundId, currentSoundVolume, currentSoundVolume, 1, loop, 1.0f)
            Log.d("AudioPlayer", "SoundPool restart() played soundId=$soundId -> streamId=$stream (volume=$currentSoundVolume)")
            Log.d("SelectionTiming", "SoundPool.play() called at ${System.currentTimeMillis()} ms -> streamId=$stream, soundId=$soundId")
            if (stream != 0) {
                activeStreamId = stream
                isSoundPoolPlaying = true
            } else {
                Log.w("AudioPlayer", "SoundPool restart() failed: returned streamId=0 for soundId=$soundId")
                Log.w("SelectionTiming", "SoundPool restart() failed with streamId=0 for soundId=$soundId at ${System.currentTimeMillis()} ms")
            }
            return
        }

        mediaPlayer?.let { player ->
            if (isPrepared) {
                if (needsRewind) {
                    player.seekTo(0)
                    needsRewind = false
                }
                player.start()
                Log.d("AudioPlayer", "MediaPlayer restart() started playback")
                Log.d("SelectionTiming", "MediaPlayer.start() called at ${System.currentTimeMillis()} ms")
            } else {
                Log.w("SelectionTiming", "MediaPlayer restart() called but not prepared at ${System.currentTimeMillis()} ms")
            }
        }
    }

    /**
     * Pauses the sound without an immediate synchronous seek.
     */
    fun pause() {
        soundPool?.let { pool ->
            if (activeStreamId != 0) {
                pool.pause(activeStreamId)
                isSoundPoolPlaying = false
            }
        }

        mediaPlayer?.let { player ->
            if (isPrepared && player.isPlaying) {
                player.pause()
                needsRewind = true
            }
        }
    }

    /**
     * Stops the sound effect and resets its position to the beginning.
     */
    fun stop() {
        soundPool?.let { pool ->
            if (activeStreamId != 0) {
                pool.stop(activeStreamId)
                activeStreamId = 0
                isSoundPoolPlaying = false
            }
        }

        mediaPlayer?.let { player ->
            if (isPrepared && player.isPlaying) {
                player.pause()
                player.seekTo(0)
                needsRewind = false
            } else if (isPrepared && needsRewind) {
                player.seekTo(0)
                needsRewind = false
            }
        }
    }

    /**
     * Checks if the sound is currently playing.
     *
     * @return True if playing, false otherwise.
     */
    fun isPlaying(): Boolean {
        soundPool?.let {
            return isSoundPoolPlaying
        }
        return mediaPlayer?.isPlaying ?: false
    }

    private fun releaseSoundPool() {
        soundPool?.release()
        soundPool = null
        soundId = 0
        activeStreamId = 0
        isSoundPoolLoaded = false
        pendingPlay = false
        isSoundPoolPlaying = false
    }

    /**
     * Releases audio player resources.
     */
    fun release() {
        releaseSoundPool()
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
        needsRewind = false
    }
}
