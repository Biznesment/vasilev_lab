package com.vasilev.voicedb

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vasilev.voicedb.ui.VoiceMeterScreen
import com.vasilev.voicedb.ui.VoiceMeterViewModel
import com.vasilev.voicedb.ui.theme.VoiceDecibelMeterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            VoiceDecibelMeterTheme {
                val meterViewModel: VoiceMeterViewModel = viewModel()
                val uiState by meterViewModel.uiState.collectAsStateWithLifecycle()
                val lifecycleOwner = LocalLifecycleOwner.current

                var askedPermissionOnce by rememberSaveable { mutableStateOf(false) }
                var showOpenSettings by rememberSaveable { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    askedPermissionOnce = true
                    showOpenSettings = !granted && !ActivityCompat.shouldShowRequestPermissionRationale(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    )
                    meterViewModel.onPermissionChanged(granted)
                }

                // Request microphone permission automatically on first launch so the graph starts right away.
                LaunchedEffect(Unit) {
                    val hasPermission = hasMicPermission()
                    meterViewModel.onPermissionChanged(hasPermission)
                    if (!hasPermission && !askedPermissionOnce) {
                        askedPermissionOnce = true
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> {
                                val hasPermission = hasMicPermission()
                                if (hasPermission) {
                                    showOpenSettings = false
                                }
                                meterViewModel.onForeground(hasPermission)
                            }
                            Lifecycle.Event.ON_STOP -> {
                                meterViewModel.onBackground()
                            }
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                VoiceMeterScreen(
                    uiState = uiState,
                    showOpenSettingsAction = showOpenSettings,
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null)
                        )
                        startActivity(intent)
                    },
                    onToggleListening = meterViewModel::toggleListening,
                    onResetStats = meterViewModel::resetStats,
                    onCalibrationChange = meterViewModel::setCalibrationOffset
                )
            }
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
}
