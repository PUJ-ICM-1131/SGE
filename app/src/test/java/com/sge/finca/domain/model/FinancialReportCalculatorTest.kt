package com.sge.finca.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialReportCalculatorTest {

    @Test
    fun weekStartsOnMondayAndEndsOnSunday() {
        val range = ReportPeriod.WEEK.rangeFor(BusinessDate.of(2026, 11, 22))

        assertEquals(BusinessDate.of(2026, 11, 16), range.fromEpochDay)
        assertEquals(BusinessDate.of(2026, 11, 22), range.toEpochDay)
    }

    @Test
    fun monthIncludesLeapDay() {
        val range = ReportPeriod.MONTH.rangeFor(BusinessDate.of(2028, 2, 15))

        assertEquals(BusinessDate.of(2028, 2, 1), range.fromEpochDay)
        assertEquals(BusinessDate.of(2028, 2, 29), range.toEpochDay)
    }

    @Test
    fun yearIncludesJanuaryThroughDecember() {
        val range = ReportPeriod.YEAR.rangeFor(BusinessDate.of(2026, 7, 10))

        assertEquals(BusinessDate.of(2026, 1, 1), range.fromEpochDay)
        assertEquals(BusinessDate.of(2026, 12, 31), range.toEpochDay)
    }

    @Test
    fun calculatesTotalsCountsCategoriesAndPendingSync() {
        val day = BusinessDate.of(2026, 11, 22)
        val range = ReportPeriod.MONTH.rangeFor(day)

        val transactions = listOf(
            transaction("income-1", TransactionType.INCOME, 2_000_000L, "a", "Ventas", day),
            transaction("income-2", TransactionType.INCOME, 1_000_000L, "b", "Otros", day),
            transaction(
                "expense-1",
                TransactionType.EXPENSE,
                500_000L,
                "c",
                "Insumos",
                day,
                SyncState.PENDING,
            ),
            transaction(
                "outside",
                TransactionType.EXPENSE,
                900_000L,
                "c",
                "Insumos",
                BusinessDate.of(2026, 10, 31),
            ),
        )

        val report = FinancialReportCalculator.calculate(transactions, range)

        assertEquals(3_000_000L, report.incomeCents)
        assertEquals(500_000L, report.expenseCents)
        assertEquals(2_500_000L, report.balanceCents)
        assertEquals(2, report.incomeCount)
        assertEquals(1, report.expenseCount)
        assertEquals(1, report.unsyncedCount)
        assertTrue(report.hasUnsyncedTransactions)
        assertFalse(report.isEmpty)

        val sales = report.categories.single { it.categoryId == "a" }
        val other = report.categories.single { it.categoryId == "b" }
        val supplies = report.categories.single { it.categoryId == "c" }

        assertEquals(66.67, sales.percentage, 0.01)
        assertEquals(33.33, other.percentage, 0.01)
        assertEquals(100.0, supplies.percentage, 0.01)
    }

    @Test
    fun emptyPeriodHasZeroTotals() {
        val range = ReportPeriod.WEEK.rangeFor(BusinessDate.of(2026, 11, 22))

        val report = FinancialReportCalculator.calculate(emptyList(), range)

        assertTrue(report.isEmpty)
        assertEquals(0L, report.balanceCents)
        assertTrue(report.categories.isEmpty())
        assertFalse(report.hasUnsyncedTransactions)
    }

    private fun transaction(
        id: String,
        type: TransactionType,
        amountCents: Long,
        categoryId: String,
        categoryName: String,
        dateEpochDay: Long,
        syncState: SyncState = SyncState.SYNCED,
    ) = Transaction(
        id = id,
        farmId = "farm-1",
        type = type,
        amountCents = amountCents,
        categoryId = categoryId,
        categoryName = categoryName,
        dateEpochDay = dateEpochDay,
        createdBy = "user-1",
        createdAt = 0L,
        updatedAt = 0L,
        syncState = syncState,
    )
}