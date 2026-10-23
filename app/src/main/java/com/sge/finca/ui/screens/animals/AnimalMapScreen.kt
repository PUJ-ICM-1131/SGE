package com.sge.finca.ui.screens.animals

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import com.sge.finca.R
import com.sge.finca.ui.components.LocationPermissionHandler
import com.sge.finca.ui.components.MapboxMapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalMapScreen(
    viewModel: AnimalMapViewModel,
    onNavigateToRegister: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasMapboxToken = stringResource(R.string.mapbox_access_token)
        .trim()
        .startsWith("pk.")

    var useMapbox by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa de Ganado") },
                actions = {
                    TextButton(
                        onClick = { useMapbox = !useMapbox },
                        enabled = hasMapboxToken,
                    ) {
                        Text(if (useMapbox) "Google" else "Mapbox")
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
                    LaunchedEffect(Unit) {
                        viewModel.onPermissionGranted()
                    }

                    if (useMapbox && hasMapboxToken) {
                        MapboxMapView(
                            hasUserLocation = state.hasUserLocation,
                            userLatitude = state.userLatitude,
                            userLongitude = state.userLongitude,
                        )
                    } else {
                        val cameraPositionState = rememberCameraPositionState()

                        LaunchedEffect(
                            state.hasUserLocation,
                            state.userLatitude,
                            state.userLongitude,
                        ) {
                            if (state.hasUserLocation) {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(
                                            state.userLatitude,
                                            state.userLongitude,
                                        ),
                                        16f,
                                    ),
                                )
                            }
                        }

                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(
                                isMyLocationEnabled = true,
                            ),
                        )
                    }
                },
            )
        }
    }
}