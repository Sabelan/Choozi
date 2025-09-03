package com.example.boardgamerandomizer.ui.select_person

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
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
import com.example.boardgamerandomizer.ui.shared.FingerColors
import com.example.boardgamerandomizer.ui.shared.FingerPoint
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.random.Random

class FingerSelectorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fingers = mutableListOf<FingerPoint>()
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true // Good for circles
    }
    private var selectedFingerIndex: Int = -1
    private var countdownSeconds: Int = 0
    private var countDownProgress: Float? = null
    private var fingerRadius: Float = 120f
    private var countDownTimer: CountDownTimer? = null
    private var timerRunning = false
    private var selectionDone = false
    private var isRevealAnimationRunning = false

    var onSelectionCompleteListener: (() -> Unit)? = null
    var onTimerTickListener: ((secondsRemaining: Int) -> Unit)? = null
    var onTimerStartListener: (() -> Unit)? = null

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 150f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val revealPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private var revealAnimator: ValueAnimator? = null
    private var revealAnimationRadius: Float = 0f
    private var maxRevealRadius: Float = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (selectionDone || isRevealAnimationRunning) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val color = FingerColors.pickRandomColor(fingers)
                fingers.add(FingerPoint(pointerId, x, y, color))
                invalidate()
                startSelectionTimer()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (timerRunning) {
                    for (i in 0 until event.pointerCount) {
                        val pointerId = event.getPointerId(i)
                        val finger = fingers.find { it.id == pointerId }
                        finger?.let {
                            it.x = event.getX(i)
                            it.y = event.getY(i)
                        }
                    }
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                fingers.removeAll { it.id == pointerId }
                if (fingers.isEmpty() && timerRunning) {
                    cancelSelectionTimer()
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startSelectionTimer() {
        if (selectionDone || isRevealAnimationRunning) return
        // Cancel previous timer
        if (timerRunning) {
            cancelSelectionTimer()
        }
        onTimerStartListener?.invoke()
        timerRunning = true
        countdownSeconds = 3
        countDownTimer = object : CountDownTimer(3000, 50) {
            override fun onTick(millisUntilFinished: Long) {
                countDownProgress = (3000 - millisUntilFinished) / 3000.0f
                countdownSeconds = ceil(millisUntilFinished / 1000.0).toInt()
                onTimerTickListener?.invoke(countdownSeconds)
                invalidate()
            }

            override fun onFinish() {
                countdownSeconds = 0
                timerRunning = false
                selectRandomFingerAndAnimate()
                onTimerTickListener?.invoke(0)
            }
        }.start()
    }

    private fun cancelSelectionTimer() {
        countDownTimer?.cancel()
        timerRunning = false
        countdownSeconds = 0
        onTimerTickListener?.invoke(-1)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isRevealAnimationRunning || selectionDone) {
            // --- Reveal Animation or Final State ---
            if (selectedFingerIndex != -1 && fingers.indices.contains(selectedFingerIndex)) {
                val selectedFinger = fingers[selectedFingerIndex]
                revealPaint.color = selectedFinger.color // The color that expands

                // Draw the expanding circle
                canvas.drawCircle(
                    selectedFinger.x, selectedFinger.y, revealAnimationRadius, revealPaint
                )

                // NOW, ONLY DRAW THE SELECTED FINGER ON TOP OF THE REVEAL
                paint.color = selectedFinger.color // The selected finger's actual color
                selectedFinger.draw(canvas)

                // And its highlight ring
                paint.color = getContrastingColor(selectedFinger.color)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 15f
                canvas.drawCircle(selectedFinger.x, selectedFinger.y, fingerRadius + 10, paint)
                paint.style = Paint.Style.FILL // Reset style
            }
        } else {
            val progressAtStart = countDownProgress
            fingers.forEach { finger ->
                // Glow up to 20% of max glow during selection process.
                var glowAnimationProgressOverride: Float? = null
                if (!isRevealAnimationRunning && progressAtStart != null) {
                    glowAnimationProgressOverride = progressAtStart
                }
                finger.draw(canvas, glowAnimationProgressOverride)
            }

            fingers.forEach { finger ->
                finger.draw(canvas)
            }
        }

        // Draw countdown timer text (only if timer is running and no animation has started)
        if (timerRunning && !isRevealAnimationRunning && !selectionDone && countdownSeconds > 0) {
            val text = countdownSeconds.toString()
            val xPos = width / 2f
            val yPos = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(text, xPos, yPos, textPaint)
        }
    }

    private fun getContrastingColor(backgroundColor: Int): Int {
        val y =
            (299 * Color.red(backgroundColor) + 587 * Color.green(backgroundColor) + 114 * Color.blue(
                backgroundColor
            )) / 1000.0
        return if (y >= 128) Color.BLACK else Color.WHITE
    }

    private fun selectRandomFingerAndAnimate() {
        if (fingers.isNotEmpty()) {
            selectedFingerIndex = Random.Default.nextInt(fingers.size)
            val selectedFinger = fingers[selectedFingerIndex]

            // Calculate the maximum radius needed to cover the screen from the finger's position
            // This is the distance to the furthest corner
            val dx1 = selectedFinger.x
            val dy1 = selectedFinger.y
            val dx2 = width - selectedFinger.x
            val dy2 = height - selectedFinger.y

            maxRevealRadius = hypot(
                max(dx1, dx2).toDouble(), max(dy1, dy2).toDouble()
            ).toFloat()

            startRevealAnimation()
        } else {
            resetSelectionProcess()
        }
    }

    private fun startRevealAnimation() {
        isRevealAnimationRunning = true
        revealAnimationRadius = fingerRadius

        revealAnimator?.cancel()
        revealAnimator = ValueAnimator.ofFloat(revealAnimationRadius, maxRevealRadius).apply {
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                revealAnimationRadius = animation.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isRevealAnimationRunning = false
                    selectionDone = true
                    // Ensure the final radius covers everything
                    revealAnimationRadius = maxRevealRadius
                    onSelectionCompleteListener?.invoke()
                    invalidate()
                }

                override fun onAnimationCancel(animation: Animator) {
                    isRevealAnimationRunning = false;
                    // Optionally reset radius or snap to end based on desired behavior
                }
            })
        }
        revealAnimator?.start()
    }

    fun resetSelectionProcess() {
        revealAnimator?.cancel()
        countDownTimer?.cancel()

        fingers.clear()
        selectedFingerIndex = -1
        selectionDone = false
        timerRunning = false
        isRevealAnimationRunning = false
        revealAnimationRadius = 0f
        maxRevealRadius = 0f
        countdownSeconds = 0

        onTimerTickListener?.invoke(-1)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        countDownTimer?.cancel()
        revealAnimator?.cancel()
    }
}