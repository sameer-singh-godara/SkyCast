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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.skycast.constant.Const.Companion.LOADING
import com.example.skycast.constant.Const.Companion.NA
import com.example.skycast.model.weather.WeatherResult
import com.example.skycast.utils.Utils.Companion.timestampToHumanDate
import com.example.skycast.utils.Utils.Companion.buildIcon
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIconType
import com.guru.fontawesomecomposelib.FaIcons

@Composable
fun WeatherSection(weatherResponse: WeatherResult, modifier: Modifier = Modifier) {
    // Location name
    val locationName = weatherResponse.name ?: weatherResponse.coord?.let {
        "${it.lat}/${it.lon}"
    } ?: "Unknown Location"

    // Date and time formatting
    val formattedDateTime = weatherResponse.dt?.let { timestamp ->
        timestampToHumanDate(timestamp.toLong(), "EEE, MMM d • hh:mm a")
    } ?: LOADING

    // Weather description and icon
    val weatherDescription = weatherResponse.weather?.firstOrNull()?.description ?: LOADING
    val weatherIcon = weatherResponse.weather?.firstOrNull()?.icon ?: LOADING

    // Temperature and other info
    val temperature = weatherResponse.main?.temp?.let { "%.1f".format(it) + "°C" } ?: LOADING
    val windSpeed = weatherResponse.wind?.speed?.let { "${it.toInt()} m/s" } ?: LOADING
    val cloudiness = weatherResponse.clouds?.all?.let { "$it%" } ?: LOADING
    val snowVolume = weatherResponse.snow?.d1h?.let { "${it}mm" } ?: NA

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location and Date/Time
        WeatherTitleSection(
            title = locationName,
            subtitle = formattedDateTime,
            titleStyle = MaterialTheme.typography.headlineLarge, // Use typography
            subtitleStyle = MaterialTheme.typography.bodyLarge // Use typography
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Weather Icon
        WeatherImage(icon = weatherIcon)

        Spacer(modifier = Modifier.height(8.dp))

        // Temperature and Description
        WeatherTitleSection(
            title = temperature,
            subtitle = weatherDescription.replaceFirstChar { it.uppercase() },
            titleStyle = MaterialTheme.typography.displayLarge, // Use typography
            subtitleStyle = MaterialTheme.typography.bodyLarge // Use typography
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Weather Stats
        WeatherStatsRow(
            windSpeed = windSpeed,
            cloudiness = cloudiness,
            snowVolume = snowVolume
        )
    }
}

@Composable
fun WeatherTitleSection(
    title: String,
    subtitle: String,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge,
    subtitleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = titleStyle, // Use typography style
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = subtitleStyle, // Use typography style
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun WeatherStatsRow(
    windSpeed: String,
    cloudiness: String,
    snowVolume: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        WeatherStatItem(icon = FaIcons.Wind, value = windSpeed, label = "Wind")
        WeatherStatItem(icon = FaIcons.Cloud, value = cloudiness, label = "Clouds")
        WeatherStatItem(icon = FaIcons.Snowflake, value = snowVolume, label = "Snow")
    }
}

@Composable
fun WeatherStatItem(
    icon: FaIconType.SolidIcon,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FaIcon(
            faIcon = icon,
            size = 24.dp,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium, // Use typography
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall, // Use typography
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun WeatherImage(icon: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = buildIcon(icon),
        contentDescription = "Weather icon",
        modifier = modifier
            .width(120.dp)
            .height(120.dp),
        contentScale = ContentScale.Fit
    )
}