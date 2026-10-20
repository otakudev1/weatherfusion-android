package com.hardikagarwal.weatherfusion.widget

import android.widget.RemoteViews
import com.hardikagarwal.weatherfusion.R
import com.hardikagarwal.weatherfusion.domain.Confidence
import com.hardikagarwal.weatherfusion.domain.NwsAlert
import com.hardikagarwal.weatherfusion.domain.WeatherSnapshot
import com.hardikagarwal.weatherfusion.ui.Theme
import kotlin.math.roundToInt

/**
 * Pure-ish RemoteViews assembly shared by both widget sizes.
 * FRAMEWORK WIDGETS ONLY rule: layouts use TextView/LinearLayout/ImageView —
 * no Space or bare View spacers (host whitelist crash lesson).
 */
object WidgetRender {

    fun placeLine(snapshot: WeatherSnapshot): String = snapshot.place.displayName()

    fun tempLine(snapshot: WeatherSnapshot): String =
        snapshot.current.temperatureC?.let { "${it.roundToInt()}°" } ?: "—"

    fun conditionLine(snapshot: WeatherSnapshot): String =
        Theme.weatherGlyph(snapshot.current.weatherCode, snapshot.current.isDay)

    fun confidenceLine(snapshot: WeatherSnapshot): String =
        "${Theme.confidenceLabel(snapshot.current.confidence)} · ±${snapshot.current.providerSpread.spreadTenthsC / 10.0}°C"

    /** Top severity alert line, or null when none (banner view then gets GONE via empty text + visibility). */
    fun alertLine(alerts: List<NwsAlert>): String? {
        val top = alerts.maxByOrNull { severityRank(it.severity) } ?: return null
        return "${top.event} — ${top.headline ?: top.areaDesc ?: ""}"
    }

    fun alertColor(alerts: List<NwsAlert>): Int? {
        val top = alerts.maxByOrNull { severityRank(it.severity) } ?: return null
        return Theme.alertColor(top.severity)
    }

    fun dayLines(snapshot: WeatherSnapshot, maxDays: Int): List<String> =
        snapshot.current.daily.take(maxDays).map { day ->
            val hi = day.tempMaxC?.roundToInt()?.toString() ?: "—"
            val lo = day.tempMinC?.roundToInt()?.toString() ?: "—"
            "${day.date.takeLast(5)}  ${Theme.weatherGlyph(day.weatherCode)}  $hi°/$lo°"
        }

    fun confidenceBadgeColor(snapshot: WeatherSnapshot): Int = when (snapshot.current.confidence) {
        Confidence.HIGH -> Theme.confidenceColor(Confidence.HIGH)
        Confidence.MEDIUM -> Theme.confidenceColor(Confidence.MEDIUM)
        Confidence.LOW -> Theme.confidenceColor(Confidence.LOW)
    }

    private fun severityRank(severity: com.hardikagarwal.weatherfusion.domain.AlertSeverity): Int = when (severity) {
        com.hardikagarwal.weatherfusion.domain.AlertSeverity.EXTREME -> 5
        com.hardikagarwal.weatherfusion.domain.AlertSeverity.SEVERE -> 4
        com.hardikagarwal.weatherfusion.domain.AlertSeverity.MODERATE -> 3
        com.hardikagarwal.weatherfusion.domain.AlertSeverity.MINOR -> 2
        com.hardikagarwal.weatherfusion.domain.AlertSeverity.UNKNOWN -> 1
    }
}
