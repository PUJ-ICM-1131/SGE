package com.sge.finca.ui.screens.transactions

data class EditTransactionUiState(
    val form: TransactionFormState = TransactionFormState(),
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    /**
     * Solo el propietario edita o elimina (#28). Un trabajador ve el movimiento en modo lectura.
     */
    val canManage: Boolean = false,
    /** Quién lo registró, para mostrarlo en el detalle. */
    val createdByName: String? = null,
    /** La categoría original ya no existe: hay que elegir otra antes de guardar. */
    val originalCategoryName: String? = null,
    val showDeleteDialog: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Guardó o eliminó: la pantalla vuelve atrás. */
    val finished: Boolean = false,
)
