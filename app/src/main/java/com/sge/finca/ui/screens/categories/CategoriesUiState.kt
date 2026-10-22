package com.sge.finca.ui.screens.categories

data class CategoriesUiState(
    val showIncome: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
