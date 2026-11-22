package com.sge.finca.ui.screens.transactions

import com.sge.finca.domain.model.Transaction

data class TransactionDetailUiState(
    val isLoading: Boolean = true,
    val transaction: Transaction? = null,
    val notFound: Boolean = false,
    /** Muestra el lápiz para editar: solo el propietario (#28). */
    val canEdit: Boolean = false,
    /** Se eliminó estando en la edición: la pantalla de detalle se cierra sola. */
    val deleted: Boolean = false,
)
