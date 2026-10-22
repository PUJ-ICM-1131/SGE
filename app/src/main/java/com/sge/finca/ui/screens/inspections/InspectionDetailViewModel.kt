package com.sge.finca.ui.screens.inspections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.RouteStatus
import com.sge.finca.domain.repository.InspectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InspectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val inspectionRepository: InspectionRepository,
) : ViewModel() {

    private val routeId: String = checkNotNull(savedStateHandle["routeId"])

    private val _uiState = MutableStateFlow(InspectionDetailUiState())
    val uiState: StateFlow<InspectionDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val routes = inspectionRepository.getRoutes().first()
            val route = routes.firstOrNull { it.id == routeId && it.status == RouteStatus.COMPLETED }

            inspectionRepository.getRoutePoints(routeId).collect { points ->
                _uiState.update {
                    it.copy(
                        route = route,
                        points = points,
                        isLoading = false,
                    )
                }
            }
        }
    }
}
