package com.sge.finca.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle

@Composable
fun MapboxMapView(
    hasUserLocation: Boolean,
    userLatitude: Double,
    userLongitude: Double,
    modifier: Modifier = Modifier,
) {
    val viewportState = rememberMapViewportState {
        setCameraOptions {
            // Vista inicial mientras se obtiene la ubicación del usuario.
            center(Point.fromLngLat(-74.1, 4.7))
            zoom(5.0)
        }
    }

    LaunchedEffect(hasUserLocation, userLatitude, userLongitude) {
        if (hasUserLocation) {
            viewportState.easeTo(
                cameraOptions {
                    // Mapbox recibe primero longitud y después latitud.
                    center(Point.fromLngLat(userLongitude, userLatitude))
                    zoom(16.0)
                }
            )
        }
    }

    MapboxMap(
        modifier = modifier.fillMaxSize(),
        mapViewportState = viewportState,
        style = { MapStyle(style = Style.OUTDOORS) },
    )
}