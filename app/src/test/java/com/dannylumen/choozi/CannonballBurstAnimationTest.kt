package com.dannylumen.choozi

import com.dannylumen.choozi.theme.CannonballBurstAnimation
import org.junit.Assert.*
import org.junit.Test

class CannonballBurstAnimationTest {

    @Test
    fun testInitialStateNotRunning() {
        val animation = CannonballBurstAnimation()
        assertFalse("Animation should initially not be running", animation.isRunning)
    }

    @Test
    fun testStartTargetedSalvoEmptyTargets() {
        val animation = CannonballBurstAnimation()
        var completed = false
        animation.startTargetedSalvo(
            originX = 100f,
            originY = 100f,
            targets = emptyList<CannonballBurstAnimation.Target<Int>>(),
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
        val animation = CannonballBurstAnimation()
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
        val animation = CannonballBurstAnimation()
        animation.start(
            x = 100f,
            y = 100f,
            fingerRadius = 50f,
            onUpdate = {}
        )

        assertTrue("Animation should be running after start", animation.isRunning)
        animation.cancel()
        assertFalse("Animation should not be running after cancel", animation.isRunning)
    }

    @Test
    fun testStartTargetedSalvoMultipleTargetsRunningAndCancel() {
        val animation = CannonballBurstAnimation()
        val targets = listOf(
            CannonballBurstAnimation.Target(id = 1, x = 200f, y = 300f),
            CannonballBurstAnimation.Target(id = 2, x = 400f, y = 500f),
            CannonballBurstAnimation.Target(id = 3, x = 150f, y = 700f)
        )

        animation.startTargetedSalvo(
            originX = 100f,
            originY = 100f,
            targets = targets,
            fingerRadius = 50f,
            onTargetHit = {},
            onAllComplete = {},
            onUpdate = {}
        )

        assertTrue("Salvo with multiple targets should be running", animation.isRunning)
        animation.cancel()
        assertFalse("Salvo should stop running after cancel", animation.isRunning)
    }

    @Test
    fun testStartTargetedRunningAndCancel() {
        val animation = CannonballBurstAnimation()
        animation.startTargeted(
            originX = 100f,
            originY = 100f,
            targetX = 300f,
            targetY = 400f,
            fingerRadius = 50f,
            durationMs = 500L,
            onHit = {},
            onComplete = {},
            onUpdate = {}
        )

        assertTrue("Targeted shot should be running", animation.isRunning)
        animation.cancel()
        assertFalse("Targeted shot should stop running after cancel", animation.isRunning)
    }
}
