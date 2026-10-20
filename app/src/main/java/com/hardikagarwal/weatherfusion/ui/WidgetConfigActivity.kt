package com.hardikagarwal.weatherfusion.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hardikagarwal.weatherfusion.R
import com.hardikagarwal.weatherfusion.data.LocationStore
import com.hardikagarwal.weatherfusion.data.OpenMeteoApi
import com.hardikagarwal.weatherfusion.widget.WidgetRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget config binding: picking a location here writes the SAME
 * SharedPreferences record the dashboard uses (LocationStore), so the
 * widget can never fork from app state. Refreshes widgets immediately.
 */
class WidgetConfigActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var resultsBox: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_config)
        searchInput = findViewById(R.id.config_search_input)
        resultsBox = findViewById(R.id.config_results_box)
        setTitle(R.string.widget_config_title)

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { search(s?.toString()?.trim() ?: "") }
        })

        // Pre-select the app's current location so "just accept" works.
        LocationStore(this).load()?.let { searchInput.setText(it.name) }
    }

    private fun search(query: String) {
        if (query.length < 2) return
        CoroutineScope(Dispatchers.Main).launch {
            val results = runCatching { OpenMeteoApi().geocode(query) }.getOrDefault(emptyList())
            resultsBox.removeAllViews()
            results.forEach { place ->
                val row = TextView(this@WidgetConfigActivity).apply {
                    text = place.displayName()
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    setOnClickListener {
                        LocationStore(this@WidgetConfigActivity).save(place)
                        WidgetRefresher.requestRefresh(applicationContext)
                        setResult(RESULT_OK)
                        finish()
                    }
                }
                resultsBox.addView(row)
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
