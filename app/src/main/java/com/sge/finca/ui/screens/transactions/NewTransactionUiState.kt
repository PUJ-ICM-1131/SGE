package com.sge.finca.ui.screens.transactions

data class NewTransactionUiState(
    val form: TransactionFormState = TransactionFormState(),
    val isLoading: Boolean = true,
    /** El usuario no tiene finca: no hay dónde registrar el movimiento. */
    val noFarm: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
)
