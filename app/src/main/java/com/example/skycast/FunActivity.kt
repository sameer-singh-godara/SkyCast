package com.example.skycast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.semantics.LiveRegionMode
import com.example.skycast.ui.theme.SkyCastTheme
import com.example.skycast.utils.LanguageUtils
import kotlinx.coroutines.delay

class FunActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved locale
        LanguageUtils.applySavedLocale(this)

        val darkMode = intent.getBooleanExtra("darkMode", false)
        setContent {
            SkyCastTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FunScreen()
                }
            }
        }
    }
}

@Composable
fun FunScreen() {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0.0f) }
    var message by remember { mutableStateOf("") }
    var isFinalMessage by remember { mutableStateOf(false) }
    var isStarted by remember { mutableStateOf(false) }
    var showCircularProgress by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 5000),
        label = "Progress animation"
    )

    LaunchedEffect(isStarted) {
        if (isStarted && !isComplete) {
            // Interval 1: Fetching location - 15% for 5 seconds
            showCircularProgress = false
            progress = 0.15f
            message = context.getString(R.string.fetching_location)
            isFinalMessage = false
            delay(500L)
            showCircularProgress = true
            delay(5000L)
            showCircularProgress = false

            // Interval 2: Getting results - 65% for 15 seconds
            showCircularProgress = false
            progress = 0.65f
            message = context.getString(R.string.getting_results)
            isFinalMessage = false
            delay(500L)
            showCircularProgress = true
            delay(15000L)
            showCircularProgress = false

            // Interval 3: Processing data - 99% for 10 seconds
            showCircularProgress = false
            progress = 0.99f
            message = context.getString(R.string.processing_data)
            isFinalMessage = false
            delay(500L)
            showCircularProgress = true
            delay(10000L)
            showCircularProgress = false

            // Interval 4: Go out and see - 100% for 5 seconds
            showCircularProgress = false
            progress = 1.0f
            message = context.getString(R.string.go_outside_message)
            isFinalMessage = true
            showCircularProgress = false

            // Mark as complete and keep activity open for 3 seconds
            isComplete = true
            delay(3000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { contentDescription = context.getString(R.string.fun_feature) + " screen" },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isComplete) {
                Text(
                    text = context.getString(R.string.go_outside_message),
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = context.getString(R.string.go_outside_message)
                            liveRegion = LiveRegionMode.Polite
                        }
                )
            } else if (isStarted) {
                if (showCircularProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = context.getString(R.string.loading) },
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .semantics { contentDescription = context.getString(R.string.progress_bar, (animatedProgress * 100).toInt()) },
                    color = Color.Green,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = context.getString(R.string.progress_percentage, (animatedProgress * 100).toInt())
                            liveRegion = LiveRegionMode.Polite
                        }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = if (isFinalMessage) MaterialTheme.typography.displayLarge else MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = context.getString(R.string.status_message, message)
                            liveRegion = LiveRegionMode.Polite
                        }
                )
            } else {
                Button(
                    onClick = { isStarted = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = context.getString(R.string.get_weather_data) }
                ) {
                    Text(context.getString(R.string.get_weather_data))
                }
            }
        }
        Button(
            onClick = { (context as? ComponentActivity)?.finish() },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = context.getString(R.string.back) }
        ) {
            Text(context.getString(R.string.back))
        }
    }
}