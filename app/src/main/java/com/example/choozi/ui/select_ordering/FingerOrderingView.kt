package com.example.choozi.ui.select_ordering

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.example.choozi.ui.shared.AudioManager
import com.example.choozi.ui.shared.FingerColors
import com.example.choozi.ui.shared.FingerPoint
import kotlin.math.ceil

class FingerOrderingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val activeFingers = mutableListOf<FingerPoint>()
    private val assignedNumbersOrder = mutableListOf<FingerPoint>() // For animation sequence

    private var countdownTimer: CountDownTimer? = null
    private var isCountingDown = false
    private var countDownProgress: Float? = null
    private var selectionProcessStarted = false // Indicates if any finger has initiated the process
    private var selectionComplete = false
    private var selectionCompleteAndAnimationsDone = false // Indicates final state is reached
    private var currentAnimatingFingerIndex = -1
    private var countdownSecondsRemaining: Int = 0

    private val countdownTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 150f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        setShadowLayer(20f, 2f, 2f, Color.BLACK)
    }

    // Listeners (from original FingerOrderingView)
    var onSelectionCompleteListener: (() -> Unit)? =
        null // Called when numbers assigned & animations START
    var onAllAnimationsCompleteListener: (() -> Unit)? =
        null // Called when ALL glow animations are DONE

    // Audio Manager
    private val audioManager: AudioManager = AudioManager(context)

    companion object {
        private const val TAG = "FingerOrderingView"
        private const val COUNTDOWN_DURATION_SECONDS = 3
        private const val COUNTDOWN_DURATION_MS = COUNTDOWN_DURATION_SECONDS * 1000L
        private const val COUNTDOWN_INTERVAL_MS = 50L
        private const val ANIMATION_DURATION_MS = 500L
    }

    // App is for multi-finger use only - not sure how to support click events
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (selectionCompleteAndAnimationsDone) return true

        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

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
                            pointerId, x, y, color = FingerColors.pickRandomColor(activeFingers)
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
                        it.x = event.getX(i)
                        it.y = event.getY(i)
                    }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (selectionComplete) return true

                activeFingers.removeAll { it.id == pointerId }
                Log.d(TAG, "Removed finger $pointerId. Remaining: ${activeFingers.size}")

                if (activeFingers.size < 2 && selectionProcessStarted && !selectionCompleteAndAnimationsDone) {
                    // All fingers lifted *during* countdown or before selection is final
                    Log.d(TAG, "All fingers lifted before selection complete. Resetting process.")
                    internalResetProcess() // Resets the current attempt
                } else if (activeFingers.size >= 2) {
                    selectionProcessStarted = true
                    startCountdownTimer()
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startCountdownTimer() {
        if (activeFingers.size < 2 || selectionComplete) return

        audioManager.playBuildUp()
        isCountingDown = true
        countdownSecondsRemaining = COUNTDOWN_DURATION_SECONDS
        Log.d(TAG, "Starting countdown. Seconds: $countdownSecondsRemaining")

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(COUNTDOWN_DURATION_MS, COUNTDOWN_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                countDownProgress = 1f - (millisUntilFinished / COUNTDOWN_DURATION_MS.toFloat())
                countdownSecondsRemaining = ceil(millisUntilFinished / 1000.0).toInt()
                invalidate() // Keep invalidating for visual updates if any during tick
            }

            override fun onFinish() {
                Log.d(TAG, "Countdown Finished.")
                isCountingDown = false
                countdownSecondsRemaining = 0
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
        countDownProgress = 0f
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
            fingerToAnimate.isGlowing = true
            fingerToAnimate.glowAnimationProgress = 0f
            // play the final note for each finger
            audioManager.playFinalNote()

            val animator =
                ValueAnimator.ofFloat(0f, 1.0f).apply { // Animate the *additional* radius
                    duration = ANIMATION_DURATION_MS
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { animation ->
                        fingerToAnimate.glowAnimationProgress = animation.animatedValue as Float
                        invalidate()
                    }
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            Log.d(TAG, "Animation ended for finger ${fingerToAnimate.id}")
                            fingerToAnimate.isGlowing = false
                            fingerToAnimate.glowAnimationProgress = 0f
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
                it.isGlowing = false
            } // Ensure all are marked as not animating
            onAllAnimationsCompleteListener?.invoke()
            invalidate() // Final draw of the completed state
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw all active fingers (those still on screen or part of the selection)
        val fingersToDraw = if (selectionComplete) assignedNumbersOrder else activeFingers

        val progressAtStart: Float? = countDownProgress
        var glowAnimationProgressOverride: Float? = null
        if (!selectionComplete && progressAtStart != null) {
            glowAnimationProgressOverride = progressAtStart
        }
        fingersToDraw.forEach { finger ->
            // Glow up to 20% of max glow during selection process.
            finger.draw(canvas, glowAnimationProgressOverride, glowMultiplier = 0.75f)
        }

        // Draw countdown timer text
        if (isCountingDown && !selectionCompleteAndAnimationsDone && activeFingers.isNotEmpty()) {
            val countdownText = countdownSecondsRemaining.toString()
            val xPos = width / 2f
            val yPos =
                height / 2f - (countdownTextPaint.descent() + countdownTextPaint.ascent()) / 2f
            canvas.drawText(countdownText, xPos, yPos, countdownTextPaint)
        }
    }

    private fun internalResetProcess() {
        Log.d(TAG, "internalResetProcess called.")
        countdownTimer?.cancel()
        audioManager.stopAny()
        // Stop any ongoing animations (more robust animator cancellation might be needed for complex cases)
        assignedNumbersOrder.forEach {
            it.isGlowing = false
            it.glowAnimationProgress = 0f
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
        selectionCompleteAndAnimationsDone = false

        internalResetProcess()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "onDetachedFromWindow")
        countdownTimer?.cancel()
        audioManager.release()
    }
}
