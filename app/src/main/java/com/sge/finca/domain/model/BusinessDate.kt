package com.sge.finca.domain.model

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Fechas de negocio como "epoch day" (días desde 1970-01-01).
 *
 * Se usa Long en vez de java.time.LocalDate porque minSdk es 24 y el proyecto no tiene
 * desugaring: LocalDate.toEpochDay() existe a partir de API 26. Comparar y filtrar por rango
 * con un Long es directo en Room.
 */
object BusinessDate {

    private const val MILLIS_PER_DAY = 86_400_000L
    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    /** Día de hoy según el reloj y la zona horaria del celular. */
    fun today(): Long = fromMillis(System.currentTimeMillis())

    /** Convierte un instante al día calendario en la zona horaria dada (por defecto la del celular). */
    fun fromMillis(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        return of(
            year = local.get(Calendar.YEAR),
            month = local.get(Calendar.MONTH) + 1,
            day = local.get(Calendar.DAY_OF_MONTH),
        )
    }

    /** [month] va de 1 a 12. */
    fun of(year: Int, month: Int, day: Int): Long {
        require(month in 1..12) { "Mes inválido: $month" }
        val utc = Calendar.getInstance(UTC).apply {
            clear()
            set(year, month - 1, day)
        }
        return Math.floorDiv(utc.timeInMillis, MILLIS_PER_DAY)
    }

    /** Formato ISO "aaaa-mm-dd", útil para mostrar o depurar. */
    fun toIsoString(epochDay: Long): String {
        val utc = Calendar.getInstance(UTC).apply { timeInMillis = epochDay * MILLIS_PER_DAY }
        return String.format(
            Locale.ROOT,
            "%04d-%02d-%02d",
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH) + 1,
            utc.get(Calendar.DAY_OF_MONTH),
        )
    }
}
