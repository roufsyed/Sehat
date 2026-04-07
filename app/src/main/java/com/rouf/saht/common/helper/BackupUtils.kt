package com.rouf.saht.common.helper

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rouf.saht.common.model.HeartRateMonitorSettings
import com.rouf.saht.common.model.PedometerData
import com.rouf.saht.common.model.PedometerSettings
import com.rouf.saht.common.model.PersonalInformation
import com.rouf.saht.common.model.Sound
import com.rouf.saht.heartRate.data.HeartRateData
import com.rouf.saht.setting.view.CustomizationActivity
import com.rouf.saht.setting.view.DashboardSettingsActivity
import com.rouf.saht.setting.view.SettingsFragment
import io.paperdb.Paper
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object BackupUtils {

    private val gson = Gson()
    private const val KEY_ONBOARDING = "onboarding_complete"
    private const val KEY_CUSTOM_SOUNDS = "custom_meditation_sounds"

    fun exportData(context: Context, uri: Uri): Boolean {
        return try {
            val exportMap = mutableMapOf<String, Any?>()
            exportMap["heart_rate_monitor_data"] = Paper.book().read<List<HeartRateData>>("heart_rate_monitor_data", emptyList())
            exportMap["pedometer_data_list"] = Paper.book().read<List<PedometerData>>("pedometer_data_list", emptyList())
            exportMap["personal_information"] = Paper.book().read<PersonalInformation>("personal_information", null)
            exportMap["pedometer_data"] = Paper.book().read<PedometerData>("pedometer_data", null)
            exportMap["pedometer_settings"] = Paper.book().read<PedometerSettings>("pedometer_settings", null)
            exportMap["heart_rate_monitor_settings"] = Paper.book().read<HeartRateMonitorSettings>("heart_rate_monitor_settings", null)

            // Customization & preferences
            exportMap[CustomizationActivity.PREF_THEME] = Paper.book().read<String>(CustomizationActivity.PREF_THEME, null)
            exportMap[CustomizationActivity.PREF_CUSTOM_PRIMARY] = Paper.book().read<String>(CustomizationActivity.PREF_CUSTOM_PRIMARY, null)
            exportMap[CustomizationActivity.PREF_CUSTOM_SECONDARY] = Paper.book().read<String>(CustomizationActivity.PREF_CUSTOM_SECONDARY, null)
            exportMap[CustomizationActivity.PREF_DEFAULT_SCREEN] = Paper.book().read<String>(CustomizationActivity.PREF_DEFAULT_SCREEN, null)
            exportMap[SettingsFragment.PREF_DARK_MODE] = Paper.book().read<Boolean>(SettingsFragment.PREF_DARK_MODE, null)
            exportMap[SettingsFragment.PREF_DOUBLE_TAP_LOCK] = Paper.book().read<Boolean>(SettingsFragment.PREF_DOUBLE_TAP_LOCK, null)

            // Onboarding
            exportMap[KEY_ONBOARDING] = Paper.book().read<Boolean>(KEY_ONBOARDING, null)

            // Custom meditation sounds
            exportMap[KEY_CUSTOM_SOUNDS] = Paper.book().read<List<Sound>>(KEY_CUSTOM_SOUNDS, null)

            // Dashboard visibility
            exportMap[DashboardSettingsActivity.PREF_SHOW_STEPS] = Paper.book().read<Boolean>(DashboardSettingsActivity.PREF_SHOW_STEPS, null)
            exportMap[DashboardSettingsActivity.PREF_SHOW_HEART_RATE] = Paper.book().read<Boolean>(DashboardSettingsActivity.PREF_SHOW_HEART_RATE, null)
            exportMap[DashboardSettingsActivity.PREF_SHOW_BMI] = Paper.book().read<Boolean>(DashboardSettingsActivity.PREF_SHOW_BMI, null)
            exportMap[DashboardSettingsActivity.PREF_SHOW_WEEKLY_CHART] = Paper.book().read<Boolean>(DashboardSettingsActivity.PREF_SHOW_WEEKLY_CHART, null)
            exportMap[DashboardSettingsActivity.PREF_SHOW_HR_ZONES] = Paper.book().read<Boolean>(DashboardSettingsActivity.PREF_SHOW_HR_ZONES, null)

            val json = gson.toJson(exportMap)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream).use { it.write(json) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importData(context: Context, uri: Uri): Boolean {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                InputStreamReader(stream).readText()
            } ?: return false

            val type = object : TypeToken<Map<String, Any>>() {}.type
            val dataMap: Map<String, Any> = gson.fromJson(json, type)

            dataMap["heart_rate_monitor_data"]?.let {
                val listType = object : TypeToken<List<HeartRateData>>() {}.type
                Paper.book().write("heart_rate_monitor_data", gson.fromJson<List<HeartRateData>>(gson.toJson(it), listType))
            }
            dataMap["pedometer_data_list"]?.let {
                val listType = object : TypeToken<List<PedometerData>>() {}.type
                Paper.book().write("pedometer_data_list", gson.fromJson<List<PedometerData>>(gson.toJson(it), listType))
            }
            dataMap["personal_information"]?.let {
                Paper.book().write("personal_information", gson.fromJson(gson.toJson(it), PersonalInformation::class.java))
            }
            dataMap["pedometer_data"]?.let {
                Paper.book().write("pedometer_data", gson.fromJson(gson.toJson(it), PedometerData::class.java))
            }
            dataMap["pedometer_settings"]?.let {
                Paper.book().write("pedometer_settings", gson.fromJson(gson.toJson(it), PedometerSettings::class.java))
            }
            dataMap["heart_rate_monitor_settings"]?.let {
                Paper.book().write("heart_rate_monitor_settings", gson.fromJson(gson.toJson(it), HeartRateMonitorSettings::class.java))
            }

            // Customization & preferences
            dataMap[CustomizationActivity.PREF_THEME]?.let {
                Paper.book().write(CustomizationActivity.PREF_THEME, it as String)
            }
            dataMap[CustomizationActivity.PREF_CUSTOM_PRIMARY]?.let {
                Paper.book().write(CustomizationActivity.PREF_CUSTOM_PRIMARY, it as String)
            }
            dataMap[CustomizationActivity.PREF_CUSTOM_SECONDARY]?.let {
                Paper.book().write(CustomizationActivity.PREF_CUSTOM_SECONDARY, it as String)
            }
            dataMap[CustomizationActivity.PREF_DEFAULT_SCREEN]?.let {
                Paper.book().write(CustomizationActivity.PREF_DEFAULT_SCREEN, it as String)
            }
            dataMap[SettingsFragment.PREF_DARK_MODE]?.let {
                Paper.book().write(SettingsFragment.PREF_DARK_MODE, it as Boolean)
            }
            dataMap[SettingsFragment.PREF_DOUBLE_TAP_LOCK]?.let {
                Paper.book().write(SettingsFragment.PREF_DOUBLE_TAP_LOCK, it as Boolean)
            }

            // Onboarding
            dataMap[KEY_ONBOARDING]?.let {
                Paper.book().write(KEY_ONBOARDING, it as Boolean)
            }

            // Custom meditation sounds
            dataMap[KEY_CUSTOM_SOUNDS]?.let {
                val listType = object : TypeToken<List<Sound>>() {}.type
                Paper.book().write(KEY_CUSTOM_SOUNDS, gson.fromJson<List<Sound>>(gson.toJson(it), listType))
            }

            // Dashboard visibility
            for (key in listOf(
                DashboardSettingsActivity.PREF_SHOW_STEPS,
                DashboardSettingsActivity.PREF_SHOW_HEART_RATE,
                DashboardSettingsActivity.PREF_SHOW_BMI,
                DashboardSettingsActivity.PREF_SHOW_WEEKLY_CHART,
                DashboardSettingsActivity.PREF_SHOW_HR_ZONES
            )) {
                dataMap[key]?.let { Paper.book().write(key, it as Boolean) }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
