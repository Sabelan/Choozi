package com.dannylumen.choozi.ui.shared

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.hypot

/**
 * Handles touch interactions for Choozi's selection views, supporting persistent "sticky fingers"
 * when enabled in Accessibility settings.
 *
 * Sticky finger behavior:
 * - Double-tap on empty space creates a sticky finger that remains on the screen when lifted.
 * - Single-tapping on an existing sticky finger removes it.
 * - Dragging a sticky finger repositions it on the screen.
 * - Physical touches can be used alongside sticky fingers.
 */
class StickyFingerTouchHelper(
    private val context: Context,
    private val fingers: MutableList<FingerPoint>,
    private val createNewFinger: (id: Int, x: Float, y: Float) -> FingerPoint,
    private val onFingerAdded: (FingerPoint) -> Unit = {},
    private val onFingerRemoved: (FingerPoint) -> Unit = {},
    private val onFingersChanged: () -> Unit = {}
) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat().coerceAtLeast(30f)
    private val doubleTapSlop = 150f
    private val doubleTapTimeoutMs = 400L
    private val tapTimeoutMs = 350L

    private data class TapRecord(val x: Float, val y: Float, val timeMs: Long)
    private var lastTap: TapRecord? = null

    private data class DownPointerInfo(
        val startX: Float,
        val startY: Float,
        val downTimeMs: Long,
        var hasMoved: Boolean = false,
        val touchedStickyFinger: FingerPoint? = null,
        var createdStickyFinger: FingerPoint? = null
    )

    private val activePointers = mutableMapOf<Int, DownPointerInfo>()
    private var nextStickyId = -100

    /**
     * Resets internal gesture state. Call when the game round is reset.
     */
    fun reset() {
        activePointers.clear()
        lastTap = null
    }

    /**
     * Handles a motion event from View.onTouchEvent.
     * @return true if the event was handled.
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        val isStickyEnabled = SettingsManager.isStickyFingersEnabled(context)
        val action = event.actionMasked
        val now = SystemClock.uptimeMillis()

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                if (isStickyEnabled) {
                    // 1. Check if touching an existing sticky finger
                    val hitSticky = fingers.find { it.isSticky && hypot(x - it.x, y - it.y) <= it.fingerRadius * 1.3f }
                    if (hitSticky != null) {
                        activePointers[pointerId] = DownPointerInfo(
                            startX = x,
                            startY = y,
                            downTimeMs = now,
                            hasMoved = false,
                            touchedStickyFinger = hitSticky
                        )
                        return true
                    }

                    // 2. Check for double tap on empty area
                    val prevTap = lastTap
                    val isDoubleTap = prevTap != null &&
                            (now - prevTap.timeMs) <= doubleTapTimeoutMs &&
                            hypot(x - prevTap.x, y - prevTap.y) <= doubleTapSlop

                    if (isDoubleTap) {
                        lastTap = null
                        val stickyId = nextStickyId--
                        val stickyFinger = createNewFinger(stickyId, x, y).apply { isSticky = true }
                        fingers.add(stickyFinger)
                        activePointers[pointerId] = DownPointerInfo(
                            startX = x,
                            startY = y,
                            downTimeMs = now,
                            hasMoved = false,
                            createdStickyFinger = stickyFinger
                        )
                        onFingerAdded(stickyFinger)
                        onFingersChanged()
                        return true
                    }
                }

                // 3. Normal finger down
                if (fingers.none { it.id == pointerId && !it.isSticky }) {
                    val normalFinger = createNewFinger(pointerId, x, y).apply { isSticky = false }
                    fingers.add(normalFinger)
                    activePointers[pointerId] = DownPointerInfo(
                        startX = x,
                        startY = y,
                        downTimeMs = now,
                        hasMoved = false
                    )
                    onFingerAdded(normalFinger)
                    onFingersChanged()
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val px = event.getX(i)
                    val py = event.getY(i)
                    val info = activePointers[id]

                    if (info != null) {
                        if (hypot(px - info.startX, py - info.startY) > touchSlop) {
                            info.hasMoved = true
                        }
                        when {
                            info.touchedStickyFinger != null -> {
                                info.touchedStickyFinger.x = px
                                info.touchedStickyFinger.y = py
                            }
                            info.createdStickyFinger != null -> {
                                info.createdStickyFinger?.x = px
                                info.createdStickyFinger?.y = py
                            }
                            else -> {
                                val normalFinger = fingers.find { it.id == id && !it.isSticky }
                                normalFinger?.let {
                                    it.x = px
                                    it.y = py
                                }
                            }
                        }
                    }
                }
                onFingersChanged()
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val upX = event.getX(pointerIndex)
                val upY = event.getY(pointerIndex)

                handlePointerUp(pointerId, upX, upY, now, isStickyEnabled)
                onFingersChanged()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val upX = event.getX(pointerIndex)
                val upY = event.getY(pointerIndex)

                handlePointerUp(pointerId, upX, upY, now, isStickyEnabled)

                // Clean up any remaining non-sticky fingers since all physical touches lifted
                val nonStickyFingers = fingers.filter { !it.isSticky }
                if (nonStickyFingers.isNotEmpty()) {
                    fingers.removeAll(nonStickyFingers)
                    nonStickyFingers.forEach { onFingerRemoved(it) }
                }
                activePointers.clear()
                onFingersChanged()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                val nonStickyFingers = fingers.filter { !it.isSticky }
                fingers.removeAll(nonStickyFingers)
                nonStickyFingers.forEach { onFingerRemoved(it) }
                activePointers.clear()
                lastTap = null
                onFingersChanged()
                return true
            }
        }
        return false
    }

    private fun handlePointerUp(
        pointerId: Int,
        upX: Float,
        upY: Float,
        now: Long,
        isStickyEnabled: Boolean
    ) {
        val info = activePointers.remove(pointerId)

        if (info != null && isStickyEnabled) {
            when {
                info.touchedStickyFinger != null -> {
                    val isTapOnSticky = !info.hasMoved && (now - info.downTimeMs) <= tapTimeoutMs
                    if (isTapOnSticky) {
                        // Tapping on existing sticky finger removes it!
                        val toRemove = info.touchedStickyFinger
                        fingers.remove(toRemove)
                        onFingerRemoved(toRemove)
                    }
                    return
                }
                info.createdStickyFinger != null -> {
                    // Newly created sticky finger stays on screen!
                    lastTap = null
                    return
                }
            }
        }

        // Normal physical touch lifting
        if (info != null && isStickyEnabled) {
            val isTap = !info.hasMoved && (now - info.downTimeMs) <= tapTimeoutMs
            if (isTap) {
                lastTap = TapRecord(upX, upY, now)
            }
        }

        val normalFinger = fingers.find { it.id == pointerId && !it.isSticky }
        if (normalFinger != null) {
            fingers.remove(normalFinger)
            onFingerRemoved(normalFinger)
        }
    }
}
