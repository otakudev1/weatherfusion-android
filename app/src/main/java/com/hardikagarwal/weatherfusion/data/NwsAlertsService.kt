package com.hardikagarwal.weatherfusion.data

import com.hardikagarwal.weatherfusion.domain.GeoPlace
import com.hardikagarwal.weatherfusion.domain.NwsAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

/**
 * Keyless NWS official alerts via api.weather.gov. US-ONLY by design:
 * the NWS banner is intentionally silent for non-US locations (service
 * returns emptyList and the UI simply doesn't render the banner).
 */
class NwsAlertsService(private val connectTimeoutMs: Int = 10_000, private val readTimeoutMs: Int = 15_000) {

    companion object {
        // NWS requires an identifiable User-Agent; this is a personal-use app.
        private const val USER_AGENT = "WeatherFusionApp/0.1 (personal)"
    }

    /**
     * Fetch active alerts for a point. Returns empty list for:
     * - non-US places (no countryCode "US"),
     * - network/HTTP failures (alerts never break the dashboard),
     * - any mapper-tolerated malformed data.
     */
    suspend fun alertsFor(place: GeoPlace): List<NwsAlert> = withContext(Dispatchers.IO) {
        // US-only gate: country code must be US (case-insensitive).
        if (!place.countryCode.equals("US", ignoreCase = true)) return@withContext emptyList()
        val lat = place.latitude
        val lon = place.longitude
        if (lat.isNaN() || lon.isNaN()) return@withContext emptyList()

        val url = "https://api.weather.gov/alerts/active?point=${enc("%.4f".format(lat))},${enc("%.4f".format(lon))}"
        val body = httpGet(url) ?: return@withContext emptyList()
        runCatching { GeoJsonMapper.mapAlerts(body) }.getOrDefault(emptyList())
    }

    private fun httpGet(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = this@NwsAlertsService.connectTimeoutMs
                readTimeout = this@NwsAlertsService.readTimeoutMs
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/geo+json")
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
