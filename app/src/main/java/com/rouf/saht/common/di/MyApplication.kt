package com.rouf.saht.common.di

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.rouf.saht.setting.view.SettingsFragment
import dagger.hilt.android.HiltAndroidApp
import io.paperdb.Paper

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Paper.init(this)

        val isDarkMode = Paper.book().read(SettingsFragment.PREF_DARK_MODE, false) ?: false
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}