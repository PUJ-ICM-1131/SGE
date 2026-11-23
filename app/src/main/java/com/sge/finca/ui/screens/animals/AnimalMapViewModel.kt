package com.sge.finca.ui.screens.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.usecase.ObserveTrackedAnimalsUseCase
import com.sge.finca.platform.location.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class AnimalMapViewModel @Inject constructor(
    private val locationHelper: LocationHelper,
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

    /**
     * Botón "centrar en mi ubicación" (#59). Sin ubicación activada o sin señal, avisa con un
     * snackbar en vez de no hacer nada; nunca bloquea el mapa.
     */
    fun onCenterOnMeClicked() {
        if (_uiState.value.isLocating) return
        if (!locationHelper.isLocationEnabled()) {
            _uiState.update { it.copy(gpsMessage = GpsMessage.LOCATION_OFF) }
            return
        }
        _uiState.update { it.copy(isLocating = true) }
        viewModelScope.launch {
            val loc = withTimeoutOrNull(LOCATE_TIMEOUT_MS) { locationHelper.requestCurrentLocation() }
            _uiState.update {
                if (loc == null) {
                    it.copy(isLocating = false, gpsMessage = GpsMessage.NO_SIGNAL)
                } else {
                    it.copy(
                        isLocating = false,
                        userLatitude = loc.latitude,
                        userLongitude = loc.longitude,
                        hasUserLocation = true,
                        centerRequest = it.centerRequest + 1,
                    )
                }
            }
        }
    }

    fun onGpsMessageShown() {
        _uiState.update { it.copy(gpsMessage = null) }
    }

    fun onAnimalSelected(id: String) {
        _uiState.update { it.copy(selectedAnimalId = id) }
    }

    fun onSelectionCleared() {
        _uiState.update { it.copy(selectedAnimalId = null) }
    }

    private companion object {
        /** Más que esto con el indicador girando se siente como si el botón no sirviera. */
        const val LOCATE_TIMEOUT_MS = 10_000L
    }
}
