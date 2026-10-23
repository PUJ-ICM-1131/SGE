package com.sge.finca.ui.screens.routes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.sge.finca.ui.components.LocationPermissionHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteMapScreen(
    viewModel: RouteMapViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medir Distancia") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (state.origin != null) {
                        IconButton(onClick = viewModel::onClearRoute) {
                            Icon(Icons.Default.Delete, contentDescription = "Limpiar ruta")
                        }
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

                    LaunchedEffect(state.origin, state.destination) {
                        val origin = state.origin
                        val destination = state.destination
                        if (origin != null && destination != null) {
                            val bounds = LatLngBounds.builder()
                                .include(origin)
                                .include(destination)
                                .build()
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngBounds(bounds, 120),
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = true),
                            onMapClick = { latLng -> viewModel.onMapTap(latLng) },
                        ) {
                            state.origin?.let { origin ->
                                Marker(
                                    state = MarkerState(position = origin),
                                    title = "Origen",
                                    icon = BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_GREEN,
                                    ),
                                )
                            }

                            state.destination?.let { destination ->
                                Marker(
                                    state = MarkerState(position = destination),
                                    title = "Destino",
                                    icon = BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED,
                                    ),
                                )
                            }

                            val origin = state.origin
                            val destination = state.destination
                            if (origin != null && destination != null) {
                                Polyline(
                                    points = listOf(origin, destination),
                                    color = Color(0xFF1976D2),
                                    width = 8f,
                                    geodesic = true,
                                )
                            }
                        }

                        if (state.origin == null) {
                            HintOverlay(
                                text = "Toca el mapa para seleccionar el punto de origen",
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        } else if (state.destination == null) {
                            HintOverlay(
                                text = "Toca el mapa para seleccionar el destino",
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        }

                        state.distanceMeters?.let { distance ->
                            DistanceCard(
                                distanceMeters = distance,
                                origin = state.origin!!,
                                destination = state.destination!!,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun HintOverlay(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(12.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DistanceCard(
    distanceMeters: Double,
    origin: LatLng,
    destination: LatLng,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = formatDistance(distanceMeters),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CoordLabel("Origen", origin)
            CoordLabel("Destino", destination)
        }
    }
}

@Composable
private fun CoordLabel(label: String, point: LatLng) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "%.5f, %.5f".format(point.latitude, point.longitude),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.2f km".format(meters / 1000) else "%.0f m".format(meters)
