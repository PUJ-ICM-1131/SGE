package com.sge.finca.domain.model

import java.util.Calendar
import java.util.TimeZone

data class ReportDateRange(
    val fromEpochDay: Long,
    val toEpochDay: Long,
)

enum class ReportPeriod {
    WEEK,
    MONTH,
    YEAR;

    /**
     * Devuelve los días incluidos en el período que contiene la fecha indicada.
     * La semana comienza el lunes y termina el domingo.
     */
    fun rangeFor(epochDay: Long = BusinessDate.today()): ReportDateRange {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochDay * 86_400_000L
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        return when (this) {
            WEEK -> {
                val daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
                val monday = epochDay - daysSinceMonday
                ReportDateRange(monday, monday + 6)
            }

            MONTH -> ReportDateRange(
                fromEpochDay = BusinessDate.of(year, month, 1),
                toEpochDay = BusinessDate.of(
                    year,
                    month,
                    calendar.getActualMaximum(Calendar.DAY_OF_MONTH),
                ),
            )

            YEAR -> ReportDateRange(
                fromEpochDay = BusinessDate.of(year, 1, 1),
                toEpochDay = BusinessDate.of(year, 12, 31),
            )
        }
    }
}