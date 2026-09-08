package com.dannylumen.choozi.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Path
import android.graphics.PathMeasure
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class BackgroundScaleMode {
    /**
     * Scales the image uniformly so that both dimensions equal or exceed the view.
     * The image is centered and excess edges are cleanly cropped/sliced.
     */
    CENTER_CROP,

    /**
     * Stretches or fits 9-patch slice drawables where borders/corners are preserved.
     */
    NINE_PATCH,

    /**
     * Tiles the pattern across the full canvas width and height.
     */
    TILE,

    /**
     * Stretches to fill exact canvas dimensions without maintaining aspect ratio.
     */
    STRETCH
}

enum class BackgroundEffect {
    NONE,
    OCEAN_WAVES,
    NEON_LINES,
    MAGICAL_SPARKLES,
    STARS_AND_SPARKLES
}

data class ThemeBackground(
    val assetPath: String? = null,
    @param:DrawableRes val drawableRes: Int? = null,
    @param:ColorInt val backgroundColor: Int? = null,
    val scaleMode: BackgroundScaleMode = BackgroundScaleMode.CENTER_CROP,
    val effect: BackgroundEffect = BackgroundEffect.NONE
) {
    val isAnimated: Boolean
        get() = effect != BackgroundEffect.NONE

    // Convenience constructor for effect
    constructor(
        effect: BackgroundEffect,
        @ColorInt backgroundColor: Int? = null
    ) : this(
        assetPath = null,
        drawableRes = null,
        backgroundColor = backgroundColor,
        scaleMode = BackgroundScaleMode.CENTER_CROP,
        effect = effect
    )

    // Convenience constructor for asset path
    constructor(
        assetPath: String,
        scaleMode: BackgroundScaleMode = BackgroundScaleMode.CENTER_CROP
    ) : this(assetPath = assetPath, drawableRes = null, backgroundColor = null, scaleMode = scaleMode)

    // Convenience constructor for resource ID
    constructor(
        @DrawableRes drawableRes: Int,
        scaleMode: BackgroundScaleMode = BackgroundScaleMode.CENTER_CROP
    ) : this(assetPath = null, drawableRes = drawableRes, backgroundColor = null, scaleMode = scaleMode)

    companion object {
        private val bgPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }
        }

        private val wavePath by lazy { Path() }
        private val crestPath by lazy { Path() }

        private val oceanBasePaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }

        private val wavePaint0 by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = 0x660F4D82.toInt() // Gentle upper ocean wave
            }
        }

        private val wavePaint1 by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = 0x880C3455.toInt() // Deep midnight navy swell
            }
        }

        private val wavePaint2 by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = 0xAA114E7E.toInt() // Vibrant deep ocean blue
            }
        }

        private val wavePaint3 by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = 0xCC1A6B9B.toInt() // Bright rolling ocean wave
            }
        }

        private val wavePaint4 by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = 0xEE1F84B8.toInt() // Foreground sea wave
            }
        }

        private val crestPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
                color = 0x44BBE9FF.toInt() // Soft translucent sea foam highlight
            }
        }

        private var lastGradientW = -1
        private var lastGradientH = -1

        private data class NeonLineConfig(
            val color: Int,
            val points: List<Pair<Float, Float>>,
            val pulseSpeed: Float,
            val pulsePhase: Float,
            val packetSpeed: Float,
            val packetOffset: Float
        )

        private val NEON_LINES_CONFIG = listOf(
            // 0. Neon Pink - Full horizontal span across upper section
            NeonLineConfig(
                color = 0xFFFF2DAA.toInt(),
                points = listOf(-0.02f to 0.08f, 0.24f to 0.08f, 0.38f to 0.14f, 0.70f to 0.14f, 0.82f to 0.08f, 1.02f to 0.08f),
                pulseSpeed = 3.2f,
                pulsePhase = 3.4f,
                packetSpeed = 0.32f,
                packetOffset = 0.2f
            ),
            // 1. Electric Cyan - Full horizontal span across upper-mid highway
            NeonLineConfig(
                color = 0xFF00F0FF.toInt(),
                points = listOf(-0.02f to 0.22f, 0.30f to 0.22f, 0.44f to 0.28f, 0.76f to 0.28f, 0.88f to 0.34f, 1.02f to 0.34f),
                pulseSpeed = 2.4f,
                pulsePhase = 0.0f,
                packetSpeed = 0.28f,
                packetOffset = 0.1f
            ),
            // 2. Electric Orchid - Full horizontal span across mid-upper section
            NeonLineConfig(
                color = 0xFFE040FB.toInt(),
                points = listOf(-0.02f to 0.36f, 0.18f to 0.36f, 0.32f to 0.44f, 0.64f to 0.44f, 0.76f to 0.40f, 1.02f to 0.40f),
                pulseSpeed = 2.7f,
                pulsePhase = 1.1f,
                packetSpeed = 0.35f,
                packetOffset = 0.6f
            ),
            // 3. Laser Blue - Full horizontal span across center section
            NeonLineConfig(
                color = 0xFF00D4FF.toInt(),
                points = listOf(-0.02f to 0.48f, 0.26f to 0.48f, 0.38f to 0.54f, 0.66f to 0.54f, 0.78f to 0.48f, 1.02f to 0.48f),
                pulseSpeed = 3.5f,
                pulsePhase = 5.2f,
                packetSpeed = 0.26f,
                packetOffset = 0.85f
            ),
            // 4. Hot Magenta - Full horizontal span across center-lower section
            NeonLineConfig(
                color = 0xFFFF007F.toInt(),
                points = listOf(-0.02f to 0.62f, 0.20f to 0.62f, 0.34f to 0.68f, 0.68f to 0.68f, 0.82f to 0.74f, 1.02f to 0.74f),
                pulseSpeed = 2.8f,
                pulsePhase = 1.8f,
                packetSpeed = 0.24f,
                packetOffset = 0.55f
            ),
            // 5. Electric Violet - Full vertical trunk stretching from top to bottom
            NeonLineConfig(
                color = 0xFFB026FF.toInt(),
                points = listOf(0.82f to -0.02f, 0.82f to 0.20f, 0.68f to 0.32f, 0.68f to 0.64f, 0.52f to 0.76f, 0.52f to 1.02f),
                pulseSpeed = 2.1f,
                pulsePhase = 4.7f,
                packetSpeed = 0.22f,
                packetOffset = 0.75f
            ),
            // 6. Neon Rose Pink - Full horizontal span across lower section
            NeonLineConfig(
                color = 0xFFFF4081.toInt(),
                points = listOf(-0.02f to 0.78f, 0.28f to 0.78f, 0.42f to 0.84f, 0.72f to 0.84f, 0.86f to 0.80f, 1.02f to 0.80f),
                pulseSpeed = 2.5f,
                pulsePhase = 2.5f,
                packetSpeed = 0.30f,
                packetOffset = 0.35f
            ),
            // 7. Neon Rose - Full horizontal span across bottom section
            NeonLineConfig(
                color = 0xFFFF1493.toInt(),
                points = listOf(-0.02f to 0.92f, 0.22f to 0.92f, 0.36f to 0.96f, 0.68f to 0.96f, 0.80f to 0.92f, 1.02f to 0.92f),
                pulseSpeed = 3.0f,
                pulsePhase = 3.9f,
                packetSpeed = 0.27f,
                packetOffset = 0.4f
            ),
            // 8. Teal Cyan - Full vertical trunk stretching from top to bottom
            NeonLineConfig(
                color = 0xFF00E5FF.toInt(),
                points = listOf(0.18f to -0.02f, 0.18f to 0.35f, 0.30f to 0.45f, 0.30f to 0.70f, 0.40f to 0.80f, 0.40f to 1.02f),
                pulseSpeed = 2.9f,
                pulsePhase = 2.0f,
                packetSpeed = 0.25f,
                packetOffset = 0.15f
            )
        )

        private var lastNeonW = -1
        private var lastNeonH = -1
        private val cachedNeonPaths = Array(NEON_LINES_CONFIG.size) { Path() }
        private val cachedPathMeasures = Array(NEON_LINES_CONFIG.size) { PathMeasure() }
        private val cachedPathLengths = FloatArray(NEON_LINES_CONFIG.size)
        private val tempPos = FloatArray(2)

        private val cyberBasePaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val cyberPinkLightPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val cyberPurpleLightPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val cyberLinearGlowPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val cyberGridPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 1.0f
                color = 0x22E040FB.toInt()
            }
        }
        private val neonOuterGlowPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
        }
        private val neonMidGlowPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
        }
        private val neonCorePaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = 2.8f
            }
        }
        private val neonWhiteCorePaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = 1.0f
                color = Color.WHITE
            }
        }
        private val neonNodeOuterPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val neonNodeMidPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val neonNodeCorePaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = Color.WHITE
            }
        }
        private val neonTerminalRingPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 2.0f
            }
        }
        private val neonTerminalDotPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }

        private var lastFantasyW = -1
        private var lastFantasyH = -1

        private data class DistantStarConfig(
            val xRatio: Float,
            val yRatio: Float,
            val radius: Float,
            val speed: Float,
            val phase: Float,
            val color: Int
        )

        private val DISTANT_STARS_CONFIG = listOf(
            DistantStarConfig(0.05f, 0.08f, 1.6f, 2.3f, 0.5f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.12f, 0.15f, 2.2f, 1.8f, 2.1f, 0xFFFFF8E7.toInt()),
            DistantStarConfig(0.22f, 0.06f, 1.4f, 3.1f, 4.2f, 0xFFE1BEE7.toInt()),
            DistantStarConfig(0.31f, 0.18f, 2.0f, 2.5f, 1.1f, 0xFFB2EBF2.toInt()),
            DistantStarConfig(0.42f, 0.09f, 1.8f, 1.9f, 3.7f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.55f, 0.14f, 2.4f, 2.8f, 0.2f, 0xFFFFE082.toInt()),
            DistantStarConfig(0.68f, 0.07f, 1.5f, 2.1f, 5.0f, 0xFFF8BBD0.toInt()),
            DistantStarConfig(0.79f, 0.16f, 2.1f, 3.4f, 1.7f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.88f, 0.05f, 1.7f, 1.6f, 2.9f, 0xFFE0F7FA.toInt()),
            DistantStarConfig(0.94f, 0.13f, 2.3f, 2.7f, 4.5f, 0xFFFFF9C4.toInt()),
            DistantStarConfig(0.08f, 0.28f, 2.0f, 2.2f, 3.3f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.18f, 0.35f, 1.5f, 3.0f, 0.8f, 0xFFE1BEE7.toInt()),
            DistantStarConfig(0.27f, 0.24f, 2.5f, 1.7f, 2.4f, 0xFFFFE082.toInt()),
            DistantStarConfig(0.38f, 0.32f, 1.6f, 2.6f, 5.2f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.48f, 0.22f, 2.2f, 2.0f, 1.4f, 0xFFB2EBF2.toInt()),
            DistantStarConfig(0.62f, 0.29f, 1.8f, 3.2f, 3.8f, 0xFFF8BBD0.toInt()),
            DistantStarConfig(0.73f, 0.23f, 2.4f, 1.5f, 0.6f, 0xFFFFF9C4.toInt()),
            DistantStarConfig(0.85f, 0.31f, 1.7f, 2.9f, 4.1f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.92f, 0.26f, 2.0f, 2.4f, 2.0f, 0xFFE1BEE7.toInt()),
            DistantStarConfig(0.04f, 0.44f, 1.6f, 1.8f, 1.2f, 0xFFE0F7FA.toInt()),
            DistantStarConfig(0.14f, 0.52f, 2.3f, 2.8f, 4.8f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.24f, 0.46f, 1.9f, 2.1f, 2.7f, 0xFFFFE082.toInt()),
            DistantStarConfig(0.35f, 0.55f, 1.5f, 3.3f, 0.3f, 0xFFF8BBD0.toInt()),
            DistantStarConfig(0.46f, 0.43f, 2.4f, 1.6f, 3.5f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.58f, 0.51f, 1.8f, 2.5f, 1.9f, 0xFFB2EBF2.toInt()),
            DistantStarConfig(0.69f, 0.42f, 2.1f, 3.0f, 5.4f, 0xFFE1BEE7.toInt()),
            DistantStarConfig(0.81f, 0.48f, 1.7f, 2.2f, 2.2f, 0xFFFFF9C4.toInt()),
            DistantStarConfig(0.95f, 0.45f, 2.2f, 1.7f, 4.0f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.07f, 0.64f, 2.0f, 2.9f, 0.9f, 0xFFFFF8E7.toInt()),
            DistantStarConfig(0.16f, 0.71f, 1.5f, 2.0f, 3.1f, 0xFFB2EBF2.toInt()),
            DistantStarConfig(0.29f, 0.67f, 2.3f, 3.1f, 1.5f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.39f, 0.74f, 1.8f, 1.8f, 4.6f, 0xFFF8BBD0.toInt()),
            DistantStarConfig(0.52f, 0.63f, 2.2f, 2.4f, 2.6f, 0xFFFFE082.toInt()),
            DistantStarConfig(0.64f, 0.72f, 1.6f, 3.3f, 0.4f, 0xFFE1BEE7.toInt()),
            DistantStarConfig(0.76f, 0.66f, 2.5f, 1.9f, 3.9f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.87f, 0.75f, 1.7f, 2.7f, 1.8f, 0xFFE0F7FA.toInt()),
            DistantStarConfig(0.96f, 0.68f, 2.1f, 2.2f, 5.1f, 0xFFFFF9C4.toInt()),
            DistantStarConfig(0.06f, 0.85f, 1.8f, 2.6f, 2.8f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.15f, 0.92f, 2.2f, 1.7f, 0.7f, 0xFFE1BEE7.toInt()),
            DistantStarConfig(0.26f, 0.84f, 1.5f, 3.2f, 4.4f, 0xFFFFE082.toInt()),
            DistantStarConfig(0.37f, 0.93f, 2.4f, 2.1f, 1.6f, 0xFFB2EBF2.toInt()),
            DistantStarConfig(0.48f, 0.86f, 1.9f, 2.8f, 3.6f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.59f, 0.95f, 1.6f, 1.6f, 5.5f, 0xFFF8BBD0.toInt()),
            DistantStarConfig(0.71f, 0.88f, 2.3f, 3.0f, 2.3f, 0xFFFFF9C4.toInt()),
            DistantStarConfig(0.83f, 0.94f, 1.7f, 2.4f, 0.1f, 0xFFFFFFFF.toInt()),
            DistantStarConfig(0.93f, 0.87f, 2.0f, 1.9f, 3.4f, 0xFFE1BEE7.toInt())
        )

        private data class SparkleStarConfig(
            val xRatio: Float,
            val yRatio: Float,
            val outerRadiusDp: Float,
            val pulseSpeed: Float,
            val pulsePhase: Float,
            val driftSpeed: Float,
            val driftAmpDp: Float,
            val isEightPointed: Boolean,
            val color: Int
        )

        private val SPARKLE_STARS_CONFIG = listOf(
            // Top area
            SparkleStarConfig(0.18f, 0.11f, 22f, 2.1f, 0.4f, 0.5f, 6f, true, 0xFFFFD700.toInt()),
            SparkleStarConfig(0.82f, 0.10f, 26f, 1.8f, 2.7f, 0.4f, 8f, true, 0xFF00F0FF.toInt()),
            SparkleStarConfig(0.52f, 0.08f, 17f, 2.8f, 4.2f, 0.6f, 5f, false, 0xFFFF4081.toInt()),
            // Upper-mid
            SparkleStarConfig(0.36f, 0.22f, 20f, 2.3f, 1.5f, 0.5f, 7f, false, 0xFFE040FB.toInt()),
            SparkleStarConfig(0.70f, 0.25f, 18f, 2.6f, 3.8f, 0.7f, 6f, false, 0xFFFFF176.toInt()),
            SparkleStarConfig(0.08f, 0.24f, 15f, 3.1f, 5.1f, 0.4f, 5f, false, 0xFF80D8FF.toInt()),
            // Mid section
            SparkleStarConfig(0.24f, 0.40f, 28f, 1.6f, 0.9f, 0.35f, 9f, true, 0xFFFF80AB.toInt()),
            SparkleStarConfig(0.88f, 0.38f, 21f, 2.4f, 3.1f, 0.55f, 7f, false, 0xFFFFD54F.toInt()),
            SparkleStarConfig(0.60f, 0.46f, 24f, 1.9f, 2.0f, 0.45f, 8f, true, 0xFFB388FF.toInt()),
            // Lower-mid
            SparkleStarConfig(0.12f, 0.58f, 19f, 2.5f, 4.6f, 0.65f, 6f, false, 0xFF00E5FF.toInt()),
            SparkleStarConfig(0.78f, 0.62f, 25f, 2.0f, 1.2f, 0.4f, 8f, true, 0xFFFFD700.toInt()),
            SparkleStarConfig(0.44f, 0.68f, 16f, 3.0f, 3.4f, 0.7f, 5f, false, 0xFFFF4081.toInt()),
            // Bottom section
            SparkleStarConfig(0.28f, 0.82f, 22f, 2.2f, 5.5f, 0.5f, 7f, false, 0xFFE040FB.toInt()),
            SparkleStarConfig(0.86f, 0.84f, 20f, 2.7f, 2.3f, 0.6f, 6f, false, 0xFF80D8FF.toInt()),
            SparkleStarConfig(0.62f, 0.88f, 18f, 2.5f, 0.7f, 0.5f, 5f, false, 0xFFFFEE58.toInt())
        )

        private data class FloatingDustConfig(
            val initialXRatio: Float,
            val initialYRatio: Float,
            val riseSpeed: Float,
            val swaySpeed: Float,
            val swayAmpRatio: Float,
            val radiusDp: Float,
            val color: Int,
            val hasCrossGlint: Boolean
        )

        private val FLOATING_DUST_CONFIG = listOf(
            FloatingDustConfig(0.06f, 0.95f, 0.052f, 2.1f, 0.018f, 4.5f, 0xFFFFD700.toInt(), true),
            FloatingDustConfig(0.14f, 0.80f, 0.065f, 2.6f, 0.022f, 3.5f, 0xFFFF80AB.toInt(), false),
            FloatingDustConfig(0.21f, 0.65f, 0.048f, 1.8f, 0.015f, 5.0f, 0xFF80D8FF.toInt(), true),
            FloatingDustConfig(0.28f, 0.90f, 0.058f, 2.4f, 0.020f, 3.8f, 0xFFE040FB.toInt(), false),
            FloatingDustConfig(0.35f, 0.40f, 0.050f, 2.0f, 0.017f, 4.8f, 0xFFFFF176.toInt(), true),
            FloatingDustConfig(0.42f, 0.75f, 0.062f, 2.7f, 0.024f, 3.2f, 0xFF00E5FF.toInt(), false),
            FloatingDustConfig(0.49f, 0.55f, 0.045f, 1.7f, 0.016f, 5.2f, 0xFFFF4081.toInt(), true),
            FloatingDustConfig(0.56f, 0.85f, 0.056f, 2.3f, 0.019f, 3.6f, 0xFFFFD700.toInt(), false),
            FloatingDustConfig(0.63f, 0.35f, 0.068f, 2.8f, 0.023f, 4.2f, 0xFFB388FF.toInt(), true),
            FloatingDustConfig(0.70f, 0.70f, 0.049f, 1.9f, 0.015f, 3.4f, 0xFF80D8FF.toInt(), false),
            FloatingDustConfig(0.77f, 0.92f, 0.054f, 2.2f, 0.021f, 5.0f, 0xFFFF80AB.toInt(), true),
            FloatingDustConfig(0.84f, 0.48f, 0.060f, 2.5f, 0.018f, 3.7f, 0xFFFFF59D.toInt(), false),
            FloatingDustConfig(0.91f, 0.82f, 0.047f, 1.6f, 0.022f, 4.6f, 0xFF00E5FF.toInt(), true),
            FloatingDustConfig(0.96f, 0.60f, 0.064f, 2.9f, 0.016f, 3.5f, 0xFFE040FB.toInt(), false),
            FloatingDustConfig(0.09f, 0.30f, 0.051f, 2.0f, 0.020f, 4.0f, 0xFFFFD700.toInt(), true),
            FloatingDustConfig(0.18f, 0.15f, 0.059f, 2.4f, 0.017f, 3.3f, 0xFFFF4081.toInt(), false),
            FloatingDustConfig(0.25f, 0.45f, 0.046f, 1.8f, 0.023f, 4.9f, 0xFF80D8FF.toInt(), true),
            FloatingDustConfig(0.33f, 0.20f, 0.063f, 2.7f, 0.019f, 3.6f, 0xFFFFF176.toInt(), false),
            FloatingDustConfig(0.40f, 0.05f, 0.053f, 2.2f, 0.016f, 4.7f, 0xFFB388FF.toInt(), true),
            FloatingDustConfig(0.47f, 0.32f, 0.057f, 2.5f, 0.021f, 3.4f, 0xFF00E5FF.toInt(), false),
            FloatingDustConfig(0.54f, 0.18f, 0.048f, 1.9f, 0.018f, 5.1f, 0xFFFF80AB.toInt(), true),
            FloatingDustConfig(0.61f, 0.98f, 0.066f, 2.8f, 0.022f, 3.8f, 0xFFFFD700.toInt(), false),
            FloatingDustConfig(0.68f, 0.12f, 0.050f, 2.1f, 0.015f, 4.4f, 0xFFE040FB.toInt(), true),
            FloatingDustConfig(0.75f, 0.38f, 0.061f, 2.6f, 0.020f, 3.2f, 0xFF80D8FF.toInt(), false),
            FloatingDustConfig(0.82f, 0.22f, 0.044f, 1.7f, 0.024f, 5.3f, 0xFFFFEE58.toInt(), true),
            FloatingDustConfig(0.89f, 0.08f, 0.058f, 2.3f, 0.017f, 3.9f, 0xFFFF4081.toInt(), false),
            FloatingDustConfig(0.12f, 0.72f, 0.055f, 2.2f, 0.019f, 4.3f, 0xFF00E5FF.toInt(), true),
            FloatingDustConfig(0.87f, 0.65f, 0.062f, 2.7f, 0.021f, 3.5f, 0xFFB388FF.toInt(), false)
        )

        private val fantasyBasePaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val fantasyNebulaPinkPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val fantasyNebulaCyanPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val fantasyNebulaGoldPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val fantasyStarPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val fantasySparkleOuterGlowPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val fantasySparkleBodyPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val fantasySparkleCorePaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = Color.WHITE
            }
        }
        private val fantasyDustGlowPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val fantasyDustCorePaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val fantasyCrossSparklePaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeWidth = 1.4f
            }
        }
        private val fantasyShootingStarTailPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
        }
        private val fantasyShootingStarGlowTrailPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
        }
        private val fantasyShootingStarHeadPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = Color.WHITE
            }
        }
        private val fantasyShootingStarSparkPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = 0xFFFFE082.toInt()
            }
        }

        private val primarySparklePath by lazy { Path() }
        private val secondarySparklePath by lazy { Path() }
        private val innerSparklePath by lazy { Path() }
    }

    fun draw(context: Context, canvas: Canvas, viewWidth: Int, viewHeight: Int) {
        if (viewWidth <= 0 || viewHeight <= 0) return

        // 1. Animated wave effect
        if (effect == BackgroundEffect.OCEAN_WAVES) {
            drawOceanWaves(canvas, viewWidth, viewHeight)
            return
        }

        // 2. Animated neon lines effect
        if (effect == BackgroundEffect.NEON_LINES) {
            drawNeonLines(canvas, viewWidth, viewHeight)
            return
        }

        // 3. Animated magical sparkles effect
        if (effect == BackgroundEffect.MAGICAL_SPARKLES || effect == BackgroundEffect.STARS_AND_SPARKLES) {
            drawMagicalSparkles(canvas, viewWidth, viewHeight)
            return
        }

        // 2. Draw solid color if provided
        backgroundColor?.let { color ->
            canvas.drawColor(color)
        }

        // 2. Draw asset image if provided
        if (assetPath != null) {
            val bitmap = AssetLoader.loadBitmap(context, assetPath)
            if (bitmap != null) {
                when (scaleMode) {
                    BackgroundScaleMode.STRETCH, BackgroundScaleMode.NINE_PATCH -> {
                        canvas.drawBitmap(bitmap, null, Rect(0, 0, viewWidth, viewHeight), bgPaint)
                    }
                    BackgroundScaleMode.CENTER_CROP -> {
                        drawBitmapCenterCropped(bitmap, canvas, viewWidth, viewHeight)
                    }
                    BackgroundScaleMode.TILE -> {
                        val paint = Paint().apply {
                            isAntiAlias = true
                            shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                        }
                        canvas.drawRect(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), paint)
                    }
                }
                return
            }
        }

        // 3. Fallback to drawable resource if provided
        val resId = drawableRes ?: return
        val drawable = ContextCompat.getDrawable(context, resId) ?: return

        when (scaleMode) {
            BackgroundScaleMode.STRETCH, BackgroundScaleMode.NINE_PATCH -> {
                drawable.setBounds(0, 0, viewWidth, viewHeight)
                drawable.draw(canvas)
            }
            BackgroundScaleMode.CENTER_CROP -> {
                drawCenterCropped(drawable, canvas, viewWidth, viewHeight)
            }
            BackgroundScaleMode.TILE -> {
                drawTiled(drawable, canvas, viewWidth, viewHeight)
            }
        }
    }

    private fun drawBitmapCenterCropped(bitmap: Bitmap, canvas: Canvas, viewW: Int, viewH: Int) {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        if (bw <= 0f || bh <= 0f) return

        val scale = maxOf(viewW / bw, viewH / bh)
        val scaledW = bw * scale
        val scaledH = bh * scale

        val left = ((viewW - scaledW) / 2f).toInt()
        val top = ((viewH - scaledH) / 2f).toInt()
        val right = (left + scaledW).toInt()
        val bottom = (top + scaledH).toInt()

        val dstRect = Rect(left, top, right, bottom)
        canvas.drawBitmap(bitmap, null, dstRect, bgPaint)
    }

    private fun drawCenterCropped(drawable: Drawable, canvas: Canvas, viewW: Int, viewH: Int) {
        val dw = drawable.intrinsicWidth.toFloat()
        val dh = drawable.intrinsicHeight.toFloat()

        if (dw <= 0f || dh <= 0f) {
            drawable.setBounds(0, 0, viewW, viewH)
            drawable.draw(canvas)
            return
        }

        val scale = maxOf(viewW / dw, viewH / dh)
        val scaledW = dw * scale
        val scaledH = dh * scale

        val left = ((viewW - scaledW) / 2f).toInt()
        val top = ((viewH - scaledH) / 2f).toInt()
        val right = (left + scaledW).toInt()
        val bottom = (top + scaledH).toInt()

        drawable.setBounds(left, top, right, bottom)
        drawable.draw(canvas)
    }

    private fun drawTiled(drawable: Drawable, canvas: Canvas, viewW: Int, viewH: Int) {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val paint = Paint().apply {
                isAntiAlias = true
                shader = BitmapShader(drawable.bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            }
            canvas.drawRect(0f, 0f, viewW.toFloat(), viewH.toFloat(), paint)
        } else {
            val dw = drawable.intrinsicWidth.coerceAtLeast(1)
            val dh = drawable.intrinsicHeight.coerceAtLeast(1)
            var y = 0
            while (y < viewH) {
                var x = 0
                while (x < viewW) {
                    drawable.setBounds(x, y, (x + dw).coerceAtMost(viewW), (y + dh).coerceAtMost(viewH))
                    drawable.draw(canvas)
                    x += dw
                }
                y += dh
            }
        }
    }

    private fun drawOceanWaves(canvas: Canvas, viewWidth: Int, viewHeight: Int) {
        val w = viewWidth.toFloat()
        val h = viewHeight.toFloat()

        // 1. Draw base ocean gradient
        if (lastGradientW != viewWidth || lastGradientH != viewHeight) {
            lastGradientW = viewWidth
            lastGradientH = viewHeight
            oceanBasePaint.shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(
                    0xFF0A345C.toInt(), // Brighter rich ocean navy at top
                    0xFF0D4474.toInt(), // Deep vibrant blue
                    0xFF072744.toInt()  // Nautical depth
                ),
                floatArrayOf(0f, 0.40f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, oceanBasePaint)

        // 2. Draw animated rolling wave layers
        val timeSec = (System.currentTimeMillis() % 1_000_000L) / 1000f

        // Layer 0: Upper ocean swell near the top
        drawWaveLayer(
            canvas = canvas,
            viewW = w,
            viewH = h,
            baseY = h * 0.18f,
            amp = 12f,
            freq = (1.5 * Math.PI / w).toFloat(),
            speed = 0.45f,
            amp2 = 5f,
            freq2 = (3.0 * Math.PI / w).toFloat(),
            speed2 = -0.7f,
            time = timeSec,
            paint = wavePaint0,
            crestPaint = crestPaint
        )

        // Layer 1: Distant deep swell
        drawWaveLayer(
            canvas = canvas,
            viewW = w,
            viewH = h,
            baseY = h * 0.38f,
            amp = 14f,
            freq = (1.8 * Math.PI / w).toFloat(),
            speed = 0.6f,
            amp2 = 6f,
            freq2 = (3.6 * Math.PI / w).toFloat(),
            speed2 = -0.9f,
            time = timeSec,
            paint = wavePaint1,
            crestPaint = crestPaint
        )

        // Layer 2: Mid-ocean wave
        drawWaveLayer(
            canvas = canvas,
            viewW = w,
            viewH = h,
            baseY = h * 0.54f,
            amp = 20f,
            freq = (2.2 * Math.PI / w).toFloat(),
            speed = -0.85f,
            amp2 = 8f,
            freq2 = (4.0 * Math.PI / w).toFloat(),
            speed2 = 1.1f,
            time = timeSec,
            paint = wavePaint2,
            crestPaint = crestPaint
        )

        // Layer 3: Vibrant rolling swell
        drawWaveLayer(
            canvas = canvas,
            viewW = w,
            viewH = h,
            baseY = h * 0.70f,
            amp = 26f,
            freq = (2.0 * Math.PI / w).toFloat(),
            speed = 1.15f,
            amp2 = 10f,
            freq2 = (3.8 * Math.PI / w).toFloat(),
            speed2 = -0.7f,
            time = timeSec,
            paint = wavePaint3,
            crestPaint = crestPaint
        )

        // Layer 4: Foreground tide
        drawWaveLayer(
            canvas = canvas,
            viewW = w,
            viewH = h,
            baseY = h * 0.85f,
            amp = 30f,
            freq = (1.6 * Math.PI / w).toFloat(),
            speed = -1.35f,
            amp2 = 11f,
            freq2 = (3.2 * Math.PI / w).toFloat(),
            speed2 = 1.4f,
            time = timeSec,
            paint = wavePaint4,
            crestPaint = crestPaint
        )
    }

    private fun drawWaveLayer(
        canvas: Canvas,
        viewW: Float,
        viewH: Float,
        baseY: Float,
        amp: Float,
        freq: Float,
        speed: Float,
        amp2: Float,
        freq2: Float,
        speed2: Float,
        time: Float,
        paint: Paint,
        crestPaint: Paint?
    ) {
        wavePath.reset()
        crestPath.reset()

        wavePath.moveTo(0f, viewH)

        val step = (viewW / 40f).coerceAtLeast(8f)
        var x = 0f
        var isFirst = true

        while (x <= viewW) {
            val y = baseY +
                sin(x * freq + time * speed) * amp +
                cos(x * freq2 + time * speed2) * amp2

            if (isFirst) {
                wavePath.lineTo(0f, y)
                if (crestPaint != null) crestPath.moveTo(0f, y)
                isFirst = false
            } else {
                wavePath.lineTo(x, y)
                if (crestPaint != null) crestPath.lineTo(x, y)
            }
            x += step
        }

        // Final boundary point
        val endY = baseY +
            sin(viewW * freq + time * speed) * amp +
            cos(viewW * freq2 + time * speed2) * amp2
        wavePath.lineTo(viewW, endY)
        if (crestPaint != null) crestPath.lineTo(viewW, endY)

        wavePath.lineTo(viewW, viewH)
        wavePath.lineTo(0f, viewH)
        wavePath.close()

        canvas.drawPath(wavePath, paint)
        if (crestPaint != null) {
            canvas.drawPath(crestPath, crestPaint)
        }
    }

    private fun drawNeonLines(canvas: Canvas, viewWidth: Int, viewHeight: Int) {
        val w = viewWidth.toFloat()
        val h = viewHeight.toFloat()

        // 1. Rebuild paths if dimensions changed
        if (lastNeonW != viewWidth || lastNeonH != viewHeight) {
            lastNeonW = viewWidth
            lastNeonH = viewHeight

            // Base gradient leaning into deep midnight purple, violet, and dark plum
            cyberBasePaint.shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(
                    0xFF1A072E.toInt(), // Deep midnight purple at top
                    0xFF280B44.toInt(), // Rich glowing dark violet in upper-mid
                    0xFF1E0736.toInt(), // Deep plum violet mid-section
                    0xFF110321.toInt()  // Velvet dark purple-black at bottom
                ),
                floatArrayOf(0f, 0.35f, 0.70f, 1f),
                Shader.TileMode.CLAMP
            )

            // Luminous ambient pink light gradient bloom in upper-right
            val pinkRadius = maxOf(w, h) * 0.65f
            cyberPinkLightPaint.shader = RadialGradient(
                w * 0.82f, h * 0.20f,
                pinkRadius,
                intArrayOf(
                    0x4DFF2A85.toInt(), // Glowing neon pink core (~30% alpha)
                    0x20E040FB.toInt(), // Soft orchid aura (~13% alpha)
                    0x00000000          // Fade to transparent
                ),
                floatArrayOf(0f, 0.50f, 1f),
                Shader.TileMode.CLAMP
            )

            // Luminous ambient purple/violet light gradient bloom in lower-left
            val purpleRadius = maxOf(w, h) * 0.70f
            cyberPurpleLightPaint.shader = RadialGradient(
                w * 0.18f, h * 0.76f,
                purpleRadius,
                intArrayOf(
                    0x449D00FF.toInt(), // Electric purple/violet core (~27% alpha)
                    0x1C7C4DFF.toInt(), // Soft indigo glow (~11% alpha)
                    0x00000000          // Fade to transparent
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )

            // Soft diagonal light gradient wash across center
            cyberLinearGlowPaint.shader = LinearGradient(
                0f, h * 0.28f,
                w, h * 0.72f,
                intArrayOf(
                    0x00FF2A85,          // Transparent top-left
                    0x22FF4081.toInt(), // Soft translucent pink mid-wash (~13% alpha)
                    0x1EB026FF.toInt(), // Soft translucent violet wash (~12% alpha)
                    0x00B026FF          // Transparent bottom-right
                ),
                floatArrayOf(0f, 0.38f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )

            for (i in NEON_LINES_CONFIG.indices) {
                val config = NEON_LINES_CONFIG[i]
                val path = cachedNeonPaths[i]
                path.reset()
                val pts = config.points
                if (pts.isNotEmpty()) {
                    path.moveTo(pts[0].first * w, pts[0].second * h)
                    for (j in 1 until pts.size) {
                        path.lineTo(pts[j].first * w, pts[j].second * h)
                    }
                }
                cachedPathMeasures[i].setPath(path, false)
                cachedPathLengths[i] = cachedPathMeasures[i].length
            }
        }

        // 2. Draw base cyberpunk purple/violet dark gradient
        canvas.drawRect(0f, 0f, w, h, cyberBasePaint)

        val timeSec = (System.currentTimeMillis() % 1_000_000L) / 1000f

        // 3. Draw atmospheric pink and purple light gradients with subtle breathing pulse
        val pinkPulse = 0.82f + 0.18f * sin(timeSec * 1.3f)
        cyberPinkLightPaint.alpha = (255 * pinkPulse).toInt().coerceIn(0, 255)
        canvas.drawRect(0f, 0f, w, h, cyberPinkLightPaint)

        val purplePulse = 0.82f + 0.18f * cos(timeSec * 1.0f)
        cyberPurpleLightPaint.alpha = (255 * purplePulse).toInt().coerceIn(0, 255)
        canvas.drawRect(0f, 0f, w, h, cyberPurpleLightPaint)

        val linearPulse = 0.80f + 0.20f * sin(timeSec * 0.8f + 1.2f)
        cyberLinearGlowPaint.alpha = (255 * linearPulse).toInt().coerceIn(0, 255)
        canvas.drawRect(0f, 0f, w, h, cyberLinearGlowPaint)

        // 4. Draw ambient cyber grid with subtle violet glow
        val gridStep = (w / 14f).coerceIn(60f, 95f)
        val gridPulse = 0.75f + 0.25f * sin(timeSec * 1.6f)
        cyberGridPaint.alpha = (26 * gridPulse).toInt().coerceIn(8, 50)

        var gx = gridStep
        while (gx < w) {
            canvas.drawLine(gx, 0f, gx, h, cyberGridPaint)
            gx += gridStep
        }
        var gy = gridStep
        while (gy < h) {
            canvas.drawLine(0f, gy, w, gy, cyberGridPaint)
            gy += gridStep
        }

        // 4. Draw each glowing neon line and its traveling light packet
        for (i in NEON_LINES_CONFIG.indices) {
            val config = NEON_LINES_CONFIG[i]
            val path = cachedNeonPaths[i]
            val pathLen = cachedPathLengths[i]
            val pts = config.points

            // Pulsing rhythm
            val pulse = 0.65f + 0.35f * sin(timeSec * config.pulseSpeed + config.pulsePhase)

            // Layer 1: Outer soft neon aura
            neonOuterGlowPaint.color = config.color
            neonOuterGlowPaint.strokeWidth = 14f + 6f * pulse
            neonOuterGlowPaint.alpha = (45 * pulse).toInt().coerceIn(10, 100)
            canvas.drawPath(path, neonOuterGlowPaint)

            // Layer 2: Mid vibrant neon halo
            neonMidGlowPaint.color = config.color
            neonMidGlowPaint.strokeWidth = 6f + 2.5f * pulse
            neonMidGlowPaint.alpha = (115 * pulse).toInt().coerceIn(30, 200)
            canvas.drawPath(path, neonMidGlowPaint)

            // Layer 3: Intense inner colored core
            neonCorePaint.color = config.color
            neonCorePaint.alpha = (235 + 20 * pulse).toInt().coerceIn(180, 255)
            canvas.drawPath(path, neonCorePaint)

            // Layer 4: Ultra bright white-hot center line
            neonWhiteCorePaint.alpha = (180 * pulse).toInt().coerceIn(60, 255)
            canvas.drawPath(path, neonWhiteCorePaint)

            // Terminal pads (vias) on ends that are inside the screen bounds
            if (pts.isNotEmpty()) {
                val first = pts.first()
                if (first.first in 0.05f..0.95f && first.second in 0.05f..0.95f) {
                    val tx = first.first * w
                    val ty = first.second * h
                    drawTerminalPad(canvas, tx, ty, config.color, pulse)
                }
                val last = pts.last()
                if (last.first in 0.05f..0.95f && last.second in 0.05f..0.95f) {
                    val tx = last.first * w
                    val ty = last.second * h
                    drawTerminalPad(canvas, tx, ty, config.color, pulse)
                }
            }

            // Layer 5: Traveling glowing light packet
            if (pathLen > 0f) {
                val travel = ((timeSec * config.packetSpeed + config.packetOffset) % 1.0f) * pathLen
                cachedPathMeasures[i].getPosTan(travel, tempPos, null)
                val px = tempPos[0]
                val py = tempPos[1]

                neonNodeOuterPaint.color = config.color
                neonNodeOuterPaint.alpha = (80 * pulse).toInt().coerceIn(20, 150)
                canvas.drawCircle(px, py, 15f + 4f * pulse, neonNodeOuterPaint)

                neonNodeMidPaint.color = config.color
                neonNodeMidPaint.alpha = (175 * pulse).toInt().coerceIn(50, 255)
                canvas.drawCircle(px, py, 6.5f + 2f * pulse, neonNodeMidPaint)

                neonNodeCorePaint.alpha = (230 + 25 * pulse).toInt().coerceIn(200, 255)
                canvas.drawCircle(px, py, 2.8f, neonNodeCorePaint)
            }
        }
    }

    private fun drawTerminalPad(canvas: Canvas, x: Float, y: Float, color: Int, pulse: Float) {
        neonTerminalRingPaint.color = color
        neonTerminalRingPaint.alpha = (160 * pulse).toInt().coerceIn(50, 255)
        canvas.drawCircle(x, y, 6.5f, neonTerminalRingPaint)

        neonTerminalDotPaint.color = color
        neonTerminalDotPaint.alpha = (230 + 25 * pulse).toInt().coerceIn(180, 255)
        canvas.drawCircle(x, y, 3.0f, neonTerminalDotPaint)
    }

    private fun drawMagicalSparkles(canvas: Canvas, viewWidth: Int, viewHeight: Int) {
        val w = viewWidth.toFloat()
        val h = viewHeight.toFloat()

        // 1. Rebuild shaders if dimensions changed
        if (lastFantasyW != viewWidth || lastFantasyH != viewHeight) {
            lastFantasyW = viewWidth
            lastFantasyH = viewHeight

            fantasyBasePaint.shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(
                    0xFF0D0622.toInt(), // Deep mystical dark violet at top
                    0xFF1A0A33.toInt(), // Twilight purple
                    0xFF240E3E.toInt(), // Enchanted plum
                    0xFF110524.toInt()  // Deep velvet night at bottom
                ),
                floatArrayOf(0f, 0.32f, 0.68f, 1f),
                Shader.TileMode.CLAMP
            )

            val pinkRadius = maxOf(w, h) * 0.62f
            fantasyNebulaPinkPaint.shader = RadialGradient(
                w * 0.82f, h * 0.18f,
                pinkRadius,
                intArrayOf(
                    0x38E040FB.toInt(), // Fairy magenta/pink core (~22% alpha)
                    0x18FF4081.toInt(), // Soft rose aura (~9% alpha)
                    0x00000000
                ),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )

            val cyanRadius = maxOf(w, h) * 0.65f
            fantasyNebulaCyanPaint.shader = RadialGradient(
                w * 0.16f, h * 0.72f,
                cyanRadius,
                intArrayOf(
                    0x3200E5FF.toInt(), // Celestial starlight cyan (~20% alpha)
                    0x147C4DFF.toInt(), // Soft indigo glow (~8% alpha)
                    0x00000000
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )

            val goldRadius = maxOf(w, h) * 0.55f
            fantasyNebulaGoldPaint.shader = RadialGradient(
                w * 0.50f, h * 0.44f,
                goldRadius,
                intArrayOf(
                    0x25FFD54F.toInt(), // Stardust amber-gold (~15% alpha)
                    0x10FF80AB.toInt(), // Soft blush aura (~6% alpha)
                    0x00000000
                ),
                floatArrayOf(0f, 0.50f, 1f),
                Shader.TileMode.CLAMP
            )
        }

        // 2. Draw mystical twilight base sky
        canvas.drawRect(0f, 0f, w, h, fantasyBasePaint)

        val timeSec = (System.currentTimeMillis() % 1_000_000L) / 1000f

        // 3. Draw ambient breathing nebula glows
        val pinkPulse = 0.85f + 0.15f * sin(timeSec * 0.9f)
        fantasyNebulaPinkPaint.alpha = (255 * pinkPulse).toInt().coerceIn(0, 255)
        canvas.drawRect(0f, 0f, w, h, fantasyNebulaPinkPaint)

        val cyanPulse = 0.85f + 0.15f * cos(timeSec * 0.8f)
        fantasyNebulaCyanPaint.alpha = (255 * cyanPulse).toInt().coerceIn(0, 255)
        canvas.drawRect(0f, 0f, w, h, fantasyNebulaCyanPaint)

        val goldPulse = 0.80f + 0.20f * sin(timeSec * 1.1f + 1.5f)
        fantasyNebulaGoldPaint.alpha = (255 * goldPulse).toInt().coerceIn(0, 255)
        canvas.drawRect(0f, 0f, w, h, fantasyNebulaGoldPaint)

        // 4. Draw distant twinkling starfield
        for (star in DISTANT_STARS_CONFIG) {
            val twinkle = (0.28f + 0.72f * ((sin(timeSec * star.speed + star.phase) + 1f) / 2f)).coerceIn(0.1f, 1f)
            val cx = star.xRatio * w
            val cy = star.yRatio * h
            val radius = star.radius * (0.75f + 0.35f * twinkle)

            fantasyStarPaint.color = star.color
            fantasyStarPaint.alpha = (255 * twinkle).toInt().coerceIn(20, 255)
            canvas.drawCircle(cx, cy, radius, fantasyStarPaint)
        }

        // 5. Draw floating pixie dust / fairy sparkles (gently drifting upwards)
        for (dust in FLOATING_DUST_CONFIG) {
            val yProgress = (dust.initialYRatio - timeSec * dust.riseSpeed) % 1.0f
            val curYRatio = if (yProgress < 0f) yProgress + 1.0f else yProgress
            val sway = sin(timeSec * dust.swaySpeed + dust.initialYRatio * 12f) * dust.swayAmpRatio
            val cx = (dust.initialXRatio + sway).coerceIn(0.01f, 0.99f) * w
            val cy = curYRatio * h

            val twinkle = (0.35f + 0.65f * sin(timeSec * 3.8f + dust.initialXRatio * 25f)).coerceIn(0.1f, 1f)

            // Outer soft glow
            fantasyDustGlowPaint.color = dust.color
            fantasyDustGlowPaint.alpha = (45 * twinkle).toInt().coerceIn(10, 80)
            canvas.drawCircle(cx, cy, dust.radiusDp * 2.2f, fantasyDustGlowPaint)

            // Core colored particle
            fantasyDustCorePaint.color = dust.color
            fantasyDustCorePaint.alpha = (200 * twinkle).toInt().coerceIn(50, 240)
            canvas.drawCircle(cx, cy, dust.radiusDp, fantasyDustCorePaint)

            // Center white highlight
            fantasySparkleCorePaint.alpha = (230 * twinkle).toInt().coerceIn(60, 255)
            canvas.drawCircle(cx, cy, dust.radiusDp * 0.35f, fantasySparkleCorePaint)

            if (dust.hasCrossGlint) {
                val glintLen = dust.radiusDp * (1.8f + 0.8f * twinkle)
                fantasyCrossSparklePaint.color = dust.color
                fantasyCrossSparklePaint.alpha = (180 * twinkle).toInt().coerceIn(40, 220)
                canvas.drawLine(cx - glintLen, cy, cx + glintLen, cy, fantasyCrossSparklePaint)
                canvas.drawLine(cx, cy - glintLen, cx, cy + glintLen, fantasyCrossSparklePaint)
            }
        }

        // 6. Draw prominent 4-point & 8-point magical sparkle stars
        for (star in SPARKLE_STARS_CONFIG) {
            val gleam = 0.5f + 0.5f * sin(timeSec * star.pulseSpeed + star.pulsePhase)
            val driftX = sin(timeSec * star.driftSpeed + star.pulsePhase) * star.driftAmpDp
            val driftY = cos(timeSec * (star.driftSpeed * 0.8f) + star.pulsePhase) * (star.driftAmpDp * 0.6f)
            val cx = star.xRatio * w + driftX
            val cy = star.yRatio * h + driftY
            val currentR = star.outerRadiusDp * (0.68f + 0.42f * gleam)

            // Layer 1: Soft outer radial glow halo
            fantasySparkleOuterGlowPaint.color = star.color
            fantasySparkleOuterGlowPaint.alpha = (50 * gleam).toInt().coerceIn(10, 90)
            canvas.drawCircle(cx, cy, currentR * 0.65f, fantasySparkleOuterGlowPaint)

            // Layer 2: Star body
            fantasySparkleBodyPaint.color = star.color
            fantasySparkleBodyPaint.alpha = (190 + 65 * gleam).toInt().coerceIn(140, 255)

            buildConcaveStarPath(primarySparklePath, cx, cy, currentR)
            canvas.drawPath(primarySparklePath, fantasySparkleBodyPaint)

            if (star.isEightPointed) {
                buildRotatedConcaveStarPath(secondarySparklePath, cx, cy, currentR * 0.52f)
                canvas.drawPath(secondarySparklePath, fantasySparkleBodyPaint)
            }

            // Layer 3: Inner bright white slender star
            fantasySparkleCorePaint.alpha = (210 * gleam).toInt().coerceIn(60, 255)
            buildConcaveStarPath(innerSparklePath, cx, cy, currentR * 0.42f)
            canvas.drawPath(innerSparklePath, fantasySparkleCorePaint)

            // Layer 4: Brilliant white center node
            canvas.drawCircle(cx, cy, (currentR * 0.12f).coerceAtLeast(1.8f), fantasySparkleCorePaint)
        }

        // 7. Draw occasional magical shooting star with radiant tail
        val shootCycle = 5.2f
        val shootActive = 1.15f
        val cycleTime = timeSec % shootCycle

        if (cycleTime < shootActive) {
            val p = cycleTime / shootActive
            val startX = -0.08f * w
            val startY = 0.08f * h
            val endX = 1.08f * w
            val endY = 0.42f * h

            val hx = startX + (endX - startX) * p
            val hy = startY + (endY - startY) * p
            val dx = endX - startX
            val dy = endY - startY
            val len = hypot(dx, dy)

            if (len > 0f) {
                val unitX = dx / len
                val unitY = dy / len
                val trailLen = (w * 0.34f) * sin((p * Math.PI).toFloat()).coerceAtLeast(0f)

                val tx = hx - unitX * trailLen
                val ty = hy - unitY * trailLen

                // Fading gradient on tail
                fantasyShootingStarTailPaint.shader = LinearGradient(
                    hx, hy, tx, ty,
                    intArrayOf(
                        0xFFFFFFFF.toInt(),
                        0xEEFFD700.toInt(),
                        0x66E040FB.toInt(),
                        0x00000000
                    ),
                    floatArrayOf(0f, 0.18f, 0.65f, 1f),
                    Shader.TileMode.CLAMP
                )
                fantasyShootingStarTailPaint.strokeWidth = 3.2f * (1f - p * 0.25f)
                canvas.drawLine(hx, hy, tx, ty, fantasyShootingStarTailPaint)

                // Soft glow trail
                fantasyShootingStarGlowTrailPaint.shader = LinearGradient(
                    hx, hy, tx, ty,
                    intArrayOf(
                        0x8800E5FF.toInt(),
                        0x44E040FB.toInt(),
                        0x00000000
                    ),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
                fantasyShootingStarGlowTrailPaint.strokeWidth = 7.5f * (1f - p * 0.25f)
                canvas.drawLine(hx, hy, tx, ty, fantasyShootingStarGlowTrailPaint)

                // Head burst cross
                val headFlare = 14f * (1f - p * 0.2f)
                canvas.drawLine(hx - headFlare, hy, hx + headFlare, hy, fantasyShootingStarTailPaint)
                canvas.drawLine(hx, hy - headFlare, hx, hy + headFlare, fantasyShootingStarTailPaint)

                // Head center
                canvas.drawCircle(hx, hy, 3.5f, fantasyShootingStarHeadPaint)

                // Trailing spark motes
                for (k in 1..4) {
                    val sparkFrac = k * 0.22f
                    val sx = hx - unitX * (trailLen * sparkFrac) + sin(timeSec * 20f + k) * 3f
                    val sy = hy - unitY * (trailLen * sparkFrac) + cos(timeSec * 20f + k) * 3f
                    fantasyShootingStarSparkPaint.alpha = (255 * (1f - sparkFrac)).toInt().coerceIn(0, 255)
                    canvas.drawCircle(sx, sy, 2.0f, fantasyShootingStarSparkPaint)
                }
            }
        }
    }

    private fun buildConcaveStarPath(path: Path, cx: Float, cy: Float, outerR: Float) {
        path.reset()
        path.moveTo(cx, cy - outerR)
        path.quadTo(cx, cy, cx + outerR, cy)
        path.quadTo(cx, cy, cx, cy + outerR)
        path.quadTo(cx, cy, cx - outerR, cy)
        path.quadTo(cx, cy, cx, cy - outerR)
        path.close()
    }

    private fun buildRotatedConcaveStarPath(path: Path, cx: Float, cy: Float, outerR: Float) {
        val d = outerR * 0.7071f
        path.reset()
        path.moveTo(cx + d, cy - d)
        path.quadTo(cx, cy, cx + d, cy + d)
        path.quadTo(cx, cy, cx - d, cy + d)
        path.quadTo(cx, cy, cx - d, cy - d)
        path.quadTo(cx, cy, cx + d, cy - d)
        path.close()
    }
}
