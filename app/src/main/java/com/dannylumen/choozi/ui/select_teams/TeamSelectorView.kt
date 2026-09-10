package com.dannylumen.choozi.ui.select_teams

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.dannylumen.choozi.theme.CannonballBurstAnimation
import com.dannylumen.choozi.theme.LaserBurstAnimation
import com.dannylumen.choozi.theme.MagicBurstAnimation
import com.dannylumen.choozi.theme.SelectionAnimationEffect
import com.dannylumen.choozi.theme.ThemeManager
import com.dannylumen.choozi.ui.shared.AudioManager
import com.dannylumen.choozi.ui.shared.FingerColors
import com.dannylumen.choozi.ui.shared.FingerPoint
import com.dannylumen.choozi.ui.shared.UiUtils
import kotlin.math.ceil

class TeamSelectorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fingers = mutableListOf<FingerPoint>()
    private val cannonballBurstAnimation = CannonballBurstAnimation()
    private val laserBurstAnimation = LaserBurstAnimation()
    private val magicBurstAnimation = MagicBurstAnimation()
    private var countdownSeconds: Int = 0
    private var countDownProgress: Float? = null // For initial glow before selection
    private var countDownTimer: CountDownTimer? = null
    private var timerRunning = false
    private var selectionDone = false // True when teams are assigned & animation starts
    private var teamsAssignedAndAnimationsDone = false // True when team animations are complete

    private var teamAnimationOrder: List<Int> = emptyList()
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
    var onInteractionStateChangeListener: ((hasFingers: Boolean, isEndState: Boolean) -> Unit)? = null
    var onActiveFingerCountChangedListener: ((fingerCount: Int, isEndState: Boolean) -> Unit)? = null

    private fun notifyActiveFingerCountChanged() {
        val count = fingers.size
        val isEndState = selectionDone || teamsAssignedAndAnimationsDone
        onActiveFingerCountChangedListener?.invoke(count, isEndState)
        onInteractionStateChangeListener?.invoke(count > 0, isEndState)
    }

    private val audioManager = AudioManager(context)
    private val countdownTextPaint = UiUtils.getCountdownTextPaint(context)

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

    private val touchHelper = com.dannylumen.choozi.ui.shared.StickyFingerTouchHelper(
        context = context,
        fingers = fingers,
        createNewFinger = { id, x, y ->
            val currentTheme = com.dannylumen.choozi.theme.ThemeManager.getCurrentTheme(context)
            val sprite = currentTheme.getSpriteForFinger(fingers.size, fingers.mapNotNull { it.themeSprite })
            FingerPoint(id, x, y, FingerColors.NEUTRAL, themeSprite = sprite)
        },
        onFingerAdded = {
            possiblyStartSelectionProcess()
        },
        onFingerRemoved = {
            if (fingers.size < numberOfTeams && timerRunning) {
                cancelSelectionTimer()
            } else {
                possiblyStartSelectionProcess()
            }
        },
        onFingersChanged = {
            notifyActiveFingerCountChanged()
            invalidate()
        }
    )

    // App is for multi-finger use only - not sure how to support click events
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (selectionDone || teamsAssignedAndAnimationsDone) return false
        return touchHelper.onTouchEvent(event)
    }

    private fun startSelectionTimer() {
        if (selectionDone || teamsAssignedAndAnimationsDone) return
        if (fingers.size < numberOfTeams) {
            Log.d("TeamSelectorView", "Attempted to start timer with less than 2 fingers.")
            cancelSelectionTimer() // Ensure timer is cancelled if not enough players
            return
        }

        countDownTimer?.cancel() // Cancel previous timer without stopping ongoing audio

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
                Log.d("SelectionTiming", "TeamSelector countdown finished at ${System.currentTimeMillis()} ms")
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
        notifyActiveFingerCountChanged()

        // Shuffle fingers to randomize team assignment
        val shuffledFingers = fingers.shuffled()
        val shuffledColors = FingerColors.TEAM_COLORS.shuffled()
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

            val currentTheme = ThemeManager.getCurrentTheme(context)
            if (currentTheme.selectionEffect == SelectionAnimationEffect.PIRATE_CANNONS) {
                fingers.filter { it.teamId == teamToAnimate }.forEach { finger ->
                    cannonballBurstAnimation.start(finger.x, finger.y, finger.fingerRadius) {
                        invalidate()
                    }
                }
            } else if (currentTheme.selectionEffect == SelectionAnimationEffect.CYBER_LASERS) {
                fingers.filter { it.teamId == teamToAnimate }.forEach { finger ->
                    laserBurstAnimation.start(finger.x, finger.y, finger.fingerRadius, finger.color) {
                        invalidate()
                    }
                }
            } else if (currentTheme.selectionEffect == SelectionAnimationEffect.FANTASY_MAGIC) {
                fingers.filter { it.teamId == teamToAnimate }.forEach { finger ->
                    magicBurstAnimation.start(finger.x, finger.y, finger.fingerRadius) {
                        invalidate()
                    }
                }
            }

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
            Log.d("SelectionTiming", "TeamSelector selection animations complete at ${System.currentTimeMillis()} ms")
            Log.d("TeamSelectorView", "All glow animations complete.")
            teamsAssignedAndAnimationsDone = true
            notifyActiveFingerCountChanged()
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

        val currentTheme = com.dannylumen.choozi.theme.ThemeManager.getCurrentTheme(context)
        currentTheme.background?.draw(context, canvas, width, height)
        if (currentTheme.background?.isAnimated == true || currentTheme.hasAnimatedSprites) {
            postInvalidateOnAnimation()
        }

        var initialGlowProgress: Float? = null
        if (timerRunning && !selectionDone && countDownProgress != null) {
            initialGlowProgress = countDownProgress
        }
        fingers.forEach { finger ->
            // If countdown is running and selection isn't done, apply a slight glow based on countDownProgress
            finger.draw(canvas, initialGlowProgress, context = context) // Pass the initial glow
        }

        // Draw cannonball burst animation if active
        if (cannonballBurstAnimation.isRunning) {
            cannonballBurstAnimation.draw(context, canvas)
            postInvalidateOnAnimation()
        }

        // Draw laser burst animation if active
        if (laserBurstAnimation.isRunning) {
            laserBurstAnimation.draw(canvas)
            postInvalidateOnAnimation()
        }

        // Draw magic burst animation if active
        if (magicBurstAnimation.isRunning) {
            magicBurstAnimation.draw(context, canvas)
            postInvalidateOnAnimation()
        }

        // Draw countdown timer text
        if (timerRunning && !selectionDone && countdownSeconds > 0) {
            val text = countdownSeconds.toString()
            val xPos = width / 2f
            val yPos =
                (height / 2f) - ((countdownTextPaint.descent() + countdownTextPaint.ascent()) / 2f)
            canvas.drawText(text, xPos, yPos, countdownTextPaint)
        }
    }

    fun resetSelectionProcess(clearFingers: Boolean = true) {
        audioManager.stopAny()
        cannonballBurstAnimation.cancel()
        laserBurstAnimation.cancel()
        magicBurstAnimation.cancel()
        teamAnimationAnimator?.cancel()
        countDownTimer?.cancel()

        fingers.forEach { it.resetAnimationStates() /* also resets teamId if you implement it there */ }
        // default clearing of fingers but it is optional in case outer view doesn't want to
        if (clearFingers) {
            touchHelper.reset()
            fingers.clear()
        }

        selectionDone = false
        teamsAssignedAndAnimationsDone = false
        timerRunning = false
        countdownSeconds = 0
        countDownProgress = null
        notifyActiveFingerCountChanged()

        invalidate()
        Log.d("TeamSelectorView", "Selection process reset.")
    }

    private val themeChangeListener = {
        invalidate()
        if (com.dannylumen.choozi.theme.ThemeManager.getCurrentTheme(context).background?.isAnimated == true) {
            postInvalidateOnAnimation()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        com.dannylumen.choozi.theme.ThemeManager.addThemeChangeListener(themeChangeListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        com.dannylumen.choozi.theme.ThemeManager.removeThemeChangeListener(themeChangeListener)
        countDownTimer?.cancel()
        teamAnimationAnimator?.cancel()
        cannonballBurstAnimation.cancel()
        laserBurstAnimation.cancel()
        magicBurstAnimation.cancel()
        audioManager.release()
    }
}
