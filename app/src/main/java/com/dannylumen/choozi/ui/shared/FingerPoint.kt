package com.dannylumen.choozi.ui.shared

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object FingerColors {
    const val NEUTRAL = Color.LTGRAY // Neutral Finger Color

    // Limited set of team colors that have higher contrast
    val TEAM_COLORS = listOf(
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.CYAN,
        Color.MAGENTA,
    )

    val COLORS = listOf(
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.CYAN,
        Color.MAGENTA,
        Color.rgb(0, 100, 0), // Dark Green
        Color.rgb(255, 165, 0), // Orange
        Color.rgb(255, 100, 20), // Darker orange
        Color.rgb(128, 0, 128), // Purple
    )

    private var colorIndex = 0

    private fun generateRandomColorFallback(): Int {
        // Start using fixed color order as a fallback.
        val color = COLORS[colorIndex % COLORS.size]
        colorIndex++
        return color
    }

    fun pickRandomColor(
        existingFingers: List<FingerPoint>,
    ): Int {
        val usedColors = existingFingers.map { it.color }
        val availableUniqueColors = COLORS.filterNot { usedColors.contains(it) }
        return if (availableUniqueColors.isNotEmpty()) {
            availableUniqueColors.random()
        } else {
            generateRandomColorFallback()
        }
    }

    fun getContrastingColor(backgroundColor: Int): Int {
        val y =
            (299 * Color.red(backgroundColor) + 587 * Color.green(backgroundColor) + 114 * Color.blue(
                backgroundColor
            )) / 1000.0
        return if (y >= 128) Color.BLACK else Color.WHITE
    }
}


data class FingerPoint(
    // Common properties
    var id: Int,
    var x: Float,
    var y: Float,
    var color: Int,

    // Generic properties
    val fingerRadius: Float = 150f,

    // Properties for FingerOrderingView
    var assignedNumber: Int? = null,

    // --- Glow Specific Properties ---
    var isGlowing: Boolean = false,
    var glowAnimationProgress: Float = 0f, // Current value for glow animation (percentage of max glow)
    var glowAlpha: Int = 150,       // Default alpha for glow
    val maxGlowRadiusOffset: Float = 200f, // Max additional radius for glow
    val strobeDuringCountdown: Boolean = true, // Strobe glow during countdown
    val strobeRate: Float = 1.0f / 3.0f, // The rate at which the is strobing during countdown

    // For FingerSelectorView's reveal animation (kept separate if its logic is distinct)
    var isRevealing: Boolean = false,
    var revealRadius: Float = 0f,

    var teamId: Int = -1, // For team assignment
    var themeSprite: com.dannylumen.choozi.theme.ThemeSprite? = null,
    var isSticky: Boolean = false,
) {
    companion object {
        private val basePaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        private val glowPaint = Paint().apply {
            style = Paint.Style.STROKE // Or FILL if you prefer a different glow style
            isAntiAlias = true
        }
        private val badgePaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        private val textPaint = Paint().apply { // For assignedNumber
            color = Color.BLACK // Default, will be set to contrasting
            textSize = 60f // Example, can be configured
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        private val stickyHaloPaint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeWidth = 6f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(24f, 16f), 0f)
        }
        private val stickyHaloBackingPaint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeWidth = 10f
            color = Color.argb(140, 0, 0, 0)
        }
    }

    fun draw(
        canvas: Canvas,
        countDownProgress: Float? = null,
        glowMultiplier: Float = 1f,
        context: Context? = null
    ) {
        // 1. Draw the main finger circle (serves as the glowing background behind the sprite)
        basePaint.color = this.color
        canvas.drawCircle(x, y, fingerRadius, basePaint)

        // 2. Draw Glow if active
        if (isGlowing || countDownProgress != null) {
            glowPaint.color = this.color
            glowPaint.alpha = this.glowAlpha
            // Strobes should pulse at strobeRate and they should linearly grow to
            // maxGlowRadiusOffset * strobeRate per strobe
            if (countDownProgress != null && strobeDuringCountdown) {
                val strobesComplete = (countDownProgress / strobeRate).toInt()
                val maxStrobeGlowRadius = (strobesComplete + 1) * strobeRate * maxGlowRadiusOffset
                val strobeProgress = (countDownProgress % strobeRate) / strobeRate
                glowPaint.strokeWidth = maxStrobeGlowRadius * strobeProgress * glowMultiplier
            } else {
                glowAnimationProgress = countDownProgress ?: this.glowAnimationProgress
                glowPaint.strokeWidth = glowAnimationProgress * maxGlowRadiusOffset * glowMultiplier
            }
            // The glow radius is the base radius plus some animated offset
            canvas.drawCircle(x, y, fingerRadius, glowPaint)
        }

        // 3. Draw Theme Sprite on top of the circle if available
        if (themeSprite != null && context != null) {
            themeSprite?.draw(context, canvas, x, y, fingerRadius, seed = this.id)
        }

        // 4. Draw assigned number (specific to FingerOrderingView, but harmless if number is null)
        assignedNumber?.let {
            if (themeSprite != null) {
                // Draw a high-contrast badge behind number so it's clearly readable over sprites
                badgePaint.color = Color.argb(190, 0, 0, 0)
                canvas.drawCircle(x, y, fingerRadius * 0.45f, badgePaint)
                textPaint.color = Color.WHITE
            } else {
                textPaint.color = FingerColors.getContrastingColor(this.color)
            }
            textPaint.textSize = fingerRadius * 0.6f
            val textY = y - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(it.toString(), x, textY, textPaint)
        }

        // 5. Draw sticky indicator if this finger is persistent
        if (isSticky) {
            canvas.drawCircle(x, y, fingerRadius + 14f, stickyHaloBackingPaint)
            stickyHaloPaint.color = Color.WHITE
            canvas.drawCircle(x, y, fingerRadius + 14f, stickyHaloPaint)
        }
    }

    fun resetAnimationStates() {
        isGlowing = false
        glowAnimationProgress = 0f

        isRevealing = false
        revealRadius = 0f
    }
}
