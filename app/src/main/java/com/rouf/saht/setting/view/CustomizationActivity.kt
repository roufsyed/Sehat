package com.rouf.saht.setting.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rouf.saht.R
import com.rouf.saht.common.activity.BaseActivity
import com.rouf.saht.common.service.LockScreenAccessibilityService
import com.rouf.saht.databinding.ActivityCustomizationBinding
import dagger.hilt.android.AndroidEntryPoint
import io.paperdb.Paper

@AndroidEntryPoint
class CustomizationActivity : BaseActivity() {

    private lateinit var binding: ActivityCustomizationBinding

    private var themeChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentTheme = Paper.book().read(PREF_THEME, THEME_FOREST) ?: THEME_FOREST
        updateThemeSelection(currentTheme)

        val isDarkMode = Paper.book().read(SettingsFragment.PREF_DARK_MODE, false) ?: false
        binding.switchDarkMode.isChecked = isDarkMode

        setupDefaultScreen()
        setupThemeCards(currentTheme)
        setupDarkModeSwitch()
        setupDoubleTapSwitch()
    }

    override fun onResume() {
        super.onResume()
        val isServiceEnabled = LockScreenAccessibilityService.isEnabled(this)
        binding.switchDoubleTapLock.isChecked = isServiceEnabled
        if (!isServiceEnabled) {
            Paper.book().write(SettingsFragment.PREF_DOUBLE_TAP_LOCK, false)
        }
    }

    private fun setupThemeCards(currentTheme: String) {
        mapOf(
            THEME_FOREST to binding.cardThemeForest,
            THEME_OCEAN  to binding.cardThemeOcean,
            THEME_SUNSET to binding.cardThemeSunset,
            THEME_BERRY  to binding.cardThemeBerry,
        ).forEach { (key, card) ->
            card.setOnClickListener {
                if (Paper.book().read(PREF_THEME, THEME_FOREST) != key) {
                    Paper.book().write(PREF_THEME, key)
                    themeChanged = true
                    updateThemeSelection(key)
                }
            }
        }
    }

    private fun updateThemeSelection(selectedKey: String) {
        val strokePx = (2.5f * resources.displayMetrics.density).toInt()
        data class Entry(val key: String, val card: MaterialCardView, val hex: String)
        listOf(
            Entry(THEME_FOREST, binding.cardThemeForest, "#4CAF50"),
            Entry(THEME_OCEAN,  binding.cardThemeOcean,  "#2196F3"),
            Entry(THEME_SUNSET, binding.cardThemeSunset, "#FF5722"),
            Entry(THEME_BERRY,  binding.cardThemeBerry,  "#9C27B0"),
        ).forEach { (key, card, hex) ->
            if (key == selectedKey) {
                card.strokeWidth = strokePx
                card.strokeColor = Color.parseColor(hex)
            } else {
                card.strokeWidth = 0
            }
        }
    }

    private fun setupDefaultScreen() {
        val labels = SCREEN_LABELS
        val currentKey = Paper.book().read(PREF_DEFAULT_SCREEN, SCREEN_DASHBOARD) ?: SCREEN_DASHBOARD
        binding.tvDefaultScreenValue.text = labels[SCREEN_KEYS.indexOf(currentKey)]

        binding.llDefaultScreen.setOnClickListener {
            val saved = Paper.book().read(PREF_DEFAULT_SCREEN, SCREEN_DASHBOARD) ?: SCREEN_DASHBOARD
            var selected = SCREEN_KEYS.indexOf(saved).coerceAtLeast(0)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.default_screen)
                .setSingleChoiceItems(labels, selected) { _, which -> selected = which }
                .setPositiveButton(R.string.save) { _, _ ->
                    Paper.book().write(PREF_DEFAULT_SCREEN, SCREEN_KEYS[selected])
                    binding.tvDefaultScreenValue.text = labels[selected]
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun setupDarkModeSwitch() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            Paper.book().write(SettingsFragment.PREF_DARK_MODE, isChecked)
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                       else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private fun setupDoubleTapSwitch() {
        binding.switchDoubleTapLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!LockScreenAccessibilityService.isEnabled(this)) {
                    binding.switchDoubleTapLock.isChecked = false
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    Toast.makeText(
                        this,
                        "Find 'Sehat' and enable it to allow double-tap locking",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Paper.book().write(SettingsFragment.PREF_DOUBLE_TAP_LOCK, true)
                }
            } else {
                Paper.book().write(SettingsFragment.PREF_DOUBLE_TAP_LOCK, false)
            }
        }
    }

    override fun finish() {
        if (themeChanged) setResult(Activity.RESULT_OK)
        super.finish()
    }

    companion object {
        const val PREF_THEME   = "pref_theme"
        const val THEME_FOREST = "forest"
        const val THEME_OCEAN  = "ocean"
        const val THEME_SUNSET = "sunset"
        const val THEME_BERRY  = "berry"

        const val PREF_DEFAULT_SCREEN = "pref_default_screen"
        const val SCREEN_DASHBOARD    = "dashboard"
        const val SCREEN_PEDOMETER    = "pedometer"
        const val SCREEN_HEART_RATE   = "heart_rate"
        const val SCREEN_MEDITATION   = "meditation"
        const val SCREEN_SETTINGS     = "settings"

        val SCREEN_KEYS   = arrayOf(SCREEN_DASHBOARD, SCREEN_PEDOMETER, SCREEN_HEART_RATE, SCREEN_MEDITATION, SCREEN_SETTINGS)
        val SCREEN_LABELS = arrayOf("Home", "Pedometer", "Heart Rate", "Meditation", "Settings")

        fun themeResId(key: String): Int = when (key) {
            THEME_OCEAN  -> R.style.Theme_Saht_Ocean
            THEME_SUNSET -> R.style.Theme_Saht_Sunset
            THEME_BERRY  -> R.style.Theme_Saht_Berry
            else         -> R.style.Theme_Saht
        }

        fun defaultScreenNavId(key: String): Int = when (key) {
            SCREEN_PEDOMETER  -> R.id.navigation_pedometer
            SCREEN_HEART_RATE -> R.id.navigation_heartRate
            SCREEN_MEDITATION -> R.id.navigation_meditation
            SCREEN_SETTINGS   -> R.id.navigation_settings
            else              -> R.id.navigation_dashboard
        }
    }
}
