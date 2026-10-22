package com.sge.finca.ui.screens.animals

data class RegisterAnimalUiState(
    val name: String = "",
    val notes: String = "",
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationText: String = "Obteniendo ubicación GPS...",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
)
