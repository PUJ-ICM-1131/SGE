package com.sge.finca.ui.screens.home

import com.sge.finca.domain.model.BusinessDate
import com.sge.finca.domain.model.Transaction
import com.sge.finca.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSummaryTest {

    private val today = BusinessDate.of(2026, 11, 22)

    @Test
    fun emptyFarmShowsZeros() {
        val summary = HomeSummary.of(emptyList(), today)
        assertEquals(0L, summary.monthlyBalance)
        assertEquals(0, summary.incomeCount)
        assertEquals(0, summary.expenseCount)
        assertTrue(summary.recentTransactions.isEmpty())
    }

    @Test
    fun issueExampleIncome100kMinusExpense40k() {
        // "Cómo verificar" del issue: ingreso de $100.000 y egreso de $40.000 -> balance $60.000.
        val summary = HomeSummary.of(
            listOf(
                tx("1", TransactionType.INCOME, 10_000_000, today),
                tx("2", TransactionType.EXPENSE, 4_000_000, today),
            ),
            today,
        )
        assertEquals(10_000_000L, summary.monthlyIncome)
        assertEquals(4_000_000L, summary.monthlyExpense)
        assertEquals(6_000_000L, summary.monthlyBalance)
        assertEquals(1, summary.incomeCount)
        assertEquals(1, summary.expenseCount)
    }

    @Test
    fun onlyCurrentCalendarMonthCounts() {
        val summary = HomeSummary.of(
            listOf(
                tx("oct", TransactionType.INCOME, 500, BusinessDate.of(2026, 10, 31)),
                tx("nov1", TransactionType.INCOME, 100, BusinessDate.of(2026, 11, 1)),
                tx("nov30", TransactionType.EXPENSE, 30, BusinessDate.of(2026, 11, 30)),
                tx("dic", TransactionType.EXPENSE, 900, BusinessDate.of(2026, 12, 1)),
                tx("nov-2025", TransactionType.INCOME, 700, BusinessDate.of(2025, 11, 22)),
            ),
            today,
        )
        assertEquals(100L, summary.monthlyIncome)
        assertEquals(30L, summary.monthlyExpense)
        assertEquals(70L, summary.monthlyBalance)
        assertEquals(1, summary.incomeCount)
        assertEquals(1, summary.expenseCount)
    }

    @Test
    fun negativeBalanceWhenExpensesWin() {
        val summary = HomeSummary.of(listOf(tx("1", TransactionType.EXPENSE, 3_000_000, today)), today)
        assertEquals(-3_000_000L, summary.monthlyBalance)
    }

    @Test
    fun recentAreTheFiveNewestOfAnyTypeAndMonth() {
        val list = (1..7).map { day ->
            tx("t$day", if (day % 2 == 0) TransactionType.EXPENSE else TransactionType.INCOME, 100, today - day)
        } + tx("hoy-b", TransactionType.INCOME, 1, today, createdAt = 2) +
            tx("hoy-a", TransactionType.INCOME, 1, today, createdAt = 1)

        val recent = HomeSummary.of(list.shuffled(), today).recentTransactions.map { it.id }

        assertEquals(listOf("hoy-b", "hoy-a", "t1", "t2", "t3"), recent)
    }

    private fun tx(
        id: String,
        type: TransactionType,
        cents: Long,
        day: Long,
        createdAt: Long = 0,
    ) = Transaction(
        id = id,
        farmId = "f1",
        type = type,
        amountCents = cents,
        categoryId = "c",
        categoryName = "Categoría",
        dateEpochDay = day,
        createdBy = "u1",
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
