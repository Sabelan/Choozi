package com.dannylumen.choozi.theme

import androidx.annotation.RawRes
import com.dannylumen.choozi.R

enum class SelectionAnimationEffect {
    NONE,
    PIRATE_CANNONS,
    CYBER_LASERS,
    FANTASY_MAGIC
}

/**
 * Encapsulates all assets, sounds, background, and finger sprites for a theme pack.
 */
data class ThemePack(
    val id: String,
    val displayName: String,
    val buildUpAudioAsset: String? = null,
    @param:RawRes val buildUpAudioRes: Int? = null,
    val finalAudioAsset: String? = null,
    @param:RawRes val finalAudioRes: Int? = null,
    val buildUpVolume: Float = 0.4f,
    val finalAudioVolume: Float = 1.0f,
    val background: ThemeBackground? = null,
    val sprites: List<ThemeSprite> = emptyList(),
    val defaultSpriteScale: Float = 1.0f,
    val loopBuildUpAudio: Boolean = false,
    val restartAudioOnNewFinger: Boolean = true,
    val stopBuildUpOnFinalNote: Boolean = true,
    val selectionEffect: SelectionAnimationEffect = SelectionAnimationEffect.NONE,
    val randomizeSprites: Boolean = false
) {
    val hasAnimatedSprites: Boolean
        get() = sprites.any { it.sway }

    /**
     * Returns the sprite for a finger based on its index (and optional already used sprites).
     * If [randomizeSprites] is enabled, randomly selects an available sprite, prioritizing
     * sprites not yet used by other active fingers so each finger gets a different asset.
     * Otherwise cycles sequentially through available sprites in the theme.
     * Returns null if the theme has no sprites (e.g. Classic/Default).
     */
    fun getSpriteForFinger(
        index: Int,
        usedSprites: Collection<ThemeSprite?>? = null,
        random: kotlin.random.Random = kotlin.random.Random.Default
    ): ThemeSprite? {
        if (sprites.isEmpty()) return null
        if (randomizeSprites) {
            val nonNullUsed = usedSprites?.filterNotNull().orEmpty()
            val available = sprites.filterNot { nonNullUsed.contains(it) }
            return if (available.isNotEmpty()) {
                available.random(random)
            } else {
                sprites.random(random)
            }
        }
        val safeIndex = if (index >= 0) index % sprites.size else (-index) % sprites.size
        return sprites[safeIndex]
    }
}
