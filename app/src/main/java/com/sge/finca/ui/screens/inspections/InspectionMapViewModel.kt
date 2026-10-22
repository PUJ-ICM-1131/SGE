package com.sge.finca.ui.screens.inspections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.platform.location.LocationHelper
import com.sge.finca.platform.location.LocationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
class InspectionMapViewModel @Inject constructor(
    private val locationHelper: LocationHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionMapUiState())
    val uiState: StateFlow<InspectionMapUiState> = _uiState.asStateFlow()

    private var trackingJob: Job? = null
    private var timerJob: Job? = null
    private var startTimeMs = 0L
    private var accumulatedSeconds = 0L

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

    fun onStartTracking() {
        if (_uiState.value.isTracking) return
        _uiState.update {
            it.copy(
                isTracking = true,
                isPaused = false,
                elapsedSeconds = 0,
                distanceMeters = 0.0,
                pointCount = 0,
                routePoints = emptyList(),
            )
        }
        accumulatedSeconds = 0L
        startTimeMs = System.currentTimeMillis()
        startTimer()
        startLocationCollection()
    }

    fun onPauseTracking() {
        accumulatedSeconds = _uiState.value.elapsedSeconds
        trackingJob?.cancel()
        timerJob?.cancel()
        _uiState.update { it.copy(isPaused = true) }
    }

    fun onResumeTracking() {
        startTimeMs = System.currentTimeMillis()
        _uiState.update { it.copy(isPaused = false) }
        startTimer()
        startLocationCollection()
    }

    fun onStopTracking() {
        trackingJob?.cancel()
        timerJob?.cancel()
        _uiState.update { it.copy(isTracking = false, isPaused = false) }
        // TODO: persist route to repository
    }

    private fun startLocationCollection() {
        trackingJob = viewModelScope.launch {
            locationHelper.trackLocation().collect { loc ->
                addRoutePoint(loc)
            }
        }
    }

    private fun addRoutePoint(loc: LocationState) {
        _uiState.update { current ->
            val points = current.routePoints
            val distance = if (points.isNotEmpty()) {
                val last = points.last()
                current.distanceMeters + haversine(
                    last.latitude, last.longitude,
                    loc.latitude, loc.longitude,
                )
            } else {
                current.distanceMeters
            }
            current.copy(
                routePoints = points + loc,
                pointCount = points.size + 1,
                distanceMeters = distance,
                userLatitude = loc.latitude,
                userLongitude = loc.longitude,
                hasUserLocation = true,
            )
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val elapsed = accumulatedSeconds +
                    (System.currentTimeMillis() - startTimeMs) / 1_000
                _uiState.update { it.copy(elapsedSeconds = elapsed) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
        timerJob?.cancel()
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
