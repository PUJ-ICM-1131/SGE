package com.sge.finca.ui.screens.inspections

import com.sge.finca.domain.model.InspectionRoute

data class InspectionHistoryUiState(
    val routes: List<InspectionRoute> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
