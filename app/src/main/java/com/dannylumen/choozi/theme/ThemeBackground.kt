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
import android.graphics.Path
import android.graphics.PathMeasure
import kotlin.math.cos
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
    NEON_LINES
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
            // 0. Acid Lime Green - Full horizontal span across upper section
            NeonLineConfig(
                color = 0xFF39FF14.toInt(),
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
            // 2. Cyber Gold - Full horizontal span across mid-upper section
            NeonLineConfig(
                color = 0xFFFFD700.toInt(),
                points = listOf(-0.02f to 0.36f, 0.18f to 0.36f, 0.32f to 0.44f, 0.64f to 0.44f, 0.76f to 0.40f, 1.02f to 0.40f),
                pulseSpeed = 2.7f,
                pulsePhase = 1.1f,
                packetSpeed = 0.35f,
                packetOffset = 0.6f
            ),
            // 3. Laser Blue - Full horizontal span across center section
            NeonLineConfig(
                color = 0xFF00BFFF.toInt(),
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
            // 6. Neon Amber - Full horizontal span across lower section
            NeonLineConfig(
                color = 0xFFFF7700.toInt(),
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
        private val cyberGridPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 1.0f
                color = 0x1400F0FF.toInt()
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

            cyberBasePaint.shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(
                    0xFF07050F.toInt(),
                    0xFF0D0820.toInt(),
                    0xFF050712.toInt()
                ),
                floatArrayOf(0f, 0.45f, 1f),
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

        // 2. Draw base cyberpunk dark gradient
        canvas.drawRect(0f, 0f, w, h, cyberBasePaint)

        val timeSec = (System.currentTimeMillis() % 1_000_000L) / 1000f

        // 3. Draw ambient cyber grid
        val gridStep = (w / 14f).coerceIn(60f, 95f)
        val gridPulse = 0.75f + 0.25f * sin(timeSec * 1.6f)
        cyberGridPaint.alpha = (22 * gridPulse).toInt().coerceIn(6, 45)

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
}
