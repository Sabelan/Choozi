package com.dannylumen.choozi.ui.select_ordering

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.dannylumen.choozi.ui.shared.AudioManager
import com.dannylumen.choozi.ui.shared.FingerColors
import com.dannylumen.choozi.ui.shared.FingerPoint
import com.dannylumen.choozi.ui.shared.UiUtils
import kotlin.math.ceil

class FingerOrderingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val activeFingers = mutableListOf<FingerPoint>()
    private val assignedNumbersOrder = mutableListOf<FingerPoint>() // For animation sequence

    // Precompute gradients so they aren't computed during onDraw
    private val gradients = mutableListOf<LinearGradient>()

    private var countdownTimer: CountDownTimer? = null
    private var isCountingDown = false
    private var countDownProgress: Float? = null
    private var selectionProcessStarted = false
    private var selectionComplete = false
    private var selectionCompleteAndAnimationsDone = false
    private var currentAnimatingFingerIndex = -1
    private var countdownSecondsRemaining: Int = 0

    private val countdownTextPaint = UiUtils.getCountdownTextPaint(context)

    // Listeners
    var onSelectionCompleteListener: (() -> Unit)? = null
    var onAllAnimationsCompleteListener: (() -> Unit)? = null

    // Audio Manager
    private val audioManager: AudioManager = AudioManager(context)

    // For drawing lines
    private val linePath = Path()
    private val linePaint = Paint().apply {
        strokeWidth = 20f // Adjust line thickness as needed
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    private var lineAnimationProgress = 0f // Progress of the entire line drawing animation
    private var currentLineSegmentIndex = 0 // Which segment of the line is currently being animated

    companion object {
        private const val TAG = "FingerOrderingView"
        private const val COUNTDOWN_DURATION_SECONDS = 3
        private const val COUNTDOWN_DURATION_MS = COUNTDOWN_DURATION_SECONDS * 1000L
        private const val COUNTDOWN_INTERVAL_MS = 50L
        private const val ANIMATION_DURATION_MS = 500L
        private const val LINE_ANIMATION_DURATION_PER_SEGMENT_MS =
            ANIMATION_DURATION_MS // We tie the line animation to the glow animation in time
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
                    internalResetProcess() // Resets the current selection process
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
                invalidate()
            }

            override fun onFinish() {
                Log.d(TAG, "Countdown Finished.")
                isCountingDown = false
                countdownSecondsRemaining = 0
                invalidate()

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

        gradients.clear()
        for (i in 0 until assignedNumbersOrder.size - 1) {
            val p1 = assignedNumbersOrder[i]
            val p2 = assignedNumbersOrder[i + 1]
            gradients.add(
                LinearGradient(
                    p1.x, p1.y, p2.x, p2.y, p1.color, p2.color, Shader.TileMode.CLAMP
                )
            )
        }

        onSelectionCompleteListener?.invoke()
        selectionComplete = true
        countDownProgress = 0f
        currentAnimatingFingerIndex = -1
        // Reset line-animations
        linePath.reset()
        lineAnimationProgress = 0f
        currentLineSegmentIndex = 0
        invalidate()
        startNextGlowAndLineAnimation()
    }

    private fun startNextGlowAndLineAnimation() {
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

            // Start the line segment building to the finger
            if (currentAnimatingFingerIndex < assignedNumbersOrder.size - 1) {
                startLineSegmentAnimation(currentAnimatingFingerIndex)
            }
            val glowAnimator = ValueAnimator.ofFloat(0f, 1.0f).apply {
                duration = ANIMATION_DURATION_MS
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    fingerToAnimate.glowAnimationProgress = animation.animatedValue as Float
                    invalidate()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        Log.d(TAG, "Glow Animation ended for finger ${fingerToAnimate.id}")
                        fingerToAnimate.isGlowing = false
                        fingerToAnimate.glowAnimationProgress = 0f
                        startNextGlowAndLineAnimation() // For the first finger, just proceed to next glow
                        invalidate()
                    }
                })
            }
            glowAnimator.start()
        } else {
            Log.d(TAG, "All glow animations and line animations complete.")
            selectionCompleteAndAnimationsDone = true
            assignedNumbersOrder.forEach {
                it.isGlowing = false
            } // Ensure all are marked as not animating
            onAllAnimationsCompleteListener?.invoke() // Moved to end of line animation sequence
            invalidate()
        }
    }

    private fun startLineSegmentAnimation(segmentIndex: Int) {
        if (segmentIndex >= assignedNumbersOrder.size - 1) {
            Log.d(
                TAG,
                "Incorrectly calling startLineSegmentAnimation with segment index $segmentIndex when there are only ${assignedNumbersOrder.size} fingers."
            )
            invalidate()
            return
        }

        currentLineSegmentIndex = segmentIndex
        val startFinger = assignedNumbersOrder[segmentIndex]
        val endFinger = assignedNumbersOrder[segmentIndex + 1]

        Log.d(
            TAG,
            "Starting line animation for segment $segmentIndex from finger ${startFinger.id} to ${endFinger.id}"
        )

        val lineAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = LINE_ANIMATION_DURATION_PER_SEGMENT_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                // This progress is for the current segment
                // We'll use it to partially draw the current segment in onDraw
                lineAnimationProgress = animation.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    Log.d(TAG, "Line segment $segmentIndex animation ended.")
                    // Add the full segment to the path for redraws
                    // The actual drawing of this full segment happens in onDraw
                    // We just need to ensure lineAnimationProgress is 1 for this segment to be fully considered
                    lineAnimationProgress = 1f // Ensure it's fully drawn
                    invalidate()
                }
            })
        }
        lineAnimator.start()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (selectionComplete && assignedNumbersOrder.size > 1) {
            linePath.reset() // Reset path for each draw to handle animation progress

            for (i in 0 until currentLineSegmentIndex + 1) { // Iterate up to and including the current animating segment
                if (i >= assignedNumbersOrder.size - 1) break // Bounds check

                val p1 = assignedNumbersOrder[i]
                val p2 = assignedNumbersOrder[i + 1]

                linePaint.shader = gradients[i]

                if (i < currentLineSegmentIndex) { // Fully draw completed segments
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint)
                } else if (i == currentLineSegmentIndex && lineAnimationProgress > 0f) {
                    // Partially draw the current animating segment
                    val currentX = p1.x + (p2.x - p1.x) * lineAnimationProgress
                    val currentY = p1.y + (p2.y - p1.y) * lineAnimationProgress
                    canvas.drawLine(p1.x, p1.y, currentX, currentY, linePaint)
                }
            }
            linePaint.shader = null // Clear shader
        }


        // Draw all active fingers (those still on screen or part of the selection)
        val fingersToDraw = if (selectionComplete) assignedNumbersOrder else activeFingers
        val progressAtStart: Float? = countDownProgress
        var glowAnimationProgressOverride: Float? = null
        if (!selectionComplete && progressAtStart != null) {
            glowAnimationProgressOverride = progressAtStart
        }
        fingersToDraw.forEach { finger ->
            // Glow up to 75% of max glow during selection process.
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
            it.assignedNumber = null
        }
        activeFingers.forEach {
            it.assignedNumber = null
        }

        isCountingDown = false
        selectionProcessStarted = false
        selectionComplete = false

        assignedNumbersOrder.clear()
        currentAnimatingFingerIndex = -1
        countdownSecondsRemaining = 0

        // Reset line variables
        linePath.reset()
        lineAnimationProgress = 0f
        currentLineSegmentIndex = 0
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
