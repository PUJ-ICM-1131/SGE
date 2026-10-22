package com.sge.finca.ui.screens.reports

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ReportsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    fun onPeriodChanged(period: String) {
        _uiState.update { it.copy(selectedPeriod = period) }
        // TODO: recalculate report for new period
    }
}
