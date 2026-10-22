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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import com.sge.finca.ui.components.LocationPermissionHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalMapScreen(
    viewModel: AnimalMapViewModel,
    onNavigateToRegister: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mapa de Ganado") })
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
                    )
                },
            )
        }
    }
}
