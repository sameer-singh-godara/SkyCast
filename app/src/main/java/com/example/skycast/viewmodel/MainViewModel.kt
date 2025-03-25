package com.example.skycast.viewmodel

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skycast.model.MyLatLng
import com.example.skycast.model.forecast.ForecastResult
import com.example.skycast.model.weather.WeatherResult
import com.example.skycast.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class STATE {
    LOADING,
    SUCCESS,
    FAILED
}

class MainViewModel : ViewModel() {
    // Control state of View Model
    var state by mutableStateOf(STATE.LOADING)
    // Hold value from API for Weather info
    var weatherResponse: WeatherResult by mutableStateOf(WeatherResult())
    // Hold value from API for Forecast info
    var forecastResponse: ForecastResult by mutableStateOf(ForecastResult())
    var errorMessage: String by mutableStateOf(value = "")

    // Theme mode
    var darkMode by mutableStateOf(false)
        private set

    // Font size (small=0.8, medium=1.0, large=1.2)
    var fontSizeScale by mutableStateOf(1.0f)
        private set

    fun toggleDarkMode() {
        darkMode = !darkMode
    }

    fun increaseFontSize() {
        fontSizeScale = when {
            fontSizeScale < 0.8f -> 0.8f
            fontSizeScale < 1.0f -> 1.0f
            else -> 1.2f
        }
    }

    fun decreaseFontSize() {
        fontSizeScale = when {
            fontSizeScale > 1.0f -> 1.0f
            fontSizeScale > 0.8f -> 0.8f
            else -> 0.8f
        }
    }

    fun getWeatherByLocation(latLng: MyLatLng) {
        Log.d("Weather App", "API Called by Coordinates!!!")
        viewModelScope.launch {
            state = STATE.LOADING
            val apiService = RetrofitClient.getInstance()
            try {
                val apiResponse = apiService.getWeather(latLng.lat, latLng.lng)
                weatherResponse = apiResponse // Update state
                state = STATE.SUCCESS
            } catch (e: Exception) {
                errorMessage = e.message!!.toString()
                state = STATE.FAILED
            }
        }
    }

    fun getForecastByLocation(latLng: MyLatLng) {
        viewModelScope.launch {
            state = STATE.LOADING
            val apiService = RetrofitClient.getInstance()
            try {
                val apiResponse = apiService.getForecast(latLng.lat, latLng.lng)
                forecastResponse = apiResponse // Update state
                state = STATE.SUCCESS
            } catch (e: Exception) {
                errorMessage = e.message!!.toString()
                state = STATE.FAILED
            }
        }
    }

    private suspend fun getCoordinatesFromLocationName(context: Context, locationName: String): MyLatLng? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context)
                val addresses = geocoder.getFromLocationName(locationName, 1)
                if (addresses?.isNotEmpty() == true) {
                    val address = addresses[0]
                    MyLatLng(address.latitude, address.longitude)
                } else {
                    null
                }
            } catch (e: Exception) {
                errorMessage = "Location not found: ${e.message}"
                null
            }
        }
    }

    fun getWeatherByLocationName(context: Context, locationName: String) {
        Log.d("Weather App", "API Called by Name!!!")
        viewModelScope.launch {
            state = STATE.LOADING
            errorMessage = ""
            try {
                val coordinates = getCoordinatesFromLocationName(context, locationName)
                if (coordinates != null) {
                    getWeatherByLocation(coordinates)
                    getForecastByLocation(coordinates)
                    state = STATE.SUCCESS
                } else {
                    errorMessage = "Location not found"
                    state = STATE.FAILED
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
                state = STATE.FAILED
            }
        }
    }
}