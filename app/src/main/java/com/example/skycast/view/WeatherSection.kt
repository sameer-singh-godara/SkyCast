package com.example.skycast.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.skycast.R
import com.example.skycast.model.weather.WeatherResult
import com.example.skycast.utils.Utils.Companion.buildIcon
import com.example.skycast.utils.Utils.Companion.timestampToHumanDate
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIconType
import com.guru.fontawesomecomposelib.FaIcons

@Composable
fun WeatherSection(
    weatherResponse: WeatherResult,
    locationName: String, // Add locationName parameter
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Use the provided locationName
    val displayLocationName = locationName.takeIf { it.isNotEmpty() } ?: context.getString(R.string.na)

    // Date and time formatting
    val formattedDateTime = weatherResponse.dt?.let { timestamp ->
        timestampToHumanDate(timestamp.toLong(), "EEE, MMM d • hh:mm a")
    } ?: context.getString(R.string.loading)

    // Sunrise and sunset times
    val sunriseTime = weatherResponse.sys?.sunrise?.let { timestamp ->
        timestampToHumanDate(timestamp.toLong(), "hh:mm a")
    } ?: context.getString(R.string.loading)
    val sunsetTime = weatherResponse.sys?.sunset?.let { timestamp ->
        timestampToHumanDate(timestamp.toLong(), "hh:mm a")
    } ?: context.getString(R.string.loading)

    // Weather description and icon
    val weatherDescription = weatherResponse.weather?.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: context.getString(R.string.loading)
    val weatherIcon = weatherResponse.weather?.firstOrNull()?.icon ?: context.getString(R.string.loading)

    // Temperature and other info
    val temperature = weatherResponse.main?.temp?.let { "%.1f".format(it) + "°C" } ?: context.getString(R.string.loading)
    val feelsLike = weatherResponse.main?.feelsLike?.let { "%.1f".format(it) + "°C" } ?: context.getString(R.string.na)
    val tempMin = weatherResponse.main?.tempMin?.let { "%.1f".format(it) + "°C" } ?: context.getString(R.string.na)
    val tempMax = weatherResponse.main?.tempMax?.let { "%.1f".format(it) + "°C" } ?: context.getString(R.string.na)
    val pressure = weatherResponse.main?.pressure?.let { "${it.toInt()} hPa" } ?: context.getString(R.string.na)
    val humidity = weatherResponse.main?.humidity?.let { "$it%" } ?: context.getString(R.string.na)
    val windSpeed = weatherResponse.wind?.speed?.let { "${it.toInt()} m/s" } ?: context.getString(R.string.loading)
    val windDirection = weatherResponse.wind?.deg?.let { "$it°" } ?: context.getString(R.string.na)
    val visibility = weatherResponse.visibility?.let { "${it}m" } ?: context.getString(R.string.na)
    val cloudiness = weatherResponse.clouds?.all?.let { "$it%" } ?: context.getString(R.string.loading)
    val snowVolume = weatherResponse.snow?.d1h?.let { "${it}mm" } ?: context.getString(R.string.na)

    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = context.getString(R.string.current_weather_description, displayLocationName) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location, Date/Time, Sunrise, and Sunset
        WeatherTitleSection(
            title = displayLocationName,
            subtitle = context.getString(R.string.sunrise_sunset, sunriseTime, sunsetTime),
            additionalInfo = formattedDateTime,
            titleStyle = MaterialTheme.typography.headlineLarge,
            subtitleStyle = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Weather Icon
        WeatherImage(icon = weatherIcon)

        Spacer(modifier = Modifier.height(8.dp))

        // Temperature and Description
        WeatherTitleSection(
            title = temperature,
            subtitle = weatherDescription,
            additionalInfo = "",
            titleStyle = MaterialTheme.typography.displayLarge,
            subtitleStyle = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Additional Temperature Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .semantics { contentDescription = context.getString(R.string.additional_temp_details) },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherStatItem(icon = FaIcons.ThermometerHalf, value = feelsLike, label = context.getString(R.string.feels_like, feelsLike))
            WeatherStatItem(icon = FaIcons.ThermometerEmpty, value = tempMin, label = context.getString(R.string.temperature, tempMin))
            WeatherStatItem(icon = FaIcons.ThermometerFull, value = tempMax, label = context.getString(R.string.temperature, tempMax))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weather Stats (Aligned in two rows)
        WeatherStatsRow(
            windSpeed = windSpeed,
            windDirection = windDirection,
            cloudiness = cloudiness,
            snowVolume = snowVolume,
            visibility = visibility,
            pressure = pressure,
            humidity = humidity
        )
    }
}

@Composable
fun WeatherTitleSection(
    title: String,
    subtitle: String,
    additionalInfo: String,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge,
    subtitleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$title, $subtitle${if (additionalInfo.isNotEmpty()) ", $additionalInfo" else ""}"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, // Center the title text
            modifier = Modifier
                .fillMaxWidth() // Ensure the Text takes full width for centering
                .semantics { contentDescription = title }
        )
        Text(
            text = subtitle,
            style = subtitleStyle,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center, // Center the subtitle for consistency
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .semantics { contentDescription = subtitle }
        )
        if (additionalInfo.isNotEmpty()) {
            Text(
                text = additionalInfo,
                style = subtitleStyle,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center, // Center the additional info for consistency
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .semantics { contentDescription = additionalInfo }
            )
        }
    }
}

@Composable
fun WeatherStatsRow(
    windSpeed: String,
    windDirection: String,
    cloudiness: String,
    snowVolume: String,
    visibility: String,
    pressure: String,
    humidity: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .semantics { contentDescription = context.getString(R.string.weather_stats) },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // First row with 4 items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = context.getString(R.string.weather_stats) + " First row" },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherStatItem(icon = FaIcons.Wind, value = windSpeed, label = context.getString(R.string.wind))
            WeatherStatItem(icon = FaIcons.Compass, value = windDirection, label = context.getString(R.string.direction))
            WeatherStatItem(icon = FaIcons.Cloud, value = cloudiness, label = context.getString(R.string.clouds))
            WeatherStatItem(icon = FaIcons.Snowflake, value = snowVolume, label = context.getString(R.string.snow))
        }
        // Second row with 3 items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = context.getString(R.string.weather_stats) + " Second row" },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherStatItem(icon = FaIcons.Eye, value = visibility, label = context.getString(R.string.visibility))
            WeatherStatItem(icon = FaIcons.TachometerAlt, value = pressure, label = context.getString(R.string.pressure))
            WeatherStatItem(icon = FaIcons.Tint, value = humidity, label = context.getString(R.string.humidity))
        }
    }
}

@Composable
fun WeatherStatItem(
    icon: FaIconType.SolidIcon,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.semantics {
            contentDescription = "$label: $value"
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FaIcon(
            faIcon = icon,
            size = 24.dp,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { contentDescription = "$label icon" }
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 4.dp)
                .semantics { contentDescription = value }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.semantics { contentDescription = label }
        )
    }
}

@Composable
fun WeatherImage(icon: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AsyncImage(
        model = buildIcon(icon),
        contentDescription = context.getString(R.string.weather_condition_icon),
        modifier = modifier
            .width(120.dp)
            .height(120.dp)
            .semantics { contentDescription = context.getString(R.string.weather_condition_icon) },
        contentScale = ContentScale.Fit
    )
}