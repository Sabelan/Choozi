package com.example.boardgamerandomizer.ui.select_ordering

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.ceil
import kotlin.random.Random

class FingerOrderingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class FingerPoint(
        val id: Int, // Pointer ID from MotionEvent
        var position: PointF,
        var color: Int,
        var assignedNumber: Int? = null,
        var animationRadius: Float = 0f,
        var isAnimating: Boolean = false,
        var finalGlowRadius: Float = 0f // To store the radius for persistent glow
    )

    private val activeFingers = mutableListOf<FingerPoint>()
    private val assignedNumbersOrder = mutableListOf<FingerPoint>() // For animation sequence

    private var countdownTimer: CountDownTimer? = null
    private var isCountingDown = false
    private var selectionProcessStarted = false // Indicates if any finger has initiated the process
    private var selectionComplete = false
    private var selectionCompleteAndAnimationsDone = false // Indicates final state is reached
    private var currentAnimatingFingerIndex = -1
    private var countdownSecondsRemaining: Int = 0

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 80f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val countdownTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 150f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        setShadowLayer(20f, 2f, 2f, Color.BLACK)
    }


    private val circlePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val glowPaint = Paint().apply {
        style = Paint.Style.STROKE // Keep as stroke for outline effect
        strokeWidth = 20f // Glow thickness
        isAntiAlias = true
    }

    private val fingerRadius = 120f // Radius of the circle under the finger (from original)

    // Max radius for the *animation* part of the glow
    private val maxGlowAnimationRadius = fingerRadius + 200f

    // Listeners (from original FingerOrderingView)
    var onSelectionCompleteListener: (() -> Unit)? =
        null // Called when numbers assigned & animations START
    var onAllAnimationsCompleteListener: (() -> Unit)? =
        null // Called when ALL glow animations are DONE
    var onTimerTickListener: ((secondsRemaining: Int) -> Unit)? = null


    companion object {
        private const val TAG = "FingerOrderingView"
        private const val COUNTDOWN_DURATION_SECONDS = 3
        private const val COUNTDOWN_DURATION_MS = COUNTDOWN_DURATION_SECONDS * 1000L
        private const val COUNTDOWN_INTERVAL_MS = 100L
        private const val ANIMATION_DURATION_MS = 500L

        private val COLORS = listOf(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.rgb(255, 165, 0),
            Color.rgb(128, 0, 128),
            Color.rgb(0, 128, 128)
        )
        private var colorIndex = 0

        private fun generateRandomColorFallback(): Int { // Or use a fixed palette
            val color = COLORS[colorIndex % COLORS.size]
            colorIndex++
            return color
        }

        private fun pickRandomColor(activeFingers: List<FingerPoint>): Int {
            val randomizedOrder = (0 until COLORS.size).toList().shuffled()
            Log.d(TAG, "Randomized order: $randomizedOrder")
            for (index in randomizedOrder) {
                if (activeFingers.find { it.color == COLORS[index] } == null) {
                    return COLORS[index]
                }
            }
            return generateRandomColorFallback()
        }
    }

    init {
        // From original: For reveal animation, not directly used in this number assignment glow
        // revealPaint.color = Color.TRANSPARENT
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (selectionCompleteAndAnimationsDone) return true

        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        Log.d(
            TAG,
            "onTouchEvent: action=$action, pointerId=$pointerId, activeFingers=${activeFingers.size}, isCountingDown=$isCountingDown, selectionProcessStarted=$selectionProcessStarted"
        )

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (selectionComplete) {
                    return true
                }
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                if (activeFingers.find { it.id == pointerId } == null) {
                    activeFingers.add(
                        FingerPoint(
                            pointerId, PointF(x, y), color = pickRandomColor(activeFingers)
                        )
                    )
                    Log.d(TAG, "Added finger $pointerId. Count: ${activeFingers.size}")
                    if (activeFingers.isNotEmpty()) {
                        selectionProcessStarted = true
                        startCountdownTimer()
                    }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isCountingDown && !selectionProcessStarted) return true
                if (selectionComplete) return true

                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val finger = activeFingers.find { it.id == id }
                    finger?.let {
                        it.position.x = event.getX(i)
                        it.position.y = event.getY(i)
                    }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (selectionComplete) return true

                activeFingers.removeAll { it.id == pointerId }
                Log.d(TAG, "Removed finger $pointerId. Remaining: ${activeFingers.size}")

                if (activeFingers.isEmpty() && selectionProcessStarted && !selectionCompleteAndAnimationsDone) {
                    // All fingers lifted *during* countdown or before selection is final
                    Log.d(TAG, "All fingers lifted before selection complete. Resetting process.")
                    internalResetProcess() // Resets the current attempt
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startCountdownTimer() {
        if (!activeFingers.isNotEmpty() || selectionComplete) return

        isCountingDown = true
        countdownSecondsRemaining = COUNTDOWN_DURATION_SECONDS
        onTimerTickListener?.invoke(countdownSecondsRemaining) // Initial tick
        Log.d(TAG, "Starting countdown. Seconds: $countdownSecondsRemaining")

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(COUNTDOWN_DURATION_MS, COUNTDOWN_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = ceil(millisUntilFinished / 1000.0).toInt()
                if (seconds != countdownSecondsRemaining) {
                    countdownSecondsRemaining = seconds
                    onTimerTickListener?.invoke(countdownSecondsRemaining)
                }
                // Log.v(TAG, "Countdown Tick: $countdownSecondsRemaining")
                invalidate() // Keep invalidating for visual updates if any during tick
            }

            override fun onFinish() {
                Log.d(TAG, "Countdown Finished.")
                isCountingDown = false
                countdownSecondsRemaining = 0
                onTimerTickListener?.invoke(0) // Final tick
                invalidate() // Clear countdown text

                if (activeFingers.isNotEmpty()) {
                    assignNumbersAndStartAnimations()
                } else {
                    Log.d(TAG, "Countdown finished but no fingers. Resetting process.")
                    internalResetProcess()
                }
            }
        }.start()
        invalidate()
    }

    private fun assignNumbersAndStartAnimations() {
        if (activeFingers.isEmpty() || selectionCompleteAndAnimationsDone) {
            Log.d(
                TAG,
                "AssignNumbers: Skipped (active=${activeFingers.size}, complete=${selectionCompleteAndAnimationsDone})"
            )
            return
        }
        Log.d(TAG, "Assigning numbers for ${activeFingers.size} fingers.")

        // Make a mutable copy for shuffling and assigning numbers without affecting activeFingers directly yet
        val fingersToAssign = activeFingers.toMutableList()
        // Optional: Shuffle for random number assignment
        fingersToAssign.shuffle()

        assignedNumbersOrder.clear()
        fingersToAssign.forEachIndexed { index, fingerPoint ->
            // Find the original finger in activeFingers to update it
            activeFingers.find { it.id == fingerPoint.id }?.let {
                it.assignedNumber = index + 1
                assignedNumbersOrder.add(it)
            }
        }

        onSelectionCompleteListener?.invoke() // Numbers assigned, animations about to start
        selectionComplete = true
        currentAnimatingFingerIndex = -1 // Reset for new animation sequence
        invalidate() // Redraw to show numbers before animation
        startNextGlowAnimation()
    }

    private fun startNextGlowAnimation() {
        currentAnimatingFingerIndex++
        if (currentAnimatingFingerIndex < assignedNumbersOrder.size) {
            val fingerToAnimate = assignedNumbersOrder[currentAnimatingFingerIndex]
            Log.d(
                TAG,
                "Starting animation for finger ${fingerToAnimate.id}, number ${fingerToAnimate.assignedNumber}"
            )
            fingerToAnimate.isAnimating = true
            fingerToAnimate.animationRadius = 0f // Start animation from base

            val animator = ValueAnimator.ofFloat(0f, maxGlowAnimationRadius - fingerRadius)
                .apply { // Animate the *additional* radius
                    duration = ANIMATION_DURATION_MS
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { animation ->
                        fingerToAnimate.animationRadius = animation.animatedValue as Float
                        invalidate()
                    }
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            Log.d(TAG, "Animation ended for finger ${fingerToAnimate.id}")
                            fingerToAnimate.isAnimating = false
                            // For persistent glow, set a final radius or rely on a different drawing path
                            fingerToAnimate.finalGlowRadius =
                                maxGlowAnimationRadius // Or a fixed value for the outline
                            invalidate()
                            startNextGlowAnimation() // Trigger next animation
                        }
                    })
                }
            animator.start()
        } else {
            Log.d(TAG, "All glow animations complete.")
            selectionCompleteAndAnimationsDone = true
            assignedNumbersOrder.forEach {
                it.isAnimating = false
            } // Ensure all are marked as not animating
            onAllAnimationsCompleteListener?.invoke()
            invalidate() // Final draw of the completed state
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw all active fingers (those still on screen or part of the selection)
        val fingersToDraw = if (selectionComplete) assignedNumbersOrder else activeFingers

        fingersToDraw.forEach { finger ->
            // Draw base circle
            circlePaint.color = finger.color
            canvas.drawCircle(finger.position.x, finger.position.y, fingerRadius, circlePaint)

            // Draw assigned number if available
            finger.assignedNumber?.let { num ->
                val textY = finger.position.y - (textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText(num.toString(), finger.position.x, textY, textPaint)
            }

            // Draw glow animation or persistent glow
            if (finger.isAnimating) {
                glowPaint.color = finger.color
                // Fade out alpha for animating glow
                glowPaint.alpha =
                    ((maxGlowAnimationRadius - (fingerRadius + finger.animationRadius)) / (maxGlowAnimationRadius - fingerRadius) * 255).toInt()
                        .coerceIn(0, 255)
                canvas.drawCircle(
                    finger.position.x,
                    finger.position.y,
                    fingerRadius + finger.animationRadius,
                    glowPaint
                )
            }
//            else if (selectionCompleteAndAnimationsDone && finger.assignedNumber != null) {
//                // Persistent glow outline for selected fingers after animation
//                glowPaint.color = getContrastingColor(finger.color) // Or use finger.color
//                glowPaint.alpha = 255 // Full alpha
//                glowPaint.strokeWidth = persistentGlowOutlineWidth
//                canvas.drawCircle(
//                    finger.position.x,
//                    finger.position.y,
//                    fingerRadius + persistentGlowOutlineWidth / 2,
//                    glowPaint
//                ) // Centered stroke
//            }
        }
        glowPaint.strokeWidth = 20f

        // Draw countdown timer text
        if (isCountingDown && !selectionCompleteAndAnimationsDone && activeFingers.isNotEmpty()) {
            val countdownText = countdownSecondsRemaining.toString()
            val xPos = width / 2f
            val yPos =
                height / 2f - (countdownTextPaint.descent() + countdownTextPaint.ascent()) / 2f
            canvas.drawText(countdownText, xPos, yPos, countdownTextPaint)
        }
    }

    // From original FingerOrderingView
    private fun getContrastingColor(backgroundColor: Int): Int {
        val y =
            (299 * Color.red(backgroundColor) + 587 * Color.green(backgroundColor) + 114 * Color.blue(
                backgroundColor
            )) / 1000.0
        return if (y >= 128) Color.BLACK else Color.WHITE
    }

    private fun internalResetProcess() {
        Log.d(TAG, "internalResetProcess called.")
        countdownTimer?.cancel()
        // Stop any ongoing animations (more robust animator cancellation might be needed for complex cases)
        assignedNumbersOrder.forEach {
            it.isAnimating = false
            it.animationRadius = 0f
            it.assignedNumber = null // Clear assigned number if reset before completion
        }
        activeFingers.forEach { // Also clear numbers from active fingers not yet in assigned order
            it.assignedNumber = null
        }


        isCountingDown = false
        selectionProcessStarted = false
        selectionComplete = false

        // Don't clear activeFingers here, as some might still be on screen from the failed attempt.
        // Let onTouchEvent handle their removal if they lift.
        // If they stay, a new ACTION_DOWN (if all were lifted and one returns)
        // or the existing fingers (if some remained) will try to restart the process.
        assignedNumbersOrder.clear()
        currentAnimatingFingerIndex = -1
        countdownSecondsRemaining = 0
        colorIndex = 0 // Reset color picking

        onTimerTickListener?.invoke(-1) // Signal reset to listeners
        invalidate()
    }

    /**
     * Public method to completely reset the view to its initial state.
     * Called by an external button.
     */
    fun publicResetView() {
        Log.d(TAG, "publicResetView called.")
        // Clear all fingers as this is a full external reset
        activeFingers.clear()
        assignedNumbersOrder.clear()
        colorIndex = 0
        selectionCompleteAndAnimationsDone = false

        internalResetProcess()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "onDetachedFromWindow")
        countdownTimer?.cancel()
    }
}
