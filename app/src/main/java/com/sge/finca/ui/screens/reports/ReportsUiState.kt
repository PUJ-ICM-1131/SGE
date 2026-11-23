package com.sge.finca.ui.screens.reports

import com.sge.finca.domain.model.FinancialReport

data class ReportsUiState(
    val selectedPeriod: String = "Mes",
    val report: FinancialReport? = null,
    val isLoading: Boolean = false,
    val noFarm: Boolean = false,
    val errorMessage: String? = null,
) {
    val balanceCents: Long get() = report?.balanceCents ?: 0L
    val incomeCents: Long get() = report?.incomeCents ?: 0L
    val expenseCents: Long get() = report?.expenseCents ?: 0L
}