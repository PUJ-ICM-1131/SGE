package com.sge.finca.ui.screens.home

data class HomeUiState(
    val farmName: String = "SGE Finca",
    val balanceCents: Long = 0,
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    val isLoading: Boolean = false,
)
