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
    /** Recordatorios vencidos o que vencen hoy: el número de la campana. */
    val notificationCount: Int = 0,
    val noFarm: Boolean = false,
    val isLoading: Boolean = true,
)
