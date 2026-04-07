package com.rouf.saht.setting.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
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
        themeChanged = savedInstanceState?.getBoolean(KEY_THEME_CHANGED, false) ?: false
        binding = ActivityCustomizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentTheme = Paper.book().read(PREF_THEME, THEME_FOREST) ?: THEME_FOREST
        updateThemeSelection(currentTheme)
        updateCustomCardPreview()

        val isDarkMode = Paper.book().read(SettingsFragment.PREF_DARK_MODE, false) ?: false
        binding.switchDarkMode.isChecked = isDarkMode

        setupDefaultScreen()
        setupThemeCards()
        setupDarkModeSwitch()
        setupDoubleTapSwitch()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_THEME_CHANGED, themeChanged)
    }

    override fun onResume() {
        super.onResume()
        val isServiceEnabled = LockScreenAccessibilityService.isEnabled(this)
        binding.switchDoubleTapLock.isChecked = isServiceEnabled
        if (!isServiceEnabled) {
            Paper.book().write(SettingsFragment.PREF_DOUBLE_TAP_LOCK, false)
        }
    }

    private fun setupThemeCards() {
        mapOf(
            THEME_FOREST     to binding.cardThemeForest,
            THEME_OCEAN      to binding.cardThemeOcean,
            THEME_SUNSET     to binding.cardThemeSunset,
            THEME_BERRY      to binding.cardThemeBerry,
            THEME_MONO_LIGHT to binding.cardThemeMonoLight,
            THEME_MONO_DARK  to binding.cardThemeMonoDark,
        ).forEach { (key, card) ->
            card.setOnClickListener {
                if (Paper.book().read(PREF_THEME, THEME_FOREST) != key) {
                    Paper.book().write(PREF_THEME, key)
                    themeChanged = true
                    recreate()
                }
            }
        }

        binding.cardThemeCustom.setOnClickListener {
            showCustomColorDialog()
        }
    }

    private fun showCustomColorDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_theme, null)

        val etPrimary = dialogView.findViewById<TextInputEditText>(R.id.et_primary_hex)
        val etSecondary = dialogView.findViewById<TextInputEditText>(R.id.et_secondary_hex)
        val previewPrimary = dialogView.findViewById<View>(R.id.preview_primary)
        val previewSecondary = dialogView.findViewById<View>(R.id.preview_secondary)
        val pickerPrimary = dialogView.findViewById<ColorPickerView>(R.id.picker_primary)
        val pickerSecondary = dialogView.findViewById<ColorPickerView>(R.id.picker_secondary)
        val stripPrimary = dialogView.findViewById<View>(R.id.preview_strip_primary)
        val stripSecondary = dialogView.findViewById<View>(R.id.preview_strip_secondary)

        val savedPrimary = Paper.book().read(PREF_CUSTOM_PRIMARY, "#4CAF50") ?: "#4CAF50"
        val savedSecondary = Paper.book().read(PREF_CUSTOM_SECONDARY, "#F44336") ?: "#F44336"

        etPrimary.setText(savedPrimary)
        etSecondary.setText(savedSecondary)

        fun updatePreview(hex: String, preview: View, strip: View) {
            try {
                val color = Color.parseColor(hex)
                val bg = preview.background
                if (bg is GradientDrawable) {
                    bg.setColor(color)
                } else {
                    preview.setBackgroundColor(color)
                }
                strip.setBackgroundColor(color)
            } catch (_: IllegalArgumentException) { }
        }

        updatePreview(savedPrimary, previewPrimary, stripPrimary)
        updatePreview(savedSecondary, previewSecondary, stripSecondary)

        try { pickerPrimary.setColor(Color.parseColor(savedPrimary)) } catch (_: Exception) {}
        try { pickerSecondary.setColor(Color.parseColor(savedSecondary)) } catch (_: Exception) {}

        var updatingFromPicker = false

        pickerPrimary.onColorChanged = { color ->
            updatingFromPicker = true
            val hex = String.format("#%06X", 0xFFFFFF and color)
            etPrimary.setText(hex)
            updatePreview(hex, previewPrimary, stripPrimary)
            updatingFromPicker = false
        }

        pickerSecondary.onColorChanged = { color ->
            updatingFromPicker = true
            val hex = String.format("#%06X", 0xFFFFFF and color)
            etSecondary.setText(hex)
            updatePreview(hex, previewSecondary, stripSecondary)
            updatingFromPicker = false
        }

        etPrimary.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hex = s?.toString() ?: return
                if (hex.matches(Regex("#[0-9A-Fa-f]{6}"))) {
                    updatePreview(hex, previewPrimary, stripPrimary)
                    if (!updatingFromPicker) {
                        try { pickerPrimary.setColor(Color.parseColor(hex)) } catch (_: Exception) {}
                    }
                }
            }
        })

        etSecondary.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hex = s?.toString() ?: return
                if (hex.matches(Regex("#[0-9A-Fa-f]{6}"))) {
                    updatePreview(hex, previewSecondary, stripSecondary)
                    if (!updatingFromPicker) {
                        try { pickerSecondary.setColor(Color.parseColor(hex)) } catch (_: Exception) {}
                    }
                }
            }
        })

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val hexRegex = Regex("#[0-9A-Fa-f]{6}")
                val rawPrimary = etPrimary.text?.toString()?.trim() ?: ""
                val rawSecondary = etSecondary.text?.toString()?.trim() ?: ""
                val primaryHex = if (rawPrimary.matches(hexRegex)) rawPrimary else "#4CAF50"
                val secondaryHex = if (rawSecondary.matches(hexRegex)) rawSecondary else "#F44336"
                Paper.book().write(PREF_CUSTOM_PRIMARY, primaryHex)
                Paper.book().write(PREF_CUSTOM_SECONDARY, secondaryHex)
                Paper.book().write(PREF_THEME, THEME_CUSTOM)
                themeChanged = true
                recreate()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateCustomCardPreview() {
        val primaryHex = Paper.book().read(PREF_CUSTOM_PRIMARY, "#4CAF50") ?: "#4CAF50"
        val secondaryHex = Paper.book().read(PREF_CUSTOM_SECONDARY, "#F44336") ?: "#F44336"
        try { binding.customPreviewPrimary.setBackgroundColor(Color.parseColor(primaryHex)) } catch (_: Exception) {}
        try { binding.customPreviewSecondary.setBackgroundColor(Color.parseColor(secondaryHex)) } catch (_: Exception) {}
    }

    private fun updateThemeSelection(selectedKey: String) {
        val strokePx = (2.5f * resources.displayMetrics.density).toInt()
        data class Entry(val key: String, val card: MaterialCardView, val hex: String)
        val presets = listOf(
            Entry(THEME_FOREST,     binding.cardThemeForest,    "#4CAF50"),
            Entry(THEME_OCEAN,      binding.cardThemeOcean,     "#2196F3"),
            Entry(THEME_SUNSET,     binding.cardThemeSunset,    "#FF5722"),
            Entry(THEME_BERRY,      binding.cardThemeBerry,     "#9C27B0"),
            Entry(THEME_MONO_LIGHT, binding.cardThemeMonoLight, "#424242"),
            Entry(THEME_MONO_DARK,  binding.cardThemeMonoDark,  "#BDBDBD"),
        )
        presets.forEach { (key, card, hex) ->
            if (key == selectedKey) {
                card.strokeWidth = strokePx
                card.strokeColor = Color.parseColor(hex)
            } else {
                card.strokeWidth = 0
            }
        }

        // Custom card selection
        if (selectedKey == THEME_CUSTOM) {
            val customHex = Paper.book().read(PREF_CUSTOM_PRIMARY, "#4CAF50") ?: "#4CAF50"
            binding.cardThemeCustom.strokeWidth = strokePx
            try { binding.cardThemeCustom.strokeColor = Color.parseColor(customHex) } catch (_: Exception) {}
        } else {
            binding.cardThemeCustom.strokeWidth = 0
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
        private const val KEY_THEME_CHANGED = "theme_changed"

        const val PREF_THEME   = "pref_theme"
        const val THEME_FOREST = "forest"
        const val THEME_OCEAN  = "ocean"
        const val THEME_SUNSET = "sunset"
        const val THEME_BERRY      = "berry"
        const val THEME_MONO_LIGHT = "mono_light"
        const val THEME_MONO_DARK  = "mono_dark"
        const val THEME_CUSTOM     = "custom"

        const val PREF_CUSTOM_PRIMARY   = "pref_custom_primary"
        const val PREF_CUSTOM_SECONDARY = "pref_custom_secondary"

        const val PREF_DEFAULT_SCREEN = "pref_default_screen"
        const val SCREEN_DASHBOARD    = "dashboard"
        const val SCREEN_PEDOMETER    = "pedometer"
        const val SCREEN_HEART_RATE   = "heart_rate"
        const val SCREEN_MEDITATION   = "meditation"
        const val SCREEN_SETTINGS     = "settings"

        val SCREEN_KEYS   = arrayOf(SCREEN_DASHBOARD, SCREEN_PEDOMETER, SCREEN_HEART_RATE, SCREEN_MEDITATION, SCREEN_SETTINGS)
        val SCREEN_LABELS = arrayOf("Home", "Pedometer", "Heart Rate", "Meditation", "Settings")

        fun themeResId(key: String): Int = when (key) {
            THEME_OCEAN      -> R.style.Theme_Saht_Ocean
            THEME_SUNSET     -> R.style.Theme_Saht_Sunset
            THEME_BERRY      -> R.style.Theme_Saht_Berry
            THEME_MONO_LIGHT -> R.style.Theme_Saht_MonoLight
            THEME_MONO_DARK  -> R.style.Theme_Saht_MonoDark
            THEME_CUSTOM     -> R.style.Theme_Saht_Custom
            else             -> R.style.Theme_Saht
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
