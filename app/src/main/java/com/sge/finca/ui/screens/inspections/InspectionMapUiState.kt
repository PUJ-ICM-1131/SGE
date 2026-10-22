package com.sge.finca.ui.screens.inspections

data class InspectionMapUiState(
    val isTracking: Boolean = false,
    val elapsedSeconds: Long = 0,
    val distanceMeters: Double = 0.0,
    val pointCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
