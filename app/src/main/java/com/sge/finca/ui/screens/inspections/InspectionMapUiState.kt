package com.sge.finca.ui.screens.inspections

import com.sge.finca.platform.location.LocationState
import com.sge.finca.ui.components.GpsMessage

data class InspectionMapUiState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0,
    val distanceMeters: Double = 0.0,
    val pointCount: Int = 0,
    val hasGpsSignal: Boolean = false,
    val routePoints: List<LocationState> = emptyList(),
    val userLatitude: Double = 4.628,
    val userLongitude: Double = -74.065,
    val hasUserLocation: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Buscando la posición tras tocar "centrar en mi ubicación". */
    val isLocating: Boolean = false,
    /** Sube cada vez que hay que centrar el mapa, aunque la posición no haya cambiado. */
    val centerRequest: Int = 0,
    /** Aviso pendiente de mostrar en el snackbar; la pantalla lo limpia al mostrarlo. */
    val gpsMessage: GpsMessage? = null,
)
