package com.sge.finca.ui.screens.inspections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.sge.finca.R
import com.sge.finca.domain.model.InspectionRoute
import com.sge.finca.ui.components.MapboxRouteMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionDetailScreen(
    viewModel: InspectionDetailViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasMapboxToken = stringResource(R.string.mapbox_access_token).trim().startsWith("pk.")
    var useMapbox by rememberSaveable { mutableStateOf(false) }
    var mapboxFailed by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mapboxFailed) {
        if (mapboxFailed) {
            snackbarHostState.showSnackbar("Mapbox no pudo cargar. Se muestra Google Maps.")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Inspección") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { useMapbox = !useMapbox },
                        enabled = hasMapboxToken && !mapboxFailed,
                    ) {
                        Text(if (useMapbox) "Google" else "Mapbox")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                val points = state.points.map { LatLng(it.latitude, it.longitude) }
                val cameraPositionState = rememberCameraPositionState {
                    if (points.isNotEmpty()) {
                        val bounds = LatLngBounds.builder().apply {
                            points.forEach { include(it) }
                        }.build()
                        position = CameraPosition.fromLatLngZoom(bounds.center, 15f)
                    }
                }

                val mapModifier = Modifier.fillMaxWidth().weight(1f)
                if (useMapbox && hasMapboxToken && !mapboxFailed) {
                    MapboxRouteMap(
                        routePoints = state.points,
                        onMapLoadError = {
                            mapboxFailed = true
                            useMapbox = false
                        },
                        modifier = mapModifier,
                    )
                } else {
                    GoogleMap(
                        modifier = mapModifier,
                        cameraPositionState = cameraPositionState,
                    ) {
                        if (points.size >= 2) {
                            Polyline(
                                points = points,
                                color = Color(0xFF2E7D32),
                                width = 8f,
                            )
                        }
                    }
                }

                state.route?.let { route ->
                    RouteStatsBar(route)
                }
            }
        }
    }
}

@Composable
private fun RouteStatsBar(route: InspectionRoute) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatColumn("Fecha", formatDate(route.startTime))
        StatColumn("Distancia", formatDistance(route.totalDistanceMeters))
        StatColumn("Duración", formatDuration(route))
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale("es", "CO")).format(Date(timestamp))

private fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000) else "%.0f m".format(meters)

private fun formatDuration(route: InspectionRoute): String {
    val endTime = route.endTime ?: return "--"
    val seconds = (endTime - route.startTime) / 1000
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
