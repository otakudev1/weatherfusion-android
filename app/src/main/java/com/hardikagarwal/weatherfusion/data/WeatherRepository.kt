package com.hardikagarwal.weatherfusion.data

import com.hardikagarwal.weatherfusion.domain.Confidence
import com.hardikagarwal.weatherfusion.domain.CurrentWeather
import com.hardikagarwal.weatherfusion.domain.DailyForecast
import com.hardikagarwal.weatherfusion.domain.GeoPlace
import com.hardikagarwal.weatherfusion.domain.NwsAlert
import com.hardikagarwal.weatherfusion.domain.ProviderRead
import com.hardikagarwal.weatherfusion.domain.ProviderSpread
import com.hardikagarwal.weatherfusion.domain.WeatherSnapshot
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Thin orchestration over Open-Meteo + NWS. Widgets and dashboard both go through
 * here so the app can never fork its own data store away from the shared source.
 */
class WeatherRepository(
    private val openMeteoApi: OpenMeteoApi = OpenMeteoApi(),
    private val nwsAlertsService: NwsAlertsService = NwsAlertsService(),
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun snapshot(place: GeoPlace): WeatherSnapshot {
        val current = currentWeather(place)
        val alerts: List<NwsAlert> = runCatching { nwsAlertsService.alertsFor(place) }.getOrDefault(emptyList())
        return WeatherSnapshot(place, current, alerts)
    }

    suspend fun currentWeather(place: GeoPlace): CurrentWeather {
        val raw = openMeteoApi.forecastRaw(place.latitude, place.longitude)

        // Multi-model requests answer a JSON array of per-model blocks; be lenient
        // and fall back to a single object shape if the API ever changes.
        val blocks: List<ForecastResponse> = if (raw.trimStart().startsWith("[")) {
            runCatching { json.decodeFromString(ListSerializer(ForecastResponse.serializer()), raw) }
                .getOrDefault(emptyList())
        } else {
            runCatching { json.decodeFromString(ForecastResponse.serializer(), raw) }
                .getOrNull()?.let { listOf(it) } ?: emptyList()
        }

        val labels = listOf(
            "best_match" to "Best Match",
            "gfs_seamless" to "GFS",
            "icon_seamless" to "ICON",
        )
        val reads = blocks.mapIndexedNotNull { index, block ->
            block.current?.let { c ->
                val (id, label) = labels.getOrNull(index) ?: ("model_$index" to "Model $index")
                ProviderRead(id, label, c.temperature2m, c.weatherCode)
            }
        }
        val primary = blocks.firstOrNull()?.current
        val daily = mapDaily(blocks.firstOrNull()?.daily)
        val spread = ProviderSpread.of(reads)

        return CurrentWeather(
            temperatureC = primary?.temperature2m,
            apparentTemperatureC = primary?.apparentTemperature,
            humidityPercent = primary?.relativeHumidity2m,
            windKph = primary?.windSpeed10m,
            weatherCode = primary?.weatherCode,
            isDay = (primary?.isDay ?: 1) == 1,
            providerSpread = spread,
            confidence = Confidence.fromSpreadTenthsC(spread.spreadTenthsC),
            daily = daily,
            fetchedAtEpochMs = System.currentTimeMillis(),
        )
    }

    private fun mapDaily(daily: DailyBlock?): List<DailyForecast> {
        if (daily == null) return emptyList()
        return daily.time.mapIndexed { i, date ->
            DailyForecast(
                date = date,
                weatherCode = daily.weatherCode.getOrNull(i),
                tempMaxC = daily.temperature2mMax.getOrNull(i),
                tempMinC = daily.temperature2mMin.getOrNull(i),
                precipProbPercent = daily.precipitationProbabilityMax.getOrNull(i),
            )
        }
    }
}
