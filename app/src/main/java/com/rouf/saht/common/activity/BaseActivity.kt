package com.rouf.saht.common.activity

import androidx.appcompat.app.AppCompatActivity
import com.rouf.saht.setting.view.CustomizationActivity
import io.paperdb.Paper

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        val key = Paper.book().read(CustomizationActivity.PREF_THEME, CustomizationActivity.THEME_FOREST)
            ?: CustomizationActivity.THEME_FOREST
        setTheme(CustomizationActivity.themeResId(key))
        super.onCreate(savedInstanceState)
    }
}
