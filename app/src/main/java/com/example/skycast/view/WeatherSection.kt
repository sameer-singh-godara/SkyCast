package com.example.skycast.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // title section
    var title = ""
    if (!weatherResponse.name.isNullOrEmpty()) {
        weatherResponse.name?.let { title = it }
    } else {
        weatherResponse.coord?.let { title = "${it.lat}/${it.lon}" }
    }

    // sub title section
    var subTitle = ""
    val dateVal = (weatherResponse.dt ?: 0)
    subTitle = if (dateVal == 0) LOADING
    else timestampToHumanDate(dateVal.toLong(), "HH:mm, dd-MM-yyyy")

    // icon
    var icon = ""
    var description = ""
    weatherResponse.weather?.let {
        if (it.isNotEmpty()) {
            description = it[0].description ?: LOADING
            icon = it[0].icon ?: LOADING
        }
    }

    // temp
    var temp = weatherResponse.main?.temp?.let { "${it}°C" } ?: LOADING

    // Weather info
    val wind = weatherResponse.wind?.speed?.toString() ?: LOADING
    val clouds = weatherResponse.clouds?.all?.toString() ?: LOADING
    val snow = weatherResponse.snow?.d1h?.toString() ?: NA

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WeatherTitleSection(
            text = title,
            fontSize = 30.sp
        )

        WeatherImage(icon = icon)

        WeatherTitleSection(
            text = temp,
            fontSize = 30.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            WeatherInfo(icon = FaIcons.Wind, text = wind)
            WeatherInfo(icon = FaIcons.Cloud, text = clouds)
            WeatherInfo(icon = FaIcons.Snowflake, text = snow)
        }
    }
}

@Composable
fun WeatherInfo(icon: FaIconType.SolidIcon, text: String) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FaIcon(
            faIcon = icon,
            size = 36.dp,
            tint = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = text,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun WeatherImage(icon: String) {
    AsyncImage(
        model = buildIcon(icon),
        contentDescription = icon,
        modifier = Modifier
            .width(150.dp)
            .height(150.dp),
        contentScale = ContentScale.FillBounds

    )
}

@Composable
fun WeatherTitleSection(
    text: String,
    fontSize: TextUnit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}
