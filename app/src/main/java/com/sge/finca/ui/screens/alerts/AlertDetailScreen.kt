package com.sge.finca.ui.screens.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.sge.finca.domain.model.AlertStatus
import com.sge.finca.domain.model.AnimalAlert
import com.sge.finca.ui.screens.inspections.PaddockOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailScreen(
    viewModel: AlertDetailViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Alerta del ganado") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        val alert = state.alert
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            alert == null -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Esta alerta ya no existe o no es de tu finca.", textAlign = TextAlign.Center)
            }

            else -> Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                AlertMap(state, alert, modifier = Modifier.fillMaxWidth().weight(1f))
                AlertSummary(
                    alert = alert,
                    showForward = state.showForward,
                    onForward = viewModel::onForward,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/** La vaca (marcador rojo) y los potreros de la finca, encuadrados al cargar el mapa. */
@Composable
private fun AlertMap(state: AlertDetailUiState, alert: AnimalAlert, modifier: Modifier) {
    val cow = LatLng(alert.position.latitude, alert.position.longitude)
    val cameraPositionState = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(mapLoaded, alert.id, state.paddocks.size) {
        if (!mapLoaded) return@LaunchedEffect
        val paddockPoints = state.paddocks.flatMap { it.points }
        val update = if (paddockPoints.isEmpty()) {
            CameraUpdateFactory.newLatLngZoom(cow, 17f)
        } else {
            val bounds = LatLngBounds.builder().include(cow).apply {
                paddockPoints.forEach { include(LatLng(it.latitude, it.longitude)) }
            }.build()
            CameraUpdateFactory.newLatLngBounds(bounds, 120)
        }
        cameraPositionState.animate(update)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapLoaded = { mapLoaded = true },
    ) {
        state.paddocks.forEach { paddock -> PaddockOverlay(paddock, onClick = {}) }
        // Dentro del contenido del mapa: BitmapDescriptorFactory exige Google Maps inicializado.
        Marker(
            state = rememberMarkerState(key = alert.id, position = cow),
            title = alert.animalName,
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
        )
    }
}

@Composable
private fun AlertSummary(
    alert: AnimalAlert,
    showForward: Boolean,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val resolved = alert.status == AlertStatus.RESOLVED
        Text(
            text = if (resolved) "${alert.animalName} volvió a un potrero" else "${alert.animalName} salió de los potreros",
            style = MaterialTheme.typography.titleLarge,
            color = if (resolved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Text("Detectada: ${formatTime(alert.createdAt)}", style = MaterialTheme.typography.bodyMedium)
        alert.resolvedAt?.let {
            Text("Volvió: ${formatTime(it)}", style = MaterialTheme.typography.bodyMedium)
        }
        alert.forwardedAt?.let {
            Text(
                "Reenviada a los trabajadores: ${formatTime(it)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showForward) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onForward, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Campaign, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Reenviar a trabajadores")
            }
        }
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CO")).format(Date(timestamp))
