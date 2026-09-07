package com.dannylumen.choozi

import com.dannylumen.choozi.ui.shared.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoResetSettingsTest {

    @Test
    fun testParseAutoResetSeconds() {
        assertEquals(0, SettingsManager.parseAutoResetSeconds("none"))
        assertEquals(0, SettingsManager.parseAutoResetSeconds("NONE"))
        assertEquals(0, SettingsManager.parseAutoResetSeconds("0"))

        assertEquals(5, SettingsManager.parseAutoResetSeconds("5s"))
        assertEquals(5, SettingsManager.parseAutoResetSeconds("5S"))
        assertEquals(5, SettingsManager.parseAutoResetSeconds("5"))

        assertEquals(10, SettingsManager.parseAutoResetSeconds("10s"))
        assertEquals(10, SettingsManager.parseAutoResetSeconds("10"))

        assertEquals(15, SettingsManager.parseAutoResetSeconds("15s"))
        assertEquals(15, SettingsManager.parseAutoResetSeconds("15"))

        // Defaults to 5 seconds for null or unknown strings
        assertEquals(5, SettingsManager.parseAutoResetSeconds(null))
        assertEquals(5, SettingsManager.parseAutoResetSeconds("invalid"))
        assertEquals(5, SettingsManager.parseAutoResetSeconds(""))
    }
}
