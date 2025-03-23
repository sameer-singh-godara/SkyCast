package com.example.skycast.constant

class Const {
    companion object{
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )

        const val openWeatherMapApiKey = "ae6170a64d465935e51cee2d1d45fc8d"

        const val colorBg1 = 0xff08203e
        const val colorBg2 = 0xff557c93

        const val LOADING = "Loading..."
        const val NA = "N/A"

    }
}