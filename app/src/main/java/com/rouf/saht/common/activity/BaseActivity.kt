package com.rouf.saht.common.activity

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import com.rouf.saht.R
import com.rouf.saht.setting.view.CustomizationActivity
import io.paperdb.Paper

abstract class BaseActivity : AppCompatActivity() {

    private var isCustomTheme = false
    private var customPrimary = 0
    private var customSecondary = 0
    private var customPrimaryDark = 0
    private var customSecondaryDark = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        val key = Paper.book().read(CustomizationActivity.PREF_THEME, CustomizationActivity.THEME_FOREST)
            ?: CustomizationActivity.THEME_FOREST
        setTheme(CustomizationActivity.themeResId(key))

        if (key == CustomizationActivity.THEME_CUSTOM) {
            val primaryHex = Paper.book().read(CustomizationActivity.PREF_CUSTOM_PRIMARY, "#4CAF50") ?: "#4CAF50"
            val secondaryHex = Paper.book().read(CustomizationActivity.PREF_CUSTOM_SECONDARY, "#F44336") ?: "#F44336"
            customPrimary = try { Color.parseColor(primaryHex) } catch (_: Exception) { Color.parseColor("#4CAF50") }
            customSecondary = try { Color.parseColor(secondaryHex) } catch (_: Exception) { Color.parseColor("#F44336") }
            customPrimaryDark = ColorUtils.blendARGB(customPrimary, Color.BLACK, 0.3f)
            customSecondaryDark = ColorUtils.blendARGB(customSecondary, Color.BLACK, 0.3f)
            isCustomTheme = true
        }

        super.onCreate(savedInstanceState)

