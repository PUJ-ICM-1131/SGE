package com.sge.finca.ui.screens.inspections

import com.sge.finca.platform.location.LocationState

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
)
