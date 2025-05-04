package com.example.skycast.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.skycast.R
import com.example.skycast.model.forecast.ForecastResult
import com.example.skycast.utils.Utils.Companion.buildIcon
import com.example.skycast.utils.Utils.Companion.timestampToHumanDate

@Composable
fun ForecastSection(
    forecastResponse: ForecastResult,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = context.getString(R.string.weather_forecast_description) },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        forecastResponse.list?.let { listForecast ->
            if (listForecast.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = context.getString(R.string.forecast_list_description) },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(listForecast.withIndex().toList()) { indexedItem ->
                        val index = indexedItem.index
                        val item = indexedItem.value
                        val isFirst = index == 0
                        val isLast = index == listForecast.size - 1

                        ForecastTile(
                            temp = item.main?.temp?.let { "${it}°C" } ?: context.getString(R.string.na),
                            image = item.weather?.firstOrNull()?.icon?.let {
                                buildIcon(it, isBigSize = false)
                            } ?: context.getString(R.string.na),
                            time = item.dt?.let {
                                timestampToHumanDate(it.toLong(), "EEE, H:mm\ndd-MM-YYYY")
                            } ?: context.getString(R.string.na),
                            feelsLike = item.main?.feelsLike?.let { "%.1f".format(it) + "°C" } ?: context.getString(R.string.na),
                            pop = item.pop?.let { "${(it * 100).toInt()}%" } ?: context.getString(R.string.na),
                            windSpeed = item.wind?.speed?.let { "${it.toInt()} m/s" } ?: context.getString(R.string.na),
                            cloudiness = item.clouds?.all?.let { "$it%" } ?: context.getString(R.string.na),
                            isFirst = isFirst,
                            isLast = isLast
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastTile(
    temp: String,
    image: Any,
    time: String,
    feelsLike: String,
    pop: String,
    windSpeed: String,
    cloudiness: String,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .padding(
                start = if (isFirst) 0.dp else 5.dp,
                end = if (isLast) 0.dp else 5.dp,
                top = 5.dp,
                bottom = 5.dp
            )
            .wrapContentWidth()
            .wrapContentHeight()
            .semantics {
                contentDescription = context.getString(R.string.app_name) + " Forecast for $time: $temp, " +
                        context.getString(R.string.feels_like) + " $feelsLike, " +
                        context.getString(R.string.precipitation_probability, pop) + ", " +
                        context.getString(R.string.wind_speed, windSpeed) + ", " +
                        context.getString(R.string.cloudiness, cloudiness)
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = temp,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = context.getString(R.string.temperature, temp) }
            )

            AsyncImage(
                model = image,
                contentDescription = context.getString(R.string.weather_icon_description),
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp)
                    .semantics { contentDescription = context.getString(R.string.weather_icon_description) },
                contentScale = ContentScale.Fit
            )

            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { contentDescription = context.getString(R.string.time, time) },
                textAlign = TextAlign.Center
            )
            Text(
                text = context.getString(R.string.feels_like) + " " + feelsLike,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { contentDescription = context.getString(R.string.feels_like) + " " + feelsLike }
            )
            Text(
                text = context.getString(R.string.precipitation_probability, pop),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { contentDescription = context.getString(R.string.precipitation_probability, pop) }
            )
            Text(
                text = context.getString(R.string.wind_speed, windSpeed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { contentDescription = context.getString(R.string.wind_speed, windSpeed) }
            )
            Text(
                text = context.getString(R.string.cloudiness, cloudiness),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { contentDescription = context.getString(R.string.cloudiness, cloudiness) }
            )
        }
    }
}