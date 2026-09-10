package com.dannylumen.choozi.ui.shared

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import androidx.annotation.RawRes
import com.dannylumen.choozi.R
import com.dannylumen.choozi.theme.ThemeManager
import java.io.File
import java.io.IOException

class AudioManager(private val context: Context) {
    private val buildUpPlayer: AudioPlayer
    private var finalNotePlayer: AudioPlayer

    private var currentLoadedThemeId: String? = null

    private val themeChangeListener: () -> Unit = {
        checkAndReloadThemeIfChanged()
    }

    init {
        buildUpPlayer = AudioPlayer(context, preferLowLatency = false)
        finalNotePlayer = AudioPlayer(context, preferLowLatency = true)
        ThemeManager.addThemeChangeListener(themeChangeListener)
        loadThemeSounds()
    }

    fun loadThemeSounds() {
        val theme = ThemeManager.getCurrentTheme(context)
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
        val currentThemeId = ThemeManager.getCurrentTheme(context).id
        if (currentLoadedThemeId != currentThemeId) {
            loadThemeSounds()
        }
    }

    fun playBuildUp() {
        if (SettingsManager.isAudioMuted(context) || SettingsManager.isMusicMuted(context)) {
            return
        }
        checkAndReloadThemeIfChanged()
        val theme = ThemeManager.getCurrentTheme(context)

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
        checkAndReloadThemeIfChanged()
        val theme = ThemeManager.getCurrentTheme(context)

        if (theme.stopBuildUpOnFinalNote) {
            buildUpPlayer.pause()
        }

        if (SettingsManager.isAudioMuted(context) || SettingsManager.isSelectionMuted(context)) {
            return
        }

        Log.d("SelectionTiming", "AudioManager.playFinalNote() called at ${System.currentTimeMillis()} ms")
        finalNotePlayer.restart()
    }

    fun stopAny() {
        buildUpPlayer.stop()
        finalNotePlayer.stop()
    }

    fun release() {
        ThemeManager.removeThemeChangeListener(themeChangeListener)
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
        try {
            mediaPlayer?.isLooping = looping
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Error setting isLooping on MediaPlayer", e)
        }
        if (activeStreamId != 0) {
            soundPool?.setLoop(activeStreamId, if (looping) -1 else 0)
        }
    }

