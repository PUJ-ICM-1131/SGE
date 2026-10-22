package com.sge.finca.ui.screens.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.platform.location.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterAnimalViewModel @Inject constructor(
    private val locationHelper: LocationHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterAnimalUiState())
    val uiState: StateFlow<RegisterAnimalUiState> = _uiState.asStateFlow()

    fun fetchLocation() {
        viewModelScope.launch {
            val loc = locationHelper.requestCurrentLocation()
            if (loc != null) {
                _uiState.update {
                    it.copy(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        locationAccuracy = loc.accuracy,
                        locationText = "%.6f, %.6f (±%.0fm)".format(
                            loc.latitude, loc.longitude, loc.accuracy,
                        ),
                    )
                }
            } else {
                _uiState.update {
                    it.copy(locationText = "Ubicación no disponible")
                }
            }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun onPhotoTaken(uri: String) {
        _uiState.update { it.copy(photoUri = uri) }
    }

    fun onSave() {
        // TODO: validate and save via repository
    }
}
