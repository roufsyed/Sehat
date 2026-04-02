package com.rouf.saht.common.helper

import com.github.mikephil.charting.data.Entry
import com.rouf.saht.common.model.HeartRateMonitorData
import com.rouf.saht.common.model.HeartRateMonitorSensitivity
import com.rouf.saht.common.model.PedometerData
import io.paperdb.Paper
import java.util.Calendar

object DebugDataSeeder {

    private const val KEY_HEART_RATE = "heart_rate_monitor_data"
    private const val KEY_PEDOMETER  = "pedometer_data_list"

    private val activities = listOf("Walking", "Running", "Cycling", "Resting")

    fun seedHeartRateData() {
        val existing: MutableList<HeartRateMonitorData> =
            Paper.book().read(KEY_HEART_RATE) ?: mutableListOf()

        val calendar = Calendar.getInstance()
        repeat(7) { dayOffset ->
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -(6 - dayOffset))
            calendar.set(Calendar.HOUR_OF_DAY, 9 + (dayOffset % 4))
            calendar.set(Calendar.MINUTE, 0)

            val bpm = 65 + (dayOffset * 7) % 35   // 65–99
            val entries = (0..10).map { i ->
                Entry(i.toFloat(), (bpm - 5 + (i * 2) % 10).toFloat())
            }.toMutableList()

            existing.add(
                HeartRateMonitorData(
                    duration         = 60,
                    unit             = "Sec",
                    sensitivityLevel = HeartRateMonitorSensitivity.MEDIUM,
                    bpm              = bpm,
                    bpmGraphEntries  = entries,
                    timeStamp        = calendar.timeInMillis,
                    activityPerformed = activities[dayOffset % activities.size],
                    isResting        = dayOffset % activities.size == 3
                )
            )
        }

        Paper.book().write(KEY_HEART_RATE, existing)
    }

    fun seedPedometerData() {
        val existing: MutableList<PedometerData> =
            Paper.book().read(KEY_PEDOMETER) ?: mutableListOf()

        val calendar = Calendar.getInstance()
        val stepSamples = intArrayOf(7200, 9500, 5100, 11300, 8400, 6700, 10200)

        repeat(7) { dayOffset ->
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -(6 - dayOffset))
            calendar.set(Calendar.HOUR_OF_DAY, 7)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startTime = calendar.timeInMillis

            val durationMs = 45L * 60L * 1000L   // 45 minutes
            val endTime = startTime + durationMs

            val steps = stepSamples[dayOffset]
            val distanceMeters = steps * 0.78       // avg stride ~78 cm
            val caloriesBurned = steps * 0.04       // rough estimate

            existing.add(
                PedometerData(
                    steps                 = steps,
                    goal                  = 10000,
                    distanceMeters        = distanceMeters,
                    caloriesBurned        = caloriesBurned,
                    startTime             = startTime,
                    endTime               = endTime,
                    totalExerciseDuration = durationMs,
                    timestamp             = startTime
                )
            )
        }

        Paper.book().write(KEY_PEDOMETER, existing)
    }
}
