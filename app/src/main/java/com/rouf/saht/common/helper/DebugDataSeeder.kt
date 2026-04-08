package com.rouf.saht.common.helper

import com.github.mikephil.charting.data.Entry
import com.rouf.saht.common.model.Gender
import com.rouf.saht.common.model.HeartRateMonitorData
import com.rouf.saht.common.model.HeartRateMonitorSensitivity
import com.rouf.saht.common.model.PedometerData
import com.rouf.saht.common.model.PersonalInformation
import io.paperdb.Paper
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.random.Random

object DebugDataSeeder {

    private const val KEY_HEART_RATE    = "heart_rate_monitor_data"
    private const val KEY_PEDOMETER     = "pedometer_data_list"
    private const val KEY_PERSONAL_INFO = "personal_information"

    private val activities = listOf("Walking", "Running", "Cycling", "Resting", "Gym", "Yoga")

    fun seedAllData() {
        seedPersonalInformation()
        seedPedometerData()
        seedHeartRateData()
    }

    fun seedPersonalInformation() {
        Paper.book().write(
            KEY_PERSONAL_INFO,
            PersonalInformation(
                name         = "John",
                gender       = Gender.MALE,
                height       = "175",
                heightUnit   = "cm",
                weight       = "70",
                weightUnit   = "kg",
                selectedYear  = 1995,
                selectedMonth = 5,
                selectedDay   = 15,
                formatedDate  = "15/05/1995",
                age           = "30"
            )
        )
    }

    fun seedPedometerData() {
        val data = mutableListOf<PedometerData>()
        val cal = Calendar.getInstance()
        val rng = Random(42)

        // 6 months = ~180 days
        for (dayOffset in 179 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -dayOffset)
            cal.set(Calendar.HOUR_OF_DAY, 7)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            // Skip ~15% of days randomly (rest days / forgot phone)
            if (rng.nextFloat() < 0.15f) continue

            // Realistic step patterns:
            // - Weekdays: 5000-12000 (commute + office)
            // - Weekends: 2000-15000 (either lazy or long walk/hike)
            // - Gradual improvement trend over 6 months
            val improvementFactor = 1.0 + (180 - dayOffset) * 0.001 // +0.1% per day
            val baseSteps = when (dayOfWeek) {
                Calendar.SATURDAY, Calendar.SUNDAY -> rng.nextInt(2000, 15000)
                else -> rng.nextInt(5000, 12000)
            }
            val steps = (baseSteps * improvementFactor).roundToInt()

            val strideM = 0.70 + rng.nextDouble() * 0.16 // 0.70-0.86m stride
            val distance = steps * strideM
            val calories = steps * (0.035 + rng.nextDouble() * 0.015) // 0.035-0.05 cal/step

            // Duration: roughly 1 step per second walking, faster if running
            val durationMin = (steps / (80 + rng.nextInt(40))).toLong() // 80-120 steps/min
            val durationMs = durationMin * 60L * 1000L
            val startTime = cal.timeInMillis
            val endTime = startTime + durationMs

            data.add(
                PedometerData(
                    steps                 = steps,
                    goal                  = 10000,
                    distanceMeters        = distance,
                    caloriesBurned        = calories,
                    startTime             = startTime,
                    endTime               = endTime,
                    totalExerciseDuration = durationMs,
                    timestamp             = startTime
                )
            )
        }

        Paper.book().write(KEY_PEDOMETER, data)
    }

    fun seedHeartRateData() {
        val data = mutableListOf<HeartRateMonitorData>()
        val cal = Calendar.getInstance()
        val rng = Random(99)
        val maxHr = 190 // 220 - age 30

        // ~2-3 readings per week over 6 months = ~60-80 readings
        for (dayOffset in 179 downTo 0) {
            // Only record HR on ~40% of days
            if (rng.nextFloat() > 0.40f) continue

            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -dayOffset)

            // Random time: morning (7-9), midday (12-14), or evening (17-19)
            val timeSlot = rng.nextInt(3)
            cal.set(Calendar.HOUR_OF_DAY, when (timeSlot) { 0 -> 7 + rng.nextInt(2); 1 -> 12 + rng.nextInt(2); else -> 17 + rng.nextInt(2) })
            cal.set(Calendar.MINUTE, rng.nextInt(60))
            cal.set(Calendar.SECOND, 0)

            val activity = activities[rng.nextInt(activities.size)]

            // BPM depends on activity
            val baseBpm = when (activity) {
                "Resting" -> rng.nextInt(58, 78)
                "Yoga"    -> rng.nextInt(65, 90)
                "Walking" -> rng.nextInt(85, 115)
                "Cycling" -> rng.nextInt(105, 145)
                "Gym"     -> rng.nextInt(110, 155)
                "Running" -> rng.nextInt(130, 175)
                else      -> rng.nextInt(70, 120)
            }

            // Simulate 20-30 second measurement with BPM variation
            val measurementSeconds = 20 + rng.nextInt(11)
            val bpmEntries = mutableListOf<Entry>()
            var peakBpm = baseBpm
            for (i in 0 until measurementSeconds) {
                // BPM fluctuates +/- 8 around base, with slight upward trend
                val fluctuation = rng.nextInt(-8, 9)
                val trend = (i * 0.3).toInt()
                val bpm = (baseBpm + fluctuation + trend).coerceIn(45, 200)
                bpmEntries.add(Entry(i.toFloat(), bpm.toFloat()))
                if (bpm > peakBpm) peakBpm = bpm
            }

            // Final BPM is average of last 5 readings
            val finalBpm = bpmEntries.takeLast(5).map { it.y.toInt() }.average().roundToInt()

            // Calculate zone
            val percent = (finalBpm.toFloat() / maxHr) * 100
            val zone = when {
                percent < 50  -> "Resting"
                percent < 70  -> "Moderate"
                percent < 85  -> "Vigorous"
                else          -> "Intense"
            }

            // Zone distribution from entries
            val zoneDist = mutableMapOf<String, Int>()
            for (entry in bpmEntries) {
                val pct = (entry.y / maxHr) * 100
                val z = when {
                    pct < 50  -> "Resting"
                    pct < 70  -> "Moderate"
                    pct < 85  -> "Vigorous"
                    else      -> "Intense"
                }
                zoneDist[z] = (zoneDist[z] ?: 0) + 1
            }
            val total = zoneDist.values.sum().toFloat()
            val zoneDistribution = zoneDist.mapValues { (_, count) ->
                (count / total * 100).roundToInt() / 100f
            }

            data.add(
                HeartRateMonitorData(
                    duration         = measurementSeconds,
                    unit             = "Sec",
                    sensitivityLevel = HeartRateMonitorSensitivity.MEDIUM,
                    bpm              = finalBpm,
                    bpmGraphEntries  = bpmEntries,
                    timeStamp        = cal.timeInMillis,
                    activityPerformed = activity,
                    isResting        = activity == "Resting",
                    zone             = zone,
                    zoneDistribution = zoneDistribution
                )
            )
        }

        Paper.book().write(KEY_HEART_RATE, data)
    }
}
