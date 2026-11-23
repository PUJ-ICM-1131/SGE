package com.sge.finca.domain.model

data class ReportCategoryTotal(
    val type: TransactionType,
    val categoryId: String,
    val categoryName: String,
    val amountCents: Long,
    val transactionCount: Int,
    /** Porcentaje respecto al total de ingresos o egresos, según el tipo. */
    val percentage: Double,
)

data class FinancialReport(
    val range: ReportDateRange,
    val incomeCents: Long,
    val expenseCents: Long,
    val incomeCount: Int,
    val expenseCount: Int,
    val categories: List<ReportCategoryTotal>,
    val unsyncedCount: Int,
) {
    val balanceCents: Long get() = incomeCents - expenseCents
    val isEmpty: Boolean get() = incomeCount == 0 && expenseCount == 0
    val hasUnsyncedTransactions: Boolean get() = unsyncedCount > 0
}

object FinancialReportCalculator {

    fun calculate(
        transactions: List<Transaction>,
        range: ReportDateRange,
    ): FinancialReport {
        val inPeriod = transactions.filter {
            it.dateEpochDay in range.fromEpochDay..range.toEpochDay
        }

        val income = inPeriod.filter { it.type == TransactionType.INCOME }
        val expenses = inPeriod.filter { it.type == TransactionType.EXPENSE }

        val incomeCents = income.sumOf { it.amountCents }
        val expenseCents = expenses.sumOf { it.amountCents }

        val categories = inPeriod
            .groupBy { it.type to it.categoryId }
            .map { (_, movements) ->
                val first = movements.first()
                val amountCents = movements.sumOf { it.amountCents }
                val typeTotal = when (first.type) {
                    TransactionType.INCOME -> incomeCents
                    TransactionType.EXPENSE -> expenseCents
                }

                ReportCategoryTotal(
                    type = first.type,
                    categoryId = first.categoryId,
                    categoryName = first.categoryName,
                    amountCents = amountCents,
                    transactionCount = movements.size,
                    percentage = if (typeTotal == 0L) {
                        0.0
                    } else {
                        amountCents.toDouble() * 100.0 / typeTotal
                    },
                )
            }
            .sortedWith(
                compareBy<ReportCategoryTotal> { it.type }
                    .thenByDescending { it.amountCents }
            )

        return FinancialReport(
            range = range,
            incomeCents = incomeCents,
            expenseCents = expenseCents,
            incomeCount = income.size,
            expenseCount = expenses.size,
            categories = categories,
            unsyncedCount = inPeriod.count { it.syncState != SyncState.SYNCED },
        )
    }
}