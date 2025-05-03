package com.example.skycast.utils

import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import java.util.Locale

object LanguageUtils {
    private const val PREF_LANGUAGE_KEY = "pref_language"
    private const val DEFAULT_LANGUAGE = "en" // Fallback to English if no preference is set

    fun applySavedLocale(context: Context) {
        // Retrieve the saved language or default to English
        val savedLanguage = getSavedLanguage(context) ?: DEFAULT_LANGUAGE
        setLocale(context, savedLanguage)
    }

    fun setLocale(context: Context, languageCode: String) {
        // Save the selected language to SharedPreferences
        saveLanguage(context, languageCode)

        // Apply the locale
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    private fun saveLanguage(context: Context, languageCode: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(PREF_LANGUAGE_KEY, languageCode).apply()
    }

    fun getSavedLanguage(context: Context): String? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PREF_LANGUAGE_KEY, null)
    }
}