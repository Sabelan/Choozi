package com.dannylumen.choozi.ui.settings

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
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
    }
}
