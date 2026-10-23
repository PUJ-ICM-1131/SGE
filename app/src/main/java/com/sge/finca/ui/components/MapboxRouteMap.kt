package com.sge.finca.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.MapLoadingErrorType
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.sge.finca.domain.model.RoutePoint

@Composable
fun MapboxRouteMap(
    routePoints: List<RoutePoint>,
    onMapLoadError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnMapLoadError = rememberUpdatedState(onMapLoadError)
    var satelliteStyle by rememberSaveable { mutableStateOf(false) }
    val points = remember(routePoints) {
        routePoints.sortedBy { it.order }.map { Point.fromLngLat(it.longitude, it.latitude) }
    }
    val center = remember(points) {
        if (points.isEmpty()) Point.fromLngLat(-74.1, 4.7)
        else Point.fromLngLat(
            points.map { it.longitude() }.average(),
            points.map { it.latitude() }.average(),
        )
    }
    val viewportState = rememberMapViewportState {
        setCameraOptions {
            center(center)
            zoom(if (points.isEmpty()) 5.0 else 15.0)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState,
            style = {
                MapStyle(style = if (satelliteStyle) Style.STANDARD_SATELLITE else Style.OUTDOORS)
            },
        ) {
            DisposableMapEffect(Unit) { mapView ->
                val subscription = mapView.mapboxMap.subscribeMapLoadingError { error ->
                    if (error.type == MapLoadingErrorType.STYLE ||
                        error.message.contains("401") || error.message.contains("403")
                    ) {
                        currentOnMapLoadError.value()
                    }
                }
                onDispose { subscription.cancel() }
            }
            if (points.size >= 2) {
                PolylineAnnotation(points = points) {
                    lineColor = Color(0xFF2E7D32)
                    lineWidth = 5.0
                }
            }
        }
        FilledTonalButton(
            onClick = { satelliteStyle = !satelliteStyle },
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        ) {
            Text(if (satelliteStyle) "Mapa rural" else "Satélite")
        }
    }
}
