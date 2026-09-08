package com.dannylumen.choozi.theme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.animation.DecelerateInterpolator
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Handles the cyberpunk theme laser beam animation:
 * - Radial laser bursts (e.g. Teams mode, final Order mech): fires high-speed laser bolts in 8 directions.
 * - Targeted laser shots (e.g. Select mode, Order mode): fires directed energy beams from an origin mech
 *   to target mechs with muzzle charge flares, traveling laser bolts with white cores, and impact shockwave explosions.
 */
class LaserBurstAnimation {

    data class Target<T>(
        val id: T,
        val x: Float,
        val y: Float
    )

    val isRunning: Boolean
        get() = activeBursts.isNotEmpty()

    private val laserCorePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
        strokeWidth = 3.5f
    }

    private val laserMidPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val laserOuterPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val headGlowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val sparkPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val sparkLinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.5f
    }

    private val shockwavePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val flarePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private data class LaserBolt(
        val angleRad: Float,
        val maxDistance: Float,
        val color: Int,
        val sparkOffsets: List<Float>
    )

    private class ActiveBurst(
        val originX: Float,
        val originY: Float,
        val baseFingerRadius: Float,
        val bolts: List<LaserBolt>,
        val animator: ValueAnimator?,
        var progress: Float = 0f,
        var hitTriggered: Boolean = false,
        val onHit: (() -> Unit)? = null,
        val onComplete: (() -> Unit)? = null
    )

    private val activeBursts = mutableListOf<ActiveBurst>()

    /**
     * Radial burst in 8 directions (used by Team mode and fallback).
     */
    fun start(
        x: Float,
        y: Float,
        fingerRadius: Float,
        color: Int = 0xFF00F0FF.toInt(),
        onUpdate: () -> Unit
    ) {
        val numBolts = 8
        val baseDistance = fingerRadius * 3.4f
        val bolts = mutableListOf<LaserBolt>()

        val colors = listOf(
            0xFF00F0FF.toInt(), // Cyan
            0xFFFF007F.toInt(), // Hot Magenta
            0xFF39FF14.toInt(), // Lime
            0xFFB026FF.toInt()  // Violet
        )

        for (i in 0 until numBolts) {
            val baseAngle = (2.0 * Math.PI * i / numBolts).toFloat()
            val boltColor = colors[i % colors.size]
            val sparkOffsets = listOf(0.2f, 0.4f, 0.6f, 0.8f)

            bolts.add(
                LaserBolt(
                    angleRad = baseAngle,
                    maxDistance = baseDistance,
                    color = boltColor,
                    sparkOffsets = sparkOffsets
                )
            )
        }

        lateinit var burst: ActiveBurst

        val animator = ValueAnimator.ofFloat(0f, 1f)?.apply {
            duration = 550L
            interpolator = DecelerateInterpolator(1.4f)
            addUpdateListener { anim ->
                burst.progress = anim.animatedValue as Float
                onUpdate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    activeBursts.remove(burst)
                    onUpdate()
                }

                override fun onAnimationCancel(animation: Animator) {
                    activeBursts.remove(burst)
                }
            })
        }

        burst = ActiveBurst(
            originX = x,
            originY = y,
            baseFingerRadius = fingerRadius,
            bolts = bolts,
            animator = animator
        )

        activeBursts.add(burst)
        animator?.start()
    }

    /**
     * Targeted laser beam shot fired from origin directly to target (used by Order mode).
     */
    fun startTargeted(
        originX: Float,
        originY: Float,
        targetX: Float,
        targetY: Float,
        fingerRadius: Float,
        color: Int = 0xFF00F0FF.toInt(),
        durationMs: Long? = null,
        onHit: (() -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        onUpdate: () -> Unit
    ) {
        val dx = targetX - originX
        val dy = targetY - originY
        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (distance < 1f) {
            onHit?.invoke()
            onComplete?.invoke()
            onUpdate()
            return
        }

        val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
        val sparkOffsets = listOf(0.15f, 0.35f, 0.55f, 0.75f, 0.90f)

        val bolt = LaserBolt(
            angleRad = angle,
            maxDistance = distance,
            color = color,
            sparkOffsets = sparkOffsets
        )

        val flightDuration = durationMs ?: (380L + (distance / 4f).toLong()).coerceIn(420L, 650L)

        lateinit var burst: ActiveBurst

        val animator = ValueAnimator.ofFloat(0f, 1f)?.apply {
            duration = flightDuration
            interpolator = DecelerateInterpolator(1.4f)
            addUpdateListener { anim ->
                val p = anim.animatedValue as Float
                burst.progress = p
                if (p >= 0.70f && !burst.hitTriggered) {
                    burst.hitTriggered = true
                    burst.onHit?.invoke()
                }
                onUpdate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!burst.hitTriggered) {
                        burst.hitTriggered = true
                        burst.onHit?.invoke()
                    }
                    burst.onComplete?.invoke()
                    activeBursts.remove(burst)
                    onUpdate()
                }

                override fun onAnimationCancel(animation: Animator) {
                    activeBursts.remove(burst)
                }
            })
        }

        burst = ActiveBurst(
            originX = originX,
            originY = originY,
            baseFingerRadius = fingerRadius,
            bolts = listOf(bolt),
            animator = animator,
            onHit = onHit,
            onComplete = onComplete
        )

        activeBursts.add(burst)
        animator?.start()
    }

    /**
     * Targeted salvo of lasers fired from origin mech to multiple target mechs simultaneously (used by Select mode).
     */
    fun <T> startTargetedSalvo(
        originX: Float,
        originY: Float,
        targets: List<Target<T>>,
        fingerRadius: Float,
        color: Int = 0xFF00F0FF.toInt(),
        onTargetHit: ((targetId: T) -> Unit)? = null,
        onAllComplete: (() -> Unit)? = null,
        onUpdate: () -> Unit
    ) {
        if (targets.isEmpty()) {
            onAllComplete?.invoke()
            onUpdate()
            return
        }

        var remainingShots = targets.size

        for (target in targets) {
            startTargeted(
                originX = originX,
                originY = originY,
                targetX = target.x,
                targetY = target.y,
                fingerRadius = fingerRadius,
                color = color,
                onHit = {
                    onTargetHit?.invoke(target.id)
                },
                onComplete = {
                    remainingShots--
                    if (remainingShots <= 0) {
                        onAllComplete?.invoke()
                    }
                },
                onUpdate = onUpdate
            )
        }
    }

    fun cancel() {
        val burstsToCancel = ArrayList(activeBursts)
        activeBursts.clear()
        for (burst in burstsToCancel) {
            burst.animator?.cancel()
        }
    }

    fun draw(canvas: Canvas) {
        if (activeBursts.isEmpty()) return

        val bursts = ArrayList(activeBursts)

        for (burst in bursts) {
            val p = burst.progress
            val originX = burst.originX
            val originY = burst.originY
            val baseFingerRadius = burst.baseFingerRadius

            // 1. Initial Muzzle Energy Discharge Flare at mech origin (first 28% of time)
            if (p < 0.28f) {
                val flareAge = p / 0.28f
                val flareFade = 1f - flareAge
                val flareRadius = baseFingerRadius * (0.6f + flareAge * 0.8f)

                // Outer neon aura flare
                flarePaint.color = 0xFF00F0FF.toInt()
                flarePaint.alpha = (flareFade * 140).toInt().coerceIn(0, 255)
                canvas.drawCircle(originX, originY, flareRadius, flarePaint)

                // Mid bright flare
                flarePaint.alpha = (flareFade * 220).toInt().coerceIn(0, 255)
                canvas.drawCircle(originX, originY, flareRadius * 0.5f, flarePaint)

                // White core flash
                flarePaint.color = Color.WHITE
                flarePaint.alpha = (flareFade * 255).toInt().coerceIn(0, 255)
                canvas.drawCircle(originX, originY, flareRadius * 0.25f, flarePaint)
            }

            // 2. Draw Flying Laser Bolts
            for (bolt in burst.bolts) {
                val cosA = cos(bolt.angleRad.toDouble()).toFloat()
                val sinA = sin(bolt.angleRad.toDouble()).toFloat()
                val maxDist = bolt.maxDistance

                // High speed beam: Head advances rapidly, tail follows
                val headP = min(1.0f, p * 1.35f)
                val tailP = max(0.0f, p * 1.35f - 0.38f)

                if (headP > tailP && p < 0.98f) {
                    val fadeFactor = if (p > 0.82f) (1f - (p - 0.82f) / 0.16f).coerceIn(0f, 1f) else 1f

                    val startX = originX + cosA * (maxDist * tailP)
                    val startY = originY + sinA * (maxDist * tailP)
                    val endX = originX + cosA * (maxDist * headP)
                    val endY = originY + sinA * (maxDist * headP)

                    // Layer A: Outer Neon Beam Glow
                    laserOuterPaint.color = bolt.color
                    laserOuterPaint.strokeWidth = 20f
                    laserOuterPaint.alpha = (fadeFactor * 100).toInt().coerceIn(0, 255)
                    canvas.drawLine(startX, startY, endX, endY, laserOuterPaint)

                    // Layer B: Mid Laser Glow
                    laserMidPaint.color = bolt.color
                    laserMidPaint.strokeWidth = 9f
                    laserMidPaint.alpha = (fadeFactor * 220).toInt().coerceIn(0, 255)
                    canvas.drawLine(startX, startY, endX, endY, laserMidPaint)

                    // Layer C: White Laser Core
                    laserCorePaint.alpha = (fadeFactor * 255).toInt().coerceIn(0, 255)
                    canvas.drawLine(startX, startY, endX, endY, laserCorePaint)

                    // Laser Head Glowing Bead
                    headGlowPaint.color = bolt.color
                    headGlowPaint.alpha = (fadeFactor * 130).toInt().coerceIn(0, 255)
                    canvas.drawCircle(endX, endY, 15f, headGlowPaint)

                    headGlowPaint.alpha = (fadeFactor * 240).toInt().coerceIn(0, 255)
                    canvas.drawCircle(endX, endY, 7f, headGlowPaint)

                    headGlowPaint.color = Color.WHITE
                    headGlowPaint.alpha = (fadeFactor * 255).toInt().coerceIn(0, 255)
                    canvas.drawCircle(endX, endY, 3.5f, headGlowPaint)
                }

                // 3. Impact Energy Explosion & Shockwave at target point (when p >= 0.70f)
                if (p >= 0.70f) {
                    val impactAge = (p - 0.70f) / 0.30f
                    val impactFade = (1f - impactAge).coerceIn(0f, 1f)
                    val targetX = originX + cosA * maxDist
                    val targetY = originY + sinA * maxDist

                    // Primary shockwave ring
                    val shockRadius1 = baseFingerRadius * (0.35f + impactAge * 1.8f)
                    shockwavePaint.color = bolt.color
                    shockwavePaint.strokeWidth = (1f - impactAge) * 7f + 1.5f
                    shockwavePaint.alpha = (impactFade * 230).toInt().coerceIn(0, 255)
                    canvas.drawCircle(targetX, targetY, shockRadius1, shockwavePaint)

                    // Secondary inner fast ring
                    if (impactAge >= 0.12f) {
                        val innerAge = (impactAge - 0.12f) / 0.88f
                        val shockRadius2 = baseFingerRadius * (0.2f + innerAge * 1.4f)
                        shockwavePaint.color = Color.WHITE
                        shockwavePaint.strokeWidth = (1f - innerAge) * 4f + 1f
                        shockwavePaint.alpha = ((1f - innerAge) * 190).toInt().coerceIn(0, 255)
                        canvas.drawCircle(targetX, targetY, shockRadius2, shockwavePaint)
                    }

                    // Radiating Cyber Energy Sparks
                    val numSparks = 8
                    for (s in 0 until numSparks) {
                        val sparkAngle = bolt.angleRad + (Math.PI.toFloat() * 0.5f) + (s.toFloat() * 2f * Math.PI.toFloat() / numSparks)
                        val sparkDist1 = baseFingerRadius * (0.2f + impactAge * 0.6f)
                        val sparkDist2 = baseFingerRadius * (0.35f + impactAge * 1.8f)

                        val sx1 = targetX + cos(sparkAngle.toDouble()).toFloat() * sparkDist1
                        val sy1 = targetY + sin(sparkAngle.toDouble()).toFloat() * sparkDist1
                        val sx2 = targetX + cos(sparkAngle.toDouble()).toFloat() * sparkDist2
                        val sy2 = targetY + sin(sparkAngle.toDouble()).toFloat() * sparkDist2

                        sparkLinePaint.color = bolt.color
                        sparkLinePaint.alpha = (impactFade * 240).toInt().coerceIn(0, 255)
                        canvas.drawLine(sx1, sy1, sx2, sy2, sparkLinePaint)

                        sparkPaint.color = Color.WHITE
                        sparkPaint.alpha = (impactFade * 255).toInt().coerceIn(0, 255)
                        canvas.drawCircle(sx2, sy2, 3.0f, sparkPaint)
                    }
                }
            }
        }
    }
}
