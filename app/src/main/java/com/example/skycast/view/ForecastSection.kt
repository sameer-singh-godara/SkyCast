package com.example.skycast.view

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.skycast.constant.Const.Companion.NA
import com.example.skycast.model.forecast.ForecastResult
import com.example.skycast.utils.Utils.Companion.buildIcon
import com.example.skycast.utils.Utils.Companion.timestampToHumanDate

@Composable
fun ForecastSection(
    forecastResponse: ForecastResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        forecastResponse.list?.let { listForecast ->
            if (listForecast.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listForecast) { item ->
                        ForecastTile(
                            temp = item.main?.temp?.let { "${it}°C" } ?: NA,
                            image = item.weather?.firstOrNull()?.icon?.let {
                                buildIcon(it, isBigSize = false)
                            } ?: NA,
                            time = item.dt?.let {
                                timestampToHumanDate(it.toLong(), "EEE, HH:mm\ndd-MM-YYYY")
                            } ?: NA,
                            feelsLike = item.main?.feelsLike?.let { "%.1f".format(it) + "°C" } ?: NA,
                            pop = item.pop?.let { "${(it * 100).toInt()}%" } ?: NA,
                            windSpeed = item.wind?.speed?.let { "${it.toInt()} m/s" } ?: NA,
                            cloudiness = item.clouds?.all?.let { "$it%" } ?: NA
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(10.dp)
            .width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = temp,
                style = MaterialTheme.typography.bodyLarge, // Use typography
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall, // Use typography
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Feels: $feelsLike",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Pop: $pop",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Wind: $windSpeed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Clouds: $cloudiness",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}