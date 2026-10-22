package com.sge.finca.ui.screens.animals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.sge.finca.domain.model.TrackedAnimal
import com.sge.finca.ui.components.AnimalAvatar
import com.sge.finca.ui.components.LocationPermissionHandler
import kotlinx.coroutines.delay

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
                        val context = LocalContext.current
                        val density = LocalDensity.current
                        val sprites = remember {
                            CowSprites.load(context, with(density) { 88.dp.roundToPx() })
                        }
                        state.animals.forEach { tracked ->
                            key(tracked.animal.id) {
                                AnimalMarker(
                                    tracked = tracked,
                                    sprites = sprites,
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
 * Marcador de vaca animado con sprites:
 *  - Desliza suavemente hacia cada nueva posición (interpolación lineal).
 *  - Mira hacia donde camina (este/oeste).
 *  - Cambia de pose según la velocidad: quieta, caminando o corriendo, ciclando fotogramas.
 * La posición y la velocidad salen de TrackedAnimal (registro o ubicación en vivo).
 */
@Composable
@GoogleMapComposable
private fun AnimalMarker(
    tracked: TrackedAnimal,
    sprites: CowSprites,
    onClick: () -> Unit,
) {
    val target = LatLng(tracked.latitude, tracked.longitude)
    val markerState = rememberMarkerState(key = tracked.animal.id, position = target)

    val progress = remember { Animatable(1f) }
    var facingEast by remember { mutableStateOf(true) }
    var frame by remember { mutableIntStateOf(0) }

    // 1) Deslizamiento suave desde donde está dibujada ahora hasta la nueva posición
    LaunchedEffect(target) {
        val start = markerState.position
        if (start == target) return@LaunchedEffect
        if (target.longitude != start.longitude) facingEast = target.longitude > start.longitude
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = SLIDE_MILLIS, easing = LinearEasing)) {
            markerState.position = LatLng(
                start.latitude + (target.latitude - start.latitude) * value,
                start.longitude + (target.longitude - start.longitude) * value,
            )
        }
    }

    // 2) Pose según la velocidad
    val pose = when {
        tracked.speedMetersPerSecond >= RUN_SPEED -> CowPose.RUN
        tracked.isMoving || progress.isRunning -> CowPose.WALK
        else -> CowPose.IDLE
    }

    // 3) Ciclo de fotogramas de la pose actual
    LaunchedEffect(pose) {
        frame = 0
        if (pose.frames > 1) {
            while (true) {
                delay(pose.frameMillis)
                frame = (frame + 1) % pose.frames
            }
        }
    }

    Marker(
        state = markerState,
        icon = sprites.frame(pose, frame, facingEast),
        anchor = Offset(0.5f, 0.86f), // el punto del mapa queda en las patas (centro de la sombra)
        title = tracked.animal.name,
        onClick = {
            onClick()
            true
        },
    )
}

/** Tiempo de deslizamiento: igual al intervalo de actualización para movimiento continuo. */
private const val SLIDE_MILLIS = 2000

/** Desde esta velocidad (m/s) la vaca se dibuja corriendo. */
private const val RUN_SPEED = 2.5f

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
