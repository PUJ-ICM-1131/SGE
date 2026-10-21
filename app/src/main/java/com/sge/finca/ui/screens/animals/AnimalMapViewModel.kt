package com.sge.finca.ui.screens.animals

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AnimalMapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AnimalMapUiState())
    val uiState: StateFlow<AnimalMapUiState> = _uiState.asStateFlow()

    // TODO: observe animal records from repository
}
