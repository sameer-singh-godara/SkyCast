package com.example.skycast

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.example.skycast.utils.LanguageUtils
import com.example.skycast.utils.LocationUtils
import com.example.skycast.view.ForecastSection
import com.example.skycast.view.LanguageSelectionDialog
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

    private fun startLocationUpdate() {
        if (::locationCallback.isInitialized && permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(100)
                .build()
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private suspend fun fetchLocationWithTimeout(timeoutMs: Long = 5000L): MyLatLng? {
        if (!permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            return null
        }
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

        // Apply saved locale before setting content
        LanguageUtils.applySavedLocale(this)

        initLocationClient()
        initViewModel()

        setContent {
            val viewModel: MainViewModel = viewModel()

            // Sync ViewModel language with saved preference
            LaunchedEffect(Unit) {
                viewModel.setLanguage(this@MainActivity, LanguageUtils.getSavedLanguage(this@MainActivity) ?: "en")
            }

            CompositionLocalProvider(LocalFontScale provides viewModel.fontSizeScale) {
                LaunchedEffect(viewModel.darkMode, viewModel.fontSizeScale, viewModel.language) {
                    // Trigger recomposition on theme, font size, or language change
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
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Permission request on app launch
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionMap ->
            val areGranted = permissionMap.values.all { it }
            if (areGranted) {
                locationRequired = true
                Toast.makeText(context, context.getString(R.string.permission_granted), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
            }
        }

        LaunchedEffect(Unit) {
            if (permissions.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }) {
                permissionLauncher.launch(permissions)
            }
        }

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

        // Monitor navigation route changes to prevent issues
        LaunchedEffect(currentRoute) {
            if (navController.graph != null) {
                if (currentRoute !in listOf(Screen.Home.route, Screen.Search.route, Screen.Settings.route)) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        }

        Scaffold(
            modifier = Modifier.semantics { contentDescription = context.getString(R.string.app_name) + " Main Navigation" },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = context.getString(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.semantics { contentDescription = context.getString(R.string.app_name) + " App Title" }
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
                    val isLocationEnabled = LocationUtils.isLocationEnabled(context)
                    val batteryPercentage = BatteryUtils.observeBatteryLevel(context)
                    val shouldAutoRefresh = BatteryUtils.shouldAutoRefresh(batteryPercentage)
                    val allPermissionsGranted = permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

                    if (currentRoute == Screen.Home.route && isLocationEnabled && !shouldAutoRefresh && batteryPercentage != -1 && allPermissionsGranted) {
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
                                                Toast.makeText(context, context.getString(R.string.retrying_location), Toast.LENGTH_SHORT).show()
                                                delay(5000L)
                                            }
                                        }
                                        viewModel.updateCurrentLocation(location)
                                        fetchWeatherInformation(viewModel, location, context)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = context.getString(R.string.refresh) + " weather data" }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.semantics { contentDescription = context.getString(R.string.refresh) + " button" }
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.semantics { contentDescription = context.getString(R.string.refresh) + " icon" }
                                    )
                                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                    Text(context.getString(R.string.refresh))
                                }
                            }
                        }
                    }

                    NavigationBar(
                        modifier = Modifier.semantics { contentDescription = context.getString(R.string.app_name) + " Bottom navigation bar" }
                    ) {
                        listOf(
                            Screen.Home to Icons.Default.Home,
                            Screen.Search to Icons.Default.Search,
                            Screen.Settings to Icons.Default.Settings
                        ).forEach { (screen, icon) ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        icon,
                                        contentDescription = context.getString(
                                            when (screen) {
                                                Screen.Home -> R.string.home
                                                Screen.Search -> R.string.search
                                                Screen.Settings -> R.string.settings
                                            }
                                        ) + " icon",
                                        modifier = Modifier.semantics {
                                            contentDescription = context.getString(
                                                when (screen) {
                                                    Screen.Home -> R.string.home
                                                    Screen.Search -> R.string.search
                                                    Screen.Settings -> R.string.settings
                                                }
                                            ) + " navigation icon"
                                        }
                                    )
                                },
                                label = {
                                    Text(
                                        context.getString(
                                            when (screen) {
                                                Screen.Home -> R.string.home
                                                Screen.Search -> R.string.search
                                                Screen.Settings -> R.string.settings
                                            }
                                        ),
                                        modifier = Modifier.semantics {
                                            contentDescription = context.getString(
                                                when (screen) {
                                                    Screen.Home -> R.string.home
                                                    Screen.Search -> R.string.search
                                                    Screen.Settings -> R.string.settings
                                                }
                                            ) + " navigation label"
                                        }
                                    )
                                },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "Navigate to " + context.getString(
                                        when (screen) {
                                            Screen.Home -> R.string.home
                                            Screen.Search -> R.string.search
                                            Screen.Settings -> R.string.settings
                                        }
                                    )
                                }
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
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    composable(Screen.Home.route) { LocationScreen(context, viewModel, coroutineScope, permissionLauncher) }
                    composable(Screen.Search.route) { SearchScreen(context, viewModel) }
                    composable(Screen.Settings.route) { SettingsScreen(context, viewModel) }
                }
            }
        }
    }

    private fun fetchWeatherInformation(mainViewModel: MainViewModel, currentLocation: MyLatLng, context: Context) {
        mainViewModel.getWeatherByLocation(currentLocation, context)
        mainViewModel.getForecastByLocation(currentLocation, context)
    }

    private fun initViewModel() {
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    @Composable
    private fun LocationScreen(
        context: Context,
        viewModel: MainViewModel,
        coroutineScope: CoroutineScope,
        permissionLauncher: androidx.activity.compose.ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
    ) {
        var allPermissionsGranted by remember { mutableStateOf(permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) }

        // Continuously check permission status every 5 seconds
        LaunchedEffect(Unit) {
            while (true) {
                allPermissionsGranted = permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                delay(5_000L)
            }
        }

        if (!allPermissionsGranted) {
            // Show permission request UI if permissions are not granted
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ErrorSection(
                    errorMessage = context.getString(R.string.permissions_required),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(permissions) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .semantics { contentDescription = context.getString(R.string.request_permissions) }
                ) {
                    Text(context.getString(R.string.request_permissions))
                }
            }
        } else {
            // Proceed with weather fetching and display if permissions are granted
            var isLocationEnabled by remember { mutableStateOf(LocationUtils.isLocationEnabled(context)) }

            LaunchedEffect(Unit) {
                while (true) {
                    isLocationEnabled = LocationUtils.isLocationEnabled(context)
                    delay(5_000L)
                }
            }

            val batteryPercentage = BatteryUtils.observeBatteryLevel(context)
            val refreshInterval = when {
                batteryPercentage > 60 -> 30_000L
                batteryPercentage in 30..60 -> 120_000L
                else -> Long.MAX_VALUE
            }

            LaunchedEffect(allPermissionsGranted) {
                if (!viewModel.hasInitialFetchCompleted && allPermissionsGranted) {
                    while (!viewModel.hasInitialFetchCompleted) {
                        val location = fetchLocationWithTimeout()
                        if (location != null && location.lat != 0.0 && location.lng != 0.0) {
                            viewModel.updateLastFetchedLocation(location)
                            viewModel.updateCurrentLocation(location)
                            fetchWeatherInformation(viewModel, location, context)
                            viewModel.setInitialFetchCompleted(true)
                        } else {
                            Toast.makeText(context, context.getString(R.string.retrying_location), Toast.LENGTH_SHORT).show()
                            delay(5_000L)
                        }
                    }
                }
            }

            if (viewModel.hasInitialFetchCompleted) {
                LaunchedEffect(isLocationEnabled, refreshInterval, batteryPercentage, allPermissionsGranted) {
                    if (isLocationEnabled && allPermissionsGranted) {
                        var lastUpdateTime = System.currentTimeMillis()
                        while (true) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdateTime >= refreshInterval) {
                                val location = fetchLocationWithTimeout()
                                if (location != null && location.lat != 0.0 && location.lng != 0.0) {
                                    viewModel.updateLastFetchedLocation(location)
                                    viewModel.updateCurrentLocation(location)
                                    fetchWeatherInformation(viewModel, location, context)
                                }
                                lastUpdateTime = currentTime
                            }
                            delay(1000L)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = context.getString(R.string.home) + " screen content" }
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
                            errorMessage = context.getString(R.string.location_services_disabled),
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
                                    WeatherSection(
                                        weatherResponse = viewModel.weatherResponse,
                                        locationName = viewModel.locationName
                                    )
                                    ForecastSection(viewModel.forecastResponse)
                                }
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
                .semantics { contentDescription = context.getString(R.string.search) + " screen content" },
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = locationQuery,
                    onValueChange = { locationQuery = it },
                    label = { Text(context.getString(R.string.enter_city_name), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .semantics { contentDescription = context.getString(R.string.enter_city_name) + " input field" },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
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
                        .semantics { contentDescription = if (isLoading) context.getString(R.string.searching) else context.getString(R.string.search_button) + " for weather by city" },
                    enabled = !isLoading && locationQuery.isNotBlank()
                ) {
                    Text(if (isLoading) context.getString(R.string.searching) else context.getString(R.string.search_button))
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (viewModel.searchState) {
                    STATE.LOADING -> if (isLoading) LoadingSection()
                    STATE.FAILED -> ErrorSection(
                        errorMessage = viewModel.searchErrorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                    STATE.SUCCESS -> if (showResults) {
                        if (viewModel.searchWeatherResponse.name?.isNotEmpty() == true) {
                            WeatherSection(
                                weatherResponse = viewModel.searchWeatherResponse,
                                locationName = viewModel.searchLocationName
                            )
                        }
                        if (viewModel.searchForecastResponse.list?.isNotEmpty() == true) {
                            ForecastSection(viewModel.searchForecastResponse)
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
    fun SettingsScreen(context: Context, viewModel: MainViewModel) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .semantics { contentDescription = context.getString(R.string.settings) + " screen content" }
        ) {
            // Font Size Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = context.getString(R.string.font_size) + " settings card" },
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
                        text = context.getString(R.string.font_size),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = context.getString(R.string.font_size) + " settings underlined title" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = context.getString(R.string.font_size) + " adjustment controls" },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.decreaseFontSize() },
                            enabled = viewModel.fontSizeScale > 0.5f,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = context.getString(R.string.decrease_font_size) }
                        ) { Text(context.getString(R.string.decrease_font_size)) }
                        Text(
                            text = context.getString(
                                when {
                                    viewModel.fontSizeScale <= 0.8f -> R.string.small
                                    viewModel.fontSizeScale <= 1.0f -> R.string.medium
                                    else -> R.string.large
                                }
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .semantics {
                                    contentDescription = context.getString(R.string.font_size) + ": " + context.getString(
                                        when {
                                            viewModel.fontSizeScale <= 0.8f -> R.string.small
                                            viewModel.fontSizeScale <= 1.0f -> R.string.medium
                                            else -> R.string.large
                                        }
                                    )
                                }
                        )
                        Button(
                            onClick = { viewModel.increaseFontSize() },
                            enabled = viewModel.fontSizeScale < 1.5f,
                            modifier = Modifier
                                .width(100.dp)
                                .semantics { contentDescription = context.getString(R.string.increase_font_size) }
                        ) { Text(context.getString(R.string.increase_font_size)) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appearance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = context.getString(R.string.appearance) + " settings card" },
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
                        text = context.getString(R.string.appearance),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = context.getString(R.string.appearance) + " settings underlined title" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = context.getString(R.string.appearance) + " toggle" },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(if (viewModel.darkMode) R.string.dark_mode else R.string.light_mode),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics {
                                contentDescription = context.getString(R.string.appearance) + ": " + context.getString(if (viewModel.darkMode) R.string.dark_mode else R.string.light_mode)
                            }
                        )
                        Switch(
                            checked = viewModel.darkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            modifier = Modifier.semantics {
                                contentDescription = context.getString(
                                    if (viewModel.darkMode) R.string.toggle_to_light_mode else R.string.toggle_to_dark_mode
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Language Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = context.getString(R.string.language) + " settings card" },
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
                        text = context.getString(R.string.language),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = context.getString(R.string.language) + " settings underlined title" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LanguageSelectionDialog(viewModel) { languageCode ->
                        // Handle language change by restarting activity
                        context.startActivity(Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                        (context as? ComponentActivity)?.finish()
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fun Feature Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = context.getString(R.string.fun_feature) + " card" },
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
                        text = context.getString(R.string.fun_feature),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = context.getString(R.string.fun_feature) }
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
                            .semantics { contentDescription = context.getString(R.string.lets_have_fun) }
                    ) {
                        Text(context.getString(R.string.lets_have_fun))
                    }
                }
            }
        }
    }

    @Composable
    fun ErrorSection(errorMessage: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.error) {
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = context.getString(R.string.app_name) + " Error message" },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = errorMessage,
                color = color,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics { contentDescription = context.getString(R.string.app_name) + " Error: $errorMessage" }
            )
        }
    }

    @Composable
    fun LoadingSection() {
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = context.getString(R.string.loading) + " indicator" },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = context.getString(R.string.loading) + " weather data" }
            )
        }
    }

    private fun initLocationClient() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }
}