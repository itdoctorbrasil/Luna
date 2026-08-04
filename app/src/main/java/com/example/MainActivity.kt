package com.example

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.Coil
import coil.ImageLoader
import com.example.data.NetworkClientHelper
import com.example.ui.LunaLauncherScreen
import com.example.ui.LunaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LunaViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Trigger weather update after permissions decision
        viewModel.systemMonitor.fetchRealWeather()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge()

        try {
            val imageLoader = ImageLoader.Builder(this)
                .okHttpClient(NetworkClientHelper.okHttpClient)
                .crossfade(true)
                .build()
            Coil.setImageLoader(imageLoader)
        } catch (_: Exception) {}

        checkAndRequestPermissions()

        setContent {
            val darkColorScheme = darkColorScheme(
                primary = Color(0xFF00E5FF),
                background = Color(0xFF0F172A),
                surface = Color(0xFF1E293B)
            )

            MaterialTheme(colorScheme = darkColorScheme) {
                // BackHandler prevents the back button from closing/ending the TV launcher
                BackHandler(enabled = true) {
                    viewModel.onBackPressed()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    LunaLauncherScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val recordAudioGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val permissionsToRequest = mutableListOf<String>()
        if (!fineLocationGranted) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (!recordAudioGranted) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onBackPressed() {
        if (!viewModel.onBackPressed()) {
            // Do not call super.onBackPressed() to keep launcher running continuously
        }
    }
}
