package com.dannylumen.choozi.theme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.animation.DecelerateInterpolator
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Handles the pirate theme cannon animation:
 * - Radial bursts (e.g. Teams mode): fires cannonballs in 8 directions with water splash rings.
 * - Targeted shots (e.g. Select mode, Order mode): fires cannonballs directly from an origin ship
 *   to one or more target ships with trailing smoke puffs, ballistic arc, and impact splash ripple rings.
 */
class CannonballBurstAnimation {

    data class Target<T>(
        val id: T,
        val x: Float,
        val y: Float
    )

    val isRunning: Boolean
        get() = activeBursts.isNotEmpty()

    private val smokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val splashPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val spritePaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    private data class SmokePuff(
        val spawnProgress: Float,
        val emitDistanceFactor: Float,
        val maxRadius: Float,
        val baseAlpha: Int = 180
    )

    private data class Cannonball(
        val angleRad: Float,
        val maxDistance: Float,
        val size: Float,
        val spinSpeed: Float,
        val puffs: List<SmokePuff>,
        val arcHeight: Float = 0f
    )

    private class ActiveBurst(
        val originX: Float,
        val originY: Float,
        val baseFingerRadius: Float,
        val cannonballs: List<Cannonball>,
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
    fun start(x: Float, y: Float, fingerRadius: Float, onUpdate: () -> Unit) {
        val numBalls = 8
        val baseDistance = fingerRadius * 3.2f
        val cannonballs = mutableListOf<Cannonball>()

        for (i in 0 until numBalls) {
            val baseAngle = (2.0 * Math.PI * i / numBalls).toFloat()
            val angleJitter = (Random.nextFloat() - 0.5f) * 0.25f // +/- ~7 degrees
            val angle = baseAngle + angleJitter

            val distanceJitter = baseDistance * (0.85f + Random.nextFloat() * 0.35f)
            val ballSize = fingerRadius * (0.35f + Random.nextFloat() * 0.1f)
            val spin = if (Random.nextBoolean()) 720f else -720f

            val puffs = listOf(
                SmokePuff(spawnProgress = 0.10f, emitDistanceFactor = 0.15f, maxRadius = ballSize * 0.8f),
                SmokePuff(spawnProgress = 0.25f, emitDistanceFactor = 0.35f, maxRadius = ballSize * 1.1f),
                SmokePuff(spawnProgress = 0.45f, emitDistanceFactor = 0.60f, maxRadius = ballSize * 1.4f),
                SmokePuff(spawnProgress = 0.65f, emitDistanceFactor = 0.85f, maxRadius = ballSize * 1.7f)
            )

            cannonballs.add(
                Cannonball(
                    angleRad = angle,
                    maxDistance = distanceJitter,
                    size = ballSize,
                    spinSpeed = spin,
                    puffs = puffs,
                    arcHeight = ballSize * 0.4f
                )
            )
        }

        lateinit var burst: ActiveBurst

        val animator = ValueAnimator.ofFloat(0f, 1f)?.apply {
            duration = 850L
            interpolator = DecelerateInterpolator(1.3f)
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
            cannonballs = cannonballs,
            animator = animator
        )

        activeBursts.add(burst)
        animator?.start()
    }

    /**
     * Targeted shot fired from an origin point directly to a target point (used by Order mode).
     */
    fun startTargeted(
        originX: Float,
        originY: Float,
        targetX: Float,
        targetY: Float,
        fingerRadius: Float,
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
        val ballSize = fingerRadius * 0.42f
        val spin = if (Random.nextBoolean()) 720f else -720f

        val numPuffs = (distance / (fingerRadius * 0.85f)).toInt().coerceIn(4, 9)
        val puffs = mutableListOf<SmokePuff>()
        for (i in 1..numPuffs) {
            val factor = i.toFloat() / (numPuffs + 1)
            puffs.add(
                SmokePuff(
                    spawnProgress = factor * 0.8f,
                    emitDistanceFactor = factor,
                    maxRadius = ballSize * (0.8f + factor * 1.0f)
                )
            )
        }

        val cannonball = Cannonball(
            angleRad = angle,
            maxDistance = distance,
            size = ballSize,
            spinSpeed = spin,
            puffs = puffs,
            arcHeight = 0f // Straight line trajectory between ships
        )

        val flightDuration = durationMs ?: (450L + (distance / 2.5f).toLong()).coerceIn(550L, 850L)

        lateinit var burst: ActiveBurst

        val animator = ValueAnimator.ofFloat(0f, 1f)?.apply {
            duration = flightDuration
            interpolator = DecelerateInterpolator(1.2f)
            addUpdateListener { anim ->
                val p = anim.animatedValue as Float
                burst.progress = p
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
            cannonballs = listOf(cannonball),
            animator = animator,
            onHit = onHit,
            onComplete = onComplete
        )

        activeBursts.add(burst)
        animator?.start()
    }

    /**
     * Targeted salvo firing from an origin ship to multiple target ships simultaneously (used by Select mode).
     * Calls [onTargetHit] for each target when its incoming cannonball strikes,
     * and [onAllComplete] once all cannonballs in the salvo finish.
     */
    fun <T> startTargetedSalvo(
        originX: Float,
        originY: Float,
        targets: List<Target<T>>,
        fingerRadius: Float,
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

    fun draw(context: Context, canvas: Canvas) {
        if (activeBursts.isEmpty()) return

        val cannonballBitmap: Bitmap? = AssetLoader.loadBitmap(context, "themes/pirate/cannonball.png")
        val bursts = ArrayList(activeBursts)

        for (burst in bursts) {
            val p = burst.progress
            val originX = burst.originX
            val originY = burst.originY
            val baseFingerRadius = burst.baseFingerRadius

            // 1. Initial Muzzle Flash / Smoke at ship origin (first 250ms)
            if (p < 0.3f) {
                val muzzleP = p / 0.3f
                val muzzleRadius = baseFingerRadius * (0.5f + muzzleP * 0.8f)
                val muzzleAlpha = ((1f - muzzleP) * 160).toInt().coerceIn(0, 255)
                smokePaint.color = Color.argb(muzzleAlpha, 235, 235, 240)
                canvas.drawCircle(originX, originY, muzzleRadius, smokePaint)
            }

            // 2. Trailing Smoke Puffs behind each cannonball
            for (cb in burst.cannonballs) {
                val cosA = cos(cb.angleRad.toDouble()).toFloat()
                val sinA = sin(cb.angleRad.toDouble()).toFloat()

                for (puff in cb.puffs) {
                    if (p >= puff.spawnProgress) {
                        val age = (p - puff.spawnProgress) / (1f - puff.spawnProgress)
                        if (age in 0f..1f) {
                            val puffDist = cb.maxDistance * puff.emitDistanceFactor
                            val puffX = originX + cosA * puffDist
                            val puffY = originY + sinA * puffDist
                            val currentRadius = puff.maxRadius * (0.4f + 0.6f * age)
                            val alpha = ((1f - age) * puff.baseAlpha).toInt().coerceIn(0, 255)
                            smokePaint.color = Color.argb(alpha, 210, 215, 225)
                            canvas.drawCircle(puffX, puffY, currentRadius, smokePaint)
                        }
                    }
                }
            }

            // 3. Water Splash & Ripple Rings when cannonball reaches target area (p >= 0.7)
            if (p >= 0.7f) {
                val splashAge = (p - 0.7f) / 0.3f
                for (cb in burst.cannonballs) {
                    val impactX = originX + cos(cb.angleRad.toDouble()).toFloat() * cb.maxDistance
                    val impactY = originY + sin(cb.angleRad.toDouble()).toFloat() * cb.maxDistance

                    // Primary splash ring
                    val ringRadius1 = cb.size * (0.5f + splashAge * 1.8f)
                    val ringAlpha1 = ((1f - splashAge) * 220).toInt().coerceIn(0, 255)
                    splashPaint.color = Color.argb(ringAlpha1, 220, 245, 255)
                    canvas.drawCircle(impactX, impactY, ringRadius1, splashPaint)

                    // Secondary lagging ripple ring
                    if (splashAge >= 0.15f) {
                        val ring2Age = (splashAge - 0.15f) / 0.85f
                        val ringRadius2 = cb.size * (0.3f + ring2Age * 1.4f)
                        val ringAlpha2 = ((1f - ring2Age) * 160).toInt().coerceIn(0, 255)
                        splashPaint.color = Color.argb(ringAlpha2, 200, 240, 255)
                        canvas.drawCircle(impactX, impactY, ringRadius2, splashPaint)
                    }
                }
            }

            // 4. Draw Flying Cannonballs
            if (p < 0.95f) {
                val flightFade = if (p > 0.8f) 1f - (p - 0.8f) / 0.15f else 1f
                spritePaint.alpha = (flightFade.coerceIn(0f, 1f) * 255).toInt()

                for (cb in burst.cannonballs) {
                    val dist = cb.maxDistance * p
                    val ballX = originX + cos(cb.angleRad.toDouble()).toFloat() * dist
                    val ballY = originY + sin(cb.angleRad.toDouble()).toFloat() * dist

                    val arcY = -sin(Math.PI * p).toFloat() * cb.arcHeight
                    val finalY = ballY + arcY
                    val rotationDeg = cb.spinSpeed * p

                    canvas.save()
                    canvas.translate(ballX, finalY)
                    canvas.rotate(rotationDeg)

                    val halfSize = cb.size / 2f

                    if (cannonballBitmap != null) {
                        val dstRect = Rect(
                            (-halfSize).toInt(),
                            (-halfSize).toInt(),
                            halfSize.toInt(),
                            halfSize.toInt()
                        )
                        canvas.drawBitmap(cannonballBitmap, null, dstRect, spritePaint)
                    } else {
                        val ballPaint = Paint().apply {
                            isAntiAlias = true
                            color = Color.DKGRAY
                            style = Paint.Style.FILL
                        }
                        canvas.drawCircle(0f, 0f, halfSize, ballPaint)
                    }

                    canvas.restore()
                }
            }
        }
    }
}
