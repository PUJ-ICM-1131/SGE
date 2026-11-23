package com.sge.finca.ui.screens.animals

import com.sge.finca.domain.model.TrackedAnimal
import com.sge.finca.ui.components.GpsMessage

data class AnimalMapUiState(
    val userLatitude: Double = 4.628,
    val userLongitude: Double = -74.065,
    val hasUserLocation: Boolean = false,
    val animals: List<TrackedAnimal> = emptyList(),
    val selectedAnimalId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Buscando la posición tras tocar "centrar en mi ubicación" (#59). */
    val isLocating: Boolean = false,
    /** Sube cada vez que hay que centrar el mapa, aunque la posición no haya cambiado. */
    val centerRequest: Int = 0,
    /** Aviso pendiente de mostrar en el snackbar; la pantalla lo limpia al mostrarlo. */
    val gpsMessage: GpsMessage? = null,
) {
    val selectedAnimal: TrackedAnimal?
        get() = animals.firstOrNull { it.animal.id == selectedAnimalId }
}
