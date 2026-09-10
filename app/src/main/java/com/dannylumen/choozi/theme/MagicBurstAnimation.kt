package com.dannylumen.choozi.theme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.animation.DecelerateInterpolator
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Handles fantasy theme magical spell animations:
 * - Randomly selects between Fire (Fireball / Flame Burst) and Ice (Frostbolt / Frost Nova) spells.
 * - Radial magical bursts (e.g. Teams mode, fallback): fires elemental spell bolts in 8 directions.
 * - Targeted spell shots (e.g. Select mode, Order mode): fires directed magical projectiles from a
 *   winning wizard/witch to target fingers, complete with casting runes, particle trails, and elemental impact explosions.
 */
class MagicBurstAnimation {

    enum class MagicElement {
        FIRE,
        ICE
    }

    data class Target<T>(
        val id: T,
        val x: Float,
        val y: Float
    )

    val isRunning: Boolean
        get() = activeBursts.isNotEmpty()

    private data class SparkleParticle(
        val angleRad: Float,
        val speed: Float,
        val sizeDp: Float,
        val color: Int
    )

    private data class SpellProjectile(
        val angleRad: Float,
        val maxDistance: Float,
        val size: Float,
        val particles: List<SparkleParticle>
    )

    private class ActiveBurst(
        val originX: Float,
        val originY: Float,
        val baseFingerRadius: Float,
        val element: MagicElement,
        val projectiles: List<SpellProjectile>,
        val animator: ValueAnimator?,
        var progress: Float = 0f,
        var hitTriggered: Boolean = false,
        val onHit: (() -> Unit)? = null,
        val onComplete: (() -> Unit)? = null
    )

    private val activeBursts = mutableListOf<ActiveBurst>()

    companion object {
        fun pickRandomElement(): MagicElement =
            if (Random.nextBoolean()) MagicElement.FIRE else MagicElement.ICE

        // Fire paints
        private val fireCorePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.WHITE
        }

