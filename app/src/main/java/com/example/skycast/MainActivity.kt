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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import coil.request.Disposable
import com.example.skycast.constant.Const.Companion.permissions
import com.example.skycast.model.MyLatLng
import com.example.skycast.ui.theme.SkyCastTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.coroutineContext


class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var locationCallback: LocationCallback
    private var locationRequired: Boolean = false

    override fun onResume() {
        super.onResume()
        if (locationRequired) startLocationUpdate();
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

        setContent {

            // this will save our current location
            var currentLocation by remember {
                mutableStateOf(MyLatLng(0.0,0.0))
            }

            //Implement location callback
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

            SkyCastTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationScreen(this@MainActivity, currentLocation)
                }
            }
        }
    }

    @Composable
    private fun LocationScreen(context: Context, currentLocation: MyLatLng) {
        // request run time permission
        val launcherMultiplePermissions = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionMap->
            val areGranted = permissionMap.values.reduce{
                accepted, next -> accepted && next
            }
            if (areGranted){
                locationRequired = true;
                startLocationUpdate();
                Toast.makeText(context, "PERMISSION GRANTED", Toast.LENGTH_SHORT).show()
            }
            else{
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
                if(permissions.all{
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }){
                    // if permission accepted
                    startLocationUpdate()
                }
                else {
                    launcherMultiplePermissions.launch(permissions)
                }
            }
        })

        Text(text = "${currentLocation.lat}/${currentLocation.lng}")
    }

    private fun initLocationClient() {
        fusedLocationProviderClient = LocationServices
            .getFusedLocationProviderClient(this)
    }
}


