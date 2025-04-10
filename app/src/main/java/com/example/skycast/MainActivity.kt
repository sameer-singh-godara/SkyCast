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
import androidx.compose.ui.graphics.Color
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
        locationCallback?.let {
            fusedLocationProviderClient?.removeLocationUpdates(it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdate() {
        locationCallback?.let {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 100
            )
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(100)
                .build()
            fusedLocationProviderClient?.requestLocationUpdates(
                locationRequest,
                it,
                Looper.getMainLooper()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initLocationClient()
        initViewModel()

        setContent {
            val viewModel: MainViewModel = viewModel()

            // Provide fontScale via CompositionLocal and trigger recomposition
            CompositionLocalProvider(LocalFontScale provides viewModel.fontSizeScale) {
                // Debug: Show toast when fontSizeScale changes
                LaunchedEffect(viewModel.fontSizeScale) {
                    Toast.makeText(this@MainActivity, "Font Scale: ${viewModel.fontSizeScale}", Toast.LENGTH_SHORT).show()
                }

                // Force recomposition when darkMode or fontSizeScale changes
                LaunchedEffect(viewModel.darkMode, viewModel.fontSizeScale) {
                    // This empty block ensures recomposition
                }

                SkyCastTheme(darkTheme = viewModel.darkMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
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

        // Implement location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                for (location in p0.locations) {
                    currentLocation = MyLatLng(
                        location.latitude,
                        location.longitude
                    )
                }
            }
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search") },
                        selected = currentRoute == Screen.Search.route,
                        onClick = {
                            navController.navigate(Screen.Search.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    LocationScreen(context, currentLocation, viewModel)
                }
                composable(Screen.Search.route) {
                    SearchScreen(context, viewModel)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(viewModel)
                }
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
        mainViewModel = ViewModelProvider(this@MainActivity)[MainViewModel::class.java]
    }

    @Composable
    private fun LocationScreen(
        context: Context,
        currentLocation: MyLatLng,
        viewModel: MainViewModel
    ) {
        // request run time permission
        val launcherMultiplePermissions = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionMap ->
            val areGranted = permissionMap.values.reduce { accepted, next ->
                accepted && next
            }
            if (areGranted) {
                locationRequired = true
                startLocationUpdate()
                Toast.makeText(context, "PERMISSION GRANTED", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "PERMISSION DENIED", Toast.LENGTH_SHORT).show()
            }
        }

        val systemUiController = rememberSystemUiController()

        DisposableEffect(key1 = true, effect = {
            systemUiController.isSystemBarsVisible = false // hide the status bar
            onDispose {
                systemUiController.isSystemBarsVisible = true // show the status bar
            }
        })

        LaunchedEffect(key1 = currentLocation, block = {
            coroutineScope {
                if (permissions.all {
                        ContextCompat.checkSelfPermission(
                            context,
                            it
                        ) == PackageManager.PERMISSION_GRANTED
                    }) {
                    // if permission accepted
                    startLocationUpdate()
                } else {
                    launcherMultiplePermissions.launch(permissions)
                }
            }
        })

        LaunchedEffect(key1 = true, block = {
            fetchWeatherInformation(mainViewModel, currentLocation)
        })

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            val marginTop = screenHeight * 0.1f // I want margin top by 20% height
            val marginTopPx = with(LocalDensity.current) { marginTop.toPx() }

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        // define the layout for the child
                        layout(
                            placeable.width,
                            placeable.height + marginTopPx.toInt()
                        ) {
                            placeable.placeRelative(0, marginTopPx.toInt())
                        }
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (mainViewModel.state) {
                    STATE.LOADING -> {
                        LoadingSection()
                    }
                    STATE.FAILED -> {
                        ErrorSection(mainViewModel.errorMessage)
                    }
                    else -> {
                        WeatherSection(mainViewModel.weatherResponse)
                        ForecastSection(mainViewModel.forecastResponse)
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    // fetch API when location change
                    fetchWeatherInformation(mainViewModel, currentLocation)
                },
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
        fusedLocationProviderClient = LocationServices
            .getFusedLocationProviderClient(this)
    }
}

@Composable
fun SearchScreen(context: Context, viewModel: MainViewModel) {
    var locationQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val marginTop = screenHeight * 0.1f
        val marginTopPx = with(LocalDensity.current) { marginTop.toPx() }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(
                        placeable.width,
                        placeable.height + marginTopPx.toInt()
                    ) {
                        placeable.placeRelative(0, marginTopPx.toInt())
                    }
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = locationQuery,
                onValueChange = { locationQuery = it },
                label = {
                    Text(
                        "Enter city name (e.g., London)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
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
                    .padding(horizontal = 16.dp),
                enabled = !isLoading && locationQuery.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Search")
                }
            }

            when (viewModel.state) {
                STATE.LOADING -> {
                    if (isLoading) {
                        LoadingSection()
                    }
                }
                STATE.FAILED -> {
                    ErrorSection(viewModel.errorMessage)
                }
                STATE.SUCCESS -> {
                    // Only show results if we've performed a search
                    if (showResults) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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
        }

        // Observe viewModel state changes
        LaunchedEffect(viewModel.state) {
            if (viewModel.state == STATE.SUCCESS) {
                isLoading = false
                showResults = true
            } else if (viewModel.state == STATE.FAILED) {
                isLoading = false
                showResults = false
                Toast.makeText(context, viewModel.errorMessage, Toast.LENGTH_SHORT).show()
            }
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

@Composable
fun WeatherInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Font Size Settings
        Text("Font Size", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.decreaseFontSize() },
                enabled = viewModel.fontSizeScale > 0.8f
            ) {
                Text("Decrease")
            }

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
                enabled = viewModel.fontSizeScale < 1.2f
            ) {
                Text("Increase")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Theme Mode Setting
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