package com.sge.finca.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.MapLoadingErrorType
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.MapStyle
import com.sge.finca.domain.model.TrackedAnimal
import com.sge.finca.ui.screens.animals.CowVariant

@Composable
fun MapboxMapView(
    hasUserLocation: Boolean,
    userLatitude: Double,
    userLongitude: Double,
    animals: List<TrackedAnimal>,
    onAnimalSelected: (String) -> Unit,
    onMapLoadError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnMapLoadError = rememberUpdatedState(onMapLoadError)
    // Primer toque: origen. Segundo toque: destino. El tercero inicia otra selección.
    var selectedPoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var satelliteStyle by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val widthPx = with(LocalDensity.current) { 48.dp.roundToPx() }
    val cowIcons = remember(context, widthPx) {
        CowVariant.entries.associateWith { variant ->
            val sheet = BitmapFactory.decodeResource(context.resources, variant.sheetRes)
            // La hoja tiene 4 columnas y 2 filas. Primer cuadro de la fila de reposo.
            val cell = Bitmap.createBitmap(sheet, 0, sheet.height / 2, sheet.width / 4, sheet.height / 2)
            val scaled = Bitmap.createScaledBitmap(
                cell,
                widthPx,
                widthPx * cell.height / cell.width,
                true,
            )
            scaled.asImageBitmap()
        }
    }
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

    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState,
            style = {
                MapStyle(style = if (satelliteStyle) Style.STANDARD_SATELLITE else Style.OUTDOORS)
            },
            onMapClickListener = { point ->
                selectedPoints = if (selectedPoints.size == 2) listOf(point) else selectedPoints + point
                false
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
            selectedPoints.forEachIndexed { index, point ->
                key("route_point_$index") {
                    CircleAnnotation(point = point) {
                        circleColor = if (index == 0) Color(0xFF1565C0) else Color(0xFFD32F2F)
                        circleRadius = 8.0
                        circleStrokeColor = Color.White
                        circleStrokeWidth = 2.0
                    }
                }
            }
            if (selectedPoints.size == 2) {
                PolylineAnnotation(points = selectedPoints) {
                    lineColor = Color(0xFF1565C0)
                    lineWidth = 5.0
                }
            }
            animals.forEach { tracked ->
                key(tracked.animal.id) {
                    val variant = CowVariant.fromId(tracked.animal.variant)
                    val cowIcon = rememberIconImage(
                        key = "cow_${tracked.animal.id}_${variant.id}",
                        painter = BitmapPainter(cowIcons.getValue(variant)),
                    )
                    PointAnnotation(point = Point.fromLngLat(tracked.longitude, tracked.latitude)) {
                        iconImage = cowIcon
                        interactionsState.onClicked {
                            onAnimalSelected(tracked.animal.id)
                            true
                        }
                    }
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
