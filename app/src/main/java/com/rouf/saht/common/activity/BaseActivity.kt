package com.rouf.saht.common.activity

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import com.rouf.saht.R
import com.rouf.saht.setting.view.CustomizationActivity
import io.paperdb.Paper

abstract class BaseActivity : AppCompatActivity() {

    private var isCustomTheme = false
    private var customPrimary = 0
    private var customSecondary = 0
    private var customPrimaryDark = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        val key = Paper.book().read(CustomizationActivity.PREF_THEME, CustomizationActivity.THEME_FOREST)
            ?: CustomizationActivity.THEME_FOREST
        setTheme(CustomizationActivity.themeResId(key))

        if (key == CustomizationActivity.THEME_CUSTOM) {
            val primaryHex = Paper.book().read(CustomizationActivity.PREF_CUSTOM_PRIMARY, "#4CAF50") ?: "#4CAF50"
            val secondaryHex = Paper.book().read(CustomizationActivity.PREF_CUSTOM_SECONDARY, "#F44336") ?: "#F44336"
            try {
                customPrimary = Color.parseColor(primaryHex)
                customSecondary = Color.parseColor(secondaryHex)
                customPrimaryDark = ColorUtils.blendARGB(customPrimary, Color.BLACK, 0.3f)
                isCustomTheme = true
            } catch (_: IllegalArgumentException) {}
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
        val defaultSecondary = ContextCompat.getColor(this, R.color.red_500)
        applyCustomColorsRecursive(view, defaultPrimary, defaultSecondary)
    }

    private fun applyCustomColorsRecursive(view: View, defaultPrimary: Int, defaultSecondary: Int) {
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
            }
            is LinearProgressIndicator -> {
                view.setIndicatorColor(customPrimary)
            }
            is FloatingActionButton -> {
                view.backgroundTintList = ColorStateList.valueOf(customPrimary)
            }
            is MaterialButton -> {
                if (view.backgroundTintList?.defaultColor == defaultPrimary) {
                    view.backgroundTintList = ColorStateList.valueOf(customPrimary)
                }
                if (view.currentTextColor == defaultPrimary) {
                    view.setTextColor(customPrimary)
                }
                if (view.strokeColor?.defaultColor == defaultPrimary) {
                    view.strokeColor = ColorStateList.valueOf(customPrimary)
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

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyCustomColorsRecursive(view.getChildAt(i), defaultPrimary, defaultSecondary)
            }
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
