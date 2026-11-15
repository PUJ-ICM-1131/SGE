package com.sge.finca.ui.screens.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.TrackedAnimal
import com.sge.finca.domain.repository.AnimalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimalListViewModel @Inject constructor(
    private val animalRepository: AnimalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimalListUiState())
    val uiState: StateFlow<AnimalListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            animalRepository.getAnimals().collect { animals ->
                _uiState.update {
                    it.copy(
                        animals = animals.map { animal ->
                            TrackedAnimal(
                                animal = animal,
                                latitude = animal.latitude,
                                longitude = animal.longitude,
                                isMoving = false,
                                speedMetersPerSecond = 0f,
                            )
                        },
                        isLoading = false,
                    )
                }
            }
        }
    }
}
