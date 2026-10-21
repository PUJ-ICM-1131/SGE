package com.sge.finca.ui.screens.inspections

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InspectionMapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionMapUiState())
    val uiState: StateFlow<InspectionMapUiState> = _uiState.asStateFlow()

    fun onStartTracking() {
        _uiState.update { it.copy(isTracking = true) }
        // TODO: start GPS tracking via LocationHelper
    }

    fun onStopTracking() {
        _uiState.update { it.copy(isTracking = false) }
        // TODO: stop GPS tracking and save route
    }
}
