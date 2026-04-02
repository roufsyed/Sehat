package com.rouf.saht.common.helper

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rouf.saht.common.model.HeartRateMonitorSettings
import com.rouf.saht.common.model.PedometerData
import com.rouf.saht.common.model.PedometerSettings
import com.rouf.saht.common.model.PersonalInformation
import com.rouf.saht.heartRate.data.HeartRateData
import io.paperdb.Paper
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object BackupUtils {

    private val gson = Gson()

    fun exportData(context: Context, uri: Uri): Boolean {
        return try {
            val exportMap = mutableMapOf<String, Any?>()
            exportMap["heart_rate_monitor_data"] = Paper.book().read<List<HeartRateData>>("heart_rate_monitor_data", emptyList())
            exportMap["pedometer_data_list"] = Paper.book().read<List<PedometerData>>("pedometer_data_list", emptyList())
            exportMap["personal_information"] = Paper.book().read<PersonalInformation>("personal_information", null)
            exportMap["pedometer_data"] = Paper.book().read<PedometerData>("pedometer_data", null)
            exportMap["pedometer_settings"] = Paper.book().read<PedometerSettings>("pedometer_settings", null)
            exportMap["heart_rate_monitor_settings"] = Paper.book().read<HeartRateMonitorSettings>("heart_rate_monitor_settings", null)

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
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
