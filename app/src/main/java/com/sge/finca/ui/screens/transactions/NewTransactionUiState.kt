package com.sge.finca.ui.screens.transactions

data class NewTransactionUiState(
    val isIncome: Boolean = true,
    val amount: String = "",
    val category: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
)
