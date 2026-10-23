package com.sge.finca.ui.screens.animals

import com.sge.finca.domain.model.TrackedAnimal

data class AnimalListUiState(
    val animals: List<TrackedAnimal> = emptyList(),
    val isLoading: Boolean = true,
)
