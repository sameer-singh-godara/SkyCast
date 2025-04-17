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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.coroutineScope

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

    @Composable
    private fun AppNavigation(context: Context, viewModel: MainViewModel) {
        val navController = rememberNavController()
        var currentLocation by remember { mutableStateOf(MyLatLng(0.0, 0.0)) }
        val systemUiController = rememberSystemUiController()

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
            bottomBar = {
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
                            label = { Text(screen.route.capitalize()) },
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
        ) { innerPadding ->
            NavHost(navController, startDestination = Screen.Home.route, modifier = Modifier.padding(innerPadding)) {
                composable(Screen.Home.route) { LocationScreen(context, currentLocation, viewModel) }
                composable(Screen.Search.route) { SearchScreen(context, viewModel) }
                composable(Screen.Settings.route) { SettingsScreen(viewModel) }
            }
        }
    }

    private fun fetchWeatherInformation(mainViewModel: MainViewModel, currentLocation: MyLatLng) {
        mainViewModel.state = STATE.LOADING
        mainViewModel.getWeatherByLocation(currentLocation)
        mainViewModel.getForecastByLocation(currentLocation)
        mainViewModel.state = STATE.SUCCESS
    }

    private fun initViewModel() {
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    @Composable
    private fun LocationScreen(context: Context, currentLocation: MyLatLng, viewModel: MainViewModel) {
        val launcherMultiplePermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionMap ->
            val areGranted = permissionMap.values.all { it }
            if (areGranted) {
                locationRequired = true
                startLocationUpdate()
                Toast.makeText(context, "PERMISSION GRANTED", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "PERMISSION DENIED", Toast.LENGTH_SHORT).show()
            }
        }

        LaunchedEffect(currentLocation) {
            coroutineScope {
                if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                    startLocationUpdate()
                } else {
                    launcherMultiplePermissions.launch(permissions)
                }
            }
        }

        LaunchedEffect(Unit) {
            fetchWeatherInformation(mainViewModel, currentLocation)
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
                when (mainViewModel.state) {
                    STATE.LOADING -> LoadingSection()
                    STATE.FAILED -> ErrorSection(mainViewModel.errorMessage)
                    else -> {
                        WeatherSection(mainViewModel.weatherResponse)
                        ForecastSection(mainViewModel.forecastResponse)
                    }
                }
            }

            FloatingActionButton(
                onClick = { fetchWeatherInformation(mainViewModel, currentLocation) },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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

@Composable
fun SearchScreen(context: Context, viewModel: MainViewModel) {
    var locationQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }

    val systemUiController = rememberSystemUiController()

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

            when (viewModel.state) {
                STATE.LOADING -> if (isLoading) LoadingSection()
                STATE.FAILED -> ErrorSection(viewModel.errorMessage)
                STATE.SUCCESS -> if (showResults) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (viewModel.weatherResponse.name?.isNotEmpty() == true) {
                            WeatherSection(viewModel.weatherResponse)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        if (viewModel.forecastResponse.list?.isNotEmpty() == true) {
                            ForecastSection(viewModel.forecastResponse)
                        }
                    }
                }
            }
        }

        LaunchedEffect(viewModel.state) {
            when (viewModel.state) {
                STATE.SUCCESS -> {
                    isLoading = false
                    showResults = true
                }
                STATE.FAILED -> {
                    isLoading = false
                    showResults = false
                    Toast.makeText(context, viewModel.errorMessage, Toast.LENGTH_SHORT).show()
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
        Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 24.dp))

        Text("Font Size", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { viewModel.decreaseFontSize() }, enabled = viewModel.fontSizeScale > 0.5f) { Text("Decrease") }
            Text(
                when {
                    viewModel.fontSizeScale <= 0.8f -> "Small"
                    viewModel.fontSizeScale <= 1.0f -> "Medium"
                    else -> "Large"
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Button(onClick = { viewModel.increaseFontSize() }, enabled = viewModel.fontSizeScale < 1.5f) { Text("Increase") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (viewModel.darkMode) "Dark Mode" else "Light Mode", style = MaterialTheme.typography.titleLarge)
            Switch(checked = viewModel.darkMode, onCheckedChange = { viewModel.toggleDarkMode() })
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