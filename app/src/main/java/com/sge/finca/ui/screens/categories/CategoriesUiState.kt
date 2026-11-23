package com.sge.finca.ui.screens.categories

import com.sge.finca.domain.model.Category

data class CategoriesUiState(
    val showIncome: Boolean = true,
    val categories: List<Category> = emptyList(),
    val hasFarm: Boolean = false,
    val isOwner: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)