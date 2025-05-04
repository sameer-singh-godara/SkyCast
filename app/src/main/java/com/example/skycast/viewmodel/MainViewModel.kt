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
import java.util.Locale

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
    var locationName by mutableStateOf("")
        private set

    var searchState by mutableStateOf(STATE.IDLE)
        private set
    var searchWeatherResponse by mutableStateOf(WeatherResult())
        private set
    var searchForecastResponse by mutableStateOf(ForecastResult())
        private set
    var searchErrorMessage by mutableStateOf("")
        private set
    var searchLocationName by mutableStateOf("")
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

    fun getWeatherByLocation(latLng: MyLatLng, context: Context? = null) {
        Log.d(TAG, "Fetching weather for coordinates: $latLng")
        viewModelScope.launch {
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

                    // Fetch the localized location name if context is provided
                    if (context != null) {
                        locationName = getLocationNameFromCoordinates(context, latLng) ?: weatherResponse.name ?: "${latLng.lat}/${latLng.lng}"
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
                state = STATE.FAILED
                Log.e(TAG, "Weather fetch failed: $errorMessage")
            }
        }
    }

    fun getForecastByLocation(latLng: MyLatLng, context: Context? = null) {
        Log.d(TAG, "getForecastByLocation called, delegating to getWeatherByLocation for: $latLng")
        getWeatherByLocation(latLng, context)
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

                        // Fetch the location name in the selected language using the coordinates
                        searchLocationName = getLocationNameFromCoordinates(context, coordinates) ?: locationName
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
                // Step 1: Geocode the location name in English to ensure reliability
                val englishLocale = Locale("en", "US")
                val englishGeocoder = Geocoder(context, englishLocale)
                val englishAddresses = englishGeocoder.getFromLocationName(locationName, 1)
                if (englishAddresses.isNullOrEmpty()) {
                    Log.w(TAG, "No coordinates found for location: $locationName in English")
                    return@withContext null
                }

                val address = englishAddresses[0]
                val coordinates = MyLatLng(address.latitude, address.longitude)
                Log.d(TAG, "Geocoded $locationName to coordinates: $coordinates in English")
                coordinates
            } catch (e: Exception) {
                Log.e(TAG, "Geocoding error for $locationName in English: ${e.message}")
                null
            }
        }
    }

    private suspend fun getLocationNameFromCoordinates(context: Context, latLng: MyLatLng): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Step 1: Fetch the address in English to ensure we get a reliable full address
                val englishLocale = Locale("en", "US")
                val englishGeocoder = Geocoder(context, englishLocale)
                val englishAddresses = englishGeocoder.getFromLocation(latLng.lat, latLng.lng, 1)
                if (englishAddresses.isNullOrEmpty()) {
                    Log.w(TAG, "No English address found for coordinates: $latLng")
                    return@withContext null
                }

                val englishAddress = englishAddresses[0]
                val englishAddressParts = mutableListOf<String>()
                englishAddress.subLocality?.let { if (it.isNotBlank()) englishAddressParts.add(it) }
                englishAddress.locality?.let { if (it.isNotBlank()) englishAddressParts.add(it) }
                englishAddress.adminArea?.let { if (it.isNotBlank()) englishAddressParts.add(it) }
                englishAddress.countryName?.let { if (it.isNotBlank()) englishAddressParts.add(it) }

                if (englishAddressParts.isEmpty()) {
                    Log.w(TAG, "No English address parts found for coordinates: $latLng")
                    return@withContext null
                }

                val fullEnglishAddress = englishAddressParts.joinToString(", ")
                Log.d(TAG, "Fetched English address for coordinates $latLng: $fullEnglishAddress")

                // Step 2: Use the selected language's locale to translate the full address
                val targetLocale = when (language) {
                    "en" -> Locale("en", "US") // English (US)
                    "hi" -> Locale("hi", "IN") // Hindi (India)
                    "pa" -> Locale("pa", "IN") // Punjabi (India)
                    "mr" -> Locale("mr", "IN") // Marathi (India)
                    "gu" -> Locale("gu", "IN") // Gujarati (India)
                    "bn" -> Locale("bn", "IN") // Bengali (India)
                    "ta" -> Locale("ta", "IN") // Tamil (India)
                    "te" -> Locale("te", "IN") // Telugu (India)
                    "ml" -> Locale("ml", "IN") // Malayalam (India)
                    "kn" -> Locale("kn", "IN") // Kannada (India)
                    else -> Locale("en", "US") // Default to English (US)
                }

                // If the target language is English, return the English address directly
                if (language == "en") {
                    return@withContext fullEnglishAddress
                }

                // Attempt to translate the full English address by geocoding it again in the target language
                val targetGeocoder = Geocoder(context, targetLocale)
                val targetAddresses = targetGeocoder.getFromLocationName(fullEnglishAddress, 1)
                if (!targetAddresses.isNullOrEmpty()) {
                    val targetAddress = targetAddresses[0]
                    val targetAddressParts = mutableListOf<String>()
                    targetAddress.subLocality?.let { if (it.isNotBlank()) targetAddressParts.add(it) }
                    targetAddress.locality?.let { if (it.isNotBlank()) targetAddressParts.add(it) }
                    targetAddress.adminArea?.let { if (it.isNotBlank()) targetAddressParts.add(it) }
                    targetAddress.countryName?.let { if (it.isNotBlank()) targetAddressParts.add(it) }

                    if (targetAddressParts.isNotEmpty()) {
                        val translatedAddress = targetAddressParts.joinToString(", ")
                        Log.d(TAG, "Translated address for coordinates $latLng to $language: $translatedAddress")
                        translatedAddress
                    } else {
                        Log.w(TAG, "No translated address parts found for $fullEnglishAddress in language: $language, falling back to English")
                        fullEnglishAddress
                    }
                } else {
                    Log.w(TAG, "No translated address found for $fullEnglishAddress in language: $language, falling back to English")
                    fullEnglishAddress
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reverse geocoding error for $latLng in language $language: ${e.message}")
                null
            }
        }
    }
}