        if (isCustomTheme) {
            window.statusBarColor = customPrimaryDark
            window.navigationBarColor = customPrimaryDark

            supportFragmentManager.registerFragmentLifecycleCallbacks(
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentViewCreated(
                        fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?
                    ) {
                        applyCustomColors(v)
                    }
                }, true
            )
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (isCustomTheme) {
            applyCustomColors(window.decorView)
        }
    }

    private fun applyCustomColors(view: View) {
        val defaultPrimary = ContextCompat.getColor(this, R.color.green_500)
        val defaultPrimaryDark = ContextCompat.getColor(this, R.color.green_700)
        val defaultSecondary = ContextCompat.getColor(this, R.color.red_500)
        applyCustomColorsRecursive(view, defaultPrimary, defaultPrimaryDark, defaultSecondary)
    }

    private fun applyCustomColorsRecursive(
        view: View, defaultPrimary: Int, defaultPrimaryDark: Int, defaultSecondary: Int
    ) {
        when (view) {
            is BottomNavigationView -> {
                val states = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(customPrimary, ContextCompat.getColor(this, R.color.dark_grey))
                )
                view.itemIconTintList = states
                view.itemTextColor = states
                view.itemActiveIndicatorColor = ColorStateList.valueOf(
                    ColorUtils.setAlphaComponent(customPrimary, 40)
                )
            }
            is Chip -> {
                if (view.chipBackgroundColor?.defaultColor == defaultPrimary) {
                    view.chipBackgroundColor = ColorStateList.valueOf(customPrimary)
                } else if (view.chipBackgroundColor?.defaultColor == defaultSecondary) {
                    view.chipBackgroundColor = ColorStateList.valueOf(customSecondary)
                }
                if (view.chipStrokeColor?.defaultColor == defaultPrimary) {
                    view.chipStrokeColor = ColorStateList.valueOf(customPrimary)
                }
            }
            is TextInputLayout -> {
                if (view.boxStrokeColor == defaultPrimary) {
                    view.boxStrokeColor = customPrimary
                }
                if (view.hintTextColor?.defaultColor == defaultPrimary) {
                    view.hintTextColor = ColorStateList.valueOf(customPrimary)
                }
            }
            is SwitchMaterial -> {
                val uncheckedThumb = view.thumbTintList
                    ?.getColorForState(intArrayOf(-android.R.attr.state_checked), Color.GRAY) ?: Color.GRAY
                val uncheckedTrack = view.trackTintList
                    ?.getColorForState(intArrayOf(-android.R.attr.state_checked), Color.LTGRAY) ?: Color.LTGRAY
                view.thumbTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(customPrimary, uncheckedThumb)
                )
                view.trackTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(ColorUtils.setAlphaComponent(customPrimary, 128), uncheckedTrack)
                )
            }
            is CircularProgressIndicator -> {
                view.setIndicatorColor(customPrimary)
                if (view.trackColor == defaultPrimary) {
                    view.trackColor = ColorUtils.setAlphaComponent(customPrimary, 60)
                }
            }
            is LinearProgressIndicator -> {
                view.setIndicatorColor(customPrimary)
            }
            is FloatingActionButton -> {
                view.backgroundTintList = ColorStateList.valueOf(customPrimary)
            }
            is MaterialCardView -> {
                if (view.strokeColorStateList?.defaultColor == defaultPrimary) {
                    view.strokeColor = customPrimary
                }
            }
            is MaterialButton -> {
                if (view.backgroundTintList?.defaultColor == defaultPrimary) {
                    view.backgroundTintList = ColorStateList.valueOf(customPrimary)
                } else if (view.backgroundTintList?.defaultColor == defaultSecondary) {
                    view.backgroundTintList = ColorStateList.valueOf(customSecondary)
                }
                if (view.currentTextColor == defaultPrimary) {
                    view.setTextColor(customPrimary)
                } else if (view.currentTextColor == defaultSecondary) {
                    view.setTextColor(customSecondary)
                }
                if (view.strokeColor?.defaultColor == defaultPrimary) {
                    view.strokeColor = ColorStateList.valueOf(customPrimary)
                } else if (view.strokeColor?.defaultColor == defaultSecondary) {
                    view.strokeColor = ColorStateList.valueOf(customSecondary)
                }
            }
            is android.widget.SeekBar -> {
                view.thumbTintList = ColorStateList.valueOf(customPrimary)
                view.progressTintList = ColorStateList.valueOf(customPrimary)
                // Update the thumb drawable if it's a GradientDrawable (e.g., green_thumb.xml)
                val thumb = view.thumb
                if (thumb is android.graphics.drawable.StateListDrawable) {
                    try {
                        val method = android.graphics.drawable.StateListDrawable::class.java
                            .getDeclaredMethod("getStateDrawable", Int::class.javaPrimitiveType)
                        method.isAccessible = true
                        val inner = method.invoke(thumb, 0)
                        if (inner is GradientDrawable) {
                            inner.setColor(customPrimary)
                        }
                    } catch (_: Exception) {}
                } else if (thumb is GradientDrawable) {
                    thumb.setColor(customPrimary)
                }
            }
            is ImageView -> {
                val tint = ImageViewCompat.getImageTintList(view)
                if (tint?.defaultColor == defaultPrimary) {
                    ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(customPrimary))
                } else if (tint?.defaultColor == defaultSecondary) {
                    ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(customSecondary))
                }
            }
            is TextView -> {
                if (view.currentTextColor == defaultPrimary) {
                    view.setTextColor(customPrimary)
                } else if (view.currentTextColor == defaultSecondary) {
                    view.setTextColor(customSecondary)
                }
            }
        }

        // Handle drawable backgrounds (gradients and shapes with strokes)
        val bg = view.background
        if (bg is GradientDrawable) {
            applyCustomGradientDrawable(bg, defaultPrimary, defaultPrimaryDark, defaultSecondary)
        }
        // Handle backgroundTint on generic views (not just MaterialButton)
        view.backgroundTintList?.let { tint ->
            if (tint.defaultColor == defaultPrimary) {
                view.backgroundTintList = ColorStateList.valueOf(customPrimary)
            } else if (tint.defaultColor == defaultSecondary) {
                view.backgroundTintList = ColorStateList.valueOf(customSecondary)
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyCustomColorsRecursive(view.getChildAt(i), defaultPrimary, defaultPrimaryDark, defaultSecondary)
            }
        }
    }

    private fun applyCustomGradientDrawable(
        drawable: GradientDrawable, defaultPrimary: Int, defaultPrimaryDark: Int, defaultSecondary: Int
    ) {
        try {
            val colorsField = GradientDrawable::class.java.getDeclaredField("mGradientColors")
            colorsField.isAccessible = true
            val colors = colorsField.get(drawable) as? IntArray
            if (colors != null && colors.size >= 2) {
                var changed = false
                val newColors = colors.copyOf()
                for (i in newColors.indices) {
                    when (newColors[i]) {
                        defaultPrimary -> { newColors[i] = customPrimary; changed = true }
                        defaultPrimaryDark -> { newColors[i] = customPrimaryDark; changed = true }
                        defaultSecondary -> { newColors[i] = customSecondary; changed = true }
                    }
                }
                if (changed) drawable.colors = newColors
            } else {
                // Solid color or stroke
                val solidField = GradientDrawable::class.java.getDeclaredField("mFillColors")
                solidField.isAccessible = true
                val solidColors = solidField.get(drawable) as? ColorStateList
                if (solidColors?.defaultColor == defaultPrimary) {
                    drawable.setColor(customPrimary)
                }
            }
        } catch (_: Exception) {
            // Fallback: check stroke via reflection
        }

        // Handle stroke color
        try {
            val strokeField = GradientDrawable::class.java.getDeclaredField("mStrokeColors")
            strokeField.isAccessible = true
            val strokeColors = strokeField.get(drawable) as? ColorStateList
            if (strokeColors != null) {
                when (strokeColors.defaultColor) {
                    defaultPrimary -> {
                        val width = getStrokeWidth(drawable)
                        drawable.setStroke(width, customPrimary)
                    }
                    defaultSecondary -> {
                        val width = getStrokeWidth(drawable)
                        drawable.setStroke(width, customSecondary)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun getStrokeWidth(drawable: GradientDrawable): Int {
        return try {
            val field = GradientDrawable::class.java.getDeclaredField("mStrokeWidth")
            field.isAccessible = true
            field.getInt(drawable)
        } catch (_: Exception) {
            2
        }
    }

    companion object {
        fun effectivePrimary(context: Context): Int {
            val key = Paper.book().read(CustomizationActivity.PREF_THEME, CustomizationActivity.THEME_FOREST)
                ?: CustomizationActivity.THEME_FOREST
            if (key == CustomizationActivity.THEME_CUSTOM) {
                try {
                    return Color.parseColor(
                        Paper.book().read(CustomizationActivity.PREF_CUSTOM_PRIMARY, "#4CAF50") ?: "#4CAF50"
                    )
                } catch (_: IllegalArgumentException) {}
            }
            return MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorPrimary, Color.GREEN
            )
        }

        fun effectivePrimaryDark(context: Context): Int {
            val key = Paper.book().read(CustomizationActivity.PREF_THEME, CustomizationActivity.THEME_FOREST)
                ?: CustomizationActivity.THEME_FOREST
            if (key == CustomizationActivity.THEME_CUSTOM) {
                try {
                    val primary = Color.parseColor(
                        Paper.book().read(CustomizationActivity.PREF_CUSTOM_PRIMARY, "#4CAF50") ?: "#4CAF50"
                    )
                    return ColorUtils.blendARGB(primary, Color.BLACK, 0.3f)
                } catch (_: IllegalArgumentException) {}
            }
            return MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorPrimaryVariant, Color.GREEN
            )
        }

        fun effectiveSecondary(context: Context): Int {
            val key = Paper.book().read(CustomizationActivity.PREF_THEME, CustomizationActivity.THEME_FOREST)
                ?: CustomizationActivity.THEME_FOREST
            if (key == CustomizationActivity.THEME_CUSTOM) {
                try {
                    return Color.parseColor(
                        Paper.book().read(CustomizationActivity.PREF_CUSTOM_SECONDARY, "#F44336") ?: "#F44336"
                    )
                } catch (_: IllegalArgumentException) {}
            }
            return MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorSecondary, Color.RED
            )
        }
    }
}
