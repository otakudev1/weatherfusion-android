package com.hardikagarwal.weatherfusion.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Open-Meteo wire models. NOTE: Open-Meteo omits fields rather than sending nulls,
 * so every field is nullable and mappers tolerate missing data.
 */
@Serializable
data class GeocodingResult(
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("admin1") val admin1: String? = null,
    val timezone: String? = null,
)

/** Geocoding endpoint returns a bare JSON array; name makes this parseable as one element. */
@Serializable
data class GeocodingEnvelope(
    val results: List<GeocodingResult> = emptyList(),
)

@Serializable
data class ForecastResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val current: CurrentBlock? = null,
    val daily: DailyBlock? = null,
)

@Serializable
data class CurrentBlock(
    @SerialName("time") val time: String? = null,
    @SerialName("temperature_2m") val temperature2m: Double? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @SerialName("precipitation") val precipitation: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("wind_speed_10m") val windSpeed10m: Double? = null,
    @SerialName("is_day") val isDay: Int? = null,
)

@Serializable
data class DailyBlock(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val temperature2mMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val temperature2mMin: List<Double> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int> = emptyList(),
)
