package com.dannylumen.choozi

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dannylumen.choozi.databinding.ActivityMainBinding
import com.dannylumen.choozi.ui.shared.SettingsManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before anything else.
        SettingsManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            isFabAllowedByDestination = (destination.id != R.id.navigation_settings)
            isFabAllowedByGameInteraction = true
            updateFabVisibility()
        }

        binding.fabThemeSelector.setOnClickListener {
            com.dannylumen.choozi.theme.ThemeSelectorBottomSheet.newInstance {
                refreshCurrentGameView()
            }.show(supportFragmentManager, com.dannylumen.choozi.theme.ThemeSelectorBottomSheet.TAG)
        }
    }

    private var isFabAllowedByDestination = true
    private var isFabAllowedByGameInteraction = true

    fun setThemeFabVisible(visible: Boolean) {
        isFabAllowedByGameInteraction = visible
        updateFabVisibility()
    }

    private fun updateFabVisibility() {
        val shouldShow = isFabAllowedByDestination && isFabAllowedByGameInteraction
        if (shouldShow) {
            binding.fabThemeSelector.show()
        } else {
            binding.fabThemeSelector.hide()
        }
    }

    private fun refreshCurrentGameView() {
        binding.navHostFragmentActivityMain.invalidateAll()
    }

    private fun android.view.View.invalidateAll() {
        invalidate()
        if (this is android.view.ViewGroup) {
            for (i in 0 until childCount) {
                getChildAt(i).invalidateAll()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}