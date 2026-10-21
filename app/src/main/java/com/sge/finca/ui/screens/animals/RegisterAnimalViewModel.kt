package com.sge.finca.ui.screens.animals

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RegisterAnimalViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterAnimalUiState())
    val uiState: StateFlow<RegisterAnimalUiState> = _uiState.asStateFlow()

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun onPhotoTaken(uri: String) {
        _uiState.update { it.copy(photoUri = uri) }
    }

    fun onLocationObtained(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                latitude = lat,
                longitude = lng,
                locationText = "%.6f, %.6f".format(lat, lng),
            )
        }
    }

    fun onSave() {
        // TODO: validate and save via repository
    }
}
