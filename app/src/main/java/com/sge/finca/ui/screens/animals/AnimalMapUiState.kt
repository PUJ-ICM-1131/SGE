package com.sge.finca.ui.screens.animals

data class AnimalMapUiState(
    val userLatitude: Double = 4.628,
    val userLongitude: Double = -74.065,
    val hasUserLocation: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
