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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.skycast.constant.Const.Companion.NA
import com.example.skycast.constant.Const.Companion.cardColor
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
                            } ?: NA
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
                .fillMaxSize()  // Make column fill entire card
                .padding(8.dp),  // Reduce padding for better space utilization
            verticalArrangement = Arrangement.Center,  // Center vertically
            horizontalAlignment = Alignment.CenterHorizontally  // Center horizontally
        ) {
            Text(
                text = temp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)  // Add spacing between elements
            )

            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp),
                contentScale = ContentScale.Fit  // Changed to Fit for better image scaling
            )

            Text(
                text = time,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),  // Add spacing between elements
                textAlign = TextAlign.Center  // Ensure text is centered for multi-line text
            )
        }
    }
}