package com.dannylumen.choozi.theme

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * Defines a sprite for a finger touch point.
 *
 * Can be loaded from a colocated asset path (e.g. "themes/pirate/ship.png")
 * or from an Android drawable resource ID (e.g. R.drawable.ic_pirate_ship).
 *
 * @property assetPath Relative path in assets/ (e.g. "themes/pirate/ship.png").
 * @property drawableRes The Android drawable resource for this sprite.
 * @property scale Scale factor relative to the finger diameter (radius * 2).
 *                 e.g. 1.0f means it fits inside the finger circle; 1.2f is slightly larger.
 * @property offsetXRatio Horizontal offset multiplier relative to target size (for art alignment).
 * @property offsetYRatio Vertical offset multiplier relative to target size (for art alignment).
 */
data class ThemeSprite(
    val assetPath: String? = null,
    @param:DrawableRes val drawableRes: Int? = null,
    val scale: Float = 1.0f,
    val offsetXRatio: Float = 0f,
    val offsetYRatio: Float = 0f,
    val sway: Boolean = false,
    val maxSwayAngleDegrees: Float = 7.5f,
    val swayPeriodMs: Long = 2800L,
    val verticalBobDistance: Float = 5f
) {
    // Convenience constructor for resource ID
    constructor(
        @DrawableRes drawableRes: Int,
        scale: Float = 1.0f,
        offsetXRatio: Float = 0f,
        offsetYRatio: Float = 0f,
        sway: Boolean = false,
        maxSwayAngleDegrees: Float = 7.5f,
        swayPeriodMs: Long = 2800L,
        verticalBobDistance: Float = 5f
    ) : this(
        assetPath = null,
        drawableRes = drawableRes,
        scale = scale,
        offsetXRatio = offsetXRatio,
        offsetYRatio = offsetYRatio,
        sway = sway,
        maxSwayAngleDegrees = maxSwayAngleDegrees,
        swayPeriodMs = swayPeriodMs,
        verticalBobDistance = verticalBobDistance
    )

    // Convenience constructor for asset path
    constructor(
        assetPath: String,
        scale: Float = 1.0f,
        offsetXRatio: Float = 0f,
        offsetYRatio: Float = 0f,
        sway: Boolean = false,
        maxSwayAngleDegrees: Float = 7.5f,
        swayPeriodMs: Long = 2800L,
        verticalBobDistance: Float = 5f
    ) : this(
        assetPath = assetPath,
        drawableRes = null,
        scale = scale,
        offsetXRatio = offsetXRatio,
        offsetYRatio = offsetYRatio,
        sway = sway,
        maxSwayAngleDegrees = maxSwayAngleDegrees,
        swayPeriodMs = swayPeriodMs,
        verticalBobDistance = verticalBobDistance
    )

    companion object {
        private val spritePaint by lazy {
            Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }
        }
    }

    /**
     * Draws this sprite centered at (centerX, centerY) with aspect-ratio-preserving bounds
     * that fit proportionally inside (fingerRadius * 2 * scale).
     *
     * If [sway] is enabled, applies a slow natural rotation and vertical bobbing to simulate
     * floating in ocean waves, using [seed] to give different touch points a distinct wave phase.
     */
    fun draw(
        context: Context,
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        fingerRadius: Float,
        seed: Int = 0
    ) {
        val targetBoxSize = fingerRadius * 2f * scale

        var finalCenterX = centerX + (offsetXRatio * targetBoxSize)
        var finalCenterY = centerY + (offsetYRatio * targetBoxSize)
        var rotationAngle = 0f

        if (sway) {
            val now = android.os.SystemClock.uptimeMillis()
            val phase = seed * 1.35f
            val cycle = ((now % swayPeriodMs).toDouble() / swayPeriodMs) * 2.0 * Math.PI
            rotationAngle = (kotlin.math.sin(cycle + phase) * maxSwayAngleDegrees).toFloat()
            val bob = (kotlin.math.cos(cycle + phase) * verticalBobDistance).toFloat()
            finalCenterY += bob
        }

        if (rotationAngle != 0f) {
            canvas.save()
            canvas.rotate(rotationAngle, finalCenterX, finalCenterY)
        }

        try {
            // 1. Draw from asset if available
            if (assetPath != null) {
                val bitmap = AssetLoader.loadBitmap(context, assetPath)
                if (bitmap != null) {
                    val bw = bitmap.width.toFloat().coerceAtLeast(1f)
                    val bh = bitmap.height.toFloat().coerceAtLeast(1f)
                    val aspect = bw / bh

                    val drawW = if (aspect >= 1f) targetBoxSize else targetBoxSize * aspect
                    val drawH = if (aspect >= 1f) targetBoxSize / aspect else targetBoxSize

                    val left = (finalCenterX - drawW / 2f).toInt()
                    val top = (finalCenterY - drawH / 2f).toInt()
                    val right = (finalCenterX + drawW / 2f).toInt()
                    val bottom = (finalCenterY + drawH / 2f).toInt()

                    val dstRect = Rect(left, top, right, bottom)
                    canvas.drawBitmap(bitmap, null, dstRect, spritePaint)
                    return
                }
            }

            // 2. Fallback to drawable resource if available
            val resId = drawableRes ?: return
            val drawable = ContextCompat.getDrawable(context, resId) ?: return

            val intrinsicW = drawable.intrinsicWidth.toFloat().coerceAtLeast(1f)
            val intrinsicH = drawable.intrinsicHeight.toFloat().coerceAtLeast(1f)
            val aspect = intrinsicW / intrinsicH

            val drawW = if (aspect >= 1f) targetBoxSize else targetBoxSize * aspect
            val drawH = if (aspect >= 1f) targetBoxSize / aspect else targetBoxSize

            val left = (finalCenterX - drawW / 2f).toInt()
            val top = (finalCenterY - drawH / 2f).toInt()
            val right = (finalCenterX + drawW / 2f).toInt()
            val bottom = (finalCenterY + drawH / 2f).toInt()

            drawable.setBounds(left, top, right, bottom)
            drawable.draw(canvas)
        } finally {
            if (rotationAngle != 0f) {
                canvas.restore()
            }
        }
    }
}
