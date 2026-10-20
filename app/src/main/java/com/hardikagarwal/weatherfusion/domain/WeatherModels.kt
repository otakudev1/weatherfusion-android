package com.hardikagarwal.weatherfusion.domain

/** Domain models shared by dashboard, widgets, and (later) the service layer. */

data class GeoPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val countryCode: String? = null,
    val admin1: String? = null,
    val timezone: String? = null,
) {
    /** "Berlin, Germany" / "Austin, TX, United States" */
    fun displayName(): String = listOfNotNull(admin1, country).joinToString(", ").ifBlank { name }
}

/** One provider model's current-temp read for the provider spread UI. */
data class ProviderRead(
    val providerId: String,   // "best_match" | "gfs_seamless" | "icon_seamless"
    val label: String,        // "Best Match" | "GFS" | "ICON"
    val temperatureC: Double?,
    val weatherCode: Int?,
)

/** Spread of provider reads around their mean, in tenths of °C (to avoid float noise). */
data class ProviderSpread(
    val reads: List<ProviderRead>,
    val spreadTenthsC: Int,
) {
    companion object {
        fun of(reads: List<ProviderRead>): ProviderSpread {
            val temps = reads.mapNotNull { it.temperatureC }
            if (temps.size < 2) return ProviderSpread(reads, 0)
            val mean = temps.sum() / temps.size
            val maxDev = temps.maxOf { kotlin.math.abs(it - mean) }
            return ProviderSpread(reads, Math.round(maxDev * 10).toInt())
        }
    }
}

/** Confidence badge derived from provider agreement (v0.1 heuristic). */
enum class Confidence { HIGH, MEDIUM, LOW;

    companion object {
        fun fromSpreadTenthsC(tenths: Int): Confidence = when {
            tenths <= 5 -> HIGH      // <= 0.5 °C agreement
            tenths <= 15 -> MEDIUM   // <= 1.5 °C
            else -> LOW
        }
    }
}

data class DailyForecast(
    val date: String,             // yyyy-MM-dd from Open-Meteo
    val weatherCode: Int?,
    val tempMaxC: Double?,
    val tempMinC: Double?,
    val precipProbPercent: Int?,
)

data class CurrentWeather(
    val temperatureC: Double?,
    val apparentTemperatureC: Double?,
    val humidityPercent: Int?,
    val windKph: Double?,
    val weatherCode: Int?,
    val isDay: Boolean,
    val providerSpread: ProviderSpread,
    val confidence: Confidence,
    val daily: List<DailyForecast>,
    val fetchedAtEpochMs: Long,
)

data class WeatherSnapshot(
    val place: GeoPlace,
    val current: CurrentWeather,
    val alerts: List<NwsAlert>,
)

enum class AlertSeverity { EXTREME, SEVERE, MODERATE, MINOR, UNKNOWN }

data class NwsAlert(
    val id: String,
    val event: String,
    val severity: AlertSeverity,
    val headline: String?,
    val areaDesc: String?,
    val onset: String?,      // ISO-8601, kept as string in domain
    val expires: String?,    // ISO-8601
    val effective: String?,
)
