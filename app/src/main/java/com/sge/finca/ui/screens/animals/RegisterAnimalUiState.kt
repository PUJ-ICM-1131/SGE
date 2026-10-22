package com.sge.finca.ui.screens.animals

data class RegisterAnimalUiState(
    val name: String = "",
    val notes: String = "",
    /** URI local (FileProvider) de la foto; null si aún no hay. */
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Float? = null,
    val locationText: String = "Obteniendo ubicación GPS...",
    val isCameraOpen: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
)