package com.rouf.saht.common.helper

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object Util {

    fun roundToTwoDecimalPlaces(value: Float): Double {
        return BigDecimal(value.toString()) // Convert to BigDecimal to handle precision
            .setScale(2, RoundingMode.HALF_UP) // Round to 2 decimal places
            .toDouble() // Convert back to Float
    }

    fun formatWithCommas(value: Int?): String {
        return try {
            if (value is Number) {
                NumberFormat.getNumberInstance(Locale.US).format(value)
            } else {
                throw IllegalArgumentException("Invalid number format: $value")
            }
        } catch (e: Exception) {
            "Invalid Input"
        }
    }

    fun formatDuration(milliseconds: Double): String {
        val totalSeconds = (milliseconds / 1000).toLong()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60

        return when {
            hours > 0 && minutes > 0 -> "$hours hr $minutes min"
            hours > 0                -> "$hours hr"
            else                     -> "$minutes min"
        }
    }

    fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters < 1000) {
            "${distanceMeters.toInt()} m"  // Convert to meters (whole number)
        } else {
            "${roundToTwoDecimalPlaces(distanceMeters / 1000)} km"  // Convert to kilometers
        }
    }

    fun roundToTwoDecimalPlaces(value: Double): String {
        return String.format("%.2f", value)
    }

    fun boldSubstring(fullText: String, boldText: String): SpannableString {
        val spannable = SpannableString(fullText)
        val startIndex = fullText.indexOf(boldText)
        if (startIndex != -1) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                startIndex,
                startIndex + boldText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }
}