package com.hardikagarwal.weatherfusion.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hardikagarwal.weatherfusion.R
import com.hardikagarwal.weatherfusion.data.LocationStore
import com.hardikagarwal.weatherfusion.domain.AlertSeverity
import com.hardikagarwal.weatherfusion.widget.WidgetRefresher

/**
 * Dashboard: current temp card, provider spread + confidence badge, 5-day strip,
 * severity-tinted NWS alert banner (US only — silently absent elsewhere),
 * and a location search that feeds the widget config binding.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: DashboardViewModel
    private lateinit var locationStore: LocationStore

    private lateinit var searchInput: EditText
    private lateinit var searchResultsBox: LinearLayout
    private lateinit var alertBanner: TextView
    private lateinit var placeLabel: TextView
    private lateinit var tempLabel: TextView
    private lateinit var feelsLikeLabel: TextView
    private lateinit var conditionLabel: TextView
    private lateinit var humidityLabel: TextView
    private lateinit var windLabel: TextView
    private lateinit var confidenceBadge: TextView
    private lateinit var spreadLabel: TextView
    private lateinit var providerRows: LinearLayout
    private lateinit var dailyStrip: LinearLayout
    private lateinit var statusLabel: TextView
    private lateinit var refreshBar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        locationStore = LocationStore(this)
        bindViews()
        setupSearch()

        viewModel.state.observe(this) { state -> render(state) }
        viewModel.searchResults.observe(this) { results -> renderResults(results) }

        locationStore.load()?.let { viewModel.loadSnapshot(it) }
    }

    private fun bindViews() {
        searchInput = findViewById(R.id.search_input)
        searchResultsBox = findViewById(R.id.search_results_box)
        alertBanner = findViewById(R.id.alert_banner)
        placeLabel = findViewById(R.id.place_label)
        tempLabel = findViewById(R.id.temp_label)
        feelsLikeLabel = findViewById(R.id.feels_like_label)
        conditionLabel = findViewById(R.id.condition_label)
        humidityLabel = findViewById(R.id.humidity_label)
        windLabel = findViewById(R.id.wind_label)
        confidenceBadge = findViewById(R.id.confidence_badge)
        spreadLabel = findViewById(R.id.spread_label)
        providerRows = findViewById(R.id.provider_rows)
        dailyStrip = findViewById(R.id.daily_strip)
        statusLabel = findViewById(R.id.status_label)
        refreshBar = findViewById(R.id.refresh_bar)
        refreshBar.setOnClickListener {
            locationStore.load()?.let { viewModel.loadSnapshot(it) }
        }
    }

    private fun setupSearch() {
        searchInput.setOnEditorActionListener { _, _, _ ->
            viewModel.search(searchInput.text.toString().trim())
            true
        }
        searchInput.addTextChangedListener(simpleWatcher { text -> viewModel.search(text.trim()) })
    }

    private fun renderResults(results: List<com.hardikagarwal.weatherfusion.domain.GeoPlace>) {
        searchResultsBox.removeAllViews()
        results.forEach { place ->
            val row = TextView(this).apply {
                text = place.displayName()
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setOnClickListener {
                    searchInput.setText(place.name)
                    searchResultsBox.removeAllViews()
                    viewModel.onPlaceChosen(place, locationStore)
                }
            }
            searchResultsBox.addView(row)
        }
    }

    private fun render(state: DashboardState) {
        when (state) {
            is DashboardState.NoLocation -> {
                statusLabel.visibility = View.VISIBLE
                statusLabel.text = getString(R.string.no_location_hint)
                refreshBar.visibility = View.GONE
                alertBanner.visibility = View.GONE
            }
            is DashboardState.Loading -> {
                statusLabel.visibility = View.VISIBLE
                statusLabel.text = getString(R.string.loading)
            }
            is DashboardState.Error -> {
                statusLabel.visibility = View.VISIBLE
                statusLabel.text = getString(R.string.error_prefix, state.message)
            }
            is DashboardState.Ready -> {
                statusLabel.visibility = View.GONE
                refreshBar.visibility = View.VISIBLE
                val snap = state.snapshot
                placeLabel.text = snap.place.displayName()
                tempLabel.text = snap.current.temperatureC?.let { "${kotlin.math.round(it).toInt()}°" } ?: "—"
                feelsLikeLabel.text = snap.current.apparentTemperatureC?.let { getString(R.string.feels_like, kotlin.math.round(it).toInt()) } ?: ""
                conditionLabel.text = Theme.weatherGlyph(snap.current.weatherCode, snap.current.isDay)
                humidityLabel.text = snap.current.humidityPercent?.let { getString(R.string.humidity_pct, it) } ?: ""
                windLabel.text = snap.current.windKph?.let { getString(R.string.wind_kph, kotlin.math.round(it).toInt()) } ?: ""

                // Confidence badge + provider spread
                confidenceBadge.text = getString(R.string.confidence_fmt, Theme.confidenceLabel(snap.current.confidence))
                confidenceBadge.background.setTint(Theme.confidenceColor(snap.current.confidence))
                spreadLabel.text = getString(R.string.spread_fmt, snap.current.providerSpread.spreadTenthsC / 10.0)
                providerRows.removeAllViews()
                snap.current.providerSpread.reads.forEach { read ->
                    val row = TextView(this).apply {
                        text = "${read.label}: ${read.temperatureC?.let { "${kotlin.math.round(it * 10) / 10.0}°C" } ?: "—"}"
                        setPadding(0, dp(2), 0, dp(2))
                    }
                    providerRows.addView(row)
                }

                // 5-day strip
                dailyStrip.removeAllViews()
                snap.current.daily.take(5).forEach { day ->
                    val cell = TextView(this).apply {
                        text = "${day.date.takeLast(5)}  ${Theme.weatherGlyph(day.weatherCode)}  ${day.tempMaxC?.let { "${kotlin.math.round(it).toInt()}°" } ?: "—"}/${day.tempMinC?.let { "${kotlin.math.round(it).toInt()}°" } ?: "—"}"
                        setPadding(dp(12), dp(8), dp(12), dp(8))
                    }
                    dailyStrip.addView(cell)
                }

                // NWS alert banner: severity-tinted, US-only, silently absent otherwise
                val alert = topAlert(snap.alerts)
                if (alert == null) {
                    alertBanner.visibility = View.GONE
                } else {
                    alertBanner.visibility = View.VISIBLE
                    alertBanner.text = "${alert.event} — ${alert.headline ?: alert.areaDesc ?: ""}"
                    alertBanner.setBackgroundColor(Theme.alertColor(alert.severity))
                }

                // Keep the widget in sync with the dashboard's fresh data.
                WidgetRefresher.requestRefresh(applicationContext)
            }
        }
    }

    private fun topAlert(alerts: List<com.hardikagarwal.weatherfusion.domain.NwsAlert>): com.hardikagarwal.weatherfusion.domain.NwsAlert? =
        alerts.maxByOrNull { severityRank(it.severity) }

    private fun severityRank(severity: AlertSeverity): Int = when (severity) {
        AlertSeverity.EXTREME -> 5
        AlertSeverity.SEVERE -> 4
        AlertSeverity.MODERATE -> 3
        AlertSeverity.MINOR -> 2
        AlertSeverity.UNKNOWN -> 1
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun simpleWatcher(onChange: (String) -> Unit): android.text.TextWatcher =
        object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { s?.let { onChange(it.toString()) } }
        }
}
