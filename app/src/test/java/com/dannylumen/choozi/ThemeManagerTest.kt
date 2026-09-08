package com.dannylumen.choozi

import com.dannylumen.choozi.theme.BackgroundEffect
import com.dannylumen.choozi.theme.BackgroundScaleMode
import com.dannylumen.choozi.theme.ThemeBackground
import com.dannylumen.choozi.theme.ThemeManager
import com.dannylumen.choozi.theme.ThemePack
import com.dannylumen.choozi.theme.ThemeSprite
import org.junit.Assert.*
import org.junit.Test

class ThemeManagerTest {

    @Test
    fun testDefaultThemesRegistered() {
        val themes = ThemeManager.getAvailableThemes()
        assertTrue("Theme registry should have at least 2 themes", themes.size >= 2)

        val defaultTheme = themes.first()
        assertEquals(ThemeManager.DEFAULT_THEME_ID, defaultTheme.id)
        assertEquals("themes/default/build_up.mp3", defaultTheme.buildUpAudioAsset)
        assertEquals("themes/default/final_bell.mp3", defaultTheme.finalAudioAsset)
        assertTrue(defaultTheme.sprites.isEmpty())

        val pirateTheme = ThemeManager.getTheme("pirate")
        assertNotNull(pirateTheme)
        assertEquals("Pirate", pirateTheme.displayName)
        assertEquals("themes/pirate/saber_and_saltwater_short.mp3", pirateTheme.buildUpAudioAsset)
        assertEquals("themes/pirate/final_long.mp3", pirateTheme.finalAudioAsset)
        assertEquals(com.dannylumen.choozi.theme.SelectionAnimationEffect.PIRATE_CANNONS, pirateTheme.selectionEffect)
        assertEquals(1, pirateTheme.sprites.size)
        val shipSprite = pirateTheme.sprites.first()
        assertEquals("themes/pirate/ship.png", shipSprite.assetPath)
        assertTrue("Pirate ship should have sway enabled", shipSprite.sway)
        assertTrue(pirateTheme.hasAnimatedSprites)
        assertNotNull(pirateTheme.background)
        assertEquals(BackgroundEffect.OCEAN_WAVES, pirateTheme.background?.effect)
        assertTrue(pirateTheme.background?.isAnimated == true)

        val cyberpunkTheme = ThemeManager.getTheme("cyberpunk")
        assertNotNull(cyberpunkTheme)
        assertEquals("Cyberpunk", cyberpunkTheme.displayName)
        assertEquals("themes/cyberpunk/cyberpunk.mp3", cyberpunkTheme.buildUpAudioAsset)
        assertEquals("themes/cyberpunk/final.mp3", cyberpunkTheme.finalAudioAsset)
        assertEquals(0.4f, cyberpunkTheme.buildUpVolume, 0.001f)
        assertEquals(1.0f, cyberpunkTheme.finalAudioVolume, 0.001f)
        assertEquals(com.dannylumen.choozi.theme.SelectionAnimationEffect.CYBER_LASERS, cyberpunkTheme.selectionEffect)
        assertEquals(1, cyberpunkTheme.sprites.size)
        val mechSprite = cyberpunkTheme.sprites.first()
        assertEquals("themes/cyberpunk/mech.png", mechSprite.assetPath)
        assertEquals(0.85f, mechSprite.scale, 0.001f)
        assertEquals(-0.05f, mechSprite.offsetXRatio, 0.001f)
        assertFalse("Cyberpunk mech should not sway", mechSprite.sway)
        assertFalse(cyberpunkTheme.hasAnimatedSprites)
        assertNotNull(cyberpunkTheme.background)
        assertEquals(BackgroundEffect.NEON_LINES, cyberpunkTheme.background?.effect)
        assertTrue(cyberpunkTheme.background?.isAnimated == true)
        assertEquals(0xFF0B0813.toInt(), cyberpunkTheme.background?.backgroundColor)

        val fantasyTheme = ThemeManager.getTheme("fantasy")
        assertNotNull(fantasyTheme)
        assertEquals("Fantasy", fantasyTheme.displayName)
        assertEquals("themes/fantasy/build_up.mp3", fantasyTheme.buildUpAudioAsset)
        assertEquals("themes/fantasy/final.mp3", fantasyTheme.finalAudioAsset)
        assertEquals(0.5f, fantasyTheme.buildUpVolume, 0.001f)
        assertEquals(1.0f, fantasyTheme.finalAudioVolume, 0.001f)
        assertEquals(1, fantasyTheme.sprites.size)
        val wandSprite = fantasyTheme.sprites.first()
        assertEquals("themes/fantasy/wand.png", wandSprite.assetPath)
        assertFalse("Fantasy wand should not sway", wandSprite.sway)
        assertFalse(fantasyTheme.hasAnimatedSprites)
        assertNotNull(fantasyTheme.background)
        assertEquals(BackgroundEffect.STARS_AND_SPARKLES, fantasyTheme.background?.effect)
        assertTrue(fantasyTheme.background?.isAnimated == true)
        assertEquals(0xFF0E0725.toInt(), fantasyTheme.background?.backgroundColor)
    }

