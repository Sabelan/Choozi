package com.example.boardgamerandomizer.ui.shared

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object FingerColors {
    val NEUTRAL = Color.LTGRAY // Neutral Finger Color
    val COLORS = listOf(
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.CYAN,
        Color.MAGENTA,
        Color.rgb(255, 130, 50), // Dark orange
        Color.rgb(255, 165, 0), // Orange
        Color.rgb(128, 0, 128), // Purple
        Color.rgb(255, 20, 147), // Deep Pink / Hot Pink
//        Color.rgb(255, 105, 180), // Pink Rose -- too close to magenta
//        Color.rgb(124, 252, 0), // Lawn Green / Bright Chartreuse
//        Color.rgb(0, 255, 127), // Spring Green -- too close to green

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
    val id: Int,
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
) {
    companion object {
        private val teamRevealPaint = Paint().apply { // For team reveal/color change
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        private val basePaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        private val glowPaint = Paint().apply {
            style = Paint.Style.STROKE // Or FILL if you prefer a different glow style
            isAntiAlias = true
        }
        private val textPaint = Paint().apply { // For assignedNumber
            color = Color.BLACK // Default, will be set to contrasting
            textSize = 60f // Example, can be configured
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    fun draw(canvas: Canvas, countDownProgress: Float? = null, glowMultiplier: Float = 1f) {
        // 1. Draw the main finger circle
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
//                Log.d(
//                    "FingerPoint",
//                    """"Strobe situation: Total percent complete ${countDownProgress * 100}
//                        |which means a total of $strobesComplete should've already happened,
//                        |the current strobe's max strobe radius is $maxStrobeGlowRadius,
//                        |and the progress within the current strobe is $strobeProgress""".trimMargin()
//                )
                glowPaint.strokeWidth = maxStrobeGlowRadius * strobeProgress * glowMultiplier
            } else {
                glowAnimationProgress = countDownProgress ?: this.glowAnimationProgress
                glowPaint.strokeWidth = glowAnimationProgress * maxGlowRadiusOffset * glowMultiplier
            }
            // The glow radius is the base radius plus some animated offset
            canvas.drawCircle(x, y, fingerRadius, glowPaint)
        }

        // 3. Draw assigned number (specific to FingerOrderingView, but harmless if number is null)
        assignedNumber?.let {
            textPaint.color = FingerColors.getContrastingColor(this.color)
            // Adjust text size based on baseFingerRadius if needed
            textPaint.textSize = fingerRadius * 0.6f
            val textY = y - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(it.toString(), x, textY, textPaint)
        }
    }

    fun resetAnimationStates() {
        isGlowing = false
        glowAnimationProgress = 0f

        isRevealing = false
        revealRadius = 0f
    }
}
