package com.dannylumen.choozi.ui.settings

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.dannylumen.choozi.R
import com.dannylumen.choozi.ui.shared.SettingsManager

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        // Find the theme preference by its key
        val themePreference: ListPreference? =
            findPreference(getString(R.string.settings_theme_key))

        themePreference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val themeValue = newValue as String
                SettingsManager.applyTheme(themeValue)
                true
            }

        // --- Sound preference logic ---
        val muteAllPreference: SwitchPreferenceCompat? =
            findPreference(getString(R.string.settings_mute_key))

        muteAllPreference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val isMutedAll = newValue as Boolean
                updateDependentSoundPreferences(isMutedAll)
                true // Return true to update the state of the preference
            }

        // Set the initial state of dependent switches when the screen loads
        val isMutedAll = preferenceManager.sharedPreferences?.getBoolean(
            getString(R.string.settings_mute_key),
            false
        ) ?: false
        updateDependentSoundPreferences(isMutedAll)
    }

    private fun updateDependentSoundPreferences(isMutedAll: Boolean) {
        // Find the preference objects
        val muteMusicPreference: SwitchPreferenceCompat? =
            findPreference(getString(R.string.settings_mute_music_key))
        val muteSelectionPreference: SwitchPreferenceCompat? =
            findPreference(getString(R.string.settings_mute_selection_key))

        if (isMutedAll) {
            // When "Mute All" is ON:
            // 1. Check the music and selection switches
            muteMusicPreference?.isChecked = true
            muteSelectionPreference?.isChecked = true
            // 2. Disable them so they can't be changed
            muteMusicPreference?.isEnabled = false
            muteSelectionPreference?.isEnabled = false
        } else {
            // When "Mute All" is OFF:
            // 1. Simply re-enable the switches. Their checked state is preserved from
            //    before, allowing them to be individually toggled.
            muteMusicPreference?.isEnabled = true
            muteSelectionPreference?.isEnabled = true
        }
    }
}
