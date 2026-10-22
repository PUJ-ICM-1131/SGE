package com.sge.finca.ui.screens.inspections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.RouteStatus
import com.sge.finca.domain.repository.InspectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InspectionHistoryViewModel @Inject constructor(
    private val inspectionRepository: InspectionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionHistoryUiState())
    val uiState: StateFlow<InspectionHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            inspectionRepository.getRoutes().collect { routes ->
                _uiState.update {
                    it.copy(
                        routes = routes.filter { r -> r.status == RouteStatus.COMPLETED },
                        isLoading = false,
                    )
                }
            }
        }
    }
}
