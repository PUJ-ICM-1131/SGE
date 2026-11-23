package com.sge.finca.ui.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.sge.finca.R
import com.sge.finca.ui.components.CenterOnMeButton
import com.sge.finca.ui.components.GpsMessageEffect
import com.sge.finca.ui.components.LocationPermissionHandler
import com.sge.finca.ui.components.MapboxMapView
import com.sge.finca.ui.components.onHeightChanged
import com.sge.finca.ui.screens.animals.AnimalInfoCard
import com.sge.finca.ui.screens.animals.AnimalMapViewModel
import com.sge.finca.ui.screens.animals.AnimalMarker
import com.sge.finca.ui.screens.animals.CowVariant
import com.sge.finca.ui.screens.animals.rememberCowSprites
import com.sge.finca.ui.screens.inspections.InspectionMapViewModel
import com.sge.finca.ui.screens.inspections.PaddockDetailDialog
import com.sge.finca.ui.screens.inspections.PaddockOverlay
import com.sge.finca.ui.screens.inspections.SavePaddockDialog
import com.sge.finca.ui.screens.inspections.TrackingControls
import com.sge.finca.ui.screens.inspections.TrackingStatsOverlay

/**
 * Mapa de la finca (#72): vacas en tiempo real, potreros (#71) y el recorrido de la inspección en
 * curso en un solo mapa. Reutiliza los ViewModels de inspección y de ganado sin cambiar su lógica;
 * la ubicación y el botón de centrar salen del de inspección.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmMapScreen(
    inspectionViewModel: InspectionMapViewModel,
    animalViewModel: AnimalMapViewModel,
    onNavigateToInspectionHistory: () -> Unit,
    onNavigateToRouteMap: () -> Unit,
    onNavigateToRegisterAnimal: () -> Unit,
    onNavigateToAnimalList: () -> Unit,
) {
    val inspection by inspectionViewModel.uiState.collectAsStateWithLifecycle()
    val herd by animalViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val hasMapboxToken = stringResource(R.string.mapbox_access_token).trim().startsWith("pk.")
    var useMapbox by rememberSaveable { mutableStateOf(false) }
    var mapboxFailed by remember { mutableStateOf(false) }
    // Mapbox solo dibuja las vacas: mientras se inspecciona se usa Google para ver el recorrido.
    val showMapbox = useMapbox && hasMapboxToken && !mapboxFailed && !inspection.isTracking

    LaunchedEffect(Unit) {
        inspectionViewModel.gpsLostEvent.collect {
            snackbarHostState.showSnackbar("Señal GPS perdida. Verifica tu ubicación.")
        }
    }
    LaunchedEffect(mapboxFailed) {
        if (mapboxFailed) snackbarHostState.showSnackbar("Mapbox no pudo cargar. Se muestra Google Maps.")
    }
    GpsMessageEffect(
        message = inspection.gpsMessage,
        snackbarHostState = snackbarHostState,
        onShown = inspectionViewModel::onGpsMessageShown,
    )
    LaunchedEffect(inspection.paddockMessage) {
        inspection.paddockMessage?.let {
            snackbarHostState.showSnackbar(it)
            inspectionViewModel.onPaddockMessageShown()
        }
    }

    inspection.paddockDraft?.let { draft ->
        SavePaddockDialog(
            areaM2 = draft.areaM2,
            suggestedName = "Potrero ${inspection.paddocks.size + 1}",
            onSave = inspectionViewModel::onSavePaddock,
            onDismiss = inspectionViewModel::onDiscardPaddock,
        )
    }
    var selectedPaddockId by remember { mutableStateOf<String?>(null) }
    inspection.paddocks.firstOrNull { it.id == selectedPaddockId }?.let { paddock ->
        PaddockDetailDialog(
            paddock = paddock,
            canManage = inspection.canManagePaddocks,
            onRename = { name ->
                inspectionViewModel.onRenamePaddock(paddock.id, name)
                selectedPaddockId = null
            },
            onDelete = {
                inspectionViewModel.onDeletePaddock(paddock.id)
                selectedPaddockId = null
            },
            onDismiss = { selectedPaddockId = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa") },
                actions = {
                    IconButton(onClick = onNavigateToAnimalList) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lista de animales")
                    }
                    IconButton(onClick = onNavigateToRouteMap) {
                        Icon(Icons.Default.Straighten, contentDescription = "Medir distancia")
                    }
                    IconButton(onClick = onNavigateToInspectionHistory) {
                        Icon(Icons.Default.History, contentDescription = "Historial de inspecciones")
                    }
                    TextButton(
                        onClick = { useMapbox = !useMapbox },
                        enabled = hasMapboxToken && !mapboxFailed && !inspection.isTracking,
                    ) {
                        Text(if (showMapbox) "Google" else "Mapbox")
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
                    LaunchedEffect(Unit) { inspectionViewModel.onPermissionGranted() }

                    // Lo que flota sobre el mapa (#60): el mapa corre su zoom y sus logos.
                    val density = LocalDensity.current
                    var statsHeight by remember { mutableStateOf(0.dp) }
                    var bottomHeight by remember { mutableStateOf(0.dp) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showMapbox) {
                            MapboxMapView(
                                hasUserLocation = inspection.hasUserLocation,
                                userLatitude = inspection.userLatitude,
                                userLongitude = inspection.userLongitude,
                                animals = herd.animals,
                                onAnimalSelected = animalViewModel::onAnimalSelected,
                                onMapLoadError = {
                                    mapboxFailed = true
                                    useMapbox = false
                                },
                                centerRequest = inspection.centerRequest,
                                ornamentPadding = PaddingValues(bottom = bottomHeight),
                            )
                        } else {
                            val cameraPositionState = rememberCameraPositionState()
                            LaunchedEffect(inspection.hasUserLocation, inspection.centerRequest) {
                                if (inspection.hasUserLocation) {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(inspection.userLatitude, inspection.userLongitude),
                                            17f,
                                        ),
                                    )
                                }
                            }
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                properties = MapProperties(isMyLocationEnabled = true),
                                // El botón propio (#59) reemplaza al de Google, que no avisa si
                                // el GPS está apagado.
                                uiSettings = MapUiSettings(myLocationButtonEnabled = false),
                                contentPadding = PaddingValues(
                                    top = if (inspection.isTracking) statsHeight else 0.dp,
                                    bottom = bottomHeight,
                                ),
                                onMapClick = { animalViewModel.onSelectionCleared() },
                            ) {
                                inspection.paddocks.forEach { paddock ->
                                    PaddockOverlay(paddock, onClick = { selectedPaddockId = paddock.id })
                                }
                                if (inspection.routePoints.size >= 2) {
                                    Polyline(
                                        points = inspection.routePoints.map {
                                            LatLng(it.latitude, it.longitude)
                                        },
                                        color = Color(0xFF2E7D32),
                                        width = 8f,
                                    )
                                }
                                // Aquí y no antes del GoogleMap: BitmapDescriptorFactory exige que
                                // Google Maps ya esté inicializado, si no la app se cierra.
                                val sprites = rememberCowSprites()
                                herd.animals.forEach { tracked ->
                                    key(tracked.animal.id) {
                                        AnimalMarker(
                                            tracked = tracked,
                                            sprites = sprites.getValue(CowVariant.fromId(tracked.animal.variant)),
                                            onClick = { animalViewModel.onAnimalSelected(tracked.animal.id) },
                                        )
                                    }
                                }
                            }
                        }

                        if (inspection.isTracking) {
                            TrackingStatsOverlay(
                                elapsedSeconds = inspection.elapsedSeconds,
                                distanceMeters = inspection.distanceMeters,
                                hasGpsSignal = inspection.hasGpsSignal,
                                isPaused = inspection.isPaused,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .onHeightChanged(density) { statsHeight = it },
                            )
                        }

                        // Abajo: la tarjeta del animal seleccionado (si hay) sobre los controles.
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .onHeightChanged(density) { bottomHeight = it }
                                .padding(16.dp),
                        ) {
                            herd.selectedAnimal?.let { tracked ->
                                AnimalInfoCard(
                                    tracked = tracked,
                                    onClose = animalViewModel::onSelectionCleared,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            TrackingControls(
                                isTracking = inspection.isTracking,
                                isPaused = inspection.isPaused,
                                onStart = inspectionViewModel::onStartTracking,
                                onPause = inspectionViewModel::onPauseTracking,
                                onResume = inspectionViewModel::onResumeTracking,
                                onStop = inspectionViewModel::onStopTracking,
                                onRegisterAnimal = onNavigateToRegisterAnimal,
                            )
                        }

                        CenterOnMeButton(
                            isLocating = inspection.isLocating,
                            onClick = inspectionViewModel::onCenterOnMeClicked,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                        )

                        // Los avisos salen encima del panel inferior, nunca sobre él (#77).
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = bottomHeight),
                        )
                    }
                },
            )
        }
    }
}
