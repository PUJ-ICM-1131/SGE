package com.sge.finca.ui.screens.inspections

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.sge.finca.ui.components.LocationPermissionHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionMapScreen(
    viewModel: InspectionMapViewModel,
    onNavigateToHistory: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.gpsLostEvent.collect {
            snackbarHostState.showSnackbar("Señal GPS perdida. Verifica tu ubicación.")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Inspección de Campo") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "Historial")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LocationPermissionHandler(
                onPermissionGranted = {
                    LaunchedEffect(Unit) { viewModel.onPermissionGranted() }

                    val cameraPositionState = rememberCameraPositionState()

                    LaunchedEffect(state.hasUserLocation) {
                        if (state.hasUserLocation) {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(state.userLatitude, state.userLongitude),
                                    17f,
                                ),
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = true),
                        ) {
                            if (state.routePoints.size >= 2) {
                                Polyline(
                                    points = state.routePoints.map {
                                        LatLng(it.latitude, it.longitude)
                                    },
                                    color = Color(0xFF2E7D32),
                                    width = 8f,
                                )
                            }
                        }

                        if (state.isTracking) {
                            TrackingStatsOverlay(
                                elapsedSeconds = state.elapsedSeconds,
                                distanceMeters = state.distanceMeters,
                                hasGpsSignal = state.hasGpsSignal,
                                isPaused = state.isPaused,
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        }

                        TrackingControls(
                            isTracking = state.isTracking,
                            isPaused = state.isPaused,
                            onStart = viewModel::onStartTracking,
                            onPause = viewModel::onPauseTracking,
                            onResume = viewModel::onResumeTracking,
                            onStop = viewModel::onStopTracking,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun TrackingStatsOverlay(
    elapsedSeconds: Long,
    distanceMeters: Double,
    hasGpsSignal: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(12.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isPaused) {
            Text(
                text = "PAUSADO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatItem("Tiempo", formatDuration(elapsedSeconds))
            StatItem("Distancia", formatDistance(distanceMeters))
            GpsIndicator(hasSignal = hasGpsSignal)
        }
    }
}

@Composable
private fun GpsIndicator(hasSignal: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (hasSignal) Color(0xFF4CAF50) else Color(0xFFF44336),
                    shape = CircleShape,
                ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "GPS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrackingControls(
    isTracking: Boolean,
    isPaused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!isTracking) {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text("Iniciar Inspección")
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                if (isPaused) {
                    Button(onClick = onResume, modifier = Modifier.weight(1f)) {
                        Text("Reanudar")
                    }
                } else {
                    OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) {
                        Text("Pausar")
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Finalizar")
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000)
    else "%.0f m".format(meters)
