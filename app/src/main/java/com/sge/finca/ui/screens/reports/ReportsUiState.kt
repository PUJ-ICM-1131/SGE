package com.sge.finca.ui.screens.reports

data class ReportsUiState(
    val selectedPeriod: String = "Mes",
    val balanceCents: Long = 0,
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    val isLoading: Boolean = false,
)
