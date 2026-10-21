package com.sge.finca.ui.screens.transactions

data class EditTransactionUiState(
    val amount: String = "",
    val category: String = "",
    val note: String = "",
    val showDeleteDialog: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)
