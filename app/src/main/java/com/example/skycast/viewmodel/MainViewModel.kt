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
    var state by mutableStateOf(STATE.LOADING)
    var weatherResponse: WeatherResult by mutableStateOf(WeatherResult())
    var forecastResponse: ForecastResult by mutableStateOf(ForecastResult())
    var errorMessage: String by mutableStateOf(value = "")

    var darkMode by mutableStateOf(false)
        private set

    var fontSizeScale by mutableStateOf(1.0f)
        private set

    fun toggleDarkMode() {
        darkMode = !darkMode
    }

    fun increaseFontSize() {
        fontSizeScale = (fontSizeScale + 0.1f).coerceAtMost(1.5f) // Max 1.5f
        Log.d("FontSize", "Font size increased to $fontSizeScale")
    }

    fun decreaseFontSize() {
        fontSizeScale = (fontSizeScale - 0.1f).coerceAtLeast(0.5f) // Min 0.5f
        Log.d("FontSize", "Font size decreased to $fontSizeScale")
    }

    fun getWeatherByLocation(latLng: MyLatLng) {
        Log.d("Weather App", "API Called by Coordinates!!!")
        viewModelScope.launch {
            state = STATE.LOADING
            val apiService = RetrofitClient.getInstance()
            try {
                val apiResponse = apiService.getWeather(latLng.lat, latLng.lng)
                weatherResponse = apiResponse
                state = STATE.SUCCESS
            } catch (e: Exception) {
                errorMessage = e.message!!
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
                forecastResponse = apiResponse
                state = STATE.SUCCESS
            } catch (e: Exception) {
                errorMessage = e.message!!
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