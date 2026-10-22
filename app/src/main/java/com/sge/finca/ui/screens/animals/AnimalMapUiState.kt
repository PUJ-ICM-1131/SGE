package com.sge.finca.ui.screens.animals

import com.sge.finca.domain.model.Animal

data class AnimalMapUiState(
    val userLatitude: Double = 4.628,
    val userLongitude: Double = -74.065,
    val hasUserLocation: Boolean = false,
    val animals: List<Animal> = emptyList(),
    val selectedAnimalId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedAnimal: Animal?
        get() = animals.firstOrNull { it.id == selectedAnimalId }
}