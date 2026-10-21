package com.sge.finca.ui.screens.categories

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CategoriesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    fun onFilterChanged(showIncome: Boolean) {
        _uiState.update { it.copy(showIncome = showIncome) }
    }

    // TODO: CRUD operations via repository
}