    /**
     * Extracts an asset to cacheDir if needed, ensuring MediaPlayer or SoundPool
     * can reliably open it as a standard file without AssetFileDescriptor compression issues.
     */
    private fun getOrCreateCacheFile(assetPath: String): File {
        val sanitized = assetPath.replace('/', '_').replace('\\', '_')
        val cacheFile = File(context.cacheDir, "sound_$sanitized")
        try {
            var expectedLength = -1L
            try {
                context.assets.openFd(assetPath).use { afd ->
                    expectedLength = afd.length
                }
            } catch (_: Exception) {
                // Asset might be compressed by AAPT, openFd may fail; fallback to checking existence & size > 0
            }

            if (cacheFile.exists() && cacheFile.length() > 0L) {
                if (expectedLength <= 0L || cacheFile.length() == expectedLength) {
                    return cacheFile
                }
            }
        } catch (_: Exception) {}

        context.assets.open(assetPath).use { input ->
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return cacheFile
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
                .setMaxStreams(8)
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
                        val wasPending = pendingPlay
                        releaseSoundPool()
                        pendingPlay = wasPending
                        loadMediaPlayer(soundResId, assetPath, onError)
                    }
                }
            }
            soundPool = pool

            soundId = if (assetPath != null) {
                val cacheFile = getOrCreateCacheFile(assetPath)
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
                val wasPending = pendingPlay
                releaseSoundPool()
                pendingPlay = wasPending
                loadMediaPlayer(soundResId, assetPath, onError)
            }
        } catch (e: Exception) {
            Log.w("AudioPlayer", "SoundPool load error, falling back to MediaPlayer", e)
            val wasPending = pendingPlay
            releaseSoundPool()
            pendingPlay = wasPending
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

            val player = MediaPlayer()
            player.setAudioAttributes(audioAttributes)
            player.setVolume(currentSoundVolume, currentSoundVolume)
            player.isLooping = isLooping

            player.setOnCompletionListener {
                Log.d("AudioPlayer", "MediaPlayer playback completed")
                needsRewind = true
                try {
                    player.seekTo(0)
                } catch (e: Exception) {
                    Log.w("AudioPlayer", "Error rewinding on completion", e)
                }
            }

            player.setOnErrorListener { _, what, extra ->
                isPrepared = false
                val errorMessage = "MediaPlayer error - what: $what, extra: $extra"
                Log.e("AudioPlayer", errorMessage)
                onError?.invoke(IOException(errorMessage))
                true
            }

            var sourceConfigured = false

            if (assetPath != null) {
                // Attempt 1: Direct AssetFileDescriptor
                try {
                    val afd = context.assets.openFd(assetPath)
                    try {
                        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        player.prepare()
                        sourceConfigured = true
                    } finally {
                        // Crucial: Only close afd AFTER prepare() finishes!
                        try {
                            afd.close()
                        } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    Log.w("AudioPlayer", "Direct openFd/prepare failed for $assetPath (${e.message}), trying cache file fallback")
                }

                // Attempt 2: Fallback to cached file on disk (handles compressed assets or fd offset quirks)
                if (!sourceConfigured) {
                    try {
                        player.reset()
                        player.setAudioAttributes(audioAttributes)
                        player.setVolume(currentSoundVolume, currentSoundVolume)
                        player.isLooping = isLooping
                        player.setOnCompletionListener {
                            needsRewind = true
                            try {
                                player.seekTo(0)
                            } catch (_: Exception) {}
                        }
                        player.setOnErrorListener { _, what, extra ->
                            isPrepared = false
                            val errorMessage = "MediaPlayer error - what: $what, extra: $extra"
                            Log.e("AudioPlayer", errorMessage)
                            onError?.invoke(IOException(errorMessage))
                            true
                        }
                        val cacheFile = getOrCreateCacheFile(assetPath)
                        player.setDataSource(cacheFile.absolutePath)
                        player.prepare()
                        sourceConfigured = true
                    } catch (e: Exception) {
                        Log.e("AudioPlayer", "Cache file fallback failed for $assetPath", e)
                    }
                }
            } else if (soundResId != null && soundResId != 0) {
                // Attempt 1: openRawResourceFd
                try {
                    val afd = context.resources.openRawResourceFd(soundResId)
                    if (afd != null) {
                        try {
                            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            player.prepare()
                            sourceConfigured = true
                        } finally {
                            try {
                                afd.close()
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    Log.w("AudioPlayer", "openRawResourceFd failed for $soundResId (${e.message}), trying MediaPlayer.create")
                }

                // Attempt 2: MediaPlayer.create
                if (!sourceConfigured) {
                    try {
                        player.release()
                        val createdPlayer = MediaPlayer.create(context, soundResId, audioAttributes, 0)
                            ?: MediaPlayer.create(context, soundResId)
                        if (createdPlayer != null) {
                            createdPlayer.setAudioAttributes(audioAttributes)
                            createdPlayer.setVolume(currentSoundVolume, currentSoundVolume)
                            createdPlayer.isLooping = isLooping
                            createdPlayer.setOnCompletionListener {
                                needsRewind = true
                                try {
                                    createdPlayer.seekTo(0)
                                } catch (_: Exception) {}
                            }
                            createdPlayer.setOnErrorListener { _, what, extra ->
                                isPrepared = false
                                val errorMessage = "MediaPlayer error - what: $what, extra: $extra"
                                Log.e("AudioPlayer", errorMessage)
                                onError?.invoke(IOException(errorMessage))
                                true
                            }
                            mediaPlayer = createdPlayer
                            isPrepared = true
                            sourceConfigured = true
                            if (pendingPlay) {
                                pendingPlay = false
                                play()
                            }
                            return
                        }
                    } catch (e: Exception) {
                        Log.e("AudioPlayer", "MediaPlayer.create failed for resId $soundResId", e)
                    }
                }
            }

            if (sourceConfigured) {
                mediaPlayer = player
                isPrepared = true
                Log.d("AudioPlayer", "MediaPlayer prepared successfully (asset=$assetPath, resId=$soundResId)")
                if (pendingPlay) {
                    Log.d("AudioPlayer", "Executing pendingPlay on newly prepared MediaPlayer")
                    pendingPlay = false
                    play()
                }
            } else {
                player.release()
                mediaPlayer = null
                isPrepared = false
                val msg = "Could not initialize MediaPlayer for asset=$assetPath, resId=$soundResId"
                Log.e("AudioPlayer", msg)
                onError?.invoke(IOException(msg))
            }
        } catch (e: Exception) {
            isPrepared = false
            mediaPlayer = null
            Log.e("AudioPlayer", "Failed to load sound in MediaPlayer (res: $soundResId, asset: $assetPath)", e)
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
                Log.d("SelectionTiming", "SoundPool play() queued as pendingPlay (soundId=$soundId) at $now ms")
                pendingPlay = true
                return
            }
            if (isSoundPoolPlaying && activeStreamId != 0) {
                return
            }
            val loop = if (isLooping) -1 else 0
            val stream = pool.play(soundId, currentSoundVolume, currentSoundVolume, 1, loop, 1.0f)
            Log.d("SelectionTiming", "SoundPool.play() called at ${System.currentTimeMillis()} ms -> streamId=$stream, soundId=$soundId")
            if (stream != 0) {
                activeStreamId = stream
                isSoundPoolPlaying = true
            } else {
                Log.w("AudioPlayer", "SoundPool.play() returned 0 for soundId=$soundId")
            }
            return
        }

        mediaPlayer?.let { player ->
            if (!isPrepared) {
                Log.d("AudioPlayer", "MediaPlayer play() queued as pendingPlay at $now ms")
                pendingPlay = true
                return
            }
            try {
                if (player.isPlaying) {
                    return
                }
                if (needsRewind || (player.duration > 0 && player.currentPosition >= player.duration - 100)) {
                    player.seekTo(0)
                    needsRewind = false
                }
                player.start()
                Log.d("SelectionTiming", "MediaPlayer.start() called at ${System.currentTimeMillis()} ms")
            } catch (e: Exception) {
                Log.e("AudioPlayer", "MediaPlayer.play() failed", e)
            }
            return
        }

        // Neither ready yet; queue for when prepared/loaded
        pendingPlay = true
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
                pendingPlay = true
                return
            }
            if (activeStreamId != 0) {
                pool.stop(activeStreamId)
            }
            val loop = if (isLooping) -1 else 0
            val stream = pool.play(soundId, currentSoundVolume, currentSoundVolume, 1, loop, 1.0f)
            Log.d("AudioPlayer", "SoundPool restart() played soundId=$soundId -> streamId=$stream (volume=$currentSoundVolume)")
            if (stream != 0) {
                activeStreamId = stream
                isSoundPoolPlaying = true
            } else {
                Log.w("AudioPlayer", "SoundPool restart() failed: returned streamId=0 for soundId=$soundId")
            }
            return
        }

        mediaPlayer?.let { player ->
            if (!isPrepared) {
                Log.d("AudioPlayer", "MediaPlayer restart() queued as pendingPlay at $now ms")
                pendingPlay = true
                return
            }
            try {
                player.seekTo(0)
                needsRewind = false
                player.start()
                Log.d("AudioPlayer", "MediaPlayer restart() started playback")
            } catch (e: Exception) {
                Log.e("AudioPlayer", "MediaPlayer.restart() failed", e)
            }
            return
        }

        // Neither ready yet; queue for when prepared/loaded
        pendingPlay = true
    }

    /**
     * Pauses the sound without an immediate synchronous seek.
     */
    fun pause() {
        pendingPlay = false
        soundPool?.let { pool ->
            if (activeStreamId != 0) {
                pool.pause(activeStreamId)
                isSoundPoolPlaying = false
            }
        }

        mediaPlayer?.let { player ->
            if (isPrepared) {
                try {
                    if (player.isPlaying) {
                        player.pause()
                        needsRewind = true
                    }
                } catch (e: Exception) {
                    Log.w("AudioPlayer", "MediaPlayer pause error", e)
                }
            }
        }
    }

    /**
     * Stops the sound effect and resets its position to the beginning.
     */
    fun stop() {
        pendingPlay = false
        soundPool?.let { pool ->
            if (activeStreamId != 0) {
                pool.stop(activeStreamId)
                activeStreamId = 0
                isSoundPoolPlaying = false
            }
        }

        mediaPlayer?.let { player ->
            if (isPrepared) {
                try {
                    if (player.isPlaying) {
                        player.pause()
                    }
                    player.seekTo(0)
                    needsRewind = false
                } catch (e: Exception) {
                    Log.w("AudioPlayer", "MediaPlayer stop error", e)
                }
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
        return try {
            if (isPrepared) mediaPlayer?.isPlaying == true else false
        } catch (e: Exception) {
            false
        }
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
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Error releasing MediaPlayer", e)
        }
        mediaPlayer = null
        isPrepared = false
        needsRewind = false
        pendingPlay = false
    }
}
