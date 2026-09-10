package com.dannylumen.choozi.theme

import android.content.Context
import androidx.preference.PreferenceManager
import com.dannylumen.choozi.R

object ThemeManager {
    const val DEFAULT_THEME_ID = "default"
    const val THEME_PACK_PREF_KEY = "theme_pack_settings_key"

    private val themeRegistry = LinkedHashMap<String, ThemePack>()

    init {
        // 1. Default (Classic) Theme
        registerTheme(
            ThemePack(
                id = DEFAULT_THEME_ID,
                displayName = "Classic",
                buildUpAudioAsset = "themes/classic/build_up.mp3",
                buildUpAudioRes = R.raw.build_up,
                finalAudioAsset = "themes/classic/final_bell.mp3",
                finalAudioRes = R.raw.final_bell,
                buildUpVolume = 0.4f,
                finalAudioVolume = 1.0f,
                background = null,
                sprites = emptyList(), // Classic glowing circles
                defaultSpriteScale = 1.0f
            )
        )

        // 2. Pirate Theme Pack
        registerPirateTheme()

        // 3. Cyberpunk Theme Pack
        registerCyberpunkTheme()

        // 4. Fantasy Theme Pack
        registerFantasyTheme()
    }

    /**
     * Registers a theme pack. Call this anytime to add new themes to Choozi!
     */
    fun registerTheme(theme: ThemePack) {
        themeRegistry[theme.id] = theme
    }

    /**
     * Unregisters a theme pack by ID.
     */
    fun unregisterTheme(id: String) {
        if (id != DEFAULT_THEME_ID) {
            themeRegistry.remove(id)
        }
    }

    /**
     * Returns all registered themes in registration order.
     */
    fun getAvailableThemes(): List<ThemePack> {
        return themeRegistry.values.toList()
    }

    /**
     * Retrieves a theme by its unique ID. Returns default if not found.
     */
    fun getTheme(id: String?): ThemePack {
        return themeRegistry[id] ?: themeRegistry[DEFAULT_THEME_ID]!!
    }

    /**
     * Retrieves the active theme based on SharedPreferences.
     */
    fun getCurrentTheme(context: Context): ThemePack {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val selectedId = prefs.getString(THEME_PACK_PREF_KEY, DEFAULT_THEME_ID)
        return getTheme(selectedId)
    }

    private val themeChangeListeners = mutableListOf<() -> Unit>()

    fun addThemeChangeListener(listener: () -> Unit) {
        if (!themeChangeListeners.contains(listener)) {
            themeChangeListeners.add(listener)
        }
    }

    fun removeThemeChangeListener(listener: () -> Unit) {
        themeChangeListeners.remove(listener)
    }

    /**
     * Updates the active theme in SharedPreferences and notifies listeners.
     */
    fun setCurrentTheme(context: Context, themeId: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(THEME_PACK_PREF_KEY, themeId).apply()
        val listeners = ArrayList(themeChangeListeners)
        for (listener in listeners) {
            listener.invoke()
        }
    }

    private fun registerPirateTheme() {
        registerTheme(
            ThemePack(
                id = "pirate",
                displayName = "Pirate",
                buildUpAudioAsset = "themes/pirate/saber_and_saltwater_short.mp3",
                finalAudioAsset = "themes/pirate/final_long.mp3",
                buildUpVolume = 0.5f,
                finalAudioVolume = 0.6f,
                background = ThemeBackground(
                    effect = BackgroundEffect.OCEAN_WAVES
                ),
                sprites = listOf(
                    ThemeSprite(
                        assetPath = "themes/pirate/ship.png",
                        scale = .8f,
                        sway = true,
                        maxSwayAngleDegrees = 8f,
                        swayPeriodMs = 4800L,
                        verticalBobDistance = 5f
                    )
                ),
                defaultSpriteScale = 1f,
                loopBuildUpAudio = true,
                restartAudioOnNewFinger = false,
                stopBuildUpOnFinalNote = true,
                selectionEffect = SelectionAnimationEffect.PIRATE_CANNONS
            )
        )
    }

    private fun registerCyberpunkTheme() {
        registerTheme(
            ThemePack(
                id = "cyberpunk",
                displayName = "Cyberpunk",
                buildUpAudioAsset = "themes/cyberpunk/cyberpunk.mp3",
                finalAudioAsset = "themes/cyberpunk/final.mp3",
                buildUpVolume = 0.4f,
                finalAudioVolume = 1.0f,
                background = ThemeBackground(
                    effect = BackgroundEffect.NEON_LINES,
                    backgroundColor = 0xFF0B0813.toInt()
                ),
                sprites = listOf(
                    ThemeSprite(
                        assetPath = "themes/cyberpunk/mech.png",
                        scale = .85f,
                        offsetXRatio = -0.05f,
                        sway = false
                    )
                ),
                defaultSpriteScale = 1.0f,
                loopBuildUpAudio = true,
                restartAudioOnNewFinger = false,
                stopBuildUpOnFinalNote = true,
                selectionEffect = SelectionAnimationEffect.CYBER_LASERS
            )
        )
    }

    private fun registerFantasyTheme() {
        registerTheme(
            ThemePack(
                id = "fantasy",
                displayName = "Fantasy",
                buildUpAudioAsset = "themes/fantasy/build_up.mp3",
                finalAudioAsset = "themes/fantasy/final.mp3",
                buildUpVolume = 0.5f,
                finalAudioVolume = 1.0f,
                background = ThemeBackground(
                    assetPath = "themes/fantasy/background.jpeg",
                    scaleMode = BackgroundScaleMode.CENTER_CROP,
                    effect = BackgroundEffect.STARS_AND_SPARKLES
                ),
                sprites = listOf(
                    ThemeSprite(
                        assetPath = "themes/fantasy/wizard.png",
                        scale = 0.85f,
                        sway = false
                    ),
                    ThemeSprite(
                        assetPath = "themes/fantasy/witch.png",
                        scale = 0.85f,
                        sway = false
                    )
                ),
                defaultSpriteScale = 1.0f,
                randomizeSprites = true,
                loopBuildUpAudio = true,
                restartAudioOnNewFinger = false,
                stopBuildUpOnFinalNote = true,
                selectionEffect = SelectionAnimationEffect.FANTASY_MAGIC
            )
        )
    }
}
