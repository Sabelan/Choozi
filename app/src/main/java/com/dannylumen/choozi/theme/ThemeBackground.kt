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

import android.graphics.LinearGradient
import android.graphics.Path
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
    OCEAN_WAVES
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
    }

    fun draw(context: Context, canvas: Canvas, viewWidth: Int, viewHeight: Int) {
        if (viewWidth <= 0 || viewHeight <= 0) return

        // 1. Animated wave effect
        if (effect == BackgroundEffect.OCEAN_WAVES) {
            drawOceanWaves(canvas, viewWidth, viewHeight)
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
}
