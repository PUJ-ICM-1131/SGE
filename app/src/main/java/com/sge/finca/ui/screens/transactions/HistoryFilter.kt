package com.sge.finca.ui.screens.transactions

import com.sge.finca.domain.model.BusinessDate
import com.sge.finca.domain.model.Transaction
import com.sge.finca.domain.model.TransactionFilter
import com.sge.finca.domain.model.TransactionType
import com.sge.finca.ui.format.BusinessDateFormat
import java.util.Calendar
import java.util.TimeZone

/** Periodos del historial. Semana, mes y año son calendario (desde el lunes, el día 1, el 1 de enero). */
enum class HistoryPeriod(val label: String) {
    WEEK("Semana"),
    MONTH("Mes"),
    YEAR("Año"),
    ALL("Todo"),
    CUSTOM("Personalizado"),
}

/**
 * Filtros combinables del historial. Un campo en null significa "sin filtrar por eso".
 * [customFrom]/[customTo] (epoch days, inclusivos) solo aplican con [HistoryPeriod.CUSTOM].
 */
data class HistoryFilter(
    val type: TransactionType? = null,
    val period: HistoryPeriod = HistoryPeriod.MONTH,
    val customFrom: Long? = null,
    val customTo: Long? = null,
    val categoryId: String? = null,
) {
    val isDefault: Boolean get() = this == HistoryFilter()

    /** Rango de fechas [desde, hasta] del periodo, ambos inclusivos; null = sin límite. */
    fun dateRange(today: Long = BusinessDate.today()): Pair<Long?, Long?> = when (period) {
        HistoryPeriod.WEEK -> startOfWeek(today) to today
        HistoryPeriod.MONTH -> startOfMonth(today) to today
        HistoryPeriod.YEAR -> startOfYear(today) to today
        HistoryPeriod.ALL -> null to null
        HistoryPeriod.CUSTOM -> customFrom to customTo
    }

    /** Lo que se le pide a Room: el filtrado corre en la consulta, no en memoria. */
    fun toTransactionFilter(today: Long = BusinessDate.today()): TransactionFilter {
        val (from, to) = dateRange(today)
        return TransactionFilter(fromEpochDay = from, toEpochDay = to, type = type, categoryId = categoryId)
    }

    /** Texto del periodo para el resumen: "Este mes", "1 oct 2026 – 20 oct 2026"… */
    fun periodDescription(): String = when (period) {
        HistoryPeriod.WEEK -> "Esta semana"
        HistoryPeriod.MONTH -> "Este mes"
        HistoryPeriod.YEAR -> "Este año"
        HistoryPeriod.ALL -> "Todo el historial"
        HistoryPeriod.CUSTOM -> {
            val from = customFrom?.let(BusinessDateFormat::format) ?: "…"
            val to = customTo?.let(BusinessDateFormat::format) ?: "…"
            "$from – $to"
        }
    }

    companion object {
        /** epoch day 0 (1970-01-01) fue jueves: con +3 el lunes queda en 0. */
        fun startOfWeek(day: Long): Long = day - Math.floorMod(day + 3, 7L)

        fun startOfMonth(day: Long): Long {
            val c = utcCalendar(day)
            return BusinessDate.of(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, 1)
        }

        fun startOfYear(day: Long): Long = BusinessDate.of(utcCalendar(day).get(Calendar.YEAR), 1, 1)

        private fun utcCalendar(day: Long): Calendar =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = BusinessDateFormat.toUtcMillis(day)
            }
    }
}

/** Totales del periodo filtrado, en centavos. */
data class HistorySummary(
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
) {
    val balanceCents: Long get() = incomeCents - expenseCents

    companion object {
        fun of(transactions: List<Transaction>): HistorySummary =
            HistorySummary(
                incomeCents = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents },
                expenseCents = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents },
            )
    }
}
