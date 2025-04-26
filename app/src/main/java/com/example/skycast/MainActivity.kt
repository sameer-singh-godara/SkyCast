package com.example.skycast

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
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
                    // Empty block to trigger recomposition
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
        var currentLocation by remember { mutableStateOf(MyLatLng(0.0, 0.0)) }
        val systemUiController = rememberSystemUiController()
        val coroutineScope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            systemUiController.isSystemBarsVisible = true
            onDispose { }
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0.locations.forEach { location ->
                    currentLocation = MyLatLng(location.latitude, location.longitude)
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "SkyCast",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary
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
                    // Show manual refresh button only if battery < 30% and location is enabled
                    val isLocationEnabled = LocationUtils.isLocationEnabled(context)
                    val batteryPercentage = BatteryUtils.observeBatteryLevel(context)
                    val shouldAutoRefresh = BatteryUtils.shouldAutoRefresh(batteryPercentage)
                    if (isLocationEnabled && !shouldAutoRefresh && batteryPercentage != -1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.Main) {
                                        val location = fetchLocationWithTimeout()
                                        if (location != null && location.lat != 0.0 && location.lng != 0.0) {
                                            currentLocation = location
                                            fetchWeatherInformation(viewModel, currentLocation)
                                        } else {
                                            Toast.makeText(context, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    }

                    NavigationBar {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        listOf(
                            Screen.Home to Icons.Default.Home,
                            Screen.Search to Icons.Default.Search,
                            Screen.Settings to Icons.Default.Settings
                        ).forEach { (screen, icon) ->
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = screen.route) },
                                label = { Text(screen.route.capitalize(Locale.current)) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                            inclusive = screen == Screen.Home
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { LocationScreen(context, currentLocation, viewModel, coroutineScope) }
                composable(Screen.Search.route) { SearchScreen(context, viewModel) }
                composable(Screen.Settings.route) { SettingsScreen(viewModel) }
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
        currentLocation: MyLatLng,
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
        var hasInitialFetchCompleted by remember { mutableStateOf(false) }
        var lastFetchedLocation by remember { mutableStateOf(MyLatLng(0.0, 0.0)) }

        LaunchedEffect(Unit) {
            while (true) {
                isLocationEnabled = LocationUtils.isLocationEnabled(context)
                delay(5_000L) // Check every 5 seconds
            }
        }

        // Initial fetch with retries until success
        LaunchedEffect(Unit) {
            while (!hasInitialFetchCompleted) {
                val location = fetchLocationWithTimeout()
                if (location != null && location.lat != 0.0 && location.lng != 0.0) {
                    lastFetchedLocation = location
                    fetchWeatherInformation(viewModel, lastFetchedLocation)
                    hasInitialFetchCompleted = true
                } else {
                    Toast.makeText(context, "Retrying to fetch location...", Toast.LENGTH_SHORT).show()
                    delay(5_000L) // Wait 5 seconds before retrying
                }
            }
        }

        // Periodic location fetch with battery-saving technique after initial fetch
        if (hasInitialFetchCompleted) {
            LaunchedEffect(isLocationEnabled, refreshInterval, batteryPercentage) {
                if (isLocationEnabled) {
                    if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                        var lastUpdateTime = System.currentTimeMillis()
                        while (true) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdateTime >= refreshInterval) {
                                val location = fetchLocationWithTimeout()
                                if (location != null && location.lat != 0.0 && location.lng != 0.0) {
                                    lastFetchedLocation = location
                                    fetchWeatherInformation(viewModel, lastFetchedLocation)
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

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            val marginTop = screenHeight * 0.1f
            val marginTopPx = with(LocalDensity.current) { marginTop.toPx() }

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height + marginTopPx.toInt()) {
                            placeable.placeRelative(0, marginTopPx.toInt())
                        }
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isLocationEnabled && hasInitialFetchCompleted) {
                    ErrorSection("Location services are not enabled. Please enable location to view weather data.")
                } else if (!hasInitialFetchCompleted) {
                    LoadingSection()
                } else {
                    when (viewModel.state) {
                        STATE.LOADING -> LoadingSection()
                        STATE.FAILED -> ErrorSection(viewModel.errorMessage)
                        else -> {
                            if (lastFetchedLocation.lat == 0.0 && lastFetchedLocation.lng == 0.0) {
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

        val systemUiController = rememberSystemUiController()

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    enabled = !isLoading && locationQuery.isNotBlank()
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Search")
                }

                when (viewModel.searchState) {
                    STATE.LOADING -> if (isLoading) LoadingSection()
                    STATE.FAILED -> ErrorSection(viewModel.searchErrorMessage)
                    STATE.SUCCESS -> if (showResults) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
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
        val systemUiController = rememberSystemUiController()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Font Size", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.decreaseFontSize() },
                    enabled = viewModel.fontSizeScale > 0.5f
                ) { Text("Decrease") }
                Text(
                    when {
                        viewModel.fontSizeScale <= 0.8f -> "Small"
                        viewModel.fontSizeScale <= 1.0f -> "Medium"
                        else -> "Large"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Button(
                    onClick = { viewModel.increaseFontSize() },
                    enabled = viewModel.fontSizeScale < 1.5f
                ) { Text("Increase") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (viewModel.darkMode) "Dark Mode" else "Light Mode",
                    style = MaterialTheme.typography.titleLarge
                )
                Switch(
                    checked = viewModel.darkMode,
                    onCheckedChange = { viewModel.toggleDarkMode() }
                )
            }
        }
    }

    @Composable
    fun ErrorSection(errorMessage: String) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = errorMessage)
        }
    }

    @Composable
    fun LoadingSection() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    }

    private fun initLocationClient() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }
}