package com.sge.finca.ui.format

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Fecha y hora de los recordatorios, en la zona horaria del celular.
 * A diferencia de las fechas de negocio (epoch day), aquí la hora importa.
 */
object ReminderTimeFormat {
    private val spanishColombia: Locale = Locale.forLanguageTag("es-CO")

    /** "lun 23 nov · 7:00 a. m." */
    fun format(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
        "${formatDate(millis, timeZone)} · ${formatTime(millis, timeZone)}"

    /** "lun 23 nov 2026" */
    fun formatDate(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("EEE d MMM yyyy", spanishColombia)
            .apply { this.timeZone = timeZone }
            .format(millis)
            .replace(".", "")

    /** "7:00 a. m." */
    fun formatTime(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("h:mm a", spanishColombia).apply { this.timeZone = timeZone }.format(millis)

    fun hourOf(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Int =
        Calendar.getInstance(timeZone).apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

    fun minuteOf(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Int =
        Calendar.getInstance(timeZone).apply { timeInMillis = millis }.get(Calendar.MINUTE)

    /**
     * Une el día que entrega el DatePicker (medianoche UTC) con la hora elegida, en la zona local.
     * Sin esto, en Colombia (UTC-5) el día saldría corrido.
     */
    fun combine(dateUtcMillis: Long, hour: Int, minute: Int, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val day = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateUtcMillis }
        return Calendar.getInstance(timeZone).apply {
            clear()
            set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH), hour, minute)
        }.timeInMillis
    }

    /** Medianoche UTC del día local de [millis], para inicializar el DatePicker. */
    fun toPickerDate(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    /** Próxima hora en punto con al menos 30 min de margen: valor por defecto de un recordatorio nuevo. */
    fun nextRoundHour(now: Long, timeZone: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(timeZone).apply {
            timeInMillis = now + 30 * 60_000L
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
