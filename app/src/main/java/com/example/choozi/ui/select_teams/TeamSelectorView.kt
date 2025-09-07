package com.example.choozi.ui.select_teams

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
import com.example.choozi.ui.shared.AudioManager
import com.example.choozi.ui.shared.FingerColors
import com.example.choozi.ui.shared.FingerPoint
import kotlin.math.ceil

class TeamSelectorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fingers = mutableListOf<FingerPoint>()
    private var countdownSeconds: Int = 0
    private var countDownProgress: Float? = null // For initial glow before selection
    private var countDownTimer: CountDownTimer? = null
    private var timerRunning = false
    private var selectionDone = false // True when teams are assigned & animation starts
    private var teamsAssignedAndAnimationsDone = false // True when team animations are complete

    private var teamAnimationOrder: List<Int> = emptyList<Int>()
    private var teamAnimationIndex = -1
    var numberOfTeams: Int = 2 // Default to 2 teams
        set(value) {
            if (value in 2..4) { // Basic validation
                field = value
                Log.d("TeamSelectorView", "Number of teams set to: $field")
                // Potentially reset if selection is in progress and team count changes?
                // For now, assume it's set before fingers are placed or reset is called.
            } else {
                Log.w("TeamSelectorView", "Invalid number of teams: $value. Must be 2, 3, or 4.")
            }
        }

    var onTeamAssignmentCompleteListener: (() -> Unit)? = null // Teams assigned, animation starts
    var onAllTeamAnimationsCompleteListener: (() -> Unit)? = null // All team animations finished
    var onTimerStartListener: (() -> Unit)? = null

    private val audioManager = AudioManager(context)
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 150f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private var teamAnimationAnimator: ValueAnimator? = null

    companion object {
        private const val COUNTDOWN_DURATION_MS = 3000L // 3s reveal timer
        private const val TEAM_ANIMATION_DURATION_MS = 800L // Duration for team reveal
    }

    fun possiblyStartSelectionProcess() {
        if (fingers.size >= numberOfTeams) {
            startSelectionTimer()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (selectionDone || teamsAssignedAndAnimationsDone) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (selectionDone) return true // Don't add fingers if selection process (countdown done) has started

                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                // ALL FINGERS ARE NEUTRAL COLOR INITIALLY
                fingers.add(
                    FingerPoint(
                        pointerId, x, y, FingerColors.NEUTRAL
                    )
                )
                invalidate()
                possiblyStartSelectionProcess()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!selectionDone) {
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
                if (selectionDone) return true // Don't remove if selection already happened

                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                fingers.removeAll { it.id == pointerId }

                if (fingers.isEmpty() && timerRunning) {
                    cancelSelectionTimer()
                } else if (fingers.size < numberOfTeams && timerRunning) {
                    cancelSelectionTimer() // Cancel if not enough for teams
                    Log.d("TeamSelectorView", "Not enough fingers for teams, timer cancelled.")
                } else {
                    possiblyStartSelectionProcess()
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startSelectionTimer() {
        if (selectionDone || teamsAssignedAndAnimationsDone) return
        if (fingers.size < numberOfTeams) {
            Log.d("TeamSelectorView", "Attempted to start timer with less than 2 fingers.")
            cancelSelectionTimer() // Ensure timer is cancelled if not enough players
            return
        }

        cancelSelectionTimer() // Cancel previous timer

        audioManager.playBuildUp()
        onTimerStartListener?.invoke()
        timerRunning = true
        countdownSeconds = 3 // Or your desired countdown
        countDownTimer = object : CountDownTimer(COUNTDOWN_DURATION_MS, 50) {
            override fun onTick(millisUntilFinished: Long) {
                countDownProgress = 1f - (millisUntilFinished / COUNTDOWN_DURATION_MS.toFloat())
                countdownSeconds = ceil(millisUntilFinished / 1000.0).toInt()
                invalidate()
            }

            override fun onFinish() {
                countdownSeconds = 0
                timerRunning = false
                countDownProgress = 1f // Full progress for glow
                assignTeamsAndStartAnimation()
            }
        }.start()
        invalidate()
    }

    private fun cancelSelectionTimer() {
        countDownTimer?.cancel()
        audioManager.stopAny()
        timerRunning = false
        countdownSeconds = 0
        countDownProgress = null
        invalidate()
    }

    private fun assignTeamsAndStartAnimation() {
        if (fingers.size < 2) {
            Log.d("TeamSelectorView", "Not enough fingers to assign teams.")
            resetSelectionProcess() // Or handle this state differently
            return
        }
        selectionDone = true // Mark that the initial selection (countdown) is done
        audioManager.stopAny()

        // Shuffle fingers to randomize team assignment
        val shuffledFingers = fingers.shuffled()
        val shuffledColors = FingerColors.COLORS.shuffled()
        shuffledFingers.forEachIndexed { index, finger ->
            val teamId = index % numberOfTeams
            finger.teamId = teamId
            finger.color = shuffledColors[teamId]
        }

        onTeamAssignmentCompleteListener?.invoke() // Notify fragment teams are assigned

        teamAnimationOrder =
            (0 until numberOfTeams).toList().shuffled() // Animate teams in random order
        teamAnimationIndex = -1

        invalidate() // Initial draw with team colors before animation progresses

        startNextGlowAnimation()
    }

    private fun startNextGlowAnimation() {
        teamAnimationIndex++
        if (teamAnimationIndex < teamAnimationOrder.size) {
            val teamToAnimate = teamAnimationOrder[teamAnimationIndex]
            Log.d(
                "TeamSelectorView", "Starting animation for team $teamToAnimate"
            )
            // play the final note for each team
            audioManager.playFinalNote()
            fingers.forEach { finger ->
                finger.glowAnimationProgress = 0f
                finger.isGlowing = (finger.teamId == teamToAnimate)
            }

            val animator =
                ValueAnimator.ofFloat(0f, 1.0f).apply { // Animate the *additional* radius
                    duration = TEAM_ANIMATION_DURATION_MS
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { animation ->
                        fingers.forEach { finger ->
                            if (finger.teamId == teamToAnimate) {
                                finger.glowAnimationProgress = animation.animatedValue as Float
                            }
                        }
                        invalidate()
                    }
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            Log.d(
                                "TeamSelectorView", "Animation ended for finger $teamToAnimate"
                            )
                            invalidate()
                            startNextGlowAnimation() // Trigger next animation
                        }
                    })
                }
            animator.start()
        } else {
            Log.d("TeamSelectorView", "All glow animations complete.")
            teamsAssignedAndAnimationsDone = true
            fingers.forEach { finger ->
                finger.glowAnimationProgress = 0f
                finger.isGlowing = false
            }
            audioManager.stopAny()
            onAllTeamAnimationsCompleteListener?.invoke()
            invalidate() // Final draw of the completed state
        }
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var initialGlowProgress: Float? = null
        if (timerRunning && !selectionDone && countDownProgress != null) {
            initialGlowProgress = countDownProgress
        }
        fingers.forEach { finger ->
            // If countdown is running and selection isn't done, apply a slight glow based on countDownProgress
            finger.draw(canvas, initialGlowProgress) // Pass the initial glow
        }

        // Draw countdown timer text
        if (timerRunning && !selectionDone && countdownSeconds > 0) {
            val text = countdownSeconds.toString()
            val xPos = width / 2f
            val yPos = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(text, xPos, yPos, textPaint)
        }
    }

    fun resetSelectionProcess(clearFingers: Boolean = true) {
        audioManager.stopAny()
        teamAnimationAnimator?.cancel()
        countDownTimer?.cancel()

        fingers.forEach { it.resetAnimationStates() /* also resets teamId if you implement it there */ }
        // default clearing of fingers but it is optional in case outer view doesn't want to
        if (clearFingers) {
            fingers.clear()
        }

        selectionDone = false
        teamsAssignedAndAnimationsDone = false
        timerRunning = false
        countdownSeconds = 0
        countDownProgress = null

        invalidate()
        Log.d("TeamSelectorView", "Selection process reset.")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        countDownTimer?.cancel()
        teamAnimationAnimator?.cancel()
        audioManager.release()
    }
}