    @Test
    fun testUnknownThemeFallbackToDefault() {
        val unknown = ThemeManager.getTheme("non_existent_theme_id")
        assertEquals(ThemeManager.DEFAULT_THEME_ID, unknown.id)

        val nullTheme = ThemeManager.getTheme(null)
        assertEquals(ThemeManager.DEFAULT_THEME_ID, nullTheme.id)
    }

    @Test
    fun testSpriteCyclingForFingers() {
        val pirateTheme = ThemeManager.getTheme("pirate")
        val spriteCount = pirateTheme.sprites.size
        assertEquals(1, spriteCount)

        val sprite0 = pirateTheme.getSpriteForFinger(0)
        val sprite1 = pirateTheme.getSpriteForFinger(1)
        val spriteCyceled = pirateTheme.getSpriteForFinger(spriteCount)

        assertNotNull(sprite0)
        assertNotNull(sprite1)
        assertEquals("themes/pirate/ship.png", sprite0?.assetPath)
        assertEquals(sprite0, sprite1)
        assertEquals(sprite0, spriteCyceled)

        // Default theme has no sprites
        val defaultTheme = ThemeManager.getTheme(ThemeManager.DEFAULT_THEME_ID)
        assertNull(defaultTheme.getSpriteForFinger(0))

        // Cyberpunk theme mech sprite
        val cyberpunkTheme = ThemeManager.getTheme("cyberpunk")
        val mechSprite0 = cyberpunkTheme.getSpriteForFinger(0)
        assertNotNull(mechSprite0)
        assertEquals("themes/cyberpunk/mech.png", mechSprite0?.assetPath)
        assertEquals(mechSprite0, cyberpunkTheme.getSpriteForFinger(1))

        // Fantasy theme wand sprite
        val fantasyTheme = ThemeManager.getTheme("fantasy")
        val wandSprite0 = fantasyTheme.getSpriteForFinger(0)
        assertNotNull(wandSprite0)
        assertEquals("themes/fantasy/wand.png", wandSprite0?.assetPath)
        assertEquals(wandSprite0, fantasyTheme.getSpriteForFinger(1))
    }

    @Test
    fun testDynamicThemeRegistration() {
        val testThemeId = "space_odyssey"
        val customTheme = ThemePack(
            id = testThemeId,
            displayName = "Space Odyssey",
            buildUpAudioRes = R.raw.build_up,
            finalAudioRes = R.raw.final_bell,
            sprites = listOf(
                ThemeSprite(R.drawable.ic_launcher_foreground, scale = 1.0f)
            )
        )

        ThemeManager.registerTheme(customTheme)
        val retrieved = ThemeManager.getTheme(testThemeId)
        assertEquals("Space Odyssey", retrieved.displayName)
        assertEquals(1, retrieved.sprites.size)

        // Clean up
        ThemeManager.unregisterTheme(testThemeId)
        val afterUnregister = ThemeManager.getTheme(testThemeId)
        assertEquals(ThemeManager.DEFAULT_THEME_ID, afterUnregister.id)
    }

