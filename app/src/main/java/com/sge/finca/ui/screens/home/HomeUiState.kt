package com.sge.finca.ui.screens.home

import com.sge.finca.domain.model.Transaction

data class HomeUiState(
    val farmName: String = "SGE Finca",
    /** Totales del mes en curso, en centavos. */
    val balanceCents: Long = 0,
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    /** Las [HomeViewModel.RECENT_COUNT] más recientes. */
    val recentTransactions: List<Transaction> = emptyList(),
    val noFarm: Boolean = false,
    val isLoading: Boolean = true,
)
