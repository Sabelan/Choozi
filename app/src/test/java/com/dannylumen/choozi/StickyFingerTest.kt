package com.dannylumen.choozi

import android.graphics.Color
import com.dannylumen.choozi.ui.shared.FingerPoint
import org.junit.Assert.*
import org.junit.Test

class StickyFingerTest {

    @Test
    fun testFingerPointStickyPropertyDefaultsToFalse() {
        val finger = FingerPoint(
            id = 1,
            x = 100f,
            y = 200f,
            color = Color.RED
        )
        assertFalse("FingerPoint should not be sticky by default", finger.isSticky)
    }

    @Test
    fun testFingerPointStickyCanBeToggled() {
        val finger = FingerPoint(
            id = -100,
            x = 150f,
            y = 250f,
            color = Color.BLUE,
            isSticky = true
        )
        assertTrue("FingerPoint should be sticky", finger.isSticky)
        finger.isSticky = false
        assertFalse("FingerPoint should now be non-sticky", finger.isSticky)
    }

    @Test
    fun testStickyFingerPreservationOnClearNonSticky() {
        val fingers = mutableListOf(
            FingerPoint(id = 0, x = 50f, y = 50f, color = Color.RED, isSticky = false),
            FingerPoint(id = -100, x = 100f, y = 100f, color = Color.GREEN, isSticky = true),
            FingerPoint(id = 1, x = 200f, y = 200f, color = Color.BLUE, isSticky = false),
            FingerPoint(id = -101, x = 300f, y = 300f, color = Color.YELLOW, isSticky = true)
        )

        // When physical touches lift, remove all non-sticky fingers
        val nonSticky = fingers.filter { !it.isSticky }
        fingers.removeAll(nonSticky)

        assertEquals(2, fingers.size)
        assertTrue(fingers.all { it.isSticky })
        assertEquals(-100, fingers[0].id)
        assertEquals(-101, fingers[1].id)
    }

    @Test
    fun testStickyFingerHitDetectionMath() {
        val stickyFinger = FingerPoint(
            id = -100,
            x = 200f,
            y = 300f,
            color = Color.RED,
            fingerRadius = 150f,
            isSticky = true
        )

        val hitThreshold = stickyFinger.fingerRadius * 1.3f // 195f

        // Touch directly on center
        val distCenter = kotlin.math.hypot(200f - stickyFinger.x, 300f - stickyFinger.y)
        assertTrue(distCenter <= hitThreshold)

        // Touch slightly offset within radius
        val distNear = kotlin.math.hypot(250f - stickyFinger.x, 350f - stickyFinger.y)
        assertTrue(distNear <= hitThreshold)

        // Touch far away
        val distFar = kotlin.math.hypot(500f - stickyFinger.x, 600f - stickyFinger.y)
        assertFalse(distFar <= hitThreshold)
    }

    @Test
    fun testDoubleTapMath() {
        val tap1X = 100f
        val tap1Y = 100f
        val tap1Time = 1000L

        val doubleTapSlop = 150f
        val doubleTapTimeout = 400L

        // Valid second tap: 150ms later, 30px away
        val tap2X = 120f
        val tap2Y = 110f
        val tap2Time = 1150L
        val dt = tap2Time - tap1Time
        val dist = kotlin.math.hypot(tap2X - tap1X, tap2Y - tap1Y)
        assertTrue(dt <= doubleTapTimeout && dist <= doubleTapSlop)

        // Invalid second tap: too late (500ms)
        val tapLateTime = 1600L
        val dtLate = tapLateTime - tap1Time
        assertFalse(dtLate <= doubleTapTimeout)

        // Invalid second tap: too far (300px)
        val tapFarX = 400f
        val tapFarY = 100f
        val distFar = kotlin.math.hypot(tapFarX - tap1X, tapFarY - tap1Y)
        assertFalse(distFar <= doubleTapSlop)
    }
}
