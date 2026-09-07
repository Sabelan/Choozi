package com.dannylumen.choozi.ui.shared

import android.content.Context
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import com.dannylumen.choozi.R
import kotlin.math.ceil

/**
 * Manages the automatic reset countdown timer on reset buttons across game modes.
 *
 * When activated via [startAutoReset], reads the user's configured timer from [SettingsManager]:
 * - If 0 (None): shows the button with standard "Reset" text and does not countdown.
 * - If > 0: displays a live countdown on the second line (e.g. "Reset\nAuto-reset in 5s")
 *   and automatically invokes [onReset] when expired.
 *
 * Provides [performReset] for manual clicks (cancelling any running timer)
 * and [cancel] to stop ongoing timers.
 */
class AutoResetController(
    private val button: Button,
    private val onReset: () -> Unit
) {
    private var countDownTimer: CountDownTimer? = null

    /**
     * Shows the reset button and starts the auto-reset countdown if configured (> 0 seconds).
     */
    fun startAutoReset(context: Context) {
        cancel()
        val seconds = SettingsManager.getAutoResetSeconds(context)
        button.visibility = View.VISIBLE

        if (seconds <= 0) {
            button.text = context.getString(R.string.reset_button)
            return
        }

        updateButtonText(context, seconds)

        // Add 100ms grace period so the initial second displays fully
        val totalDurationMs = seconds * 1000L + 100L
        val intervalMs = 250L

        countDownTimer = object : CountDownTimer(totalDurationMs, intervalMs) {
            private var lastDisplayedSecond: Int = seconds

            override fun onTick(millisUntilFinished: Long) {
                val remainingSeconds = ceil(millisUntilFinished / 1000.0).toInt().coerceIn(1, seconds)
                if (remainingSeconds != lastDisplayedSecond) {
                    lastDisplayedSecond = remainingSeconds
                    updateButtonText(context, remainingSeconds)
                }
            }

            override fun onFinish() {
                cancel()
                onReset()
            }
        }.start()
    }

    private fun updateButtonText(context: Context, secondsLeft: Int) {
        button.text = context.getString(R.string.reset_button_with_countdown, secondsLeft)
    }

    /**
     * Cancels any active countdown and restores standard button text.
     */
    fun cancel() {
        countDownTimer?.cancel()
        countDownTimer = null
        button.text = button.context.getString(R.string.reset_button)
    }

    /**
     * Handles manual reset click: cancels timer and invokes reset callback.
     */
    fun performReset() {
        cancel()
        onReset()
    }
}