    @Test
    fun testThemeAudioConfiguration() {
        val defaultTheme = ThemeManager.getTheme(ThemeManager.DEFAULT_THEME_ID)
        assertTrue(defaultTheme.restartAudioOnNewFinger)
        assertFalse(defaultTheme.loopBuildUpAudio)
        assertTrue(defaultTheme.stopBuildUpOnFinalNote)

        val pirateTheme = ThemeManager.getTheme("pirate")
        assertFalse("Pirate theme should not restart audio on new finger", pirateTheme.restartAudioOnNewFinger)
        assertTrue("Pirate theme should loop long build up audio", pirateTheme.loopBuildUpAudio)
        assertTrue("Pirate theme should stop build up when final note plays", pirateTheme.stopBuildUpOnFinalNote)

        val cyberpunkTheme = ThemeManager.getTheme("cyberpunk")
        assertFalse("Cyberpunk theme should not restart audio on new finger", cyberpunkTheme.restartAudioOnNewFinger)
        assertTrue("Cyberpunk theme should loop long build up audio", cyberpunkTheme.loopBuildUpAudio)
        assertTrue("Cyberpunk theme should stop build up when final note plays", cyberpunkTheme.stopBuildUpOnFinalNote)

        val fantasyTheme = ThemeManager.getTheme("fantasy")
        assertFalse("Fantasy theme should not restart audio on new finger", fantasyTheme.restartAudioOnNewFinger)
        assertTrue("Fantasy theme should loop long build up audio", fantasyTheme.loopBuildUpAudio)
        assertTrue("Fantasy theme should stop build up when final note plays", fantasyTheme.stopBuildUpOnFinalNote)
    }

    @Test
    fun testAvailableThemesForSelector() {
        val themes = ThemeManager.getAvailableThemes()
        val defaultTheme = themes.find { it.id == ThemeManager.DEFAULT_THEME_ID }
        assertNotNull(defaultTheme)
        assertTrue(defaultTheme!!.sprites.isEmpty())

        val pirateTheme = themes.find { it.id == "pirate" }
        assertNotNull(pirateTheme)
        assertEquals(1, pirateTheme!!.sprites.size)
        assertEquals("themes/pirate/ship.png", pirateTheme.sprites.first().assetPath)

        val cyberpunkTheme = themes.find { it.id == "cyberpunk" }
        assertNotNull(cyberpunkTheme)
        assertEquals(1, cyberpunkTheme!!.sprites.size)
        assertEquals("themes/cyberpunk/mech.png", cyberpunkTheme.sprites.first().assetPath)

        val fantasyTheme = themes.find { it.id == "fantasy" }
        assertNotNull(fantasyTheme)
        assertEquals(1, fantasyTheme!!.sprites.size)
        assertEquals("themes/fantasy/wand.png", fantasyTheme.sprites.first().assetPath)
    }

    @Test
    fun testSpritePointTowardsCenter() {
        // Sprites default to pointing towards the center of the map
        val defaultSprite = ThemeSprite("themes/pirate/ship.png")
        assertTrue(defaultSprite.pointTowardsCenter)

        val customDisabledSprite = ThemeSprite("themes/pirate/ship.png", pointTowardsCenter = false)
        assertFalse(customDisabledSprite.pointTowardsCenter)

        // All registered theme sprites should point towards center
        val pirateSprite = ThemeManager.getTheme("pirate").sprites.first()
        assertTrue("Pirate ship should point towards center", pirateSprite.pointTowardsCenter)

        val cyberpunkSprite = ThemeManager.getTheme("cyberpunk").sprites.first()
        assertTrue("Cyberpunk mech should point towards center", cyberpunkSprite.pointTowardsCenter)

        val fantasySprite = ThemeManager.getTheme("fantasy").sprites.first()
        assertTrue("Fantasy wand should point towards center", fantasySprite.pointTowardsCenter)
    }

    @Test
    fun testThemeChangeListenerRegistration() {
        var notified = false
        val listener = { notified = true }
        ThemeManager.addThemeChangeListener(listener)
        // Verify listener can be added and removed without error
        ThemeManager.removeThemeChangeListener(listener)
        assertFalse(notified)
    }
}
