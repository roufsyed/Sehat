package com.rouf.saht.setting.view

import android.content.res.Configuration
import android.os.Bundle
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.MaterialColors
import com.rouf.saht.common.activity.BaseActivity
import com.rouf.saht.databinding.ActivityDashboardSettingsBinding
import io.paperdb.Paper

class DashboardSettingsActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val surfaceColor = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorSurface, 0
        )
        binding.appBar.setBackgroundColor(surfaceColor)
        binding.toolbar.setBackgroundColor(surfaceColor)
        window.statusBarColor = surfaceColor
        val isNight = resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = !isNight

        loadState()
        setupListeners()
    }

    private fun loadState() {
        binding.switchSteps.isChecked = Paper.book().read(PREF_SHOW_STEPS, true) ?: true
        binding.switchHeartRate.isChecked = Paper.book().read(PREF_SHOW_HEART_RATE, true) ?: true
        binding.switchBmi.isChecked = Paper.book().read(PREF_SHOW_BMI, true) ?: true
        binding.switchWeeklyChart.isChecked = Paper.book().read(PREF_SHOW_WEEKLY_CHART, true) ?: true
        binding.switchHrZones.isChecked = Paper.book().read(PREF_SHOW_HR_ZONES, true) ?: true
    }

    private fun setupListeners() {
        binding.switchSteps.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_STEPS, checked)
        }
        binding.switchHeartRate.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_HEART_RATE, checked)
        }
        binding.switchBmi.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_BMI, checked)
        }
        binding.switchWeeklyChart.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_WEEKLY_CHART, checked)
        }
        binding.switchHrZones.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_HR_ZONES, checked)
        }
    }

    companion object {
        const val PREF_SHOW_STEPS = "dashboard_show_steps"
        const val PREF_SHOW_HEART_RATE = "dashboard_show_heart_rate"
        const val PREF_SHOW_BMI = "dashboard_show_bmi"
        const val PREF_SHOW_WEEKLY_CHART = "dashboard_show_weekly_chart"
        const val PREF_SHOW_HR_ZONES = "dashboard_show_hr_zones"
    }
}
