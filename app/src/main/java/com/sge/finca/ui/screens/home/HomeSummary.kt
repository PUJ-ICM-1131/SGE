package com.sge.finca.ui.screens.home

import com.sge.finca.domain.model.BusinessDate
import com.sge.finca.domain.model.Transaction
import com.sge.finca.domain.model.TransactionType

/**
 * Números del inicio (issue #33), calculados en el celular a partir de Room.
 *
 * - Mes en curso = mismo año y mes calendario que [today], por fecha de negocio. Un movimiento
 *   con fecha del mes siguiente no cuenta todavía.
 * - Recientes = los [recentCount] más nuevos de cualquier tipo y fecha.
 */
data class HomeSummary(
    val monthlyIncome: Long = 0,
    val monthlyExpense: Long = 0,
    val incomeCount: Int = 0,
    val expenseCount: Int = 0,
    val recentTransactions: List<Transaction> = emptyList(),
) {
    val monthlyBalance: Long get() = monthlyIncome - monthlyExpense

    companion object {
        const val RECENT_COUNT = 5

        fun of(
            transactions: List<Transaction>,
            today: Long,
            recentCount: Int = RECENT_COUNT,
        ): HomeSummary {
            val month = monthKey(today)
            val thisMonth = transactions.filter { monthKey(it.dateEpochDay) == month }
            val incomes = thisMonth.filter { it.type == TransactionType.INCOME }
            val expenses = thisMonth.filter { it.type == TransactionType.EXPENSE }
            return HomeSummary(
                monthlyIncome = incomes.sumOf { it.amountCents },
                monthlyExpense = expenses.sumOf { it.amountCents },
                incomeCount = incomes.size,
                expenseCount = expenses.size,
                recentTransactions = transactions
                    .sortedWith(
                        compareByDescending<Transaction> { it.dateEpochDay }
                            .thenByDescending { it.createdAt },
                    )
                    .take(recentCount),
            )
        }

        /** "2026-11" a partir de la fecha de negocio. */
        private fun monthKey(epochDay: Long): String = BusinessDate.toIsoString(epochDay).substring(0, 7)
    }
}
