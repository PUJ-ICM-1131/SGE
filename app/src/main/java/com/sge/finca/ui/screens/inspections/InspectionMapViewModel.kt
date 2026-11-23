package com.sge.finca.ui.screens.inspections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.Coordinate
import com.sge.finca.domain.model.InspectionRoute
import com.sge.finca.domain.model.PaddockGeometry
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.model.RoutePoint
import com.sge.finca.domain.repository.InspectionRepository
import com.sge.finca.domain.repository.PaddockRepository
import com.sge.finca.domain.repository.SessionRepository
import com.sge.finca.platform.location.CenterOnMeResult
import com.sge.finca.platform.location.LocationHelper
import com.sge.finca.platform.location.LocationState
import com.sge.finca.ui.components.GpsMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
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
    private val paddockRepository: PaddockRepository,
    sessionRepository: SessionRepository,
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
    private var farmId: String? = null

    init {
        // Potreros de la finca activa (#71); se recargan si cambia la finca o el rol.
        viewModelScope.launch {
            sessionRepository.session
                .filter { !it.loading }
                .map { it.farmId?.takeIf(String::isNotBlank) to it.can(Permission.CONFIGURE_FARM) }
                .distinctUntilChanged()
                .collectLatest { (farmId, canManage) ->
                    this@InspectionMapViewModel.farmId = farmId
                    _uiState.update { it.copy(canManagePaddocks = canManage, paddocks = emptyList()) }
                    if (farmId == null) return@collectLatest
                    paddockRepository.observePaddocks(farmId).collect { paddocks ->
                        _uiState.update { it.copy(paddocks = paddocks) }
                    }
                }
        }
    }

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

    /**
     * Botón "centrar en mi ubicación", igual que en el mapa de ganado (#59): sin ubicación
     * activada o sin señal avisa con un snackbar en vez de no hacer nada.
     */
    fun onCenterOnMeClicked() {
        if (_uiState.value.isLocating) return
        _uiState.update { it.copy(isLocating = true) }
        viewModelScope.launch {
            val result = locationHelper.locateForCentering()
            _uiState.update {
                when (result) {
                    CenterOnMeResult.LocationOff ->
                        it.copy(isLocating = false, gpsMessage = GpsMessage.LOCATION_OFF)
                    CenterOnMeResult.NoSignal ->
                        it.copy(isLocating = false, gpsMessage = GpsMessage.NO_SIGNAL)
                    is CenterOnMeResult.Located -> it.copy(
                        isLocating = false,
                        userLatitude = result.location.latitude,
                        userLongitude = result.location.longitude,
                        hasUserLocation = true,
                        centerRequest = it.centerRequest + 1,
                    )
                }
            }
        }
    }

    fun onGpsMessageShown() {
        _uiState.update { it.copy(gpsMessage = null) }
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
        // Si se volvió cerca del inicio, el recorrido encierra un área que se puede guardar (#71).
        val walked = state.routePoints.map { Coordinate(it.latitude, it.longitude) }
        val draft = walked.takeIf(PaddockGeometry::isClosedArea)
            ?.let { PaddockDraft(it, PaddockGeometry.areaM2(it)) }
        _uiState.update {
            it.copy(isTracking = false, isPaused = false, hasGpsSignal = false, paddockDraft = draft)
        }

        viewModelScope.launch {
            currentRouteId?.let { id ->
                inspectionRepository.finishRoute(id, state.distanceMeters)
            }
            currentRouteId = null
        }
    }

    fun onSavePaddock(name: String) {
        val draft = _uiState.value.paddockDraft ?: return
        paddockAction("No se pudo guardar el potrero") { farmId ->
            paddockRepository.create(farmId, name, draft.points)
            _uiState.update { it.copy(paddockDraft = null, paddockMessage = "Potrero guardado") }
        }
    }

    fun onDiscardPaddock() {
        _uiState.update { it.copy(paddockDraft = null) }
    }

    fun onRenamePaddock(id: String, name: String) =
        paddockAction("No se pudo renombrar el potrero") { paddockRepository.rename(it, id, name) }

    fun onDeletePaddock(id: String) =
        paddockAction("No se pudo eliminar el potrero") { paddockRepository.delete(it, id) }

    fun onPaddockMessageShown() {
        _uiState.update { it.copy(paddockMessage = null) }
    }

    /** Los repositorios validan permisos y datos; su mensaje va al snackbar. */
    private fun paddockAction(errorMessage: String, action: suspend (farmId: String) -> Unit) {
        val farmId = farmId ?: run {
            _uiState.update { it.copy(paddockMessage = "Tu cuenta aún no pertenece a una finca") }
            return
        }
        viewModelScope.launch {
            try {
                action(farmId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(paddockMessage = e.message ?: errorMessage) }
            }
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
