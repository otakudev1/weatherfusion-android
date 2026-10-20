package com.hardikagarwal.weatherfusion.data

import android.content.Context
import com.hardikagarwal.weatherfusion.domain.GeoPlace
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SharedPreferences-backed store for the last-searched location.
 * Widgets and dashboard both read from here — one source of truth,
 * no separate widget-local state that could fork from the app.
 */
class LocationStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun save(place: GeoPlace) {
        prefs.edit().putString(KEY_PLACE, json.encodeToString(place)).apply()
    }

    fun load(): GeoPlace? = runCatching {
        prefs.getString(KEY_PLACE, null)?.let { json.decodeFromString<GeoPlace>(it) }
    }.getOrNull()

    fun clear() {
        prefs.edit().remove(KEY_PLACE).apply()
    }

    private companion object {
        const val PREFS = "weatherfusion"
        const val KEY_PLACE = "last_place"
    }
}
