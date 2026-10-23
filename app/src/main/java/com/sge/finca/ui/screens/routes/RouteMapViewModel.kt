package com.sge.finca.ui.screens.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sge.finca.platform.location.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@HiltViewModel
class RouteMapViewModel @Inject constructor(
    private val locationHelper: LocationHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteMapUiState())
    val uiState: StateFlow<RouteMapUiState> = _uiState.asStateFlow()

    fun onPermissionGranted() {
        viewModelScope.launch {
            val loc = locationHelper.requestCurrentLocation()
            if (loc != null) {
                _uiState.update {
                    it.copy(
                        userLatitude = loc.latitude,
                        userLongitude = loc.longitude,
                        hasUserLocation = true,
                    )
                }
            }
        }
    }

    fun onMapTap(point: LatLng) {
        val current = _uiState.value
        when {
            current.origin == null -> {
                _uiState.update { it.copy(origin = point, destination = null, distanceMeters = null) }
            }
            current.destination == null -> {
                val distance = haversine(
                    current.origin.latitude, current.origin.longitude,
                    point.latitude, point.longitude,
                )
                _uiState.update { it.copy(destination = point, distanceMeters = distance) }
            }
            else -> {
                _uiState.update { it.copy(origin = point, destination = null, distanceMeters = null) }
            }
        }
    }

    fun onClearRoute() {
        _uiState.update { it.copy(origin = null, destination = null, distanceMeters = null) }
    }

    companion object {
        private const val EARTH_RADIUS_M = 6_371_000.0

        fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
            return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
