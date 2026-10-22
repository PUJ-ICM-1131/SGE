package com.sge.finca.ui.screens.inspections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.InspectionRoute
import com.sge.finca.domain.model.RoutePoint
import com.sge.finca.domain.repository.InspectionRepository
import com.sge.finca.platform.location.LocationHelper
import com.sge.finca.platform.location.LocationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@HiltViewModel
class InspectionMapViewModel @Inject constructor(
    private val locationHelper: LocationHelper,
    private val inspectionRepository: InspectionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionMapUiState())
    val uiState: StateFlow<InspectionMapUiState> = _uiState.asStateFlow()

    private val _gpsLostEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val gpsLostEvent: SharedFlow<Unit> = _gpsLostEvent.asSharedFlow()

    private var trackingJob: Job? = null
    private var timerJob: Job? = null
    private var gpsWatchdogJob: Job? = null
    private var startTimeMs = 0L
    private var accumulatedSeconds = 0L
    private var currentRouteId: String? = null
    private var pointOrder = 0

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
        pointOrder = 0
        startTimeMs = System.currentTimeMillis()

        viewModelScope.launch {
            val route = InspectionRoute(
                id = UUID.randomUUID().toString(),
                startTime = startTimeMs,
            )
            currentRouteId = inspectionRepository.createRoute(route)
        }

        startTimer()
        startLocationCollection()
        startGpsWatchdog()
    }

    fun onPauseTracking() {
        accumulatedSeconds = _uiState.value.elapsedSeconds
        trackingJob?.cancel()
        timerJob?.cancel()
        gpsWatchdogJob?.cancel()
        _uiState.update { it.copy(isPaused = true, hasGpsSignal = false) }
    }

    fun onResumeTracking() {
        startTimeMs = System.currentTimeMillis()
        _uiState.update { it.copy(isPaused = false) }
        startTimer()
        startLocationCollection()
        startGpsWatchdog()
    }

    fun onStopTracking() {
        trackingJob?.cancel()
        timerJob?.cancel()
        gpsWatchdogJob?.cancel()
        val state = _uiState.value
        _uiState.update { it.copy(isTracking = false, isPaused = false, hasGpsSignal = false) }

        viewModelScope.launch {
            currentRouteId?.let { id ->
                inspectionRepository.finishRoute(id, state.distanceMeters)
            }
            currentRouteId = null
        }
    }

    private fun startLocationCollection() {
        trackingJob = viewModelScope.launch {
            locationHelper.trackLocation().collect { loc ->
                _uiState.update { it.copy(hasGpsSignal = true) }
                resetGpsWatchdog()
                if (isValidPoint(loc)) {
                    addRoutePoint(loc)
                }
            }
        }
    }

    private fun startGpsWatchdog() {
        gpsWatchdogJob?.cancel()
        gpsWatchdogJob = viewModelScope.launch {
            delay(GPS_SIGNAL_TIMEOUT_MS)
            _uiState.update { it.copy(hasGpsSignal = false) }
            _gpsLostEvent.tryEmit(Unit)
        }
    }

    private fun resetGpsWatchdog() {
        startGpsWatchdog()
    }

    private fun isValidPoint(loc: LocationState): Boolean {
        val points = _uiState.value.routePoints
        if (points.isEmpty()) return true

        val last = points.last()
        val distance = haversine(last.latitude, last.longitude, loc.latitude, loc.longitude)
        val timeDiffSeconds = (loc.timestamp - last.timestamp) / 1000.0
        if (timeDiffSeconds <= 0) return false

        val speedMs = distance / timeDiffSeconds
        return speedMs <= MAX_WALKING_SPEED_MS
    }

    private fun addRoutePoint(loc: LocationState) {
        val routeId = currentRouteId ?: return
        pointOrder++

        viewModelScope.launch {
            inspectionRepository.addPoint(
                RoutePoint(
                    id = UUID.randomUUID().toString(),
                    routeId = routeId,
                    order = pointOrder,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy,
                    timestamp = loc.timestamp,
                ),
            )
        }

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
                pointCount = pointOrder,
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
        gpsWatchdogJob?.cancel()
    }

    companion object {
        private const val EARTH_RADIUS_M = 6_371_000.0
        private const val MAX_WALKING_SPEED_MS = 27.8
        private const val GPS_SIGNAL_TIMEOUT_MS = 10_000L

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
