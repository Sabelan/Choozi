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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Handles the pirate theme victory animation:
 * Fires cannonballs in 8 directions from one or more winning ships with trailing smoke puffs,
 * spinning cast-iron projectiles, and water impact splash rings.
 *
 * Supports concurrent bursts across multiple touch points (e.g. during finger ordering or team selection).
 */
class CannonballBurstAnimation {

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
        val puffs: List<SmokePuff>
    )

    private class ActiveBurst(
        val originX: Float,
        val originY: Float,
        val baseFingerRadius: Float,
        val cannonballs: List<Cannonball>,
        val animator: ValueAnimator,
        var progress: Float = 0f
    )

    private val activeBursts = mutableListOf<ActiveBurst>()

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
                    puffs = puffs
                )
            )
        }

        lateinit var burst: ActiveBurst

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
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
        animator.start()
    }

    fun cancel() {
        val burstsToCancel = ArrayList(activeBursts)
        activeBursts.clear()
        for (burst in burstsToCancel) {
            burst.animator.cancel()
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

            // 3. Water Splash Rings when cannonball reaches target area (p >= 0.7)
            if (p >= 0.7f) {
                val splashAge = (p - 0.7f) / 0.3f
                for (cb in burst.cannonballs) {
                    val impactX = originX + cos(cb.angleRad.toDouble()).toFloat() * cb.maxDistance
                    val impactY = originY + sin(cb.angleRad.toDouble()).toFloat() * cb.maxDistance
                    val ringRadius = cb.size * (0.5f + splashAge * 1.5f)
                    val ringAlpha = ((1f - splashAge) * 200).toInt().coerceIn(0, 255)

                    splashPaint.color = Color.argb(ringAlpha, 220, 245, 255)
                    canvas.drawCircle(impactX, impactY, ringRadius, splashPaint)
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

                    val arcY = -sin(Math.PI * p).toFloat() * (cb.size * 0.4f)
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
