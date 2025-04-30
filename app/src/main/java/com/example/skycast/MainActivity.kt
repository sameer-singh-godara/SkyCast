package com.example.skycast

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.skycast.constant.Const.Companion.permissions
import com.example.skycast.model.MyLatLng
import com.example.skycast.ui.theme.LocalFontScale
import com.example.skycast.ui.theme.SkyCastTheme
import com.example.skycast.utils.BatteryUtils
import com.example.skycast.utils.LocationUtils
import com.example.skycast.view.ForecastSection
import com.example.skycast.view.WeatherSection
import com.example.skycast.viewmodel.MainViewModel
import com.example.skycast.viewmodel.STATE
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Settings : Screen("settings")
}

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationRequired: Boolean = false
    private lateinit var mainViewModel: MainViewModel

    override fun onResume() {
        super.onResume()
        if (locationRequired) startLocationUpdate()
    }

    override fun onPause() {
        super.onPause()
        if (::locationCallback.isInitialized) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdate() {
        if (::locationCallback.isInitialized) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(100)
                .build()
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLocationWithTimeout(timeoutMs: Long = 5000L): MyLatLng? {
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.locations.firstOrNull()?.let { location ->
                            continuation.resume(MyLatLng(location.latitude, location.longitude))
                            fusedLocationProviderClient.removeLocationUpdates(this)
                        }
                    }
                }
                locationCallback = callback
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(3000)
                    .setMaxUpdateDelayMillis(100)
                    .build()
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
                continuation.invokeOnCancellation {
                    fusedLocationProviderClient.removeLocationUpdates(callback)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initLocationClient()
        initViewModel()

        setContent {
            val viewModel: MainViewModel = viewModel()

            CompositionLocalProvider(LocalFontScale provides viewModel.fontSizeScale) {
                LaunchedEffect(viewModel.darkMode, viewModel.fontSizeScale) {
                    // Trigger recomposition on theme or font size change
                }

                SkyCastTheme(darkTheme = viewModel.darkMode, fontScale = viewModel.fontSizeScale) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AppNavigation(this@MainActivity, viewModel)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AppNavigation(context: Context, viewModel: MainViewModel) {
        val navController = rememberNavController()
        val systemUiController = rememberSystemUiController()
        val coroutineScope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            systemUiController.isSystemBarsVisible = true
            onDispose { }
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0.locations.forEach { location ->
                    viewModel.updateCurrentLocation(MyLatLng(location.latitude, location.longitude))
                }
            }
        }

        Scaffold(
            modifier = Modifier.semantics { contentDescription = "SkyCast Main Navigation" },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "SkyCast",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.semantics { contentDescription = "SkyCast App Title" }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            bottomBar = {
                Column {
                    // Show manual refresh button only on Home screen if battery < 30% and location is enabled
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isLocationEnabled = LocationUtils.isLocationEnabled(context)
                    val batteryPercentage = BatteryUtils.observeBatteryLevel(context)
                    val shouldAutoRefresh = BatteryUtils.shouldAutoRefresh(batteryPercentage)

                    if (currentRoute == Screen.Home.route && isLocationEnabled && !shouldAutoRefresh && batteryPercentage != -1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.Main) {
                                        viewModel.state = STATE.NOTHING
                                        var location: MyLatLng? = null
                                        while (location == null || location.lat == 0.0 || location.lng == 0.0) {
                                            location = fetchLocationWithTimeout()
                                            if (location == null || location.lat == 0.0 || location.lng == 0.0) {
                                                Toast.makeText(context, "Retrying to fetch location...", Toast.LENGTH_SHORT).show()
                                                delay(5000L)
                                            }
                                        }
                                        viewModel.updateCurrentLocation(location)
                                        fetchWeatherInformation(viewModel, location)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Refresh weather data" }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.semantics { contentDescription = "Refresh button" }
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null, // Described by parent semantics
                                        modifier = Modifier.semantics { contentDescription = "Refresh icon" }
                                    )
                                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                    Text("Refresh")
                                }
                            }
                        }
                    }

                    NavigationBar(
                        modifier = Modifier.semantics { contentDescription = "Bottom navigation bar" }
                    ) {
                        val currentNavRoute = navBackStackEntry?.destination?.route

                        listOf(
                            Screen.Home to Icons.Default.Home,
                            Screen.Search to Icons.Default.Search,
                            Screen.Settings to Icons.Default.Settings
                        ).forEach { (screen, icon) ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        icon,
                                        contentDescription = "${screen.route.capitalize(Locale.current)} icon",
                                        modifier = Modifier.semantics { contentDescription = "${screen.route.capitalize(Locale.current)} navigation icon" }
                                    )
                                },
                                label = {
                                    Text(
                                        screen.route.capitalize(Locale.current),
                                        modifier = Modifier.semantics { contentDescription = "${screen.route.capitalize(Locale.current)} navigation label" }
                                    )
                                },
                                selected = currentNavRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                            inclusive = screen == Screen.Home
                                        }
                                    }
                                },
                                modifier = Modifier.semantics { contentDescription = "Navigate to ${screen.route.capitalize(Locale.current)}" }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)) {
                NavHost(
                    navController,
                    startDestination = Screen.Home.route
                ) {
                    composable(Screen.Home.route) { LocationScreen(context, viewModel, coroutineScope) }
                    composable(Screen.Search.route) { SearchScreen(context, viewModel) }
                    composable(Screen.Settings.route) { SettingsScreen(viewModel) }
                }
            }
        }
    }

    private fun fetchWeatherInformation(mainViewModel: MainViewModel, currentLocation: MyLatLng) {
        mainViewModel.getWeatherByLocation(currentLocation)
        mainViewModel.getForecastByLocation(currentLocation)
    }

    private fun initViewModel() {
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    @Composable
    private fun LocationScreen(
        context: Context,
        viewModel: MainViewModel,
        coroutineScope: CoroutineScope
    ) {
        val launcherMultiplePermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionMap ->
            val areGranted = permissionMap.values.all { it }
            if (areGranted) {
                locationRequired = true
                Toast.makeText(context, "PERMISSION GRANTED", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "PERMISSION DENIED", Toast.LENGTH_SHORT).show()
            }
        }

        // Monitor battery level
        val batteryPercentage = BatteryUtils.observeBatteryLevel(context)
        val refreshInterval = when {
            batteryPercentage > 60 -> 30_000L // 30 seconds
            batteryPercentage in 30..60 -> 120_000L // 2 minutes
            else -> Long.MAX_VALUE // Disable auto-refresh for < 30%
        }

        // Continuously check location services status
        var isLocationEnabled by remember { mutableStateOf(LocationUtils.isLocationEnabled(context)) }

        LaunchedEffect(Unit) {
            while (true) {
                isLocationEnabled = LocationUtils.isLocationEnabled(context)
                delay(5_000L) // Check every 5 seconds
            }
        }

        // Initial fetch with retries until success
        LaunchedEffect(Unit) {
            if (!viewModel.hasInitialFetchCompleted) {
                while (!viewModel.hasInitialFetchCompleted) {
                    val location = fetchLocationWithTimeout()
                    if (location != null && location.lat != 0.0 && location.lng != 0.0) {
                        viewModel.updateLastFetchedLocation(location)
                        viewModel.updateCurrentLocation(location)
                        fetchWeatherInformation(viewModel, location)
                        viewModel.setInitialFetchCompleted(true)
                    } else {
                        Toast.makeText(context, "Retrying to fetch location...", Toast.LENGTH_SHORT).show()
                        delay(5_000L) // Wait 5 seconds before retrying
                    }
                }
            }
        }

        // Periodic location fetch with battery-saving technique after initial fetch
        if (viewModel.hasInitialFetchCompleted) {
            LaunchedEffect(isLocationEnabled, refreshInterval, batteryPercentage) {
                if (isLocationEnabled) {
                    if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                        var lastUpdateTime = System.currentTimeMillis()
                        while (true) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdateTime >= refreshInterval) {
                                val location = fetchLocationWithTimeout()
                                if (location != null && location.lat != 0.0 && location.lng != 0.0) {
                                    viewModel.updateLastFetchedLocation(location)
                                    viewModel.updateCurrentLocation(location)
                                    fetchWeatherInformation(viewModel, location)
                                }
                                lastUpdateTime = currentTime
                            }
                            delay(1000L) // Check interval timing every second
                        }
                    } else {
                        launcherMultiplePermissions.launch(permissions)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Home screen content" }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isLocationEnabled) {
                    ErrorSection(
                        errorMessage = "Location services are not enabled. Please enable location to view weather data.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (!viewModel.hasInitialFetchCompleted || viewModel.state == STATE.NOTHING) {
                    LoadingSection()
                } else {
                    when (viewModel.state) {
                        STATE.LOADING -> LoadingSection()
                        STATE.FAILED -> ErrorSection(
                            errorMessage = viewModel.errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                        else -> {
                            if (viewModel.lastFetchedLocation.lat == 0.0 && viewModel.lastFetchedLocation.lng == 0.0) {
                                LoadingSection()
                            } else {
                                WeatherSection(viewModel.weatherResponse)
                                ForecastSection(viewModel.forecastResponse)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SearchScreen(context: Context, viewModel: MainViewModel) {
        var locationQuery by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var showResults by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Search screen content" },
            contentAlignment = Alignment.TopEnd
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = locationQuery,
                    onValueChange = { locationQuery = it },
                    label = { Text("Enter city name (e.g., London)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .semantics { contentDescription = "City name input field" },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (locationQuery.isNotBlank()) {
                            isLoading = true
                            showResults = false
                            viewModel.getWeatherByLocationName(context, locationQuery)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .semantics { contentDescription = if (isLoading) "Searching" else "Search for weather by city" },
                    enabled = !isLoading && locationQuery.isNotBlank()
                ) {
                    Text(if (isLoading) "Searching..." else "Search")
                }

                when (viewModel.searchState) {
                    STATE.LOADING -> if (isLoading) LoadingSection()
                    STATE.FAILED -> ErrorSection(
                        errorMessage = viewModel.searchErrorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                    STATE.SUCCESS -> if (showResults) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Search results" },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (viewModel.searchWeatherResponse.name?.isNotEmpty() == true) {
                                WeatherSection(viewModel.searchWeatherResponse)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (viewModel.searchForecastResponse.list?.isNotEmpty() == true) {
                                ForecastSection(viewModel.searchForecastResponse)
                            }
                        }
                    }
                    else -> {}
                }
            }

            LaunchedEffect(viewModel.searchState) {
                when (viewModel.searchState) {
                    STATE.SUCCESS -> {
                        isLoading = false
                        showResults = true
                    }
                    STATE.FAILED -> {
                        isLoading = false
                        showResults = false
                        Toast.makeText(context, viewModel.searchErrorMessage, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    @Composable
    fun SettingsScreen(viewModel: MainViewModel) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .semantics { contentDescription = "Settings screen content" }
        ) {
            // Font Size Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Font size settings card" },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Font Size",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Font size settings underlined title" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Font size adjustment controls" },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.decreaseFontSize() },
                            enabled = viewModel.fontSizeScale > 0.5f,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "Decrease font size" }
                        ) { Text("-ve") }
                        Text(
                            text = when {
                                viewModel.fontSizeScale <= 0.8f -> "Small"
                                viewModel.fontSizeScale <= 1.0f -> "Medium"
                                else -> "Large"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .semantics {
                                    contentDescription = "Current font size: ${when {
                                        viewModel.fontSizeScale <= 0.8f -> "Small"
                                        viewModel.fontSizeScale <= 1.0f -> "Medium"
                                        else -> "Large"
                                    }}"
                                }
                        )
                        Button(
                            onClick = { viewModel.increaseFontSize() },
                            enabled = viewModel.fontSizeScale < 1.5f,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "Increase font size" }
                        ) { Text("+ve") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appearance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Appearance settings card" },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Appearance settings underlined title" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Theme toggle" },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (viewModel.darkMode) "Dark Mode" else "Light Mode",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics {
                                contentDescription = "Current theme: ${if (viewModel.darkMode) "Dark Mode" else "Light Mode"}"
                            }
                        )
                        Switch(
                            checked = viewModel.darkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            modifier = Modifier.semantics {
                                contentDescription = "Toggle ${if (viewModel.darkMode) "light" else "dark"} mode"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fun Feature Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Fun feature card" },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Fun Feature",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Fun feature" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(this@MainActivity, FunActivity::class.java).apply {
                                putExtra("darkMode", viewModel.darkMode)
                            }
                            startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Launch fun activity" }
                    ) {
                        Text("Let's Have Fun")
                    }
                }
            }
        }
    }

    @Composable
    fun ErrorSection(errorMessage: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.error) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Error message" },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = errorMessage,
                color = color,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics { contentDescription = "Error: $errorMessage" }
            )
        }
    }

    @Composable
    fun LoadingSection() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Loading indicator" },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = "Loading weather data" }
            )
        }
    }

    private fun initLocationClient() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }
}