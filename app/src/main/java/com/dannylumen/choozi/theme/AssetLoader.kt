package com.dannylumen.choozi.theme

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

/**
 * Utility for loading and caching assets (bitmaps and audio file descriptors)
 * from the app/src/main/assets directory.
 */
object AssetLoader {
    private const val TAG = "AssetLoader"
    private val bitmapCache = mutableMapOf<String, Bitmap>()

    /**
     * Loads and caches a Bitmap from assets (e.g. "themes/pirate/ship.png").
     */
    fun loadBitmap(context: Context, assetPath: String): Bitmap? {
        return bitmapCache[assetPath] ?: try {
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.also { bitmap ->
                    bitmapCache[assetPath] = bitmap
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load asset bitmap: $assetPath", e)
            null
        }
    }

    /**
     * Opens an AssetFileDescriptor for audio playback.
     */
    fun openFd(context: Context, assetPath: String): AssetFileDescriptor? {
        return try {
            context.assets.openFd(assetPath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open asset file descriptor: $assetPath", e)
            null
        }
    }

    /**
     * Clears cached bitmaps if memory pressure occurs.
     */
    fun clearCache() {
        bitmapCache.clear()
    }
}
