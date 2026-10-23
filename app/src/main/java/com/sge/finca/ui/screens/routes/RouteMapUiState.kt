package com.sge.finca.ui.screens.routes

import com.google.android.gms.maps.model.LatLng

data class RouteMapUiState(
    val origin: LatLng? = null,
    val destination: LatLng? = null,
    val distanceMeters: Double? = null,
    val userLatitude: Double = 4.628,
    val userLongitude: Double = -74.065,
    val hasUserLocation: Boolean = false,
)
