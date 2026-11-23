package com.sge.finca.ui.screens.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.usecase.ObserveTrackedAnimalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Vacas del mapa de la finca (#72): posiciones en vivo y la vaca seleccionada. La ubicación del
 * usuario y el botón de centrar los maneja InspectionMapViewModel.
 */
@HiltViewModel
class AnimalMapViewModel @Inject constructor(
    private val observeTrackedAnimals: ObserveTrackedAnimalsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimalMapUiState())
    val uiState: StateFlow<AnimalMapUiState> = _uiState.asStateFlow()

    init {
        // Se actualiza solo: al registrar un animal o cuando llegue una posición en vivo
        viewModelScope.launch {
            observeTrackedAnimals().collect { animals ->
                _uiState.update { it.copy(animals = animals) }
            }
        }
    }

    fun onAnimalSelected(id: String) {
        _uiState.update { it.copy(selectedAnimalId = id) }
    }

    fun onSelectionCleared() {
        _uiState.update { it.copy(selectedAnimalId = null) }
    }
}
