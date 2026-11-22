package com.sge.finca.ui.screens.transactions

import com.sge.finca.domain.model.BusinessDate
import com.sge.finca.domain.model.Transaction
import com.sge.finca.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFilterTest {

    /** Domingo 22 de noviembre de 2026. */
    private val sunday = BusinessDate.of(2026, 11, 22)

    @Test
    fun `la semana empieza el lunes`() {
        assertEquals(BusinessDate.of(2026, 11, 16), HistoryFilter.startOfWeek(sunday))
        val monday = BusinessDate.of(2026, 11, 23)
        assertEquals(monday, HistoryFilter.startOfWeek(monday))
        // Semana que cruza el año: jueves 1 de enero de 2026 → lunes 29 de diciembre de 2025.
        assertEquals(BusinessDate.of(2025, 12, 29), HistoryFilter.startOfWeek(BusinessDate.of(2026, 1, 1)))
    }

    @Test
    fun `mes y año calendario hasta hoy`() {
        assertEquals(BusinessDate.of(2026, 11, 1) to sunday, HistoryFilter(period = HistoryPeriod.MONTH).dateRange(sunday))
        assertEquals(BusinessDate.of(2026, 1, 1) to sunday, HistoryFilter(period = HistoryPeriod.YEAR).dateRange(sunday))
        assertEquals(null to null, HistoryFilter(period = HistoryPeriod.ALL).dateRange(sunday))
    }

    @Test
    fun `los filtros se combinan en la consulta`() {
        val filter = HistoryFilter(
            type = TransactionType.EXPENSE,
            period = HistoryPeriod.CUSTOM,
            customFrom = BusinessDate.of(2026, 10, 1),
            customTo = BusinessDate.of(2026, 10, 20),
            categoryId = "sanidad",
        ).toTransactionFilter(sunday)

        assertEquals(BusinessDate.of(2026, 10, 1), filter.fromEpochDay)
        assertEquals(BusinessDate.of(2026, 10, 20), filter.toEpochDay)
        assertEquals(TransactionType.EXPENSE, filter.type)
        assertEquals("sanidad", filter.categoryId)
    }

    @Test
    fun `el filtro por defecto es este mes sin tipo ni categoria`() {
        val default = HistoryFilter()
        assertTrue(default.isDefault)
        assertEquals(HistoryPeriod.MONTH, default.period)
        assertNull(default.toTransactionFilter(sunday).type)
        assertFalse(default.copy(type = TransactionType.INCOME).isDefault)
    }

    @Test
    fun `descripcion del periodo personalizado`() {
        val filter = HistoryFilter(
            period = HistoryPeriod.CUSTOM,
            customFrom = BusinessDate.of(2026, 10, 1),
            customTo = BusinessDate.of(2026, 10, 20),
        )
        assertEquals("1 oct 2026 – 20 oct 2026", filter.periodDescription())
    }

    @Test
    fun `el resumen suma ingresos y egresos y calcula el balance`() {
        val summary = HistorySummary.of(
            listOf(
                tx("1", TransactionType.INCOME, 5_000_000),
                tx("2", TransactionType.INCOME, 2_500_000),
                tx("3", TransactionType.EXPENSE, 3_000_000),
            ),
        )
        assertEquals(7_500_000L, summary.incomeCents)
        assertEquals(3_000_000L, summary.expenseCents)
        assertEquals(4_500_000L, summary.balanceCents)
        assertEquals(-3_000_000L, HistorySummary.of(listOf(tx("4", TransactionType.EXPENSE, 3_000_000))).balanceCents)
    }

    private fun tx(id: String, type: TransactionType, cents: Long) = Transaction(
        id = id,
        farmId = "f1",
        type = type,
        amountCents = cents,
        categoryId = "c",
        categoryName = "Categoría",
        dateEpochDay = sunday,
        createdBy = "u1",
        createdAt = 0,
        updatedAt = 0,
    )
}
