# Choozi Theme Packs Guide

This guide explains how the Choozi Theme Pack system works and how to create and register new themes.

---

## 1. How It Works

Choozi's gameplay relies on multi-touch circles with pulsating glow animations and countdown audio. The theme system overlays custom audio and visuals on top of this foundation while preserving gameplay logic:

1. **Audio**: [`AudioManager`](file:///c:/Users/Uniqu/Documents/git/Choozi/app/src/main/java/com/dannylumen/choozi/ui/shared/AudioPlayer.kt) queries [`ThemeManager.getCurrentTheme(context)`](file:///c:/Users/Uniqu/Documents/git/Choozi/app/src/main/java/com/dannylumen/choozi/theme/ThemeManager.kt) to play the theme's build-up music during countdown and victory sound effect when a choice is made.
2. **Sprites**: When fingers touch the screen, [`FingerPoint`](file:///c:/Users/Uniqu/Documents/git/Choozi/app/src/main/java/com/dannylumen/choozi/ui/shared/FingerPoint.kt) receives a sprite from the active theme. The glowing circle is drawn first, followed by the sprite centered on top. If numbers are displayed (Ordering mode), a high-contrast badge is drawn over the sprite.
3. **Background**: The game views draw the theme's background on the canvas before rendering touch points, scaling it to fit any screen aspect ratio without distortion. Animated backgrounds (like `BackgroundEffect.OCEAN_WAVES`) run smoothly at 60+ FPS via `postInvalidateOnAnimation()`.
4. **Settings Menu**: [`SettingsFragment`](file:///c:/Users/Uniqu/Documents/git/Choozi/app/src/main/java/com/dannylumen/choozi/ui/settings/SettingsFragment.kt) dynamically reads all registered themes from [`ThemeManager.getAvailableThemes()`](file:///c:/Users/Uniqu/Documents/git/Choozi/app/src/main/java/com/dannylumen/choozi/theme/ThemeManager.kt), so any newly added theme automatically appears in the dropdown without XML changes.

---

## 2. Directory Layout for Assets

### Recommended: Colocated Theme Folders (`app/src/main/assets/themes/<theme_id>/`)

All files for a single theme can be placed together in a single dedicated folder inside `assets/`:

```
app/src/main/assets/themes/
├── default/
│   ├── build_up.wav          # Classic countdown tone
│   └── final_bell.wav        # Classic winning chime
└── pirate/
    ├── build_up.wav          # Countdown pirate music
    ├── final.wav             # Cannon victory SFX
    └── ship.png              # Galleon pirate ship finger sprite
```

| Asset Type | Supported Formats | Location | Notes |
| :--- | :--- | :--- | :--- |
| **Audio** (Music & SFX) | `.wav`, `.mp3`, `.ogg` | `themes/<theme_id>/` | Referenced via `buildUpAudioAsset` and `finalAudioAsset` |
| **Sprites** | Transparent `.png`, `.webp` | `themes/<theme_id>/` | Transparent background recommended so glowing circles show |
| **Backgrounds** | `.png`, `.webp`, `.jpg` OR Animated Effects | `themes/<theme_id>/` or `BackgroundEffect` | Image files or procedural effects like `BackgroundEffect.OCEAN_WAVES` |

### Alternative: Android Resource Folders (`res/raw/` & `res/drawable/`)
Traditional Android resource folders are also fully supported using `buildUpAudioRes`, `finalAudioRes`, and `drawableRes` properties.

---

## 3. Step-by-Step: Adding a New Theme

### Step 1: Create a Theme Folder in `assets/themes/`
Create a folder under `app/src/main/assets/themes/<theme_id>/` (e.g. `space/`):
- Drop your countdown music (`build_up.wav` or `.mp3`) and finish sound (`final.wav`).
- Drop your background image (`background.png`) or use an animated effect.
- Drop your finger sprites (`rocket.png`, `astronaut.png`, etc.).

### Step 2: Register the Theme in `ThemeManager.kt`
Open [`ThemeManager.kt`](file:///c:/Users/Uniqu/Documents/git/Choozi/app/src/main/java/com/dannylumen/choozi/theme/ThemeManager.kt) and call `registerTheme(...)`:

```kotlin
registerTheme(
    ThemePack(
        id = "space",                                   // Unique identifier (saved in SharedPreferences)
        displayName = "Space Exploration",              // Display name in Settings dropdown
        
        // --- Colocated Audio ---
        buildUpAudioAsset = "themes/space/build_up.mp3",
        finalAudioAsset = "themes/space/final.wav",
        buildUpVolume = 0.5f,
        finalAudioVolume = 1.0f,
        
        // Audio playback behavior
        restartAudioOnNewFinger = false,                // Keep music playing when fingers change
        loopBuildUpAudio = true,                        // Loop if track finishes before timer
        stopBuildUpOnFinalNote = true,                  // Cut countdown music when winner is picked
        
        // --- Background (Image or Animated Effect) ---
        background = ThemeBackground(
            assetPath = "themes/space/background.png",
            scaleMode = BackgroundScaleMode.CENTER_CROP
        ),
        
        // --- Colocated Sprites ---
        sprites = listOf(
            ThemeSprite("themes/space/rocket.png", scale = 1.15f),
            ThemeSprite("themes/space/astronaut.png", scale = 1.05f)
        ),
        defaultSpriteScale = 1.1f
    )
)
```

The new theme is immediately available in the app and in the Settings dropdown menu.

---

## 4. Fine-Tuning Sprites & Backgrounds

### Sprite Resizing & Offsets (`ThemeSprite`)
Sprites from different sources often vary in proportions or canvas padding. You can tune each sprite individually:

```kotlin
ThemeSprite(
    assetPath = "themes/pirate/ship.png",
    scale = 1.15f,       // Scale relative to the finger circle (1.0 = exact fit, >1.0 = larger)
    offsetXRatio = 0.0f, // Horizontal adjustment (-0.5 to 0.5) if artwork is off-center
    offsetYRatio = 0.0f  // Vertical adjustment
)
```
*Note: The renderer automatically calculates `FIT_CENTER` bounds to preserve the original image aspect ratio without squishing or stretching.*

### Background Effects & Slicing Modes (`ThemeBackground`)
You can use either procedural animated effects or static images:

#### 1. Animated Background Effects (`BackgroundEffect`)
- **`BackgroundEffect.OCEAN_WAVES`**: Renders deep nautical ocean blue gradients with multiple undulating, rolling wave layers and soft sea foam crest highlights that animate continuously at 60+ FPS.
- **`BackgroundEffect.NEON_LINES`**: Renders a deep synthwave violet and dark plum background bathed in glowing pink and purple radial/linear light gradients, an ambient pulsed cyber grid, and animated traveling neon circuit packets at 60+ FPS.
- **`BackgroundEffect.STARS_AND_SPARKLES`**: Renders a magical enchanted twilight night sky with pulsing celestial nebulae (fairy magenta, starlight cyan, stardust gold), a twinkling starfield, floating pixie dust sparkles drifting upwards with whimsical swaying, gleaming 4-point and 8-point fairy tale sparkle stars, and periodic shooting stars with radiant tails at 60+ FPS.

```kotlin
background = ThemeBackground(
    effect = BackgroundEffect.OCEAN_WAVES
)
```

#### 2. Static Background Slicing Modes (`BackgroundScaleMode`)
- **`CENTER_CROP`** *(Recommended for wallpapers/illustrations)*: Uniformly scales the image so it covers the whole screen, slicing excess margins off the sides or top/bottom without distortion.
- **`TILE`**: Seamlessly repeats small textures or tile patterns across the screen.
- **`NINE_PATCH`**: For Android 9-patch drawables with fixed borders and stretchable centers.
- **`STRETCH`**: Fills width and height directly without maintaining aspect ratio.

### Audio Playback & Long Track Modes
Themes with longer music tracks (such as a 1-minute theme song) can configure continuous playback and looping:

- **`restartAudioOnNewFinger = false`**: Keeps audio playing uninterrupted when additional fingers are placed down, rather than restarting from the beginning. *(Default is `true` for classic short build-up tracks)*.
- **`loopBuildUpAudio = true`**: Seamlessly loops the build-up track if it finishes while fingers are still down. *(Default is `false`)*.
- **`stopBuildUpOnFinalNote = true`**: Automatically cuts the build-up music as soon as the timer finishes so the winning sound effect plays cleanly. *(Default is `true`)*.