        private val fireInnerPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = 0xFFFFD700.toInt() // Radiant Gold
        }

        private val fireMidPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = 0xFFFF6D00.toInt() // Blaze Orange
        }

        private val fireGlowPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = 0x66FF1744.toInt() // Soft Crimson Glow
        }

        private val fireRunePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = 0xFFFF9100.toInt()
        }

        private val fireSparkPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        private val fireShockwavePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        // Ice paints
        private val iceCorePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.WHITE
        }

        private val iceInnerPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = 0xFF80D8FF.toInt() // Starlight Azure
        }

        private val iceMidPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = 0xFF00E5FF.toInt() // Neon Cyan
        }

        private val iceGlowPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = 0x550091EA.toInt() // Sapphire Chill Aura
        }

        private val iceRunePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = 0xFF00E5FF.toInt()
        }

        private val iceSparkPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        private val iceShockwavePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        // Shared reusable paths
        private val starPath = Path()
        private val shardPath = Path()
    }

    /**
     * Radial burst in 8 directions (used by Team mode and fallback).
     */
    fun start(
        x: Float,
        y: Float,
        fingerRadius: Float,
        element: MagicElement = pickRandomElement(),
        onUpdate: () -> Unit
    ) {
        val numBolts = 8
        val baseDistance = fingerRadius * 3.4f
        val projectiles = mutableListOf<SpellProjectile>()

        for (i in 0 until numBolts) {
            val baseAngle = (2.0 * Math.PI * i / numBolts).toFloat()
            val angleJitter = (Random.nextFloat() - 0.5f) * 0.2f
            val angle = baseAngle + angleJitter
            val distance = baseDistance * (0.85f + Random.nextFloat() * 0.35f)
            val size = fingerRadius * (0.32f + Random.nextFloat() * 0.1f)

            val particles = createTrailParticles(element, size)
            projectiles.add(
                SpellProjectile(
                    angleRad = angle,
                    maxDistance = distance,
                    size = size,
                    particles = particles
                )
            )
        }

        lateinit var burst: ActiveBurst

        val animator = ValueAnimator.ofFloat(0f, 1f)?.apply {
            duration = 800L
            interpolator = DecelerateInterpolator(1.2f)
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
            element = element,
            projectiles = projectiles,
            animator = animator
        )

        activeBursts.add(burst)
        animator?.start()
    }

    /**
     * Targeted shot fired from an origin wizard/witch to a target finger (used by Order mode).
     */
    fun startTargeted(
        originX: Float,
        originY: Float,
        targetX: Float,
        targetY: Float,
        fingerRadius: Float,
        durationMs: Long? = null,
        element: MagicElement = pickRandomElement(),
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
        val size = fingerRadius * 0.38f
        val particles = createTrailParticles(element, size, count = 10)

        val projectile = SpellProjectile(
            angleRad = angle,
            maxDistance = distance,
            size = size,
            particles = particles
        )

        lateinit var burst: ActiveBurst

        val animDuration = durationMs ?: 500L
        val animator = ValueAnimator.ofFloat(0f, 1f)?.apply {
            duration = animDuration
            interpolator = DecelerateInterpolator(1.1f)
            addUpdateListener { anim ->
                val p = anim.animatedValue as Float
                burst.progress = p

                // Trigger target impact at 75% progress
                if (p >= 0.75f && !burst.hitTriggered) {
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
            element = element,
            projectiles = listOf(projectile),
            animator = animator,
            onHit = onHit,
            onComplete = onComplete
        )

        activeBursts.add(burst)
        animator?.start()
    }

    /**
     * Targeted salvo firing from a winning finger to multiple losing fingers simultaneously (used by Select mode).
     */
    fun <T> startTargetedSalvo(
        originX: Float,
        originY: Float,
        targets: List<Target<T>>,
        fingerRadius: Float,
        element: MagicElement = pickRandomElement(),
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
                durationMs = 520L,
                element = element,
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

    private fun createTrailParticles(element: MagicElement, size: Float, count: Int = 8): List<SparkleParticle> {
        val list = mutableListOf<SparkleParticle>()
        val colors = if (element == MagicElement.FIRE) {
            listOf(0xFFFFD700.toInt(), 0xFFFF6D00.toInt(), 0xFFFF1744.toInt(), 0xFFFFFFFF.toInt())
        } else {
            listOf(0xFF00E5FF.toInt(), 0xFF80D8FF.toInt(), 0xFF00B0FF.toInt(), 0xFFFFFFFF.toInt())
        }

        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 0.5f + Random.nextFloat() * 1.5f
            val pSize = size * (0.18f + Random.nextFloat() * 0.22f)
            val col = colors[i % colors.size]
            list.add(SparkleParticle(angle, speed, pSize, col))
        }
        return list
    }

    fun draw(context: Context, canvas: Canvas) {
        if (activeBursts.isEmpty()) return

        for (burst in activeBursts) {
            val p = burst.progress
            val isFire = burst.element == MagicElement.FIRE

            // 1. Casting Rune at origin (early animation: 0.0 to 0.4)
            if (p < 0.45f) {
                val runeAlpha = ((1f - p / 0.45f) * 220).toInt().coerceIn(0, 255)
                val runeRadius = burst.baseFingerRadius * (0.8f + p * 1.2f)
                val runePaint = if (isFire) fireRunePaint else iceRunePaint
                runePaint.alpha = runeAlpha
                canvas.drawCircle(burst.originX, burst.originY, runeRadius, runePaint)

                // Secondary rotating rune ring
                runePaint.alpha = (runeAlpha * 0.6f).toInt()
                val spinOffset = p * 4.0f
                for (k in 0 until 6) {
                    val a = (k * Math.PI / 3.0 + spinOffset).toFloat()
                    val rx = burst.originX + cos(a) * runeRadius
                    val ry = burst.originY + sin(a) * runeRadius
                    canvas.drawCircle(rx, ry, 4.5f, runePaint)
                }
            }

            // 2. Flying Projectiles & Impact Bursts
            for (proj in burst.projectiles) {
                // Flight progress moves from 0 to 0.75
                val flightProgress = (p / 0.75f).coerceIn(0f, 1f)
                val currentDistance = proj.maxDistance * flightProgress
                val headX = burst.originX + cos(proj.angleRad) * currentDistance
                val headY = burst.originY + sin(proj.angleRad) * currentDistance

                // 2A. Trailing magic embers / ice dust
                if (flightProgress < 1.0f) {
                    for (spark in proj.particles) {
                        val lagDistance = currentDistance - (spark.speed * proj.size * 2.2f * flightProgress)
                        if (lagDistance > 0f) {
                            val sparkX = burst.originX + cos(proj.angleRad) * lagDistance + cos(spark.angleRad) * (spark.sizeDp * 2f)
                            val sparkY = burst.originY + sin(proj.angleRad) * lagDistance + sin(spark.angleRad) * (spark.sizeDp * 2f)

                            val sparkAlpha = ((1f - flightProgress * 0.4f) * 200).toInt().coerceIn(0, 255)
                            val sPaint = if (isFire) fireSparkPaint else iceSparkPaint
                            sPaint.color = spark.color
                            sPaint.alpha = sparkAlpha
                            canvas.drawCircle(sparkX, sparkY, spark.sizeDp, sPaint)
                        }
                    }

                    // 2B. Projectile Head
                    if (isFire) {
                        // Fiery outer glow
                        fireGlowPaint.alpha = (180 * (1f - flightProgress * 0.2f)).toInt()
                        canvas.drawCircle(headX, headY, proj.size * 1.8f, fireGlowPaint)

                        // Orange flame body
                        fireMidPaint.alpha = 230
                        canvas.drawCircle(headX, headY, proj.size * 1.1f, fireMidPaint)

                        // Golden flame core
                        fireInnerPaint.alpha = 245
                        canvas.drawCircle(headX, headY, proj.size * 0.7f, fireInnerPaint)

                        // Pure white-hot center node
                        fireCorePaint.alpha = 255
                        canvas.drawCircle(headX, headY, proj.size * 0.35f, fireCorePaint)
                    } else {
                        // Ice chill aura
                        iceGlowPaint.alpha = (170 * (1f - flightProgress * 0.2f)).toInt()
                        canvas.drawCircle(headX, headY, proj.size * 1.8f, iceGlowPaint)

                        // Cyan crystal mid
                        iceMidPaint.alpha = 230
                        drawDiamond(canvas, headX, headY, proj.size * 1.2f, iceMidPaint)

                        // Azure crystal inner
                        iceInnerPaint.alpha = 245
                        drawDiamond(canvas, headX, headY, proj.size * 0.75f, iceInnerPaint)

                        // Crystalline white spark node
                        iceCorePaint.alpha = 255
                        canvas.drawCircle(headX, headY, proj.size * 0.32f, iceCorePaint)
                    }
                }

                // 2C. Impact Explosion (progress: 0.65 to 1.0)
                if (p >= 0.65f) {
                    val impactProgress = ((p - 0.65f) / 0.35f).coerceIn(0f, 1f)
                    val impactX = burst.originX + cos(proj.angleRad) * proj.maxDistance
                    val impactY = burst.originY + sin(proj.angleRad) * proj.maxDistance

                    val fade = (1f - impactProgress)
                    val maxRadius = proj.size * 4.2f
                    val currentRadius = maxRadius * impactProgress

                    if (isFire) {
                        // Fiery explosion shockwave
                        fireShockwavePaint.color = 0xFFFF6D00.toInt()
                        fireShockwavePaint.strokeWidth = 6f * fade
                        fireShockwavePaint.alpha = (230 * fade).toInt()
                        canvas.drawCircle(impactX, impactY, currentRadius, fireShockwavePaint)

                        // Inner flash
                        fireInnerPaint.alpha = (190 * fade).toInt()
                        canvas.drawCircle(impactX, impactY, currentRadius * 0.6f, fireInnerPaint)

                        // Erupting fire sparks
                        for (k in 0 until 8) {
                            val a = (k * Math.PI / 4.0 + 0.3).toFloat()
                            val sparkDist = currentRadius * (0.8f + (k % 3) * 0.2f)
                            val sx = impactX + cos(a) * sparkDist
                            val sy = impactY + sin(a) * sparkDist
                            fireSparkPaint.color = if (k % 2 == 0) 0xFFFFD700.toInt() else 0xFFFF1744.toInt()
                            fireSparkPaint.alpha = (240 * fade).toInt()
                            canvas.drawCircle(sx, sy, 3.8f * fade + 1f, fireSparkPaint)
                        }
                    } else {
                        // Frost nova shockwave
                        iceShockwavePaint.color = 0xFF00E5FF.toInt()
                        iceShockwavePaint.strokeWidth = 6f * fade
                        iceShockwavePaint.alpha = (230 * fade).toInt()
                        canvas.drawCircle(impactX, impactY, currentRadius, iceShockwavePaint)

                        // Inner frost flash
                        iceInnerPaint.alpha = (180 * fade).toInt()
                        canvas.drawCircle(impactX, impactY, currentRadius * 0.55f, iceInnerPaint)

                        // Shattered diamond ice crystals
                        for (k in 0 until 8) {
                            val a = (k * Math.PI / 4.0 + 0.2).toFloat()
                            val shardDist = currentRadius * (0.75f + (k % 2) * 0.35f)
                            val sx = impactX + cos(a) * shardDist
                            val sy = impactY + sin(a) * shardDist
                            iceSparkPaint.color = if (k % 2 == 0) 0xFF00E5FF.toInt() else 0xFFFFFFFF.toInt()
                            iceSparkPaint.alpha = (240 * fade).toInt()
                            drawDiamond(canvas, sx, sy, 5.5f * fade + 1.5f, iceSparkPaint)
                        }
                    }
                }
            }
        }
    }

    private fun drawDiamond(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        shardPath.reset()
        shardPath.moveTo(cx, cy - radius)
        shardPath.lineTo(cx + radius * 0.75f, cy)
        shardPath.lineTo(cx, cy + radius)
        shardPath.lineTo(cx - radius * 0.75f, cy)
        shardPath.close()
        canvas.drawPath(shardPath, paint)
    }
}
