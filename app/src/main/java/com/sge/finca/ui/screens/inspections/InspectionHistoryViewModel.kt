package com.sge.finca.ui.screens.inspections

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InspectionHistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionHistoryUiState())
    val uiState: StateFlow<InspectionHistoryUiState> = _uiState.asStateFlow()

    // TODO: observe inspection routes from repository
}
