package com.dannylumen.choozi

import com.dannylumen.choozi.theme.MagicBurstAnimation
import org.junit.Assert.*
import org.junit.Test

class MagicBurstAnimationTest {

    @Test
    fun testInitialStateNotRunning() {
        val animation = MagicBurstAnimation()
        assertFalse("Animation should initially not be running", animation.isRunning)
    }

    @Test
    fun testPickRandomElement() {
        val elements = mutableSetOf<MagicBurstAnimation.MagicElement>()
        for (i in 0 until 50) {
            val el = MagicBurstAnimation.pickRandomElement()
            elements.add(el)
            assertTrue(el == MagicBurstAnimation.MagicElement.FIRE || el == MagicBurstAnimation.MagicElement.ICE)
        }
        assertTrue("Should produce at least one fire and one ice across 50 rolls", elements.size >= 2)
    }

    @Test
    fun testStartTargetedSalvoEmptyTargets() {
        val animation = MagicBurstAnimation()
        var completed = false
        animation.startTargetedSalvo(
            originX = 100f,
            originY = 100f,
            targets = emptyList<MagicBurstAnimation.Target<Int>>(),
            fingerRadius = 50f,
            onAllComplete = {
                completed = true
            },
            onUpdate = {}
        )

        assertTrue("Empty targets should complete immediately", completed)
        assertFalse("Should not be running for empty targets", animation.isRunning)
    }

    @Test
    fun testStartTargetedZeroDistanceCompletesImmediately() {
        val animation = MagicBurstAnimation()
        var hit = false
        var completed = false
        animation.startTargeted(
            originX = 100f,
            originY = 100f,
            targetX = 100f,
            targetY = 100f,
            fingerRadius = 50f,
            onHit = { hit = true },
            onComplete = { completed = true },
            onUpdate = {}
        )

        assertTrue("Zero distance target should trigger hit immediately", hit)
        assertTrue("Zero distance target should trigger complete immediately", completed)
        assertFalse("Zero distance target should not leave animation running", animation.isRunning)
    }

    @Test
    fun testCancelClearsRunningState() {
        val animation = MagicBurstAnimation()
        animation.start(
            x = 100f,
            y = 100f,
            fingerRadius = 50f,
            element = MagicBurstAnimation.MagicElement.FIRE,
            onUpdate = {}
        )

        assertTrue("Animation should be running after start", animation.isRunning)
        animation.cancel()
        assertFalse("Animation should not be running after cancel", animation.isRunning)
    }

    @Test
    fun testIceElementRunningState() {
        val animation = MagicBurstAnimation()
        animation.start(
            x = 150f,
            y = 200f,
            fingerRadius = 60f,
            element = MagicBurstAnimation.MagicElement.ICE,
            onUpdate = {}
        )

        assertTrue("Ice animation should be running after start", animation.isRunning)
        animation.cancel()
        assertFalse("Ice animation should not be running after cancel", animation.isRunning)
    }
}
