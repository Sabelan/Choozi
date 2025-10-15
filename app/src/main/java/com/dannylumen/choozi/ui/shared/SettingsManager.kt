package com.dannylumen.choozi.ui.shared

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.dannylumen.choozi.R

object SettingsManager {

    /**
     * Reads the mute setting value from SharedPreferences.
     * @return true if audio is muted or false if not.
     */
    fun isAudioMuted(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val muteKey = context.getString(R.string.settings_mute_key)
        return sharedPreferences.getBoolean(muteKey, false)
    }

    /**
     * Reads the mute music setting value from SharedPreferences.
     * @return true if audio is muted or false if not.
     */
    fun isMusicMuted(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val muteKey = context.getString(R.string.settings_mute_music_key)
        return sharedPreferences.getBoolean(muteKey, false)
    }

    /**
     * Reads the mute selection setting value from SharedPreferences.
     * @return true if audio is muted or false if not.
     */
    fun isSelectionMuted(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val muteKey = context.getString(R.string.settings_mute_selection_key)
        return sharedPreferences.getBoolean(muteKey, false)
    }

    /**
     * Reads the stored theme value from SharedPreferences.
     * @return The saved theme value ("system", "light", or "dark").
     */
    private fun getThemeValue(context: Context): String? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val themeKey = context.getString(R.string.settings_theme_key)
        // Default to "system" if no value is found
        return sharedPreferences.getString(themeKey, "system")
    }

    /**
     * Applies stored theme to the app from context.
     */
    fun applyTheme(context: Context) {
        val themeValue = getThemeValue(context)
        // Call the new overloaded function to avoid duplicating the 'when' block
        applyTheme(themeValue)
    }

    /**
     * Applies specified themeValue to the app.
     * This is used in the listener for when the themeValue is changed.
     */
    fun applyTheme(themeValue: String?) {
        val mode = when (themeValue) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

}
