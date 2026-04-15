package com.rouf.saht.common.helper

import android.content.Context
import androidx.core.content.ContextCompat
import com.rouf.saht.R
import com.rouf.saht.common.model.PersonalInformation
import io.paperdb.Paper

data class HeartRateZone(
    val name: String,
    val description: String,
    val colorResId: Int
)

object HeartRateZoneUtils {

    private const val KEY_PERSONAL_INFO = "personal_information"

    /**
     * Returns the heart rate zone for the given BPM, or null if age is unavailable.
     * Zones are based on % of estimated max heart rate (220 - age).
     */
    fun getZone(context: Context, bpm: Int): HeartRateZone? {
        val age = getAge() ?: return null
        if (age <= 0 || bpm <= 0) return null

        val maxHr = 220 - age
        val percent = (bpm.toFloat() / maxHr) * 100

        return when {
            percent < 50  -> HeartRateZone("Resting", "Normal resting state", R.color.zone_light)
            percent < 70  -> HeartRateZone("Moderate", "Brisk walk, easy cycling", R.color.zone_moderate)
            percent < 85  -> HeartRateZone("Vigorous", "Running, fast cycling", R.color.zone_vigorous)
            else          -> HeartRateZone("Intense", "Near max effort", R.color.zone_intense)
        }
    }

    fun getZoneColor(context: Context, bpm: Int): Int {
        val zone = getZone(context, bpm) ?: return ContextCompat.getColor(context, R.color.dark_grey)
        return ContextCompat.getColor(context, zone.colorResId)
    }

    /**
     * Calculates % of time spent in each zone from a list of per-second BPM entries.
     * Returns a map like {"Resting": 0.25, "Moderate": 0.60, "Vigorous": 0.15}
     */
    fun calculateZoneDistribution(context: Context, bpmEntries: List<com.github.mikephil.charting.data.Entry>): Map<String, Float> {
        val age = getAge() ?: return emptyMap()
        if (age <= 0 || bpmEntries.isEmpty()) return emptyMap()

        val counts = mutableMapOf<String, Int>()
        for (entry in bpmEntries) {
            val bpm = entry.y.toInt()
            if (bpm <= 0) continue
            val zone = getZone(context, bpm) ?: continue
            counts[zone.name] = (counts[zone.name] ?: 0) + 1
        }

        val total = counts.values.sum().toFloat()
        if (total == 0f) return emptyMap()
        return counts.mapValues { (_, count) -> count / total }
    }

    private fun getAge(): Int? {
        val info = Paper.book().read<PersonalInformation>(KEY_PERSONAL_INFO) ?: return null
        val ageStr = info.age
        if (ageStr.isBlank()) return null
        return ageStr.toIntOrNull()
    }
}
