package com.dannylumen.choozi.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import com.dannylumen.choozi.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ThemeSelectorBottomSheet : BottomSheetDialogFragment() {

    var onThemeSelectedListener: ((ThemePack) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.dialog_theme_selector, container, false)
        val themesContainer = root.findViewById<LinearLayout>(R.id.themes_container)

        val context = requireContext()
        val currentTheme = ThemeManager.getCurrentTheme(context)
        val availableThemes = ThemeManager.getAvailableThemes()

        for (theme in availableThemes) {
            val itemView = inflater.inflate(R.layout.item_theme_option, themesContainer, false)
            val iconView = itemView.findViewById<ImageView>(R.id.theme_icon)
            val titleView = itemView.findViewById<TextView>(R.id.theme_title)
            val radioButton = itemView.findViewById<RadioButton>(R.id.theme_radio_button)

            titleView.text = theme.displayName
            val isSelected = theme.id == currentTheme.id
            radioButton.isChecked = isSelected

            // Set the preview icon of whatever will show up underneath a finger
            val firstSprite = theme.sprites.firstOrNull()
            if (firstSprite != null) {
                if (firstSprite.assetPath != null) {
                    val bitmap = AssetLoader.loadBitmap(context, firstSprite.assetPath)
                    if (bitmap != null) {
                        iconView.setImageBitmap(bitmap)
                    } else {
                        iconView.setImageResource(R.drawable.ic_palette)
                    }
                } else if (firstSprite.drawableRes != null) {
                    iconView.setImageResource(firstSprite.drawableRes)
                } else {
                    iconView.setImageResource(R.drawable.ic_default_touch_point)
                }
            } else {
                // Default theme has no sprite - fingers display a glowing circle touch point
                iconView.setImageResource(R.drawable.ic_default_touch_point)
            }

            itemView.setOnClickListener {
                if (theme.id != currentTheme.id) {
                    ThemeManager.setCurrentTheme(context, theme.id)
                    onThemeSelectedListener?.invoke(theme)
                }
                dismiss()
            }

            themesContainer.addView(itemView)
        }

        return root
    }

    companion object {
        const val TAG = "ThemeSelectorBottomSheet"

        fun newInstance(onThemeSelected: ((ThemePack) -> Unit)? = null): ThemeSelectorBottomSheet {
            return ThemeSelectorBottomSheet().apply {
                this.onThemeSelectedListener = onThemeSelected
            }
        }
    }
}
