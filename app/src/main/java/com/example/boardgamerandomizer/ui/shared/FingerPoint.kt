package com.example.boardgamerandomizer.ui.shared

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.util.Log
import com.example.boardgamerandomizer.ui.select_ordering.FingerOrderingView

object FingerColors {
    val COLORS = listOf(
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.CYAN,
        Color.MAGENTA, Color.rgb(255, 165, 0), // Orange
        Color.rgb(128, 0, 128), // Purple
        Color.rgb(0, 128, 128)  // Teal
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
    var glowAnimationValue: Float = 0f, // Current value for glow animation (e.g., expanding radius)
    var glowColor: Int = color,     // Default to finger color, can be customized
    var glowAlpha: Int = 150,       // Default alpha for glow
    var maxGlowRadiusOffset: Float = 100f, // Max additional radius for glow

    // For FingerSelectorView's reveal animation (kept separate if its logic is distinct)
    var isRevealing: Boolean = false,
    var revealRadius: Float = 0f

) {
    // Companion object for shared Paint objects to avoid re-creation in draw
    companion object {
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

    fun draw(canvas: Canvas, glowAnimationValueOverride: Float? = null) {
        // 1. Draw the main finger circle
        basePaint.color = this.color
        canvas.drawCircle(x, y, fingerRadius, basePaint)

        // 2. Draw Glow if active
        if (isGlowing || glowAnimationValueOverride != null) {
            glowPaint.color = this.glowColor
            glowPaint.alpha = this.glowAlpha
            glowAnimationValue = glowAnimationValueOverride ?: this.glowAnimationValue
            glowPaint.strokeWidth =
                10f + (glowAnimationValue * 0.5f) // Example: make stroke thicker with animation
            // The glow radius is the base radius plus some animated offset
            val currentGlowRadius = fingerRadius + glowAnimationValue
            canvas.drawCircle(x, y, currentGlowRadius, glowPaint)
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
        glowAnimationValue = 0f

        isRevealing = false
        revealRadius = 0f
        // assignedNumber = null // Reset this in the view's logic where appropriate
    }

//    private fun getContrastingColor(backgroundColor: Int): Int {
//        val y =
//            (299 * Color.red(backgroundColor) + 587 * Color.green(backgroundColor) + 114 * Color.blue(
//                backgroundColor
//            )) / 1000.0
//        return if (y >= 128) Color.BLACK else Color.WHITE
//    }

}
