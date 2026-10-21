package com.hardikagarwal.weatherfusion.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hardikagarwal.weatherfusion.data.GeocodingResult
import com.hardikagarwal.weatherfusion.data.LocationStore
import com.hardikagarwal.weatherfusion.data.OpenMeteoApi
import com.hardikagarwal.weatherfusion.data.WeatherRepository
import com.hardikagarwal.weatherfusion.domain.GeoPlace
import com.hardikagarwal.weatherfusion.domain.WeatherSnapshot
import kotlinx.coroutines.launch

/** Dashboard state machine: drives the whole MainActivity UI. */
sealed class DashboardState {
    data object NoLocation : DashboardState()
    data object Loading : DashboardState()
    data class Ready(val snapshot: WeatherSnapshot) : DashboardState()
    data class Error(val message: String) : DashboardState()
}

class DashboardViewModel(
    private val repository: WeatherRepository = WeatherRepository(),
    private val openMeteoApi: OpenMeteoApi = OpenMeteoApi(),
) : ViewModel() {

    private val _state = MutableLiveData<DashboardState>(DashboardState.NoLocation)
    val state: LiveData<DashboardState> = _state

    private val _searchResults = MutableLiveData<List<GeoPlace>>(emptyList())
    val searchResults: LiveData<List<GeoPlace>> = _searchResults

    fun onPlaceChosen(place: GeoPlace, store: LocationStore) {
        store.save(place) // single source of truth for app + widgets
        loadSnapshot(place)
    }

    fun loadSnapshot(place: GeoPlace) {
        _state.value = DashboardState.Loading
        viewModelScope.launch {
            _state.value = try {
                DashboardState.Ready(repository.snapshot(place))
            } catch (e: Exception) {
                DashboardState.Error(e.message ?: "Network error")
            }
        }
    }

    fun search(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = runCatching { openMeteoApi.geocode(query) }
                .getOrDefault(emptyList())
                .mapNotNull { it.toGeoPlace() }
        }
    }
}

/** Data-layer wire model -> domain model; null when coordinates are missing. */
private fun GeocodingResult.toGeoPlace(): GeoPlace? {
    val lat = latitude ?: return null
    val lon = longitude ?: return null
    return GeoPlace(
        name = name,
        latitude = lat,
        longitude = lon,
        country = country,
        countryCode = countryCode,
        admin1 = admin1,
        timezone = timezone,
    )
}
