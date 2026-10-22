package com.sge.finca.ui.screens.animals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.sge.finca.domain.model.TrackedAnimal
import com.sge.finca.ui.components.AnimalAvatar
import com.sge.finca.ui.components.LocationPermissionHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalMapScreen(
    viewModel: AnimalMapViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToList: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa de Ganado") },
                actions = {
                    IconButton(onClick = onNavigateToList) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lista de animales")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToRegister) {
                Icon(Icons.Default.Add, contentDescription = "Registrar animal")
            }
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
                                    16f,
                                ),
                            )
                        }
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = true),
                        onMapClick = { viewModel.onSelectionCleared() },
                    ) {
                        state.animals.forEach { tracked ->
                            key(tracked.animal.id) {
                                AnimalMarker(
                                    tracked = tracked,
                                    onClick = { viewModel.onAnimalSelected(tracked.animal.id) },
                                )
                            }
                        }
                    }
                },
            )

            state.selectedAnimal?.let { tracked ->
                AnimalInfoCard(
                    tracked = tracked,
                    onClose = viewModel::onSelectionCleared,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
    }
}

/**
 * Marcador de vaca. La posición sale de TrackedAnimal (registro o ubicación en vivo),
 * así que cuando llegue la API el marcador se mueve solo y cambia a "corriendo".
 */
@Composable
@GoogleMapComposable
private fun AnimalMarker(
    tracked: TrackedAnimal,
    onClick: () -> Unit,
) {
    val position = LatLng(tracked.latitude, tracked.longitude)
    val markerState = rememberMarkerState(key = tracked.animal.id, position = position)

    // Si la posición cambia (ubicación en vivo), se mueve el marcador
    LaunchedEffect(position) { markerState.position = position }

    // keys: se vuelve a dibujar el ícono cuando cambia el estado de movimiento
    MarkerComposable(
        tracked.animal.id,
        tracked.isMoving,
        state = markerState,
        title = tracked.animal.name,
        onClick = {
            onClick()
            true
        },
    ) {
        CowMarkerContent(isMoving = tracked.isMoving)
    }
}

@Composable
private fun CowMarkerContent(isMoving: Boolean) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp,
        border = BorderStroke(2.dp, if (isMoving) Color(0xFFFF8F00) else Color(0xFF2E7D32)),
    ) {
        // Quieta: 🐄 — En movimiento: 🐄💨 (corriendo)
        Text(
            text = if (isMoving) "🐄💨" else "🐄",
            fontSize = 26.sp,
            modifier = Modifier.padding(6.dp),
        )
    }
}

@Composable
private fun AnimalInfoCard(
    tracked: TrackedAnimal,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animal = tracked.animal
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimalAvatar(
                photoUri = animal.photoUri,
                size = 72.dp,
                contentDescription = "Foto de ${animal.name}",
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = animal.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (tracked.isMoving) "En movimiento" else "En reposo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Lat: %.6f".format(tracked.latitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Lng: %.6f".format(tracked.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (animal.notes.isNotBlank()) {
                    Text(
                        text = animal.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
        }
    }
}
