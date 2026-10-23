package com.sge.finca.ui.screens.animals

data class AnimalDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val name: String = "",
    val notes: String = "",
    /** URI local (FileProvider) de la foto actual del formulario; null si no tiene. */
    val photoUri: String? = null,
    val variant: CowVariant = CowVariant.HOLSTEIN,
    val locationText: String = "",
    val isCameraOpen: Boolean = false,
    val isSaving: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val errorMessage: String? = null,
    /** true cuando se guardó o se eliminó: la pantalla vuelve a la lista. */
    val finished: Boolean = false,
)
