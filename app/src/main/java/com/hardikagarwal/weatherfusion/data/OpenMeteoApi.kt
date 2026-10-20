package com.hardikagarwal.weatherfusion.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

/**
 * Keyless Open-Meteo client. HttpURLConnection keeps the data layer free of
 * Android dependencies so it stays unit-testable.
 */
class OpenMeteoApi(private val connectTimeoutMs: Int = 10_000, private val readTimeoutMs: Int = 15_000) {

    val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun geocode(query: String, count: Int = 5): List<GeocodingResult> = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${enc(query)}&count=$count&language=en&format=json"
        val body = httpGet(url) ?: return@withContext emptyList()
        runCatching { json.decodeFromString<GeocodingEnvelope>(body).results }.getOrDefault(emptyList())
    }

    /**
     * Multi-model forecast (best_match + GFS + ICON) for provider spread.
     * Returns the RAW body: with `models=a,b,c` Open-Meteo answers a JSON ARRAY
     * of model blocks (single-model would be an object) — callers branch on shape.
     */
    suspend fun forecastRaw(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        val url = buildString {
            append("https://api.open-meteo.com/v1/forecast?")
            append("latitude=$latitude&longitude=$longitude")
            append("&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,is_day")
            append("&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
            append("&models=best_match,gfs_seamless,icon_seamless")
            append("&forecast_days=5&timezone=auto")
        }
        httpGet(url) ?: throw IOException("Open-Meteo forecast returned empty body")
    }

    private fun httpGet(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = this@OpenMeteoApi.connectTimeoutMs
                readTimeout = this@OpenMeteoApi.readTimeoutMs
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else null
        } catch (e: IOException) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}
