package com.sge.finca.ui.screens.home

import com.sge.finca.domain.model.FarmRole
import com.sge.finca.domain.model.Transaction

/** Estado del inicio (issue #33). Montos en centavos de peso colombiano. */
data class HomeUiState(
    val farmName: String = "",
    /** Rol en la finca activa; si aún no se conoce se asume el más restringido. */
    val userRole: FarmRole = FarmRole.WORKER,
    /** Totales del mes en curso. */
    val monthlyBalance: Long = 0,
    val monthlyIncome: Long = 0,
    val monthlyExpense: Long = 0,
    val incomeCount: Int = 0,
    val expenseCount: Int = 0,
    /** Las [HomeSummary.RECENT_COUNT] más recientes. */
    val recentTransactions: List<Transaction> = emptyList(),
    /** Registros sin subir a Firestore (#32). */
    val pendingSyncCount: Int = 0,
    /** Registros que Firestore rechazó (#32): muestran "Reintentar". */
    val failedSyncCount: Int = 0,
    /** Recordatorios vencidos o que vencen hoy: el número de la campana. */
    val notificationCount: Int = 0,
    val noFarm: Boolean = false,
    val isLoading: Boolean = true,
)
