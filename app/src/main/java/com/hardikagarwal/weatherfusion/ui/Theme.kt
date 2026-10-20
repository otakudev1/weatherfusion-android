package com.hardikagarwal.weatherfusion.ui

import android.graphics.Color

/** Central color + weather-code mapping. Both activities and widgets use this. */
object Theme {
    // Severity tints for the NWS alert banner (and widget alert strip).
    fun alertColor(severity: com.hardikagarwal.weatherfusion.domain.AlertSeverity): Int = when (severity) {
        com.hardikagarwal.weatherfusion.domain.AlertSeverity.EXTREME -> Color.parseColor("#B71C1C") // dark red
        com.hardikagarwal.weatherfusion.domain.AlertSeverity.SEVERE -> Color.parseColor("#D32F2F") // red
        com.hardikagarwal.weatherfusion.domain.AlertSeverity.MODERATE -> Color.parseColor("#EF6C00") // orange
        com.hardikagarwal.weatherfusion.domain.AlertSeverity.MINOR -> Color.parseColor("#F9A825") // amber
        com.hardikagarwal.weatherfusion.domain.AlertSeverity.UNKNOWN -> Color.parseColor("#455A64") // blue-grey
    }

    fun confidenceLabel(confidence: com.hardikagarwal.weatherfusion.domain.Confidence): String = when (confidence) {
        com.hardikagarwal.weatherfusion.domain.Confidence.HIGH -> "HIGH"
        com.hardikagarwal.weatherfusion.domain.Confidence.MEDIUM -> "MEDIUM"
        com.hardikagarwal.weatherfusion.domain.Confidence.LOW -> "LOW"
    }

    fun confidenceColor(confidence: com.hardikagarwal.weatherfusion.domain.Confidence): Int = when (confidence) {
        com.hardikagarwal.weatherfusion.domain.Confidence.HIGH -> Color.parseColor("#2E7D32")
        com.hardikagarwal.weatherfusion.domain.Confidence.MEDIUM -> Color.parseColor("#F9A825")
        com.hardikagarwal.weatherfusion.domain.Confidence.LOW -> Color.parseColor("#D32F2F")
    }

    /** WMO weather interpretation code -> short emoji-ish glyph (framework-safe text). */
    fun weatherGlyph(code: Int?, isDay: Boolean = true): String = when (code) {
        null -> "—"
        0 -> if (isDay) "Clear" else "Clear night"
        1, 2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67 -> "Rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm + hail"
        else -> "—"
    }
}
