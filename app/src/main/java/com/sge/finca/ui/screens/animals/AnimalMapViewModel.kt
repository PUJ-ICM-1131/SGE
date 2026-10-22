package com.sge.finca.ui.screens.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.repository.AnimalRepository
import com.sge.finca.platform.location.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimalMapViewModel @Inject constructor(
    private val locationHelper: LocationHelper,
    private val animalRepository: AnimalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimalMapUiState())
    val uiState: StateFlow<AnimalMapUiState> = _uiState.asStateFlow()

    init {
        // Room emite la lista cada vez que se guarda un animal nuevo
        viewModelScope.launch {
            animalRepository.getAnimals().collect { animals ->
                _uiState.update { it.copy(animals = animals) }
            }
        }
    }

    fun onPermissionGranted() {
        viewModelScope.launch {
            val loc = locationHelper.requestCurrentLocation()
            if (loc != null) {
                _uiState.update {
                    it.copy(
                        userLatitude = loc.latitude,
                        userLongitude = loc.longitude,
                        hasUserLocation = true,
                    )
                }
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