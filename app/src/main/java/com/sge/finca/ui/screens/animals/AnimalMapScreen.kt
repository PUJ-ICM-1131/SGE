package com.sge.finca.ui.screens.animals

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.sge.finca.R
import com.sge.finca.domain.model.TrackedAnimal
import com.sge.finca.ui.components.AnimalAvatar
import com.sge.finca.ui.components.LocationPermissionHandler
import com.sge.finca.ui.components.MapboxMapView
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalMapScreen(
    viewModel: AnimalMapViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToList: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasMapboxToken = stringResource(R.string.mapbox_access_token)
        .trim()
        .startsWith("pk.")
    var useMapbox by rememberSaveable { mutableStateOf(false) }
    var mapboxFailed by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mapboxFailed) {
        if (mapboxFailed) {
            snackbarHostState.showSnackbar("Mapbox no pudo cargar. Se muestra Google Maps.")
        }
    }

    // Avisos del botón "centrar en mi ubicación" (#59): snackbar, nunca un diálogo.
    val context = LocalContext.current
    LaunchedEffect(state.gpsMessage) {
        val message = state.gpsMessage ?: return@LaunchedEffect
        try {
            when (message) {
                GpsMessage.LOCATION_OFF -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "No hay señal GPS: activa la ubicación del celular para centrar el mapa.",
                        actionLabel = "Activar",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                }
                GpsMessage.NO_SIGNAL -> snackbarHostState.showSnackbar(
                    "Aún no hay señal GPS. Sal a cielo abierto e intenta de nuevo.",
                )
            }
        } finally {
            // Después de mostrarlo: limpiarlo antes reiniciaría este efecto y cortaría el aviso.
            viewModel.onGpsMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mapa de Ganado") },
                actions = {
                    IconButton(onClick = onNavigateToList) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lista de animales")
                    }
                    TextButton(
                        onClick = { useMapbox = !useMapbox },
                        enabled = hasMapboxToken && !mapboxFailed,
                    ) {
                        Text(if (useMapbox) "Google" else "Mapbox")
                    }
                },
            )
        },
        // Abajo a la izquierda (#60): a la derecha están los controles de zoom de Google Maps.
        floatingActionButtonPosition = FabPosition.Start,
        floatingActionButton = {
            // Con un animal seleccionado, su tarjeta ocupa el borde inferior: se cierra la
            // tarjeta para volver a ver el botón.
            if (state.selectedAnimal == null) {
                FloatingActionButton(onClick = onNavigateToRegister) {
                    Icon(Icons.Default.Add, contentDescription = "Registrar animal")
                }
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

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (useMapbox && hasMapboxToken && !mapboxFailed) {
                            MapboxMapView(
                                hasUserLocation = state.hasUserLocation,
                                userLatitude = state.userLatitude,
                                userLongitude = state.userLongitude,
                                animals = state.animals,
                                onAnimalSelected = viewModel::onAnimalSelected,
                                onMapLoadError = {
                                    mapboxFailed = true
                                    useMapbox = false
                                },
                                centerRequest = state.centerRequest,
                                ornamentPadding = PaddingValues(start = FabClearance),
                            )
                        } else {
                            val cameraPositionState = rememberCameraPositionState()

                            LaunchedEffect(state.hasUserLocation, state.centerRequest) {
                                if (state.hasUserLocation) {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(state.userLatitude, state.userLongitude),
                                            18f, // zoom inicial: más alto = más cerca (15 calle, 18 cuadra, 20 edificio)
                                        ),
                                    )
                                }
                            }

                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                properties = MapProperties(isMyLocationEnabled = true),
                                // El botón propio (#59) reemplaza al de Google, que no avisa si el
                                // GPS está apagado.
                                uiSettings = MapUiSettings(myLocationButtonEnabled = false),
                                // Corre el logo de Google a la derecha del botón de registrar (#60);
                                // los términos de Google Maps exigen que se vea.
                                contentPadding = PaddingValues(start = FabClearance),
                                onMapClick = { viewModel.onSelectionCleared() },
                            ) {
                                val context = LocalContext.current
                                val density = LocalDensity.current
                                val spritesByVariant = remember {
                                    val widthPx = with(density) { 48.dp.roundToPx() }
                                    CowVariant.entries.associateWith { CowSprites.load(context, widthPx, it) }
                                }
                                state.animals.forEach { tracked ->
                                    key(tracked.animal.id) {
                                        AnimalMarker(
                                            tracked = tracked,
                                            sprites = spritesByVariant.getValue(CowVariant.fromId(tracked.animal.variant)),
                                            onClick = { viewModel.onAnimalSelected(tracked.animal.id) },
                                        )
                                    }
                                }
                            }
                        }

                        CenterOnMeButton(
                            isLocating = state.isLocating,
                            onClick = viewModel::onCenterOnMeClicked,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                        )
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

/** Centra el mapa en el usuario (#59). Mientras busca la posición muestra un indicador. */
@Composable
private fun CenterOnMeButton(
    isLocating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmallFloatingActionButton(onClick = onClick, modifier = modifier) {
        if (isLocating) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.MyLocation, contentDescription = "Centrar en mi ubicación")
        }
    }
}

/** Ancho que ocupa el botón de registrar abajo a la izquierda: 16 de margen + 56 + 16. */
private val FabClearance = 88.dp
