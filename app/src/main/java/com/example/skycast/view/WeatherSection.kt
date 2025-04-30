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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.skycast.constant.Const.Companion.LOADING
import com.example.skycast.constant.Const.Companion.NA
import com.example.skycast.model.weather.WeatherResult
import com.example.skycast.utils.Utils.Companion.buildIcon
import com.example.skycast.utils.Utils.Companion.timestampToHumanDate
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

    // Sunrise and sunset times
    val sunriseTime = weatherResponse.sys?.sunrise?.let { timestamp ->
        timestampToHumanDate(timestamp.toLong(), "hh:mm a")
    } ?: LOADING
    val sunsetTime = weatherResponse.sys?.sunset?.let { timestamp ->
        timestampToHumanDate(timestamp.toLong(), "hh:mm a")
    } ?: LOADING

    // Weather description and icon
    val weatherDescription = weatherResponse.weather?.firstOrNull()?.description ?: LOADING
    val weatherIcon = weatherResponse.weather?.firstOrNull()?.icon ?: LOADING

    // Temperature and other info
    val temperature = weatherResponse.main?.temp?.let { "%.1f".format(it) + "°C" } ?: LOADING
    val feelsLike = weatherResponse.main?.feelsLike?.let { "%.1f".format(it) + "°C" } ?: NA
    val tempMin = weatherResponse.main?.tempMin?.let { "%.1f".format(it) + "°C" } ?: NA
    val tempMax = weatherResponse.main?.tempMax?.let { "%.1f".format(it) + "°C" } ?: NA
    val pressure = weatherResponse.main?.pressure?.let { "${it.toInt()} hPa" } ?: NA
    val humidity = weatherResponse.main?.humidity?.let { "$it%" } ?: NA
    val windSpeed = weatherResponse.wind?.speed?.let { "${it.toInt()} m/s" } ?: LOADING
    val windDirection = weatherResponse.wind?.deg?.let { "$it°" } ?: NA
    val visibility = weatherResponse.visibility?.let { "${it}m" } ?: NA
    val cloudiness = weatherResponse.clouds?.all?.let { "$it%" } ?: LOADING
    val snowVolume = weatherResponse.snow?.d1h?.let { "${it}mm" } ?: NA

    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Current weather information for $locationName" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location, Date/Time, Sunrise, and Sunset
        WeatherTitleSection(
            title = locationName,
            subtitle = "Sunrise: $sunriseTime | Sunset: $sunsetTime",
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
            subtitle = weatherDescription.replaceFirstChar { it.uppercase() },
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
                .semantics { contentDescription = "Additional temperature details" },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherStatItem(icon = FaIcons.ThermometerHalf, value = feelsLike, label = "Feels Like")
            WeatherStatItem(icon = FaIcons.ThermometerEmpty, value = tempMin, label = "Min Temp")
            WeatherStatItem(icon = FaIcons.ThermometerFull, value = tempMax, label = "Max Temp")
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
            modifier = Modifier.semantics { contentDescription = title }
        )
        Text(
            text = subtitle,
            style = subtitleStyle,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier
                .padding(top = 4.dp)
                .semantics { contentDescription = subtitle }
        )
        if (additionalInfo.isNotEmpty()) {
            Text(
                text = additionalInfo,
                style = subtitleStyle,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .semantics { contentDescription = "Weather statistics" },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // First row with 4 items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "First row of weather stats" },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherStatItem(icon = FaIcons.Wind, value = windSpeed, label = "Wind")
            WeatherStatItem(icon = FaIcons.Compass, value = windDirection, label = "Direction")
            WeatherStatItem(icon = FaIcons.Cloud, value = cloudiness, label = "Clouds")
            WeatherStatItem(icon = FaIcons.Snowflake, value = snowVolume, label = "Snow")
        }
        // Second row with 3 items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Second row of weather stats" },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherStatItem(icon = FaIcons.Eye, value = visibility, label = "Visibility")
            WeatherStatItem(icon = FaIcons.TachometerAlt, value = pressure, label = "Pressure")
            WeatherStatItem(icon = FaIcons.Tint, value = humidity, label = "Humidity")
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
    AsyncImage(
        model = buildIcon(icon),
        contentDescription = "Weather condition icon",
        modifier = modifier
            .width(120.dp)
            .height(120.dp)
            .semantics { contentDescription = "Weather icon for current condition" },
        contentScale = ContentScale.Fit
    )
}