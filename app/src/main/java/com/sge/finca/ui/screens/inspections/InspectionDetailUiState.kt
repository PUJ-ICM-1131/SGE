package com.sge.finca.ui.screens.inspections

import com.sge.finca.domain.model.InspectionRoute
import com.sge.finca.domain.model.RoutePoint

data class InspectionDetailUiState(
    val route: InspectionRoute? = null,
    val points: List<RoutePoint> = emptyList(),
    val isLoading: Boolean = true,
)
