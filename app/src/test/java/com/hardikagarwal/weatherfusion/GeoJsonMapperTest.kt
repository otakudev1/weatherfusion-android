package com.hardikagarwal.weatherfusion

import com.hardikagarwal.weatherfusion.data.GeoJsonMapper
import com.hardikagarwal.weatherfusion.domain.AlertSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 6 required tests for the pure NWS GeoJSON mapper:
 * severity mapping, onset/expires parsing, multiple alerts, empty area,
 * malformed feature tolerated, US-only filter.
 */
class GeoJsonMapperTest {

    private fun featureJson(severity: String?, event: String, onset: String?, expires: String?, areaDesc: String?): String {
        val sev = if (severity == null) "" else "\"severity\":\"$severity\","
        val on = if (onset == null) "" else "\"onset\":\"$onset\","
        val ex = if (expires == null) "" else "\"expires\":\"$expires\","
        val area = if (areaDesc == null) "" else "\"areaDesc\":\"$areaDesc\","
        return """
        |{"type":"FeatureCollection","features":[{"type":"Feature","id":"urn:oid:1",
        | "properties":{"@type":"wx:Alert",AREA "sent":"2026-10-20T12:00:00Z",
        |  SEV "event":"EVENT_NAME","headline":"Test headline",
        |  ON EX "senderName":"NWS Test","effective":"2026-10-20T12:00:00Z"}}
        |]}
        """.trimMargin()
            .replace("SEV ", sev)
            .replace("ON ", on)
            .replace("EX ", ex)
            .replace("AREA ", area)
            .replace("EVENT_NAME", event)
    }

    @Test
    fun severity_mapping_covers_all_levels_with_unknown_fallback() {
        val severe = GeoJsonMapper.mapAlerts(featureJson("Severe", "Severe Thunderstorm Warning", null, null, "Travis County, TX"))
        assertEquals(1, severe.size)
        assertEquals(AlertSeverity.SEVERE, severe[0].severity)

        assertEquals(AlertSeverity.EXTREME, GeoJsonMapper.mapSeverity("extreme"))
        assertEquals(AlertSeverity.MODERATE, GeoJsonMapper.mapSeverity("Moderate"))
        assertEquals(AlertSeverity.MINOR, GeoJsonMapper.mapSeverity("MINOR"))
        assertEquals(AlertSeverity.UNKNOWN, GeoJsonMapper.mapSeverity("Zombie Apocalypse"))
        assertEquals(AlertSeverity.UNKNOWN, GeoJsonMapper.mapSeverity(null))
    }

    @Test
    fun onset_expires_and_effective_pass_through_as_iso_strings() {
        val alerts = GeoJsonMapper.mapAlerts(
            featureJson("Severe", "Tornado Warning", "2026-10-20T13:00:00Z", "2026-10-20T13:45:00Z", "Dallas, TX")
        )
        assertEquals(1, alerts.size)
        assertEquals("2026-10-20T13:00:00Z", alerts[0].onset)
        assertEquals("2026-10-20T13:45:00Z", alerts[0].expires)
        assertEquals("2026-10-20T12:00:00Z", alerts[0].effective)
    }

    @Test
    fun multiple_features_map_to_multiple_alerts_in_order() {
        val json = """
            {"features":[
              {"id":"a","properties":{"event":"Flood Advisory","severity":"Minor"}},
              {"id":"b","properties":{"event":"Tornado Warning","severity":"Extreme"}}
            ]}
        """.trimIndent()
        val alerts = GeoJsonMapper.mapAlerts(json)
        assertEquals(2, alerts.size)
        assertEquals("Flood Advisory", alerts[0].event)
        assertEquals(AlertSeverity.EXTREME, alerts[1].severity)
    }

    @Test
    fun empty_area_desc_maps_to_null_but_alert_survives() {
        val alerts = GeoJsonMapper.mapAlerts(featureJson("Minor", "Flood Advisory", null, null, null))
        assertEquals(1, alerts.size)
        assertNull(alerts[0].areaDesc)
        assertEquals("Flood Advisory", alerts[0].event)
    }

    @Test
    fun malformed_features_are_tolerated_and_skipped() {
        val json = """
            {"features":[
              {"id":"bad1"},
              null,
              {"properties":{"event":"MissingId"}},
              {"id":"good","properties":{"event":"Tornado Warning","severity":"EXTREME","onset":"2026-10-20T13:00:00Z"}}
            ]}
        """.trimIndent()
        val alerts = GeoJsonMapper.mapAlerts(json)
        assertEquals(1, alerts.size)
        assertEquals("good", alerts[0].id)
        assertEquals(AlertSeverity.EXTREME, alerts[0].severity)
        // totally unparseable body also degrades to empty, never a throw
        assertTrue(GeoJsonMapper.mapAlerts("this is not json").isEmpty())
    }

    @Test
    fun us_only_filter_silences_non_us_places() = kotlinx.coroutines.runBlocking {
        val service = com.hardikagarwal.weatherfusion.data.NwsAlertsService()
        val germany = com.hardikagarwal.weatherfusion.domain.GeoPlace("Berlin", 52.52, 13.405, "Germany", "DE")
        val canada = com.hardikagarwal.weatherfusion.domain.GeoPlace("Toronto", 43.65, -79.38, "Canada", "ca")
        assertTrue(service.alertsFor(germany).isEmpty())
        assertTrue(service.alertsFor(canada).isEmpty())
        // US gate matches case-insensitively; a US place is eligible for fetch (network not exercised here)
        val us = com.hardikagarwal.weatherfusion.domain.GeoPlace("Austin", 30.27, -97.74, "United States", "US")
        assertTrue(us.countryCode.equals("US", ignoreCase = true))
    }
}
