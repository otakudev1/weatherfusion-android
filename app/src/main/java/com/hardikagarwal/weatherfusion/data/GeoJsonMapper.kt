package com.hardikagarwal.weatherfusion.data

import com.hardikagarwal.weatherfusion.domain.AlertSeverity
import com.hardikagarwal.weatherfusion.domain.NwsAlert
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * PURE Kotlin mapper for the NWS api.weather.gov /alerts GeoJSON feed.
 * No Android dependencies — unit-under-test for GeoJsonMapperTest (6 tests).
 *
 * Design rules:
 * - A malformed feature (missing/null properties, wrong types) is tolerated: skipped, never thrown.
 * - severity mapping is case-insensitive on the `severity` string.
 * - onset/expires/effective pass through as raw ISO-8601 strings (parsing is UI/clock's job).
 */
object GeoJsonMapper {

    /** Map a full /alerts GeoJSON body to domain alerts. Never throws on shape issues. */
    fun mapAlerts(json: String): List<NwsAlert> {
        val tree = runCatching {
            kotlinx.serialization.json.Json.Default.parseToJsonElement(json)
        }.getOrNull()?.jsonObjectSafe() ?: return emptyList()

        val features = tree["features"]?.jsonArraySafe() ?: return emptyList()
        val out = ArrayList<NwsAlert>(features.size)
        for (feature in features) {
            mapFeature(feature)?.let { out.add(it) }
        }
        return out
    }

    /** Map one GeoJSON feature; returns null when the feature is unusable (malformed tolerated). */
    fun mapFeature(feature: JsonElement?): NwsAlert? {
        val obj = feature?.jsonObjectSafe() ?: return null
        val props = obj["properties"]?.jsonObjectSafe() ?: return null

        val id = (obj["id"] ?: props["id"])?.jsonPrimitiveSafe() ?: return null
        val event = props["event"]?.jsonPrimitiveSafe() ?: return null

        return NwsAlert(
            id = id,
            event = event,
            severity = mapSeverity(props["severity"]?.jsonPrimitiveSafe()),
            headline = props["headline"]?.jsonPrimitiveSafe(),
            areaDesc = props["areaDesc"]?.jsonPrimitiveSafe(),
            onset = props["onset"]?.jsonPrimitiveSafe(),
            expires = props["expires"]?.jsonPrimitiveSafe(),
            effective = props["effective"]?.jsonPrimitiveSafe(),
        )
    }

    /** NWS severity strings are uppercase; be lenient anyway. Unknown -> UNKNOWN. */
    fun mapSeverity(raw: String?): AlertSeverity = when (raw?.trim()?.uppercase()) {
        "EXTREME" -> AlertSeverity.EXTREME
        "SEVERE" -> AlertSeverity.SEVERE
        "MODERATE" -> AlertSeverity.MODERATE
        "MINOR" -> AlertSeverity.MINOR
        else -> AlertSeverity.UNKNOWN
    }

    // ---- safe element helpers: null instead of throw on shape mismatch ----
    // NOTE: JsonObject.content / JsonArray.content are PRIVATE in kotlinx.serialization >= 1.8;
    // the public API is the toMap() / toList() extensions.

    private fun JsonElement.jsonObjectSafe(): Map<String, JsonElement>? =
        (this as? JsonObject)?.toMap()

    private fun JsonElement.jsonArraySafe(): List<JsonElement>? =
        (this as? JsonArray)?.toList()

    private fun JsonElement.jsonPrimitiveSafe(): String? =
        (this as? JsonPrimitive)?.content
}
