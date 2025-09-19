package com.dannylumen.choozi.ui.shared

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint

object UiUtils {

    fun getCountdownTextPaint(context: Context): Paint {
        val isDarkTheme = isSystemInDarkTheme(context)
        val textColor = if (isDarkTheme) Color.WHITE else Color.BLACK

        return Paint().apply {
            color = textColor
            textSize = 150f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    private fun isSystemInDarkTheme(context: Context): Boolean {
        return context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
}
