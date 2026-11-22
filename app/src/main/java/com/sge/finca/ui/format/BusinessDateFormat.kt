package com.sge.finca.ui.format

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Conversión y formato de fechas de negocio (epoch day, ver [com.sge.finca.domain.model.BusinessDate]).
 * El DatePicker de Material 3 trabaja con milisegundos UTC a medianoche, por eso las conversiones en UTC.
 */
object BusinessDateFormat {
    private const val MILLIS_PER_DAY = 86_400_000L
    private val spanishColombia: Locale = Locale.forLanguageTag("es-CO")

    fun toUtcMillis(epochDay: Long): Long = epochDay * MILLIS_PER_DAY

    fun fromUtcMillis(utcMillis: Long): Long = Math.floorDiv(utcMillis, MILLIS_PER_DAY)

    /** 20 de octubre de 2026 → "20 oct 2026". */
    fun format(epochDay: Long): String =
        SimpleDateFormat("d MMM yyyy", spanishColombia)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(toUtcMillis(epochDay))
            .replace(".", "")
}
