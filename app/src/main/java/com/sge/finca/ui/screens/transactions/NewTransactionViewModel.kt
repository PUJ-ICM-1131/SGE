package com.sge.finca.ui.screens.transactions

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NewTransactionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NewTransactionUiState())
    val uiState: StateFlow<NewTransactionUiState> = _uiState.asStateFlow()

    fun onTypeChanged(isIncome: Boolean) {
        _uiState.update { it.copy(isIncome = isIncome) }
    }

    fun onAmountChanged(value: String) {
        _uiState.update { it.copy(amount = value, errorMessage = null) }
    }

    fun onCategoryChanged(value: String) {
        _uiState.update { it.copy(category = value, errorMessage = null) }
    }

    fun onNoteChanged(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun onSave() {
        // TODO: validate and save via repository
    }
}
