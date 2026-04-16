package com.rouf.saht.setting.view

import android.content.res.Configuration
import android.os.Bundle
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.MaterialColors
import com.rouf.saht.R
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
        binding.toolbar.navigationIcon?.setTint(
            androidx.core.content.ContextCompat.getColor(this, R.color.dark_grey)
        )
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
        binding.switchDistance.isChecked = Paper.book().read(PREF_SHOW_DISTANCE, true) ?: true
        binding.switchCalories.isChecked = Paper.book().read(PREF_SHOW_CALORIES, true) ?: true
        binding.switchActiveDuration.isChecked = Paper.book().read(PREF_SHOW_ACTIVE_DURATION, true) ?: true
        binding.switchBpmActivity.isChecked = Paper.book().read(PREF_SHOW_BPM_BY_ACTIVITY, true) ?: true
        binding.switchPeakBpm.isChecked = Paper.book().read(PREF_SHOW_PEAK_BPM, true) ?: true
        binding.switchRecovery.isChecked = Paper.book().read(PREF_SHOW_RECOVERY, true) ?: true
        binding.switchCorrelation.isChecked = Paper.book().read(PREF_SHOW_CORRELATION, true) ?: true
        binding.switchWeeklySummary.isChecked = Paper.book().read(PREF_SHOW_WEEKLY_SUMMARY, true) ?: true
        binding.switchRecords.isChecked = Paper.book().read(PREF_SHOW_RECORDS, true) ?: true
        binding.switchInsights.isChecked = Paper.book().read(PREF_SHOW_INSIGHTS, true) ?: true
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
        binding.switchDistance.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_DISTANCE, checked)
        }
        binding.switchCalories.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_CALORIES, checked)
        }
        binding.switchActiveDuration.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_ACTIVE_DURATION, checked)
        }
        binding.switchBpmActivity.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_BPM_BY_ACTIVITY, checked)
        }
        binding.switchPeakBpm.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_PEAK_BPM, checked)
        }
        binding.switchRecovery.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_RECOVERY, checked)
        }
        binding.switchCorrelation.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_CORRELATION, checked)
        }
        binding.switchWeeklySummary.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_WEEKLY_SUMMARY, checked)
        }
        binding.switchRecords.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_RECORDS, checked)
        }
        binding.switchInsights.setOnCheckedChangeListener { _, checked ->
            Paper.book().write(PREF_SHOW_INSIGHTS, checked)
        }
    }

    companion object {
        const val PREF_SHOW_STEPS = "dashboard_show_steps"
        const val PREF_SHOW_HEART_RATE = "dashboard_show_heart_rate"
        const val PREF_SHOW_BMI = "dashboard_show_bmi"
        const val PREF_SHOW_WEEKLY_CHART = "dashboard_show_weekly_chart"
        const val PREF_SHOW_HR_ZONES = "dashboard_show_hr_zones"
        const val PREF_SHOW_DISTANCE = "dashboard_show_distance"
        const val PREF_SHOW_CALORIES = "dashboard_show_calories"
        const val PREF_SHOW_ACTIVE_DURATION = "dashboard_show_active_duration"
        const val PREF_SHOW_BPM_BY_ACTIVITY = "dashboard_show_bpm_activity"
        const val PREF_SHOW_PEAK_BPM = "dashboard_show_peak_bpm"
        const val PREF_SHOW_RECOVERY = "dashboard_show_recovery"
        const val PREF_SHOW_CORRELATION = "dashboard_show_correlation"
        const val PREF_SHOW_WEEKLY_SUMMARY = "dashboard_show_weekly_summary"
        const val PREF_SHOW_RECORDS = "dashboard_show_records"
        const val PREF_SHOW_INSIGHTS = "dashboard_show_insights"
    }
}
