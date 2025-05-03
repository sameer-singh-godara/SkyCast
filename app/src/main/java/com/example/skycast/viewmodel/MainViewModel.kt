package com.example.skycast.viewmodel

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skycast.R
import com.example.skycast.model.MyLatLng
import com.example.skycast.model.forecast.ForecastResult
import com.example.skycast.model.weather.WeatherResult
import com.example.skycast.network.RetrofitClient
import com.example.skycast.utils.LanguageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class STATE {
    IDLE,
    LOADING,
    SUCCESS,
    FAILED,
    NOTHING
}

class MainViewModel : ViewModel() {
    private companion object {
        const val TAG = "MainViewModel"
        const val DEFAULT_LANGUAGE = "en"
    }

    var state by mutableStateOf(STATE.IDLE)
    var weatherResponse by mutableStateOf(WeatherResult())
        private set
    var forecastResponse by mutableStateOf(ForecastResult())
        private set
    var errorMessage by mutableStateOf("")
        private set
    var lastFetchedLocation by mutableStateOf(MyLatLng(0.0, 0.0))
        private set
    var currentLocation by mutableStateOf(MyLatLng(0.0, 0.0))
        private set
    var hasInitialFetchCompleted by mutableStateOf(false)
        private set

    var searchState by mutableStateOf(STATE.IDLE)
        private set
    var searchWeatherResponse by mutableStateOf(WeatherResult())
        private set
    var searchForecastResponse by mutableStateOf(ForecastResult())
        private set
    var searchErrorMessage by mutableStateOf("")
        private set

    var darkMode by mutableStateOf(false)
        private set
    var fontSizeScale by mutableStateOf(1.0f)
        private set
    var language by mutableStateOf(DEFAULT_LANGUAGE)
        private set

    fun updateCurrentLocation(location: MyLatLng) {
        Log.d(TAG, "Updating current location: $location")
        currentLocation = location
    }

    fun updateLastFetchedLocation(location: MyLatLng) {
        Log.d(TAG, "Updating last fetched location: $location")
        lastFetchedLocation = location
    }

    fun setInitialFetchCompleted(completed: Boolean) {
        Log.d(TAG, "Setting initial fetch completed: $completed")
        hasInitialFetchCompleted = completed
    }

    fun toggleDarkMode() {
        darkMode = !darkMode
        Log.d(TAG, "Dark mode toggled to: $darkMode")
    }

    fun increaseFontSize() {
        fontSizeScale = (fontSizeScale + 0.1f).coerceAtMost(1.5f)
        Log.d(TAG, "Font size increased to: $fontSizeScale")
    }

    fun decreaseFontSize() {
        fontSizeScale = (fontSizeScale - 0.1f).coerceAtLeast(0.5f)
        Log.d(TAG, "Font size decreased to: $fontSizeScale")
    }

    fun setLanguage(context: Context, languageCode: String) {
        language = languageCode
        LanguageUtils.setLocale(context, languageCode)
        Log.d(TAG, "Language set to: $languageCode")
    }

    fun getWeatherByLocation(latLng: MyLatLng) {
        Log.d(TAG, "Fetching weather for coordinates: $latLng")
        viewModelScope.launch {
            if (hasInitialFetchCompleted && lastFetchedLocation == latLng && state == STATE.SUCCESS) {
                Log.d(TAG, "Skipping weather fetch: Data already available for $latLng")
                return@launch
            }

            state = if (state == STATE.NOTHING) STATE.NOTHING else STATE.LOADING
            try {
                coroutineScope {
                    val apiService = RetrofitClient.getInstance()
                    val weatherDeferred = async { apiService.getWeather(latLng.lat, latLng.lng) }
                    val forecastDeferred = async { apiService.getForecast(latLng.lat, latLng.lng) }
                    weatherResponse = weatherDeferred.await()
                    forecastResponse = forecastDeferred.await()
                    state = STATE.SUCCESS
                    Log.d(TAG, "Weather and forecast fetch successful: ${weatherResponse.name}")
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
                state = STATE.FAILED
                Log.e(TAG, "Weather fetch failed: $errorMessage")
            }
        }
    }

    fun getForecastByLocation(latLng: MyLatLng) {
        Log.d(TAG, "getForecastByLocation called, delegating to getWeatherByLocation for: $latLng")
        getWeatherByLocation(latLng)
    }

    fun getWeatherByLocationName(context: Context, locationName: String) {
        Log.d(TAG, "Fetching weather for location name: $locationName")
        viewModelScope.launch {
            searchState = STATE.LOADING
            searchErrorMessage = ""
            try {
                val coordinates = getCoordinatesFromLocationName(context, locationName)
                if (coordinates != null) {
                    coroutineScope {
                        val apiService = RetrofitClient.getInstance()
                        val weatherDeferred = async { apiService.getWeather(coordinates.lat, coordinates.lng) }
                        val forecastDeferred = async { apiService.getForecast(coordinates.lat, coordinates.lng) }
                        searchWeatherResponse = weatherDeferred.await()
                        searchForecastResponse = forecastDeferred.await()
                        searchState = STATE.SUCCESS
                        Log.d(TAG, "Weather and forecast fetch successful for $locationName")
                    }
                } else {
                    searchErrorMessage = context.getString(R.string.location_not_found)
                    searchState = STATE.FAILED
                    Log.e(TAG, "Location not found: $locationName")
                }
            } catch (e: Exception) {
                searchErrorMessage = e.message ?: context.getString(R.string.unknown_error)
                searchState = STATE.FAILED
                Log.e(TAG, "Weather fetch by name failed: $searchErrorMessage")
            }
        }
    }

    private suspend fun getCoordinatesFromLocationName(context: Context, locationName: String): MyLatLng? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context)
                val addresses = geocoder.getFromLocationName(locationName, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    MyLatLng(address.latitude, address.longitude).also {
                        Log.d(TAG, "Geocoded $locationName to coordinates: $it")
                    }
                } else {
                    Log.w(TAG, "No coordinates found for location: $locationName")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Geocoding error for $locationName: ${e.message}")
                null
            }
        }
    }
}